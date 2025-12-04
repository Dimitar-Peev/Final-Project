package com.exam.eventhub.web;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.web.dto.CategoryCreateRequest;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.ApiHelper.createMockCategory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
class AdminCategoryControllerApiTest {

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata adminPrincipal;
    private AuthenticationMetadata principal;

    @BeforeEach
    void setup() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
    }

    @Test
    void getAuthenticatedAdminRequestToManageCategories_returnsManageCategoriesView() throws Exception {

        Category category1 = createMockCategory("Music", "Musical events");
        Category category2 = createMockCategory("Sports", "Sports events");
        Category category3 = createMockCategory("Arts", "Art exhibitions");

        List<Category> mockCategories = Arrays.asList(category1, category2, category3);
        when(categoryService.getAll()).thenReturn(mockCategories);

        MockHttpServletRequestBuilder request = get("/admin/categories")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", mockCategories));

        verify(categoryService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedNonAdminRequestToManageCategories_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/categories")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToAddCategoryForm_returnsCategoryAddView() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/categories/new")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/category-add"))
                .andExpect(model().attributeExists("categoryCreateRequest"));
    }

    @Test
    void getAddCategoryFormWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        CategoryCreateRequest existingRequest = new CategoryCreateRequest();
        existingRequest.setName("ExistingCategory");
        existingRequest.setDescription("Existing description");

        MockHttpServletRequestBuilder request = get("/admin/categories/new")
                .flashAttr("categoryCreateRequest", existingRequest)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/category-add"))
                .andExpect(model().attributeExists("categoryCreateRequest"))
                .andExpect(model().attribute("categoryCreateRequest", existingRequest));
    }

    @Test
    void getAuthenticatedNonAdminRequestToAddCategoryForm_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/categories/new")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());
    }

    @Test
    void postValidCategoryCreateRequest_addsCategoryAndRedirects() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/categories")
                .param("name", "Technology")
                .param("description", "Tech events and conferences")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(categoryService, times(1)).add(any(CategoryCreateRequest.class));
    }

    @Test
    void postInvalidCategoryCreateRequest_redirectsToFormWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/categories")
                .param("name", "")
                .param("description", "")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories/new"))
                .andExpect(flash().attributeExists("categoryCreateRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "categoryCreateRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(categoryService, never()).add(any());
    }

    @Test
    void postAuthenticatedNonAdminCategoryCreateRequest_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/categories")
                .param("name", "Technology")
                .param("description", "Tech events")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).add(any());
    }

    @Test
    void postCategoryWithoutCsrf_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/categories")
                .param("name", "Technology")
                .param("description", "Tech events")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).add(any());
    }

    @Test
    void getAuthenticatedAdminRequestToEditCategoryForm_returnsCategoryEditView() throws Exception {

        UUID categoryId = UUID.randomUUID();
        Category mockCategory = createMockCategory("Music", "Musical events");
        mockCategory.setId(categoryId);
        when(categoryService.getById(categoryId)).thenReturn(mockCategory);

        MockHttpServletRequestBuilder request = get("/admin/categories/" + categoryId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/category-edit"))
                .andExpect(model().attributeExists("categoryEditRequest"));

        verify(categoryService, times(1)).getById(categoryId);
    }

    @Test
    void getEditCategoryFormWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        UUID categoryId = UUID.randomUUID();
        Category mockCategory = createMockCategory("Music", "Musical events");
        when(categoryService.getById(categoryId)).thenReturn(mockCategory);

        CategoryEditRequest existingRequest = new CategoryEditRequest();
        existingRequest.setName("ExistingName");
        existingRequest.setDescription("Existing description");

        MockHttpServletRequestBuilder request = get("/admin/categories/" + categoryId)
                .flashAttr("categoryEditRequest", existingRequest)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/category-edit"))
                .andExpect(model().attributeExists("categoryEditRequest"))
                .andExpect(model().attribute("categoryEditRequest", existingRequest));

        verify(categoryService, times(1)).getById(categoryId);
    }

    @Test
    void getAuthenticatedNonAdminRequestToEditCategoryForm_returnsForbidden() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = get("/admin/categories/" + categoryId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).getById(any());
    }

    @Test
    void putValidCategoryEditRequest_updatesCategoryAndRedirects() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/categories/" + categoryId)
                .param("name", "Updated Music")
                .param("description", "Updated description")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(categoryService, times(1)).update(eq(categoryId), any(CategoryEditRequest.class));
    }

    @Test
    void putInvalidCategoryEditRequest_redirectsToFormWithErrors() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/categories/" + categoryId)
                .param("name", "")
                .param("description", "")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories/" + categoryId))
                .andExpect(flash().attributeExists("categoryEditRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "categoryEditRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(categoryService, never()).update(any(), any());
    }

    @Test
    void putAuthenticatedNonAdminCategoryEditRequest_returnsForbidden() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/categories/" + categoryId)
                .param("name", "Updated Music")
                .param("description", "Updated description")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).update(any(), any());
    }

    @Test
    void deleteAuthenticatedAdminRequestToDeleteCategory_deletesCategoryAndRedirects() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/admin/categories/" + categoryId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(categoryService, times(1)).delete(categoryId);
    }

    @Test
    void deleteCategoryWithoutCsrf_returnsForbidden() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/admin/categories/" + categoryId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).delete(any());
    }
}
