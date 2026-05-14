package com.example.bidmart.listing.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionStatusTest {

    @Test
    void isAcceptingBids_shouldReturnTrue_forExtended() {
        assertTrue(AuctionStatus.EXTENDED.isAcceptingBids());
    }

    @Test
    void isAcceptingBids_shouldReturnFalse_forClosed() {
        assertFalse(AuctionStatus.CLOSED.isAcceptingBids());
    }

    @Test
    void isFinal_shouldReturnTrue_forClosed() {
        assertTrue(AuctionStatus.CLOSED.isFinal());
    }

    @Test
    void isFinal_shouldReturnTrue_forUnsold() {
        assertTrue(AuctionStatus.UNSOLD.isFinal());
    }

    @Test
    void isFinal_shouldReturnFalse_forExtended() {
        assertFalse(AuctionStatus.EXTENDED.isFinal());
    }

    @Test
    void isActive_shouldReturnFalse_forWon() {
        assertFalse(AuctionStatus.WON.isActive());
    }

    @Test
    void isAcceptingBids_shouldReturnFalse_forDraft() {
        assertFalse(AuctionStatus.DRAFT.isAcceptingBids());
    }

    @Test
    void isFinal_shouldReturnFalse_forDraft() {
        assertFalse(AuctionStatus.DRAFT.isFinal());
    }

    @Test
    void isFinal_shouldReturnFalse_forActive() {
        assertFalse(AuctionStatus.ACTIVE.isFinal());
    }

    @Test
    void isAcceptingBids_shouldReturnFalse_forUnsold() {
        assertFalse(AuctionStatus.UNSOLD.isAcceptingBids());
    }
}
