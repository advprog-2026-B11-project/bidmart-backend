package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ProxyChallengerOutbidException;
import com.example.bidmart.bidding.model.Bid;
import com.example.bidmart.bidding.repository.BidRepository;
import com.example.bidmart.common.event.AuctionExtendedEvent;
import com.example.bidmart.common.event.BidPlacedEvent;
import com.example.bidmart.common.event.OutbidEvent;
import com.example.bidmart.common.event.ProxyAutoBidEvent;
import com.example.bidmart.common.util.IdempotencyKeyGenerator;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.wallet.service.WalletService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProxyBiddingEngine {

    static final BigDecimal MIN_INCREMENT = BigDecimal.ONE;

    private final BidRepository bidRepository;
    private final WalletService walletService;
    private final ListingService listingService;
    private final ApplicationEventPublisher eventPublisher;

    public ProxyBiddingEngine(BidRepository bidRepository,
                              WalletService walletService,
                              ListingService listingService,
                              ApplicationEventPublisher eventPublisher) {
        this.bidRepository = bidRepository;
        this.walletService = walletService;
        this.listingService = listingService;
        this.eventPublisher = eventPublisher;
    }

    public record ProxyResolution(boolean challengerWins, BigDecimal newVisiblePrice, Bid winningProxy) {}

    // Validates re-bid rules for a buyer's own active proxy, throwing if violated.
    // Proxy re-bid: new max must strictly exceed existing limit.</li>
    // Manual override: request amount must strictly exceed existing proxy limit.</li>
    public Optional<Bid> validateAndGetActiveProxy(
            UUID buyerId, UUID listingId,
            boolean isProxyRequest, BigDecimal requestAmount, BigDecimal newMaxLimit) {

        Optional<Bid> activeProxy = bidRepository
                .findTopByListingIdAndBuyerIdAndProxyBidTrueOrderByCreatedAtDesc(listingId, buyerId);

        if (activeProxy.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal existingMax = activeProxy.get().getProxyMaxLimit();

        if (isProxyRequest) {
            if (newMaxLimit.compareTo(existingMax) <= 0) {
                throw new BidValidationException(
                        "Proxy re-bid harus menaikkan maxAmount. MaxAmount aktif saat ini: " + existingMax);
            }
        } else {
            if (requestAmount.compareTo(existingMax) <= 0) {
                throw new BidValidationException(
                        "Manual bid harus melebihi proxy maxAmount Anda sendiri (" + existingMax + ")."
                        + " Gunakan proxy bid untuk menaikkan limit.");
            }
        }

        return activeProxy;
    }

    //  Determines the outcome of a manual bid against an active proxy.
    //  If challenger > proxy max → challenger wins. Otherwise → proxy auto-counters.
    public ProxyResolution resolveManualChallenger(BigDecimal challengerAmount, Bid winningProxy) {
        BigDecimal proxyMax = winningProxy.getProxyMaxLimit();

        if (challengerAmount.compareTo(proxyMax) > 0) {
            return new ProxyResolution(true, challengerAmount, null);
        }

        BigDecimal counterPrice = proxyMax.min(challengerAmount.add(MIN_INCREMENT));
        return new ProxyResolution(false, counterPrice, winningProxy);
    }

    // Determines the outcome of proxy-vs-proxy. Higher max wins; ties go to existing proxy.
    // Visible price is set to loser's max + 1, capped at winner's max.
    public ProxyResolution resolveProxyVsProxy(Bid existingProxy, BigDecimal newProxyMaxLimit) {
        BigDecimal existingMax = existingProxy.getProxyMaxLimit();

        if (newProxyMaxLimit.compareTo(existingMax) > 0) {
            BigDecimal newVisible = existingMax.add(MIN_INCREMENT).min(newProxyMaxLimit);
            return new ProxyResolution(true, newVisible, null);
        }

        BigDecimal newVisible = newProxyMaxLimit.add(MIN_INCREMENT).min(existingMax);
        return new ProxyResolution(false, newVisible, existingProxy);
    }

    // Handles a manual challenger bid against the current winning proxy.
    // Challenger > proxy max → challenger wins; proxy released.
    // Challenger ≤ proxy max → proxy auto-counters; throws {@link ProxyChallengerOutbidException}.
    public BidResponse handleManualVsProxy(UUID challengerBuyerId, CreateBidRequest request,
                                           Listing listingEntity, Bid winningProxy) {
        ProxyResolution resolution = resolveManualChallenger(request.amount(), winningProxy);

        if (resolution.challengerWins()) {
            String holdKey = IdempotencyKeyGenerator.generate("PROXY_HOLD", challengerBuyerId, request.listingId(), request.amount());
            walletService.reserveBidFunds(challengerBuyerId, request.listingId(), request.amount(), holdKey);
            String releaseKey = IdempotencyKeyGenerator.generate("PROXY_RELEASE", winningProxy.getBuyerId(), request.listingId(), winningProxy.getReservedAmount());
            walletService.releaseBidFunds(winningProxy.getBuyerId(), request.listingId(),
                    winningProxy.getReservedAmount(), releaseKey);

            Bid saved = bidRepository.save(buildBid(request.listingId(), challengerBuyerId,
                    request.amount(), false, null));
            listingEntity.updateHighestBid(saved.getBuyerId(), saved.getAmount());
            boolean extended = handleAntiSnipe(listingEntity);
            listingService.save(listingEntity);

            eventPublisher.publishEvent(new OutbidEvent(listingEntity.getId(),
                    winningProxy.getBuyerId(), saved.getAmount()));
            eventPublisher.publishEvent(new BidPlacedEvent(saved.getId(), saved.getListingId(),
                    saved.getBuyerId(), saved.getAmount()));
            if (extended) {
                eventPublisher.publishEvent(new AuctionExtendedEvent(
                        listingEntity.getId(), listingEntity.getSellerId()));
            }
            return BidResponse.from(saved);
        }

        BigDecimal counterPrice = resolution.newVisiblePrice();

        Bid challengerBid = bidRepository.save(buildBid(request.listingId(), challengerBuyerId,
                request.amount(), false, null));

        saveAutoCounterBid(winningProxy, counterPrice);
        listingEntity.updateHighestBid(winningProxy.getBuyerId(), counterPrice);
        boolean extended = handleAntiSnipe(listingEntity);
        listingService.save(listingEntity);

        eventPublisher.publishEvent(new BidPlacedEvent(challengerBid.getId(), challengerBid.getListingId(),
                challengerBid.getBuyerId(), challengerBid.getAmount()));
        eventPublisher.publishEvent(new ProxyAutoBidEvent(listingEntity.getId(),
                winningProxy.getBuyerId(), counterPrice));
        eventPublisher.publishEvent(new OutbidEvent(listingEntity.getId(),
                challengerBuyerId, counterPrice));
        if (extended) {
            eventPublisher.publishEvent(new AuctionExtendedEvent(
                    listingEntity.getId(), listingEntity.getSellerId()));
        }

        throw new ProxyChallengerOutbidException(
                "Bid Anda dikalahkan oleh proxy yang ada. Harga tertinggi saat ini: " + counterPrice,
                counterPrice);
    }

    // Handles a new proxy bid competing against an existing active proxy.
    // New proxy max > existing max → new proxy wins; existing released.
    // Otherwise → existing proxy auto-counters; throws {@link ProxyChallengerOutbidException}.

    public BidResponse handleProxyVsProxy(UUID newBuyerId, CreateBidRequest request,
                                          Listing listingEntity, Bid existingProxy) {
        ProxyResolution resolution = resolveProxyVsProxy(existingProxy, request.proxyMaxLimit());

        if (resolution.challengerWins()) {
            String holdKey = IdempotencyKeyGenerator.generate("PROXY_HOLD", newBuyerId, request.listingId(), request.proxyMaxLimit());
            walletService.reserveBidFunds(newBuyerId, request.listingId(), request.proxyMaxLimit(), holdKey);
            String releaseKey = IdempotencyKeyGenerator.generate("PROXY_RELEASE", existingProxy.getBuyerId(), request.listingId(), existingProxy.getReservedAmount());
            walletService.releaseBidFunds(existingProxy.getBuyerId(), request.listingId(),
                    existingProxy.getReservedAmount(), releaseKey);

            BigDecimal visiblePrice = resolution.newVisiblePrice();
            Bid saved = bidRepository.save(buildBid(request.listingId(), newBuyerId,
                    visiblePrice, true, request.proxyMaxLimit()));
            listingEntity.updateHighestBid(saved.getBuyerId(), saved.getAmount());
            boolean extended = handleAntiSnipe(listingEntity);
            listingService.save(listingEntity);

            eventPublisher.publishEvent(new ProxyAutoBidEvent(listingEntity.getId(),
                    newBuyerId, visiblePrice));
            eventPublisher.publishEvent(new OutbidEvent(listingEntity.getId(),
                    existingProxy.getBuyerId(), visiblePrice));
            eventPublisher.publishEvent(new BidPlacedEvent(saved.getId(), saved.getListingId(),
                    saved.getBuyerId(), saved.getAmount()));
            if (extended) {
                eventPublisher.publishEvent(new AuctionExtendedEvent(
                        listingEntity.getId(), listingEntity.getSellerId()));
            }
            return BidResponse.from(saved);
        }

        BigDecimal counterPrice = resolution.newVisiblePrice();

        // Record the losing proxy bid attempt for history — no fund reservation needed.
        Bid losingProxyBid = bidRepository.save(buildBid(request.listingId(), newBuyerId,
                request.amount(), true, request.proxyMaxLimit()));

        saveAutoCounterBid(existingProxy, counterPrice);
        listingEntity.updateHighestBid(existingProxy.getBuyerId(), counterPrice);
        boolean extended = handleAntiSnipe(listingEntity);
        listingService.save(listingEntity);

        eventPublisher.publishEvent(new BidPlacedEvent(losingProxyBid.getId(), losingProxyBid.getListingId(),
                losingProxyBid.getBuyerId(), losingProxyBid.getAmount()));
        eventPublisher.publishEvent(new ProxyAutoBidEvent(listingEntity.getId(),
                existingProxy.getBuyerId(), counterPrice));
        eventPublisher.publishEvent(new OutbidEvent(listingEntity.getId(),
                newBuyerId, counterPrice));
        if (extended) {
            eventPublisher.publishEvent(new AuctionExtendedEvent(
                    listingEntity.getId(), listingEntity.getSellerId()));
        }

        throw new ProxyChallengerOutbidException(
                "Proxy Anda dikalahkan oleh proxy yang ada. Harga tertinggi saat ini: " + counterPrice,
                counterPrice);
    }

    // auto-counter bid for the proxy holder at the resolved counter price, preserving the original {@code proxyMaxLimit}.
    public Bid saveAutoCounterBid(Bid existingProxy, BigDecimal counterPrice) {
        Bid autoBid = new Bid();
        autoBid.setListingId(existingProxy.getListingId());
        autoBid.setBuyerId(existingProxy.getBuyerId());
        autoBid.setAmount(counterPrice);
        autoBid.setProxyBid(Boolean.TRUE);
        autoBid.setProxyMaxLimit(existingProxy.getProxyMaxLimit());
        return bidRepository.save(autoBid);
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

    private boolean handleAntiSnipe(Listing listingEntity) {
        if (listingEntity.isWithinAntiSnipingWindow()) {
            listingEntity.extendAuction();
            return true;
        }
        return false;
    }
}
