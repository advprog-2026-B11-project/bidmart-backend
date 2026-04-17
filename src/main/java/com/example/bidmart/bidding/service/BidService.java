package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.bidding.model.Bid;
import com.example.bidmart.bidding.repository.BidRepository;
import com.example.bidmart.bidding.validator.BidRuleValidator;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.wallet.service.WalletService;
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

    public BidService(
            BidRepository bidRepository,
            ListingService listingService,
            WalletService walletService,
            BidRuleValidator bidRuleValidator
    ) {
        this.bidRepository = bidRepository;
        this.listingService = listingService;
        this.walletService = walletService;
        this.bidRuleValidator = bidRuleValidator;
    }

    @Transactional
    public BidResponse placeBid(UUID buyerId, CreateBidRequest request) {
        // Phase 1: validate raw input before any I/O
        bidRuleValidator.validateRequest(request, buyerId);

        ListingSnapshot listing = listingService.getListingById(request.listingId())
                .map(this::toSnapshot)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing tidak ditemukan: " + request.listingId()));

        Optional<Bid> currentHighestBid = bidRepository
                .findTopByListingIdOrderByAmountDescCreatedAtAsc(request.listingId());

        // Phase 2: validate business context (seller check, auction window, bid amount)
        bidRuleValidator.validateBidContext(buyerId, listing, request.amount(), currentHighestBid);

        boolean proxyBid = Boolean.TRUE.equals(request.proxyBid());
        BigDecimal reserveTarget = proxyBid ? request.proxyMaxLimit() : request.amount();

        Optional<Bid> latestBidByBuyer = bidRepository
                .findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(request.listingId(), buyerId);

        BigDecimal previousReservedAmount = latestBidByBuyer
                .map(Bid::getReservedAmount)
                .orElse(BigDecimal.ZERO);

        // Only lock the incremental amount not yet reserved for this listing.
        BigDecimal additionalReserve = reserveTarget.max(previousReservedAmount)
                .subtract(previousReservedAmount);

        if (additionalReserve.compareTo(BigDecimal.ZERO) > 0) {
            walletService.reserveBidFunds(buyerId, request.listingId(), additionalReserve);
        }

        Bid bid = new Bid();
        bid.setListingId(request.listingId());
        bid.setBuyerId(buyerId);
        bid.setAmount(request.amount());
        bid.setProxyBid(proxyBid);
        bid.setProxyMaxLimit(proxyBid ? request.proxyMaxLimit() : null);

        Bid savedBid = bidRepository.save(bid);
        releasePreviousHighestBidIfOutbid(currentHighestBid, savedBid);

        return BidResponse.from(savedBid);
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

    // --- private helpers ---------------------------------------------------

    private ListingSnapshot toSnapshot(Listing listing) {
        BigDecimal startingPrice = listing.getStartingPrice() == null
                ? BigDecimal.ZERO
                : listing.getStartingPrice();

        return new ListingSnapshot(
                listing.getId(),
                listing.getSellerId(),
                startingPrice,
                listing.getEndTime(),
                listing.getStatus()
        );
    }

    private void releasePreviousHighestBidIfOutbid(Optional<Bid> previousHighestBid, Bid latestSavedBid) {
        if (previousHighestBid.isEmpty()) {
            return;
        }

        Bid previous = previousHighestBid.get();

        if (previous.getBuyerId().equals(latestSavedBid.getBuyerId())) {
            return;
        }

        walletService.releaseBidFunds(
                previous.getBuyerId(),
                previous.getListingId(),
                previous.getReservedAmount()
        );
    }
}
