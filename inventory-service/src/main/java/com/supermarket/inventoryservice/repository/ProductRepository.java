package com.supermarket.inventoryservice.repository;

import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(Category category);

    Optional<Product> findByProdName(String prodName);
}
