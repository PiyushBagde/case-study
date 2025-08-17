package com.supermarket.inventoryservice.controller;

import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.model.Product;
import com.supermarket.inventoryservice.service.ProductServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invent")
@Validated // Enables validation for path variables and request parameters
public class ProductController {

    @Autowired
    private ProductServiceImpl productServiceImpl;

    // Adds a new product based on the provided details (Admin only).
    @PostMapping("/admin/addProduct")
    public String addProduct(
            @Valid @RequestBody Product product // Validates the incoming Product object
    ) {
        Product addedProduct = productServiceImpl.addProduct(product);
        return addedProduct.getProdName() + " added successfully";
    }

    // Updates an existing product identified by prodId (Admin only).
    @PutMapping("/admin/updateProduct/{prodId}")
    public Product updateProd(
            @PathVariable @NotNull(message = "Product ID cannot be null") @Min(value=1, message="Product ID must be positive") int prodId, // Combined validation
            @Valid @RequestBody Product updatedproduct // Validates the incoming update data
    ) {
        return productServiceImpl.updateProduct(prodId, updatedproduct);
    }

    // Deletes a product by its ID (Admin only).
    @DeleteMapping("/admin/deleteProduct/{prodId}")
    public String deleteProd(
            @PathVariable @Min(value = 1, message = "Product ID must be positive") int prodId // Validates ID
    ) {
        productServiceImpl.deleteProd(prodId);
        return "Product deleted successfully";
    }

    // Retrieves a list of all products (Accessible by Admin, Biller, Customer).
    @GetMapping("/admin-biller-customer/getAllProducts")
    public List<Product> getAllProd() {
        return productServiceImpl.getAllProducts();
    }

    // Updates the stock quantity for a specific product (Admin only).
    @PutMapping("/admin/updateQuantity/{prodId}")
    public Product updateQty(
            @PathVariable @Min(value = 1, message = "Product ID must be positive") int prodId, // Validates ID
            @RequestParam @PositiveOrZero(message = "New quantity cannot be negative") int newQuantity // Validates quantity
    ) {
        return productServiceImpl.updateQuantity(prodId, newQuantity);
    }

    // Reduces the stock for a product, typically called after order processing (Biller/Customer triggered).
    @PutMapping("/reduceStock/{productId}/{quantity}")
    public void reduceStock(
            @PathVariable @Min(value = 1, message = "Product ID must be positive") int productId, // Validates ID
            @PathVariable @Positive(message = "Quantity to reduce must be positive") int quantity // Validates quantity
    ) {
        productServiceImpl.reduceStock(productId, quantity);
    }

    // Retrieves a specific product by its ID (Accessible by Biller, Customer).
    @GetMapping("/biller-customer/getProductById/{id}")
    public Product getProductById(
            @PathVariable @Min(value = 1, message = "Product ID must be positive") int id // Validates ID
    ) {
        return productServiceImpl.getProductById(id);
    }

    // Retrieves all products belonging to a specific category ID (Accessible by Biller, Customer).
    @GetMapping("/biller-customer/getProductsByCategory/{categoryId}")
    public List<Product> getProdByCategory(
            @PathVariable @Min(value = 1, message = "Category ID must be positive") int categoryId // Validates ID
    ) {
        return productServiceImpl.getProductsByCategoryId(categoryId);
    }

    // Retrieves all products belonging to a specific category name (Accessible by Customer).
    @GetMapping("/customer/getProductsByCategoryName")
    public List<Product> getProdByCategoryName(
            @RequestParam @NotBlank(message = "Category name cannot be blank") String categoryName // Validates name
    ) {
        return productServiceImpl.getProductsByCategoryName(categoryName);
    }

    // Retrieves the category associated with a specific product ID (Accessible by Biller).
    @GetMapping("/biller/getCategoryByProduct/{prodId}")
    public Category getCategoryByProduct(
            @PathVariable @Min(value = 1, message = "Product ID must be positive") int prodId // Validates ID
    ) {
        return productServiceImpl.getCategoryByProduct(prodId);
    }

    // Retrieves a specific product by name (for Biller lookup).
    @GetMapping("/biller/getProductByProdName")
    public Product findProductByNameForBiller(
            @RequestParam @NotBlank(message="Product name cannot be blank") String prodName // Validates name
    ) {
        return productServiceImpl.getProductByProdName(prodName); // Using general method based on provided code
    }

    // Retrieves a specific product by name (General access? Potentially duplicate of biller one).
    @GetMapping("/getProductByProdName")
    public Product getProductByProdName(
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName // Validates name
    ) {
        return productServiceImpl.getProductByProdName(prodName);
    }

}