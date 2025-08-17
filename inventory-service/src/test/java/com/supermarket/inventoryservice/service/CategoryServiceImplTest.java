package com.supermarket.inventoryservice.service;

import com.supermarket.inventoryservice.exception.OperationFailedException;
import com.supermarket.inventoryservice.exception.ResourceAlreadyExistsException;
import com.supermarket.inventoryservice.exception.ResourceNotFoundException;
import com.supermarket.inventoryservice.model.Category;
import com.supermarket.inventoryservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryServiceImpl;

    private Category sampleCategory;
    private Category sampleCategory2;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category(1, "Electronics", null); // Products list can be null for setup
        sampleCategory2 = new Category(2, "Clothing", null);
    }

    // --- addCategory Tests ---
    @Test
    @DisplayName("AddCategory: Success when category name does not exist")
    void addCategory_WhenNameNotExists_ShouldSaveAndReturnCategory() {
        // Arrange
        Category newCategory = new Category(0, "Books", null);
        when(categoryRepository.findByCategoryName("Books")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(99); // Simulate DB assigning ID
            return saved;
        });

        // Act
        Category result = categoryServiceImpl.addCategory(newCategory);

        // Assert
        assertNotNull(result);
        assertEquals("Books", result.getCategoryName());
        assertEquals(99, result.getCategoryId());
        verify(categoryRepository).findByCategoryName("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("AddCategory: Throws ResourceAlreadyExistsException when name exists")
    void addCategory_WhenNameExists_ShouldThrowResourceAlreadyExistsException() {
        // Arrange
        when(categoryRepository.findByCategoryName(sampleCategory.getCategoryName())).thenReturn(Optional.of(sampleCategory));

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            categoryServiceImpl.addCategory(sampleCategory); // Try to add existing
        });
        verify(categoryRepository).findByCategoryName(sampleCategory.getCategoryName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("AddCategory: Throws IllegalArgumentException for blank name")
    void addCategory_WhenNameIsBlank_ShouldThrowIllegalArgumentException() {
        // Arrange
        Category blankNameCategory = new Category(0, "  ", null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryServiceImpl.addCategory(blankNameCategory);
        });
        verify(categoryRepository, never()).findByCategoryName(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("AddCategory: Throws OperationFailedException on save error")
    void addCategory_WhenSaveFails_ShouldThrowOperationFailedException() {
        // Arrange
        Category newCategory = new Category(0, "Music", null);
        when(categoryRepository.findByCategoryName("Music")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenThrow(new DataAccessException("DB Error") {
        });

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            categoryServiceImpl.addCategory(newCategory);
        });
        verify(categoryRepository).findByCategoryName("Music");
        verify(categoryRepository).save(any(Category.class));
    }

    // --- getCategoryById Tests ---
    @Test
    @DisplayName("GetCategoryById: Returns category when found")
    void getCategoryById_WhenFound_ShouldReturnCategory() {
        // Arrange
        when(categoryRepository.findById(sampleCategory.getCategoryId())).thenReturn(Optional.of(sampleCategory));

        // Act
        Category result = categoryServiceImpl.getCategoryById(sampleCategory.getCategoryId());

        // Assert
        assertNotNull(result);
        assertEquals(sampleCategory.getCategoryId(), result.getCategoryId());
        verify(categoryRepository).findById(sampleCategory.getCategoryId());
    }

    @Test
    @DisplayName("GetCategoryById: Throws ResourceNotFoundException when not found")
    void getCategoryById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int categoryId = 999;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryServiceImpl.getCategoryById(categoryId);
        });
        verify(categoryRepository).findById(categoryId);
    }

    // --- getAllCategories Tests ---
    @Test
    @DisplayName("GetAllCategories: Returns list when categories exist")
    void getAllCategories_WhenExist_ShouldReturnList() {
        // Arrange
        List<Category> categories = List.of(sampleCategory, sampleCategory2);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<Category> result = categoryServiceImpl.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("GetAllCategories: Throws ResourceNotFoundException when none exist")
    void getAllCategories_WhenNoneExist_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryServiceImpl.getAllCategories();
        });
        verify(categoryRepository).findAll();
    }

    // --- getCategoryByName Tests ---
    @Test
    @DisplayName("GetCategoryByName: Returns category when found")
    void getCategoryByName_WhenFound_ShouldReturnCategory() {
        // Arrange
        String name = sampleCategory.getCategoryName();
        when(categoryRepository.findByCategoryName(name)).thenReturn(Optional.of(sampleCategory));

        // Act
        Category result = categoryServiceImpl.getCategoryByName(name);

        // Assert
        assertNotNull(result);
        assertEquals(name, result.getCategoryName());
        verify(categoryRepository).findByCategoryName(name);
    }

    @Test
    @DisplayName("GetCategoryByName: Throws ResourceNotFoundException when not found")
    void getCategoryByName_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        String name = "NonExistent";
        when(categoryRepository.findByCategoryName(name)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryServiceImpl.getCategoryByName(name);
        });
        verify(categoryRepository).findByCategoryName(name);
    }

    // --- updateCategoryName Tests ---
    @Test
    @DisplayName("UpdateCategoryName: Success updates name")
    void updateCategoryName_WhenValid_ShouldUpdateAndReturnCategory() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        String newName = "Updated Electronics";
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.findByCategoryName(newName)).thenReturn(Optional.empty()); // Assume new name doesn't conflict
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0)); // Return saved obj

        // Act
        Category result = categoryServiceImpl.updateCategoryName(categoryId, newName);

        // Assert
        assertNotNull(result);
        assertEquals(newName, result.getCategoryName());
        assertEquals(categoryId, result.getCategoryId());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findByCategoryName(newName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("UpdateCategoryName: Throws IllegalArgumentException for blank name")
    void updateCategoryName_WhenNameIsBlank_ShouldThrowIllegalArgumentException() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        // No need to mock findById as the blank check happens first

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryServiceImpl.updateCategoryName(categoryId, " ");
        });
        verify(categoryRepository, never()).findById(anyInt());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("UpdateCategoryName: Throws ResourceNotFoundException if category not found")
    void updateCategoryName_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int categoryId = 999;
        String newName = "New Name";
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryServiceImpl.updateCategoryName(categoryId, newName);
        });
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).findByCategoryName(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("UpdateCategoryName: Throws OperationFailedException if name conflicts")
    void updateCategoryName_WhenNameConflicts_ShouldThrowOperationFailedException() {
        // Arrange
        int categoryIdToUpdate = sampleCategory.getCategoryId(); // Updating category 1
        String conflictingName = sampleCategory2.getCategoryName(); // Trying to rename to category 2's name
        when(categoryRepository.findById(categoryIdToUpdate)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.findByCategoryName(conflictingName)).thenReturn(Optional.of(sampleCategory2)); // Name exists on different category

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            categoryServiceImpl.updateCategoryName(categoryIdToUpdate, conflictingName);
        });
        verify(categoryRepository).findById(categoryIdToUpdate);
        verify(categoryRepository).findByCategoryName(conflictingName);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // --- deleteCategory Tests ---
    @Test
    @DisplayName("DeleteCategory: Success when category exists")
    void deleteCategory_WhenExists_ShouldCallDelete() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(categoryId);

        // Act
        assertDoesNotThrow(() -> categoryServiceImpl.deleteCategory(categoryId));

        // Assert
        verify(categoryRepository).existsById(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    @DisplayName("DeleteCategory: Throws ResourceNotFoundException when category not exists")
    void deleteCategory_WhenNotExists_ShouldThrowResourceNotFoundException() {
        // Arrange
        int categoryId = 999;
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryServiceImpl.deleteCategory(categoryId);
        });
        verify(categoryRepository).existsById(categoryId);
        verify(categoryRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("DeleteCategory: Throws OperationFailedException on DataIntegrityViolationException")
    void deleteCategory_WhenIntegrityViolation_ShouldThrowOperationFailedException() {
        // Arrange
        int categoryId = sampleCategory.getCategoryId();
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("Constraint violated")).when(categoryRepository).deleteById(categoryId);

        // Act & Assert
        OperationFailedException ex = assertThrows(OperationFailedException.class, () -> {
            categoryServiceImpl.deleteCategory(categoryId);
        });
        assertTrue(ex.getMessage().contains("associated with existing products"));
        verify(categoryRepository).existsById(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    // --- getAllCategoryName Tests ---
    @Test
    @DisplayName("GetAllCategoryName: Returns sorted list of names")
    void getAllCategoryName_WhenCategoriesExist_ShouldReturnSortedNameList() {
        // Arrange - Unsorted list
        List<Category> categories = List.of(sampleCategory2, sampleCategory); // Clothing, Electronics
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<String> result = categoryServiceImpl.getAllCategoryName();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Clothing", result.get(0)); // Check sorting
        assertEquals("Electronics", result.get(1));
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("GetAllCategoryName: Returns empty list when no categories exist")
    void getAllCategoryName_WhenNoCategoriesExist_ShouldReturnEmptyList() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<String> result = categoryServiceImpl.getAllCategoryName();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
    }
}