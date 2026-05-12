package com.example.bidmart.order.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void testOrderConstructorsGettersAndSetters() {
        Order emptyOrder = new Order();
        assertNotNull(emptyOrder);

        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100000");
        Order order = new Order(listingId, buyerId, sellerId, amount, "CREATED");

        assertEquals(listingId, order.getListingId());
        assertEquals(buyerId, order.getBuyerId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(amount, order.getAmount());
        assertEquals("CREATED", order.getStatus());

        UUID newId = UUID.randomUUID();
        UUID newListingId = UUID.randomUUID();
        UUID newBuyerId = UUID.randomUUID();
        UUID newSellerId = UUID.randomUUID();
        BigDecimal newAmount = new BigDecimal("250000");
        LocalDateTime time = LocalDateTime.now();

        order.setId(newId);
        order.setListingId(newListingId);
        order.setBuyerId(newBuyerId);
        order.setSellerId(newSellerId);
        order.setAmount(newAmount);
        order.setStatus("PAID");
        order.setTrackingNumber("RESI-12345");
        order.setDisputeReason("Barang cacat");
        order.setCreatedAt(time);

        assertEquals(newId, order.getId());
        assertEquals(newListingId, order.getListingId());
        assertEquals(newBuyerId, order.getBuyerId());
        assertEquals(newSellerId, order.getSellerId());
        assertEquals(newAmount, order.getAmount());
        assertEquals("PAID", order.getStatus());
        assertEquals("RESI-12345", order.getTrackingNumber());
        assertEquals("Barang cacat", order.getDisputeReason());
        assertEquals(time, order.getCreatedAt());
    }

    @Test
    void testOrderBuilder() {
        UUID id = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500000");
        LocalDateTime time = LocalDateTime.now();

        Order order = Order.builder()
                .id(id)
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status("COMPLETED")
                .trackingNumber("RESI-999")
                .disputeReason("Tidak sesuai deskripsi")
                .createdAt(time)
                .build();

        assertEquals(id, order.getId());
        assertEquals(listingId, order.getListingId());
        assertEquals(buyerId, order.getBuyerId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(amount, order.getAmount());
        assertEquals("COMPLETED", order.getStatus());
        assertEquals("RESI-999", order.getTrackingNumber());
        assertEquals("Tidak sesuai deskripsi", order.getDisputeReason());
    }

    @Test
    void testPrePersistOnCreate() {
        Order order = new Order();

        assertNull(order.getCreatedAt());

        order.onCreate();

        assertNotNull(order.getCreatedAt());
    }
}