package com.example.bidmart.listing.service;

import com.example.bidmart.listing.model.Category;
import com.example.bidmart.listing.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new Category(categoryId, "Electronics", null);
    }

    @Test
    void getAllCategories_shouldReturnAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<Category> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals(categoryId, result.get(0).getId());
        verify(categoryRepository).findAll();
    }

    @Test
    void getRootCategories_shouldReturnCategoriesWithoutParent() {
        when(categoryRepository.findByParentIdIsNull()).thenReturn(List.of(category));

        List<Category> result = categoryService.getRootCategories();

        assertEquals(1, result.size());
        assertNull(result.get(0).getParentId());
        verify(categoryRepository).findByParentIdIsNull();
    }

    @Test
    void getChildCategories_shouldReturnCategoriesByParentId() {
        UUID parentId = UUID.randomUUID();
        Category child = new Category(UUID.randomUUID(), "Phones", parentId);

        when(categoryRepository.findByParentId(parentId)).thenReturn(List.of(child));

        List<Category> result = categoryService.getChildCategories(parentId);

        assertEquals(1, result.size());
        assertEquals(parentId, result.get(0).getParentId());
        verify(categoryRepository).findByParentId(parentId);
    }

    @Test
    void createCategory_shouldSaveRootCategory() {
        when(categoryRepository.save(category)).thenReturn(category);

        Category result = categoryService.createCategory(category);

        assertEquals(categoryId, result.getId());
        verify(categoryRepository, never()).existsById(any());
        verify(categoryRepository).save(category);
    }

    @Test
    void createCategory_shouldSaveChildCategory_whenParentExists() {
        UUID parentId = UUID.randomUUID();
        Category child = new Category(UUID.randomUUID(), "Phones", parentId);

        when(categoryRepository.existsById(parentId)).thenReturn(true);
        when(categoryRepository.save(child)).thenReturn(child);

        Category result = categoryService.createCategory(child);

        assertEquals(parentId, result.getParentId());
        verify(categoryRepository).existsById(parentId);
        verify(categoryRepository).save(child);
    }

    @Test
    void createCategory_shouldFail_whenParentDoesNotExist() {
        UUID parentId = UUID.randomUUID();
        Category child = new Category(UUID.randomUUID(), "Phones", parentId);

        when(categoryRepository.existsById(parentId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(child));
        verify(categoryRepository, never()).save(any());
    }
}