package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.model.Category;
import com.example.bidmart.listing.service.CategoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new Category(categoryId, "Electronics", null);
    }

    @Test
    void getAllCategories_shouldReturnOk() {
        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        ResponseEntity<List<Category>> response = categoryController.getAllCategories();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(categoryId, response.getBody().get(0).getId());
    }

    @Test
    void getRootCategories_shouldReturnOk() {
        when(categoryService.getRootCategories()).thenReturn(List.of(category));

        ResponseEntity<List<Category>> response = categoryController.getRootCategories();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertNull(response.getBody().get(0).getParentId());
    }

    @Test
    void getChildCategories_shouldReturnOk() {
        UUID parentId = UUID.randomUUID();
        Category child = new Category(UUID.randomUUID(), "Phones", parentId);

        when(categoryService.getChildCategories(parentId)).thenReturn(List.of(child));

        ResponseEntity<List<Category>> response = categoryController.getChildCategories(parentId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(parentId, response.getBody().get(0).getParentId());
    }

    @Test
    void createCategory_shouldReturnCreated() {
        when(categoryService.createCategory(category)).thenReturn(category);

        ResponseEntity<?> response = categoryController.createCategory(category);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(category, response.getBody());
    }

    @Test
    void createCategory_shouldReturnBadRequest_whenParentDoesNotExist() {
        when(categoryService.createCategory(category))
                .thenThrow(new IllegalArgumentException("Parent category tidak ditemukan."));

        ResponseEntity<?> response = categoryController.createCategory(category);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Parent category tidak ditemukan.", response.getBody());
    }
}