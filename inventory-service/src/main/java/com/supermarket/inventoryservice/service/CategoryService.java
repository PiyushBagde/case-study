package com.supermarket.inventoryservice.service;

import com.supermarket.inventoryservice.model.Category;

import java.util.List;

public interface CategoryService {
    Category addCategory(Category category);
    Category getCategoryById(int category_id);
    List<Category> getAllCategories();
    Category getCategoryByName(String categoryName);
    Category updateCategoryName(int category_id, String newCategoryName);
    void deleteCategory(int categoryId);
    List<String> getAllCategoryName();

}
