package com.exam.eventhub.category.service;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.repository.CategoryRepository;
import com.exam.eventhub.exception.CategoryAlreadyExistException;
import com.exam.eventhub.exception.CategoryNotFoundException;
import com.exam.eventhub.web.dto.CategoryCreateRequest;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public void initData() {
        if (categoryRepository.count() == 0) {
            initializeCategories();
        }
    }

    private void initializeCategories() {
        log.info("Initializing categories...");
        List<Category> defaultCategories = new ArrayList<>();

        Category music = new Category("Music", "Live music concerts and performances", "#ff6b6b");
        Category technology = new Category("Technology", "Tech conferences and workshops", "#4ecdc4");
        Category business = new Category("Business", "Business conferences and networking", "#45b7d1");
        Category sports = new Category("Sports", "Sporting events and competitions", "#96ceb4");
        Category art = new Category("Art", "Art exhibitions and cultural events", "#ffeaa7");
        Category education = new Category("Education", "Educational workshops and seminars", "#dda0dd");
        Category cars = new Category("Cars", "Automobile exhibition", "#ff9f43");

        defaultCategories.add(music);
        defaultCategories.add(technology);
        defaultCategories.add(business);
        defaultCategories.add(sports);
        defaultCategories.add(art);
        defaultCategories.add(education);
        defaultCategories.add(cars);

        this.categoryRepository.saveAll(defaultCategories);
    }

    @Cacheable("categories")
    public List<Category> getAll() {
        return this.categoryRepository.findAll();
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category add(CategoryCreateRequest categoryCreateRequest) {
        String name = categoryCreateRequest.getName();
        log.info("Creating category: {}", name);

        Optional<Category> byName = categoryRepository.findByName(name);
        if (byName.isPresent()) {
            throw new CategoryAlreadyExistException("The category '" + name + "' already exists.");
        }

        Category category = create(categoryCreateRequest);

        Category saved = categoryRepository.save(category);

        log.info("Category [{}] (ID: [{}]) was successfully added.", saved.getName(), saved.getId());

        return saved;
    }

    private Category create(CategoryCreateRequest categoryCreateRequest) {
        Category category = new Category();

        category.setName(categoryCreateRequest.getName());
        category.setDescription(categoryCreateRequest.getDescription());
        category.setColor(categoryCreateRequest.getColor());

        return category;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category update(UUID id, CategoryEditRequest categoryEditRequest) {

        Category category = getById(id);

        category.setName(categoryEditRequest.getName());
        category.setDescription(categoryEditRequest.getDescription());
        category.setColor(categoryEditRequest.getColor());

        Category saved = categoryRepository.save(category);

        log.info("Category [{}] (ID: [{}]) was successfully updated.", saved.getName(), saved.getId());

        return saved;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(UUID id) {
        Category category = getById(id);

        this.categoryRepository.delete(category);

        log.info("Category with ID [{}] was successfully deleted.", id);
    }

    public Category getById(UUID id) {
        return this.categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category with ID [%s] was not found.".formatted(id)));
    }

    public Category getByName(String name) {
        return this.categoryRepository.findByName(name)
                .orElseThrow(() -> new CategoryNotFoundException("Category with name [%s] was not found.".formatted(name)));
    }
}
