package com.example.bidmart.order.repository;

import com.example.bidmart.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId);

    Optional<Order> findByListingId(UUID listingId);
}