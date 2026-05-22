package com.example.bidmart.config;

import com.example.bidmart.listing.model.Category;
import com.example.bidmart.listing.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> categories = List.of(
                    createCategory("Lukisan Abstrak"),
                    createCategory("Patung Modern"),
                    createCategory("Fotografi Alam"),
                    createCategory("Karya Kontemporer"),
                    createCategory("Seni Digital"),
                    createCategory("Ilustrasi Tradisional")
            );
            categoryRepository.saveAll(categories);
        }
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}
