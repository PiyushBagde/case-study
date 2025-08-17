package com.supermarket.inventoryservice.controller;

import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.service.CategoryServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/invent")
@Validated
public class CategoryController {

    @Autowired
    private CategoryServiceImpl categoryServiceImpl;

    // routes for admin
    @PostMapping("/admin/addCategory")
    public String addCategory(@Valid @RequestBody Category category){
        Category addedCategory =  categoryServiceImpl.addCategory(category);
        return addedCategory.getCategoryName() +" Category added successfully";
    }

    @GetMapping("/admin/getAllCategory")
    public List<Category> getAllCategory() {
        return categoryServiceImpl.getAllCategories();
    }

    @GetMapping("/customer/getAllCategoryName")
    public List<String> getAllCategoryName() {
        return categoryServiceImpl.getAllCategoryName();
    }

    @DeleteMapping("/admin/deleteCategory/{categoryId}")
    public String deleteCategory(@PathVariable @Min(value = 1, message = "Category ID must be positive") int categoryId) {
        categoryServiceImpl.deleteCategory(categoryId);
        return "Category deleted successfully";
    }


    // routes for biller and customer
    @GetMapping("/getCategoryById/{category_id}")
    public Category getCategoryById(@PathVariable @Min(value = 1, message = "Category ID must be positive") int category_id) {
        return categoryServiceImpl.getCategoryById(category_id);
    }


    @GetMapping("/admin/getCategoryByName/{categoryName}")
    public Category getCategoryByName(@PathVariable @NotBlank(message = "Category name cannot be blank") String categoryName) {
        return categoryServiceImpl.getCategoryByName(categoryName);
    }

    @PutMapping("/admin/updateCategoryName/{id}")
    public Category updateCategoryName(
            @PathVariable @Min(value = 1, message = "Category ID must be positive") int id,
            @RequestParam @NotBlank(message = "New category name cannot be blank")
            @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
            String newCategoryName) {
        return categoryServiceImpl.updateCategoryName(id, newCategoryName);
    }

}