package com.exam.eventhub.category.service;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.repository.CategoryRepository;
import com.exam.eventhub.exception.CategoryAlreadyExistException;
import com.exam.eventhub.exception.CategoryNotFoundException;
import com.exam.eventhub.web.dto.CategoryCreateRequest;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ID_NOT_FOUND;
import static com.exam.eventhub.common.Constants.NAME_NOT_FOUND;
import static com.exam.eventhub.util.TestBuilder.createCategory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceUTest {

    private static final String ENTITY_NAME = "Category";

    @Captor
    private ArgumentCaptor<List<Category>> listCategoryCaptor;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void initData_whenRepositoryIsEmpty_shouldInitializeCategories() {

        when(categoryRepository.count()).thenReturn(0L);
        when(categoryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.initData();

        verify(categoryRepository).saveAll(listCategoryCaptor.capture());

        List<Category> savedCategories = listCategoryCaptor.getValue();

        List<String> expectedNames = List.of("Music", "Technology", "Business", "Sports", "Art", "Education", "Cars");

        List<String> actualNames = savedCategories.stream()
                .map(Category::getName)
                .sorted()
                .toList();

        List<String> expectedSorted = expectedNames.stream()
                .sorted()
                .toList();

        assertEquals(7, savedCategories.size());
        assertEquals(expectedSorted, actualNames);
    }

    @Test
    void initData_whenRepositoryIsNotEmpty_shouldNotInitializeCategories() {

        when(categoryRepository.count()).thenReturn(5L);

        categoryService.initData();

        verify(categoryRepository, never()).saveAll(anyList());
    }

    @Test
    void getAll_shouldReturnAllCategories() {

        Category category1 = createCategory(UUID.randomUUID(), "Music", "Description", "#ff6b6b");
        Category category2 = createCategory(UUID.randomUUID(), "Technology", "Description", "#4ecdc4");
        List<Category> expectedCategories = List.of(category1, category2);
        when(categoryRepository.findAll()).thenReturn(expectedCategories);

        List<Category> actualCategories = categoryService.getAll();

        assertEquals(expectedCategories, actualCategories);
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void add_whenCategoryDoesNotExist_shouldCreateAndReturnCategory() {

        String name = "New Category";
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName(name);
        request.setDescription("New Description");
        request.setColor("#123456");

        when(categoryRepository.findByName(name)).thenReturn(Optional.empty());

        Category savedCategory = createCategory(UUID.randomUUID(), name, "New Description", "#123456");
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.add(request);

        assertNotNull(result);
        assertEquals(savedCategory.getName(), result.getName());
        assertEquals(savedCategory.getDescription(), result.getDescription());
        assertEquals(savedCategory.getColor(), result.getColor());
        verify(categoryRepository).findByName(name);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void add_whenCategoryAlreadyExists_shouldThrowException() {

        String name = "Existing Category";
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName(name);

        Category existingCategory = createCategory(UUID.randomUUID(), name, "Description", "#123456");
        when(categoryRepository.findByName(name)).thenReturn(Optional.of(existingCategory));

        CategoryAlreadyExistException exception =
                assertThrows(CategoryAlreadyExistException.class, () -> categoryService.add(request));
        assertTrue(exception.getMessage().contains("The category '" + name + "' already exists."));

        verify(categoryRepository).findByName(name);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_whenCategoryExists_shouldUpdateAndReturnCategory() {

        UUID categoryId = UUID.randomUUID();
        Category existingCategory = createCategory(categoryId, "Old Name", "Old Description", "#000000");

        CategoryEditRequest request = new CategoryEditRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");
        request.setColor("#ffffff");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        Category updatedCategory = createCategory(categoryId, request.getName(), request.getDescription(), request.getColor());
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        Category result = categoryService.update(categoryId, request);

        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getColor(), result.getColor());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    void update_whenCategoryDoesNotExist_shouldThrowException() {

        UUID categoryId = UUID.randomUUID();
        CategoryEditRequest request = new CategoryEditRequest();
        request.setName("Updated Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception =
                assertThrows(CategoryNotFoundException.class, () -> categoryService.update(categoryId, request));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, categoryId)));

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_whenCategoryExists_shouldDeleteCategory() {

        UUID categoryId = UUID.randomUUID();
        Category category = createCategory(categoryId, "Category", "Description", "#123456");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.delete(categoryId);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    void delete_whenCategoryDoesNotExist_shouldThrowException() {

        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception =
                assertThrows(CategoryNotFoundException.class, () -> categoryService.delete(categoryId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, categoryId)));

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void getById_whenCategoryExists_shouldReturnCategory() {

        UUID categoryId = UUID.randomUUID();
        Category category = createCategory(categoryId, "Category", "Description", "#123456");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.getById(categoryId);

        assertEquals(category, result);
        assertEquals(categoryId, result.getId());
        assertEquals(category.getName(), result.getName());
        assertEquals(category.getDescription(), result.getDescription());
        assertEquals(category.getColor(), result.getColor());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void getById_whenCategoryDoesNotExist_shouldThrowException() {

        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception =
                assertThrows(CategoryNotFoundException.class, () -> categoryService.getById(categoryId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, categoryId)));

        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void getByName_whenCategoryExists_shouldReturnCategory() {

        String categoryName = "Music";
        Category category = createCategory(UUID.randomUUID(), categoryName, "Description", "#123456");

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(category));

        Category result = categoryService.getByName(categoryName);

        assertEquals(category, result);
        assertEquals(categoryName, result.getName());
        assertEquals(category.getDescription(), result.getDescription());
        assertEquals(category.getColor(), result.getColor());
        verify(categoryRepository, times(1)).findByName(categoryName);
    }

    @Test
    void getByName_whenCategoryDoesNotExist_shouldThrowException() {

        String categoryName = "NonExistent";
        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.empty());

        CategoryNotFoundException exception =
                assertThrows(CategoryNotFoundException.class, () -> categoryService.getByName(categoryName));
        assertTrue(exception.getMessage().contains(NAME_NOT_FOUND.formatted(ENTITY_NAME, categoryName)));

        verify(categoryRepository, times(1)).findByName(categoryName);
    }
}
