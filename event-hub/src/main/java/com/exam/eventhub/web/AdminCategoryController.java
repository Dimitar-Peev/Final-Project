package com.exam.eventhub.web;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.common.Constants;
import com.exam.eventhub.web.dto.CategoryCreateRequest;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import com.exam.eventhub.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Controller
@RequestMapping("/admin/categories")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private static final String ENTITY_NAME = "Category";

    private final CategoryService categoryService;

    @GetMapping
    public String manageCategories(Model model) {
        List<Category> allCategories = categoryService.getAll();
        model.addAttribute("categories", allCategories);
        return "admin/manage-categories";
    }

    @GetMapping("/new")
    public String addCategoryForm(Model model) {
        if (!model.containsAttribute("categoryCreateRequest")) {
            model.addAttribute("categoryCreateRequest", new CategoryCreateRequest());
        }

        return "admin/category-add";
    }

    @PostMapping
    public String addCategory(@Valid @ModelAttribute("categoryCreateRequest") CategoryCreateRequest categoryCreateRequest,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("categoryCreateRequest", categoryCreateRequest);
            redirectAttributes.addFlashAttribute(Constants.BINDING_MODEL + "categoryCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute("errorMessage", ERROR_MESSAGE);
            return "redirect:/admin/categories/new";
        }

        categoryService.add(categoryCreateRequest);
        redirectAttributes.addFlashAttribute("successMessage", ADD_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}")
    public String editCategoryForm(@PathVariable UUID id, Model model) {
        Category category = categoryService.getById(id);

        if (!model.containsAttribute("categoryEditRequest")) {
            model.addAttribute("categoryEditRequest", DtoMapper.mapCategoryToCategoryEditRequest(category));
        }

        model.addAttribute("categoryId", id);
        return "admin/category-edit";
    }

    @PutMapping("/{id}")
    public String updateCategory(@PathVariable UUID id,
                                 @Valid @ModelAttribute("categoryEditRequest") CategoryEditRequest categoryEditRequest,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("categoryEditRequest", categoryEditRequest);
            redirectAttributes.addFlashAttribute(Constants.BINDING_MODEL + "categoryEditRequest", bindingResult);
            redirectAttributes.addFlashAttribute("errorMessage", ERROR_MESSAGE);
            return "redirect:/admin/categories/" + id;
        }

        categoryService.update(id, categoryEditRequest);
        redirectAttributes.addFlashAttribute("successMessage", UPDATE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/categories";
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(@PathVariable UUID id, RedirectAttributes redirectAttributes) {

        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage",  DELETE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/categories";
    }
}
