package com.supermarket.inventoryservice.service;

import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.model.Product;

import java.util.List;

public interface ProductService {
    Product addProduct(Product product);
    Product getProductById(int productId);
    List<Product> getAllProducts();
    List<Product> getProductsByCategoryId(int categoryId);
    void reduceStock(int prodId, int quantity);
    Product updateProduct(int prodId, Product updatedproduct);
    Category getCategoryByProduct(int prodId);
    Product updateQuantity(int productId, int newQuantity);
    void deleteProd(int prodId);
    Product getProductByProdName(String prodName);
    List<Product> getProductsByCategoryName(String categoryName);
}
