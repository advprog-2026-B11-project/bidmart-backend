package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.service.BidService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(@RequestBody CreateBidRequest request) {
        BidResponse response = bidService.placeBid(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/listing/{listingId}")
    public List<BidResponse> getBidsByListing(@PathVariable UUID listingId) {
        return bidService.getBidsByListing(listingId);
    }

    @GetMapping("/listing/{listingId}/highest")
    public BidResponse getHighestBid(@PathVariable UUID listingId) {
        return bidService.getHighestBid(listingId);
    }

    @GetMapping("/buyer/{buyerId}")
    public List<BidResponse> getBidsByBuyer(@PathVariable UUID buyerId) {
        return bidService.getBidsByBuyer(buyerId);
    }
}
