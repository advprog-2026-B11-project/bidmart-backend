package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidConflictException;
import com.example.bidmart.bidding.exception.BidTooLowException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ProxyChallengerOutbidException;
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
import com.example.bidmart.common.util.IdempotencyKeyGenerator;
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
    private final ProxyBiddingEngine proxyEngine;

    public BidService(
            BidRepository bidRepository,
            ListingService listingService,
            WalletService walletService,
            BidRuleValidator bidRuleValidator,
            ApplicationEventPublisher eventPublisher,
            AuctionStrategyRegistry strategyRegistry,
            ProxyBiddingEngine proxyEngine
    ) {
        this.bidRepository = bidRepository;
        this.listingService = listingService;
        this.walletService = walletService;
        this.bidRuleValidator = bidRuleValidator;
        this.eventPublisher = eventPublisher;
        this.strategyRegistry = strategyRegistry;
        this.proxyEngine = proxyEngine;
    }

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional(noRollbackFor = ProxyChallengerOutbidException.class)
    public BidResponse placeBid(UUID buyerId, CreateBidRequest request) {
        bidRuleValidator.validateRequest(request, buyerId);

        Listing listingEntity = listingService.getListingByIdWithLock(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing tidak ditemukan: " + request.listingId()));

        ListingSnapshot listing = toSnapshot(listingEntity);
        AuctionStrategy strategy = strategyRegistry.getStrategy(listingEntity.getAuctionType());

        Optional<Bid> currentHighestBid = bidRepository
                .findTopByListingIdOrderByAmountDescCreatedAtAsc(request.listingId());

        boolean isProxyRequest = Boolean.TRUE.equals(request.proxyBid());
        boolean isBuyerCurrentWinner = currentHighestBid
                .map(b -> b.getBuyerId().equals(buyerId))
                .orElse(false);
        boolean currentWinnerHasActiveProxy = currentHighestBid
                .map(b -> Boolean.TRUE.equals(b.getProxyBid()))
                .orElse(false);

        // Phase 1: structural + auction-open + seller check (amount skipped for highest bidder)
        bidRuleValidator.validateBidContext(buyerId, listing, request.amount(), currentHighestBid);

        // Phase 2: validate re-bid rules only when buyer is currently winning with a proxy
        if (isBuyerCurrentWinner && currentWinnerHasActiveProxy) {
            BigDecimal maxLimitForValidation = isProxyRequest ? request.proxyMaxLimit() : BigDecimal.ZERO;
            proxyEngine.validateAndGetActiveProxy(
                    buyerId, request.listingId(), isProxyRequest, request.amount(), maxLimitForValidation);
        }

        // Phase 3: standard strategy amount check — only for challengers (not current winner)
        if (!isBuyerCurrentWinner) {
            ValidationResult strategyResult = strategy.validateBid(request.amount(), listing);
            if (!strategyResult.valid()) {
                throw new BidTooLowException(strategyResult.errorMessage(),
                        strategy.computeMinimumNextBid(listing));
            }
        }

        // Phase 4: proxy conflict resolution — delegate entirely to ProxyBiddingEngine
        if (!isBuyerCurrentWinner && currentWinnerHasActiveProxy) {
            if (isProxyRequest) {
                return proxyEngine.handleProxyVsProxy(buyerId, request, listingEntity, currentHighestBid.get());
            }
            return proxyEngine.handleManualVsProxy(buyerId, request, listingEntity, currentHighestBid.get());
        }


        // Phase 5: reserve funds (wallet service calculates the incremental delta)
        BigDecimal reserveTarget = isProxyRequest ? request.proxyMaxLimit() : request.amount();
        Optional<Bid> latestBidByBuyer = bidRepository
                .findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(request.listingId(), buyerId);
        BigDecimal previousReservedAmount = latestBidByBuyer
                .map(Bid::getReservedAmount)
                .orElse(BigDecimal.ZERO);

        if (strategy.requiresFundHolding() && reserveTarget.compareTo(previousReservedAmount) > 0) {
            String holdKey = IdempotencyKeyGenerator.generate("BID_HOLD", buyerId, request.listingId(), reserveTarget);
            walletService.reserveBidFunds(buyerId, request.listingId(), reserveTarget, holdKey);
        }

        // Phase 6: persist bid and update listing
        Bid bid = buildBid(request.listingId(), buyerId, request.amount(), isProxyRequest,
                isProxyRequest ? request.proxyMaxLimit() : null);
        Bid savedBid = bidRepository.save(bid);
        listingEntity.updateHighestBid(savedBid.getBuyerId(), savedBid.getAmount());

        boolean extended = handleAntiSnipe(listingEntity);
        listingService.save(listingEntity);

        // Phase 7: release previous highest bidder's funds (skips same-user)
        releaseOutbidFunds(currentHighestBid, savedBid)
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

    
//      Returns bids for a listing with privacy masking: {@code proxyMaxLimit} is hidden
//      for bids not owned by {@code viewerId}.
     
    public List<BidResponse> getBidsByListing(UUID listingId, UUID viewerId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }

        return bidRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(bid -> BidResponse.from(bid, viewerId))
                .toList();
    }

    public BidResponse getHighestBid(UUID listingId, UUID viewerId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }

        Bid highestBid = bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Belum ada bid untuk listing: " + listingId));

        return BidResponse.from(highestBid, viewerId);
    }

    public List<BidResponse> getBidsByBuyer(UUID buyerId) {
        if (buyerId == null) {
            throw new BidValidationException("buyerId wajib diisi.");
        }

        return bidRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(BidResponse::from)
                .toList();
    }

    private boolean handleAntiSnipe(Listing listingEntity) {
        if (listingEntity.isWithinAntiSnipingWindow()) {
            listingEntity.extendAuction();
            return true;
        }
        return false;
    }

    private Bid buildBid(UUID listingId, UUID buyerId, BigDecimal amount,
                         boolean isProxy, BigDecimal proxyMaxLimit) {
        Bid bid = new Bid();
        bid.setListingId(listingId);
        bid.setBuyerId(buyerId);
        bid.setAmount(amount);
        bid.setProxyBid(isProxy);
        bid.setProxyMaxLimit(proxyMaxLimit);
        return bid;
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

    private Optional<Bid> releaseOutbidFunds(Optional<Bid> previousHighestBid, Bid newBid) {
        if (previousHighestBid.isEmpty()) {
            return Optional.empty();
        }

        Bid previous = previousHighestBid.get();

        if (previous.getBuyerId().equals(newBid.getBuyerId())) {
            return Optional.empty();
        }

        String releaseKey = IdempotencyKeyGenerator.generate("BID_RELEASE", previous.getBuyerId(), previous.getListingId(), previous.getReservedAmount());
        walletService.releaseBidFunds(
                previous.getBuyerId(),
                previous.getListingId(),
                previous.getReservedAmount(),
                releaseKey
        );

        return Optional.of(previous);
    }
}
