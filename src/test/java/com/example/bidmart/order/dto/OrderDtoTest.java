package com.example.bidmart.order.dto;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderDtoTest {

    @Test
    void testCreateOrderRequest() {
        CreateOrderRequest request1 = new CreateOrderRequest();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);

        request1.setListingId(listingId);
        request1.setBuyerId(buyerId);
        request1.setSellerId(sellerId);
        request1.setAmount(amount);

        assertEquals(listingId, request1.getListingId());
        assertEquals(buyerId, request1.getBuyerId());
        assertEquals(sellerId, request1.getSellerId());
        assertEquals(amount, request1.getAmount());

        CreateOrderRequest request2 = new CreateOrderRequest();
        request2.setListingId(listingId);
        request2.setBuyerId(buyerId);
        request2.setSellerId(sellerId);
        request2.setAmount(amount);

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setAmount(BigDecimal.valueOf(200));
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        CreateOrderRequest reqNull = new CreateOrderRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new CreateOrderRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));
        
        CreateOrderRequest reqDiff = new CreateOrderRequest();
        reqDiff.setListingId(UUID.randomUUID());
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setListingId(listingId);
        reqDiff.setBuyerId(UUID.randomUUID());
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setBuyerId(buyerId);
        reqDiff.setSellerId(UUID.randomUUID());
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setSellerId(sellerId);
        reqDiff.setAmount(BigDecimal.valueOf(999));
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
    }

    @Test
    void testDisputeRequest() {
        DisputeRequest request1 = new DisputeRequest();
        request1.setReason("Broken");

        assertEquals("Broken", request1.getReason());

        DisputeRequest request2 = new DisputeRequest();
        request2.setReason("Broken");

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setReason("Other");
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        DisputeRequest reqNull = new DisputeRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new DisputeRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));
    }

    @Test
    void testResolveDisputeRequest() {
        ResolveDisputeRequest request1 = new ResolveDisputeRequest();
        request1.setRefundBuyer(true);

        assertTrue(request1.getRefundBuyer());

        ResolveDisputeRequest request2 = new ResolveDisputeRequest();
        request2.setRefundBuyer(true);

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setRefundBuyer(false);
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        ResolveDisputeRequest reqNull = new ResolveDisputeRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new ResolveDisputeRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));
    }

    @Test
    void testUpdateOrderStatusRequest() {
        UpdateOrderStatusRequest request1 = new UpdateOrderStatusRequest();
        request1.setStatus("SHIPPED");

        assertEquals("SHIPPED", request1.getStatus());

        UpdateOrderStatusRequest request2 = new UpdateOrderStatusRequest();
        request2.setStatus("SHIPPED");

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setStatus("DELIVERED");
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        UpdateOrderStatusRequest reqNull = new UpdateOrderStatusRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new UpdateOrderStatusRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));
    }

    @Test
    void testUpdateTrackingRequest() {
        UpdateTrackingRequest request1 = new UpdateTrackingRequest();
        request1.setTrackingNumber("RESI123");

        assertEquals("RESI123", request1.getTrackingNumber());

        UpdateTrackingRequest request2 = new UpdateTrackingRequest();
        request2.setTrackingNumber("RESI123");

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setTrackingNumber("RESI456");
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        UpdateTrackingRequest reqNull = new UpdateTrackingRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new UpdateTrackingRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));
    }

    @Test
    void testOrderModel() {
        UUID id = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);
        LocalDateTime now = LocalDateTime.now();

        Order order1 = Order.builder()
                .id(id)
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status(OrderStatus.CREATED)
                .trackingNumber("RESI123")
                .disputeReason("Broken")
                .createdAt(now)
                .build();

        assertEquals(id, order1.getId());
        assertEquals(listingId, order1.getListingId());
        assertEquals(buyerId, order1.getBuyerId());
        assertEquals(sellerId, order1.getSellerId());
        assertEquals(amount, order1.getAmount());
        assertEquals(OrderStatus.CREATED, order1.getStatus());
        assertEquals("RESI123", order1.getTrackingNumber());
        assertEquals("Broken", order1.getDisputeReason());
        assertEquals(now, order1.getCreatedAt());

        Order order2 = Order.builder()
                .id(id)
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status(OrderStatus.CREATED)
                .trackingNumber("RESI123")
                .disputeReason("Broken")
                .createdAt(now)
                .build();

        Order order3 = new Order();
        order3.setId(id);
        order3.setListingId(listingId);
        order3.setBuyerId(buyerId);
        order3.setSellerId(sellerId);
        order3.setAmount(amount);
        order3.setStatus(OrderStatus.CREATED);
        order3.setTrackingNumber("RESI123");
        assertNotNull(order1.toString());
        assertNotNull(Order.builder().toString());
    }
}
