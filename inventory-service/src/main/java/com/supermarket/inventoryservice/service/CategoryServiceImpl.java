package com.supermarket.inventoryservice.service;

import com.supermarket.inventoryservice.exception.OperationFailedException;
import com.supermarket.inventoryservice.exception.ResourceAlreadyExistsException;
import com.supermarket.inventoryservice.exception.ResourceNotFoundException;
import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
// Assuming this implements a CategoryService interface
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Adds a new category after validating name and checking for existence.
    @Override
    @Transactional
    public Category addCategory(Category category) {
        if (category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        if (categoryRepository.findByCategoryName(category.getCategoryName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Category with name '" + category.getCategoryName() + "' already exists.");
        }
        try {
            return categoryRepository.save(category);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to add category: " + category.getCategoryName()); // Added exception chaining
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while adding category: " + category.getCategoryName()); // Added exception chaining
        }
    }

    // Retrieves a specific category by its ID.
    @Override
    public Category getCategoryById(int category_id) {
        return categoryRepository.findById(category_id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with Id: " + category_id));
    }

    // Retrieves a list of all categories.
    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        // if list empty comment removed as requested
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("No categories found.");
        }
        return categories;
    }

    // Retrieves a specific category by its name.
    @Override
    public Category getCategoryByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + categoryName));
    }

    // Updates the name of an existing category after validating input and checking for conflicts.
    @Override
    public Category updateCategoryName(int category_id, String newCategoryName) {
        if (newCategoryName == null || newCategoryName.isBlank()) {
            throw new IllegalArgumentException("New category name cannot be empty.");
        }
        Category existingCategory = categoryRepository.findById(category_id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot update. Category not found with Id: " + category_id));

        categoryRepository.findByCategoryName(newCategoryName).ifPresent(conflictCategory -> {
            // Ensure the conflict is not with the category itself
            if (conflictCategory.getCategoryId() != category_id) {
                throw new OperationFailedException("Cannot update category. Name '" + newCategoryName + "' is already used by another category (ID: " + conflictCategory.getCategoryId() + ")");
            }
        });

        existingCategory.setCategoryName(newCategoryName);
        try {
            return categoryRepository.save(existingCategory);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to update category with ID: " + category_id); // Added exception chaining
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while updating category with ID: " + category_id); // Added exception chaining
        }
    }

    // Deletes a category by its ID after checking for existence.
    @Override
    @Transactional
    public void deleteCategory(int categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Cannot delete. Category not found with Id: " + categoryId);
        }
        try {
            categoryRepository.deleteById(categoryId);
        } catch (DataIntegrityViolationException e) { // Catch constraint violation specifically
            throw new OperationFailedException("Cannot delete category with ID: " + categoryId + ". It might be associated with existing products."); // Added exception chaining
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to delete category with ID: " + categoryId); // Added exception chaining
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while deleting category with ID: " + categoryId); // Added exception chaining
        }
    }

    // Retrieves a sorted list of all category names.
    @Override
    public List<String> getAllCategoryName() {
        List<Category> categoryList = categoryRepository.findAll();
        if (categoryList.isEmpty()) {
            return List.of(); // Return empty list explicitly
        }
        return categoryList.stream()
                .map(Category::getCategoryName)
                .sorted()
                .collect(Collectors.toList());
    }
}
