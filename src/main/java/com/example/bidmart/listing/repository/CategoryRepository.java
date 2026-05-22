package com.example.bidmart.listing.repository;

import com.example.bidmart.listing.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentId(UUID parentId);
    List<Category> findByParentIdIsNull();
}