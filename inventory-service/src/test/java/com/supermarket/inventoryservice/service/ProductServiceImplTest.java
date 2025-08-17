package com.supermarket.inventoryservice.service;

import com.supermarket.inventoryservice.exception.InsufficientStockException;
import com.supermarket.inventoryservice.exception.OperationFailedException;
import com.supermarket.inventoryservice.exception.ResourceAlreadyExistsException;
import com.supermarket.inventoryservice.exception.ResourceNotFoundException;
import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.model.Product;
import com.supermarket.inventoryservice.repository.CategoryRepository;
import com.supermarket.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productServiceImpl;

    private Category sampleCategory;
    private Product sampleProduct;
    private Product sampleProduct2;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category(1, "Electronics", null);
        sampleProduct = new Product(101, "Laptop", 1200.00, 50, sampleCategory);
        sampleProduct2 = new Product(102, "Keyboard", 75.00, 100, sampleCategory);
    }

    // --- addProduct Tests ---
    @Test
    @DisplayName("AddProduct: Success when valid and not exists")
    void addProduct_WhenValidAndNotExists_ShouldSaveAndReturnProduct() {
        // Arrange
        Category inputCategory = new Category(0, "Electronics", null); // Category name is what matters
        Product inputProduct = new Product(0, "New Laptop", 1300.0, 30, inputCategory);
        Product savedProduct = new Product(103, "New Laptop", 1300.0, 30, sampleCategory); // ID assigned, category obj linked

        when(categoryRepository.findByCategoryName(inputCategory.getCategoryName())).thenReturn(Optional.of(sampleCategory)); // Found the real category
        when(productRepository.findByProdName(inputProduct.getProdName())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProdId(103); // Simulate ID generation
            return p;
        });

        // Act
        Product result = productServiceImpl.addProduct(inputProduct);

        // Assert
        assertNotNull(result);
        assertEquals(savedProduct.getProdName(), result.getProdName());
        assertEquals(savedProduct.getProdId(), result.getProdId());
        assertEquals(sampleCategory.getCategoryId(), result.getCategory().getCategoryId()); // Check correct category linked

        verify(categoryRepository).findByCategoryName(inputCategory.getCategoryName());
        verify(productRepository).findByProdName(inputProduct.getProdName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("AddProduct: Throws ResourceNotFoundException if category not found")
    void addProduct_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Category inputCategory = new Category(0, "NonExistent", null);
        Product inputProduct = new Product(0, "Gadget", 50.0, 10, inputCategory);
        when(categoryRepository.findByCategoryName(inputCategory.getCategoryName())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.addProduct(inputProduct);
        });
        verify(categoryRepository).findByCategoryName(inputCategory.getCategoryName());
        verify(productRepository, never()).findByProdName(anyString());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("AddProduct: Throws ResourceAlreadyExistsException if product name exists")
    void addProduct_WhenProductNameExists_ShouldThrowResourceAlreadyExistsException() {
        // Arrange
        Category inputCategory = new Category(0, "Electronics", null);
        Product inputProduct = new Product(0, "Laptop", 1300.0, 30, inputCategory); // Name matches sampleProduct
        when(categoryRepository.findByCategoryName(inputCategory.getCategoryName())).thenReturn(Optional.of(sampleCategory));
        when(productRepository.findByProdName(inputProduct.getProdName())).thenReturn(Optional.of(sampleProduct)); // Name exists

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            productServiceImpl.addProduct(inputProduct);
        });

        verify(categoryRepository).findByCategoryName(inputCategory.getCategoryName());
        verify(productRepository).findByProdName(inputProduct.getProdName());
        verify(productRepository, never()).save(any(Product.class));
    }

    // --- getProductById Tests ---
    @Test
    @DisplayName("GetProductById: Returns product when found")
    void getProductById_WhenFound_ShouldReturnProduct() {
        // Arrange
        int productId = sampleProduct.getProdId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));

        // Act
        Product result = productServiceImpl.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertEquals(productId, result.getProdId());
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("GetProductById: Throws ResourceNotFoundException when not found")
    void getProductById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int productId = 999;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getProductById(productId);
        });
        verify(productRepository).findById(productId);
    }

    // --- getAllProducts Tests ---
    @Test
    @DisplayName("GetAllProducts: Returns list when products exist")
    void getAllProducts_WhenExist_ShouldReturnList() {
        // Arrange
        List<Product> products = List.of(sampleProduct, sampleProduct2);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<Product> result = productServiceImpl.getAllProducts();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("GetAllProducts: Throws ResourceNotFoundException when none exist")
    void getAllProducts_WhenNoneExist_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getAllProducts();
        });
        verify(productRepository).findAll();
    }

    // --- getProductsByCategoryId Tests ---
    @Test
    @DisplayName("GetProductsByCategoryId: Returns list when category and products found")
    void getProductsByCategoryId_WhenCategoryAndProductsFound_ShouldReturnList() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        List<Product> productsInCategory = List.of(sampleProduct, sampleProduct2);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.findByCategory(sampleCategory)).thenReturn(productsInCategory);

        // Act
        List<Product> result = productServiceImpl.getProductsByCategoryId(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(sampleProduct.getProdId(), result.get(0).getProdId());
        verify(categoryRepository).findById(categoryId);
        verify(productRepository).findByCategory(sampleCategory);
    }

    @Test
    @DisplayName("GetProductsByCategoryId: Throws ResourceNotFoundException when category found but no products")
    void getProductsByCategoryId_WhenCategoryFoundButNoProducts_ShouldThrowResourceNotFoundException() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.findByCategory(sampleCategory)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getProductsByCategoryId(categoryId);
        });
        verify(categoryRepository).findById(categoryId);
        verify(productRepository).findByCategory(sampleCategory);
    }

    @Test
    @DisplayName("GetProductsByCategoryId: Throws ResourceNotFoundException when category not found")
    void getProductsByCategoryId_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int categoryId = 999;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getProductsByCategoryId(categoryId);
        });
        verify(categoryRepository).findById(categoryId);
        verify(productRepository, never()).findByCategory(any(Category.class));
    }


    // --- reduceStock Tests ---
    @Test
    @DisplayName("ReduceStock: Success reduces stock")
    void reduceStock_WhenSufficientStock_ShouldReduceAndSave() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        int initialStock = sampleProduct.getStock(); // 50
        int quantityToReduce = 10;
        int expectedFinalStock = initialStock - quantityToReduce; // 40

        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        assertDoesNotThrow(() -> productServiceImpl.reduceStock(prodId, quantityToReduce));

        // Assert
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).findById(prodId);
        verify(productRepository).save(productCaptor.capture());
        assertEquals(expectedFinalStock, productCaptor.getValue().getStock());
    }

    @Test
    @DisplayName("ReduceStock: Throws IllegalArgumentException for non-positive quantity")
    void reduceStock_WhenQuantityNotPositive_ShouldThrowIllegalArgumentException() {
        // Arrange
        int prodId = sampleProduct.getProdId();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            productServiceImpl.reduceStock(prodId, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            productServiceImpl.reduceStock(prodId, -5);
        });
        verify(productRepository, never()).findById(anyInt());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("ReduceStock: Throws ResourceNotFoundException if product not found")
    void reduceStock_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int prodId = 999;
        when(productRepository.findById(prodId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.reduceStock(prodId, 10);
        });
        verify(productRepository).findById(prodId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("ReduceStock: Throws InsufficientStockException if stock too low")
    void reduceStock_WhenStockInsufficient_ShouldThrowInsufficientStockException() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        int quantityToReduce = sampleProduct.getStock() + 1; // 51
        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> {
            productServiceImpl.reduceStock(prodId, quantityToReduce);
        });
        verify(productRepository).findById(prodId);
        verify(productRepository, never()).save(any(Product.class));
    }

    // --- updateProduct Tests ---
    // (Add tests for updateProduct success, product not found, invalid inputs, category not found, save errors)
    @Test
    @DisplayName("UpdateProduct: Success updates fields")
    void updateProduct_WhenValid_ShouldUpdateAndReturnProduct() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        Category newCategory = new Category(2, "Peripherals", null);
        Product updateRequest = new Product(0, "Updated Laptop", 1400.0, 25, new Category(0, "Peripherals", null)); // Request only needs category name

        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findByCategoryName("Peripherals")).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Product result = productServiceImpl.updateProduct(prodId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Laptop", result.getProdName());
        assertEquals(1400.0, result.getPrice());
        assertEquals(25, result.getStock());
        assertEquals(newCategory.getCategoryId(), result.getCategory().getCategoryId());
        assertEquals(newCategory.getCategoryName(), result.getCategory().getCategoryName());
        verify(productRepository).findById(prodId);
        verify(categoryRepository).findByCategoryName("Peripherals");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateProduct: Throws ResourceNotFoundException if product does not exist")
    void updateProduct_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int prodId = 999;
        Product updateRequest = new Product(0, "Some Product", 10.0, 5, new Category(0, "Test", null));
        when(productRepository.findById(prodId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.updateProduct(prodId, updateRequest);
        });
        verify(productRepository).findById(prodId);
        verify(categoryRepository, never()).findByCategoryName(anyString());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateProduct: Throws IllegalArgumentException for invalid price/stock/name")
    void updateProduct_WhenInvalidInput_ShouldThrowIllegalArgumentException() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        Product invalidPrice = new Product(0, "Valid Name", -10.0, 10, new Category(0, "Test", null));
        Product invalidStock = new Product(0, "Valid Name", 10.0, -10, new Category(0, "Test", null));
        Product invalidName = new Product(0, null, 10.0, 10, new Category(0, "Test", null));

        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct)); // Need product to exist

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> productServiceImpl.updateProduct(prodId, invalidPrice));
        assertThrows(IllegalArgumentException.class, () -> productServiceImpl.updateProduct(prodId, invalidStock));
        assertThrows(IllegalArgumentException.class, () -> productServiceImpl.updateProduct(prodId, invalidName));

        verify(productRepository, times(3)).findById(prodId); // Called 3 times
        verify(productRepository, never()).save(any(Product.class));
    }


    // --- getCategoryByProduct Tests ---
    @Test
    @DisplayName("GetCategoryByProduct: Success returns category by product ID")
    void getCategoryByProduct_WhenProductFound_ShouldReturnCategory() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct));

        // Act
        Category result = productServiceImpl.getCategoryByProduct(prodId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleCategory.getCategoryId(), result.getCategoryId());
        assertEquals(sampleCategory.getCategoryName(), result.getCategoryName());
        verify(productRepository).findById(prodId);
    }

    @Test
    @DisplayName("GetCategoryByProduct: Throws ResourceNotFoundException when product not found")
    void getCategoryByProduct_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int prodId = 999;
        when(productRepository.findById(prodId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getCategoryByProduct(prodId);
        });
        verify(productRepository).findById(prodId);
    }

    @Test
    @DisplayName("GetCategoryByProduct: Throws ResourceNotFoundException when product has no category")
    void getCategoryByProduct_WhenProductHasNoCategory_ShouldThrowResourceNotFoundException() {
        // Arrange
        int prodId = sampleProduct.getProdId();
        sampleProduct.setCategory(null);
        when(productRepository.findById(prodId)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productServiceImpl.getCategoryByProduct(prodId);
        });
        verify(productRepository).findById(prodId);
    }


    // --- updateQuantity Tests ---
    @Test
    @DisplayName("UpdateQuantity: Success updates stock quantity")
    void updateQuantity_WhenValid_ShouldUpdateStock() {
        // Arrange
        int productId = sampleProduct.getProdId();
        int newQuantity = 60; // Valid new quantity
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productServiceImpl.updateQuantity(productId, newQuantity);

        // Assert
        assertNotNull(result);
        assertEquals(newQuantity, result.getStock());
        verify(productRepository).findById(productId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateQuantity: Throws ResourceNotFoundException when product not found")
    void updateQuantity_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int productId = 999; // Non-existent product ID
        int newQuantity = 10;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productServiceImpl.updateQuantity(productId, newQuantity));
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateQuantity: Throws IllegalArgumentException for negative quantity")
    void updateQuantity_WhenNegativeQuantity_ShouldThrowIllegalArgumentException() {
        // Arrange
        int productId = sampleProduct.getProdId();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> productServiceImpl.updateQuantity(productId, -5));
        verify(productRepository, never()).findById(anyInt());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("UpdateQuantity: Throws OperationFailedException on save error")
    void updateQuantity_WhenSaveFails_ShouldThrowOperationFailedException() {
        // Arrange
        int productId = sampleProduct.getProdId();
        int newQuantity = 10;
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenThrow(new RuntimeException("Save failed"));

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> productServiceImpl.updateQuantity(productId, newQuantity));
        verify(productRepository).findById(productId);
        verify(productRepository).save(any(Product.class));
    }

    // --- deleteProd Tests ---
    @Test
    @DisplayName("DeleteProd: Success deletes product")
    void deleteProd_WhenProductExists_ShouldDeleteProduct() {
        // Arrange
        int productId = sampleProduct.getProdId();
        when(productRepository.existsById(productId)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> productServiceImpl.deleteProd(productId));

        // Assert
        verify(productRepository).existsById(productId);
        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("DeleteProd: Throws ResourceNotFoundException when product not found")
    void deleteProd_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int productId = 999; // Non-existent product ID
        when(productRepository.existsById(productId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productServiceImpl.deleteProd(productId));
        verify(productRepository).existsById(productId);
        verify(productRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("DeleteProd: Throws OperationFailedException on delete error")
    void deleteProd_WhenDeleteFails_ShouldThrowOperationFailedException() {
        // Arrange
        int productId = sampleProduct.getProdId();
        when(productRepository.existsById(productId)).thenReturn(true);
        doThrow(new RuntimeException("Delete failed")).when(productRepository).deleteById(productId);

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> productServiceImpl.deleteProd(productId));
        verify(productRepository).existsById(productId);
        verify(productRepository).deleteById(productId);
    }

    // --- getProductByProdName Tests ---
    @Test
    @DisplayName("GetProductByProdName: Success returns product by name")
    void getProductByProdName_WhenProductFound_ShouldReturnProduct() {
        // Arrange
        String productName = sampleProduct.getProdName();
        when(productRepository.findByProdName(productName)).thenReturn(Optional.of(sampleProduct));

        // Act
        Product result = productServiceImpl.getProductByProdName(productName);

        // Assert
        assertNotNull(result);
        assertEquals(productName, result.getProdName());
        verify(productRepository).findByProdName(productName);
    }

    @Test
    @DisplayName("GetProductByProdName: Throws ResourceNotFoundException when product not found")
    void getProductByProdName_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        String productName = "NonExistentProduct";
        when(productRepository.findByProdName(productName)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productServiceImpl.getProductByProdName(productName));
        verify(productRepository).findByProdName(productName);
    }

    // --- getProductsByCategoryName Tests ---
    @Test
    @DisplayName("GetProductsByCategoryName: Success returns products in category")
    void getProductsByCategoryName_WhenCategoryAndProductsExist_ShouldReturnList() {
        // Arrange
        String categoryName = sampleCategory.getCategoryName();
        when(categoryRepository.findByCategoryName(categoryName)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.findByCategory(sampleCategory)).thenReturn(List.of(sampleProduct, sampleProduct2));

        // Act
        List<Product> result = productServiceImpl.getProductsByCategoryName(categoryName);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(categoryRepository).findByCategoryName(categoryName);
        verify(productRepository).findByCategory(sampleCategory);
    }

    @Test
    @DisplayName("GetProductsByCategoryName: Throws ResourceNotFoundException when category not found")
    void getProductsByCategoryName_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        String categoryName = "NonExistentCategory";
        when(categoryRepository.findByCategoryName(categoryName)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productServiceImpl.getProductsByCategoryName(categoryName));
        verify(categoryRepository).findByCategoryName(categoryName);
        verify(productRepository, never()).findByCategory(any(Category.class));
    }

    @Test
    @DisplayName("GetProductsByCategoryName: Throws ResourceNotFoundException when no products in category")
    void getProductsByCategoryName_WhenNoProductsExist_ShouldThrowResourceNotFoundException() {
        // Arrange
        String categoryName = sampleCategory.getCategoryName();
        when(categoryRepository.findByCategoryName(categoryName)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.findByCategory(sampleCategory)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productServiceImpl.getProductsByCategoryName(categoryName));
        verify(categoryRepository).findByCategoryName(categoryName);
        verify(productRepository).findByCategory(sampleCategory);
    }
}