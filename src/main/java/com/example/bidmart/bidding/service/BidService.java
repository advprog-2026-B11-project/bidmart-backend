package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidConflictException;
import com.example.bidmart.bidding.exception.BidTooLowException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.bidding.model.Bid;
import com.example.bidmart.bidding.repository.BidRepository;
import com.example.bidmart.bidding.strategy.AuctionStrategy;
import com.example.bidmart.bidding.strategy.AuctionStrategyRegistry;
import com.example.bidmart.bidding.strategy.ValidationResult;
import com.example.bidmart.bidding.validator.BidRuleValidator;
import com.example.bidmart.common.event.AuctionExtendedEvent;
import com.example.bidmart.common.event.BidPlacedEvent;
import com.example.bidmart.common.event.OutbidEvent;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.wallet.service.WalletService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final ListingService listingService;
    private final WalletService walletService;
    private final BidRuleValidator bidRuleValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final AuctionStrategyRegistry strategyRegistry;

    public BidService(
            BidRepository bidRepository,
            ListingService listingService,
            WalletService walletService,
            BidRuleValidator bidRuleValidator,
            ApplicationEventPublisher eventPublisher,
            AuctionStrategyRegistry strategyRegistry
    ) {
        this.bidRepository = bidRepository;
        this.listingService = listingService;
        this.walletService = walletService;
        this.bidRuleValidator = bidRuleValidator;
        this.eventPublisher = eventPublisher;
        this.strategyRegistry = strategyRegistry;
    }

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public BidResponse placeBid(UUID buyerId, CreateBidRequest request) {
        bidRuleValidator.validateRequest(request, buyerId);

        Listing listingEntity = listingService.getListingByIdWithLock(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing tidak ditemukan: " + request.listingId()));

        ListingSnapshot listing = toSnapshot(listingEntity);
        AuctionStrategy strategy = strategyRegistry.getStrategy(listingEntity.getAuctionType());

        Optional<Bid> currentHighestBid = bidRepository
                .findTopByListingIdOrderByAmountDescCreatedAtAsc(request.listingId());

        bidRuleValidator.validateBidContext(buyerId, listing, request.amount(), currentHighestBid);

        ValidationResult strategyResult = strategy.validateBid(request.amount(), listing);
        if (!strategyResult.valid()) {
            throw new BidTooLowException(strategyResult.errorMessage(),
                    strategy.computeMinimumNextBid(listing));
        }

        boolean proxyBid = Boolean.TRUE.equals(request.proxyBid());
        BigDecimal reserveTarget = proxyBid ? request.proxyMaxLimit() : request.amount();

        Optional<Bid> latestBidByBuyer = bidRepository
                .findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(request.listingId(), buyerId);

        BigDecimal previousReservedAmount = latestBidByBuyer
                .map(Bid::getReservedAmount)
                .orElse(BigDecimal.ZERO);

        BigDecimal additionalReserve = reserveTarget.max(previousReservedAmount)
                .subtract(previousReservedAmount);

        if (strategy.requiresFundHolding() && additionalReserve.compareTo(BigDecimal.ZERO) > 0) {
            walletService.reserveBidFunds(buyerId, request.listingId(), additionalReserve);
        }

        Bid bid = new Bid();
        bid.setListingId(request.listingId());
        bid.setBuyerId(buyerId);
        bid.setAmount(request.amount());
        bid.setProxyBid(proxyBid);
        bid.setProxyMaxLimit(proxyBid ? request.proxyMaxLimit() : null);

        Bid savedBid = bidRepository.save(bid);

        listingEntity.updateHighestBid(savedBid.getBuyerId(), savedBid.getAmount());

        boolean extended = false;
        if (listingEntity.isWithinAntiSnipingWindow()) {
            listingEntity.extendAuction();
            extended = true;
        }

        // Resolve proxy counter-bids before persisting the listing so the listing
        // is saved exactly once with the final highest-bid state.
        BidResponse finalResult = resolveProxies(savedBid, listingEntity);

        listingService.save(listingEntity);

        // Only release the previous highest bidder's funds here if they held a normal
        // (non-proxy) bid.  Proxy bids are fully managed inside resolveProxies to
        // avoid double-releases.
        releaseOutbidFunds(currentHighestBid, savedBid, finalResult.buyerId())
                .ifPresent(outbid -> eventPublisher.publishEvent(
                        new OutbidEvent(savedBid.getListingId(), outbid.getBuyerId(), savedBid.getAmount())));

        eventPublisher.publishEvent(new BidPlacedEvent(
            savedBid.getId(),
            savedBid.getListingId(),
            savedBid.getBuyerId(),
            savedBid.getAmount()
        ));

        if (extended) {
            eventPublisher.publishEvent(new AuctionExtendedEvent(
                listingEntity.getId(), listingEntity.getSellerId()));
        }

        return BidResponse.from(savedBid);
    }

    @Recover
    public BidResponse recoverFromConflict(ObjectOptimisticLockingFailureException ex,
                                           UUID buyerId, CreateBidRequest request) {
        throw new BidConflictException("Terjadi konflik penawaran, silakan coba lagi dalam beberapa saat.");
    }

    public BigDecimal getMinimumNextBid(UUID listingId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }

        Listing listingEntity = listingService.getListingById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing tidak ditemukan: " + listingId));

        AuctionStrategy strategy = strategyRegistry.getStrategy(listingEntity.getAuctionType());
        return strategy.computeMinimumNextBid(toSnapshot(listingEntity));
    }

    public List<BidResponse> getBidsByListing(UUID listingId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }

        return bidRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(BidResponse::from)
                .toList();
    }

    public BidResponse getHighestBid(UUID listingId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }

        Bid highestBid = bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Belum ada bid untuk listing: " + listingId));

        return BidResponse.from(highestBid);
    }

    public List<BidResponse> getBidsByBuyer(UUID buyerId) {
        if (buyerId == null) {
            throw new BidValidationException("buyerId wajib diisi.");
        }

        return bidRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(BidResponse::from)
                .toList();
    }

    private ListingSnapshot toSnapshot(Listing listing) {
        BigDecimal startingPrice = listing.getStartingPrice() == null
                ? BigDecimal.ZERO
                : listing.getStartingPrice();

        return new ListingSnapshot(
                listing.getId(),
                listing.getSellerId(),
                startingPrice,
                listing.getEndTime(),
                listing.getStatus(),
                listing.getCurrentHighestBid(),
                listing.getCurrentHighestBidderId()
        );
    }

    /**
     * Iteratively resolves proxy counter-bids after a new bid lands.
     * Updates listing's highest-bid state in memory; caller persists the listing.
     *
     * Fund-release contract:
     *  - Normal bid outbid       → released immediately (no proxy to counter back).
     *  - Proxy bid outbid        → NOT released mid-loop; only released when exhausted.
     *  - Rival proxy exhausted   → released at the break point.
     *  - Winner's held funds     → kept (auction-settlement releases any surplus).
     */
    private BidResponse resolveProxies(Bid incomingBid, Listing listingEntity) {
        Bid currentWinner = incomingBid;

        while (true) {
            Optional<Bid> rivalOpt = bidRepository
                    .findTopRivalProxyBid(listingEntity.getId(), currentWinner.getBuyerId());

            if (rivalOpt.isEmpty()) {
                break;
            }

            Bid rival = rivalOpt.get();
            BigDecimal counterAmount = currentWinner.getAmount().add(BigDecimal.ONE);

            if (rival.getProxyMaxLimit().compareTo(counterAmount) < 0) {
                // Rival proxy exhausted — release their held funds and stop.
                walletService.releaseBidFunds(
                        rival.getBuyerId(), rival.getListingId(), rival.getProxyMaxLimit());
                eventPublisher.publishEvent(new OutbidEvent(
                        listingEntity.getId(), rival.getBuyerId(), currentWinner.getAmount()));
                break;
            }

            // Rival proxy fires a counter-bid.
            Bid counter = new Bid();
            counter.setListingId(rival.getListingId());
            counter.setBuyerId(rival.getBuyerId());
            counter.setAmount(counterAmount);
            counter.setProxyBid(true);
            counter.setProxyMaxLimit(rival.getProxyMaxLimit());
            Bid savedCounter = bidRepository.save(counter);

            listingEntity.updateHighestBid(savedCounter.getBuyerId(), savedCounter.getAmount());

            // Release currentWinner's funds only if they have no proxy to fight back with.
            // Proxy holders keep their funds until they are truly eliminated.
            if (!Boolean.TRUE.equals(currentWinner.getProxyBid())) {
                walletService.releaseBidFunds(
                        currentWinner.getBuyerId(),
                        currentWinner.getListingId(),
                        currentWinner.getAmount());
                eventPublisher.publishEvent(new OutbidEvent(
                        listingEntity.getId(), currentWinner.getBuyerId(), savedCounter.getAmount()));
            }

            eventPublisher.publishEvent(new BidPlacedEvent(
                    savedCounter.getId(), listingEntity.getId(),
                    savedCounter.getBuyerId(), savedCounter.getAmount()));

            currentWinner = savedCounter;
        }

        return BidResponse.from(currentWinner);
    }

    private Optional<Bid> releaseOutbidFunds(Optional<Bid> previousHighestBid,
                                              Bid newBid,
                                              UUID finalWinnerBuyerId) {
        if (previousHighestBid.isEmpty()) {
            return Optional.empty();
        }

        Bid previous = previousHighestBid.get();

        if (previous.getBuyerId().equals(newBid.getBuyerId())) {
            return Optional.empty();
        }

        if (Boolean.TRUE.equals(previous.getProxyBid())) {
            return Optional.empty();
        }

        if (previous.getBuyerId().equals(finalWinnerBuyerId)) {
            return Optional.empty();
        }

        walletService.releaseBidFunds(
                previous.getBuyerId(),
                previous.getListingId(),
                previous.getReservedAmount()
        );

        return Optional.of(previous);
    }
}
