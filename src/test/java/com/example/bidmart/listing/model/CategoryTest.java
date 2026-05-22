package com.example.bidmart.listing.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void category_shouldSetAndGetFields() {
        Category category = new Category();
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        category.setId(id);
        category.setName("Electronics");
        category.setParentId(parentId);

        assertEquals(id, category.getId());
        assertEquals("Electronics", category.getName());
        assertEquals(parentId, category.getParentId());
    }

    @Test
    void noArgsConstructor_shouldCreateCategory() {
        Category category = new Category();

        assertNotNull(category);
    }

    @Test
    void allArgsConstructor_shouldSetFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Category category = new Category(id, "Fashion", parentId);

        assertEquals(id, category.getId());
        assertEquals("Fashion", category.getName());
        assertEquals(parentId, category.getParentId());
    }
}