package com.example.bidmart.listing.service;

import com.example.bidmart.listing.model.Category;
import com.example.bidmart.listing.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIdIsNull();
    }

    public List<Category> getChildCategories(UUID parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Category createCategory(Category category) {
        if (category.getParentId() != null && !categoryRepository.existsById(category.getParentId())) {
            throw new IllegalArgumentException("Parent category tidak ditemukan.");
        }
        return categoryRepository.save(category);
    }
}