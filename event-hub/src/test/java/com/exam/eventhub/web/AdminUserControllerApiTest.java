package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
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

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;
import static com.exam.eventhub.util.ApiHelper.createMockUser;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class AdminUserControllerApiTest {

    @MockitoBean
    private UserService userService;

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
    void getAuthenticatedAdminRequestToListUsers_returnsManageUsersView() throws Exception {

        User user1 = createMockUser();
        user1.setUsername("user1");

        User user2 = createMockUser();
        user2.setUsername("user2");

        User admin = createMockUser();
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        List<User> mockUsers = Arrays.asList(user1, user2, admin);
        when(userService.getAll()).thenReturn(mockUsers);

        MockHttpServletRequestBuilder request = get("/admin/users")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", mockUsers));

        verify(userService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedNonAdminRequestToListUsers_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/users")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getAll();
    }

    @Test
    void getUnauthenticatedRequestToListUsers_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/users");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToEditUser_returnsUserEditView() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        User mockUser = createMockUser();
        mockUser.setId(targetUserId);

        when(userService.getById(targetUserId)).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = get("/admin/users/{id}/edit", targetUserId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", mockUser));

        verify(userService, times(1)).getById(targetUserId);
    }

    @Test
    void getAuthenticatedNonAdminRequestToEditUser_returnsForbidden() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = get("/admin/users/{id}/edit", targetUserId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getById(any());
    }

    @Test
    void putAuthenticatedAdminRequestToSaveUser_updatesUserAndRedirects() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/users/" + targetUserId)
                .param("username", "updatedUser")
                .param("email", "updated@example.com")
                .param("firstName", "Updated")
                .param("lastName", "User")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService, times(1)).updateUser(eq(targetUserId), any(User.class));
    }

    @Test
    void putAuthenticatedNonAdminRequestToSaveUser_returnsForbidden() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/users/" + targetUserId)
                .param("username", "updatedUser")
                .param("email", "updated@example.com")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void patchAuthenticatedAdminRequestToToggleUserStatus_whenUserIsNotBlocked_blocksUserAndRedirects() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(targetUserId);
        mockUser.setBlocked(false);
        when(userService.getById(targetUserId)).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = patch("/admin/users/{id}/status", targetUserId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "User blocked successfully!"));

        verify(userService, times(1)).getById(targetUserId);
        verify(userService, times(1)).blockUser(targetUserId);
        verify(userService, never()).unblockUser(any());
    }

    @Test
    void patchAuthenticatedAdminRequestToToggleUserStatus_whenUserIsBlocked_unblocksUserAndRedirects() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(targetUserId);
        mockUser.setBlocked(true);
        when(userService.getById(targetUserId)).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = patch("/admin/users/{id}/status", targetUserId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "User unblocked successfully!"));

        verify(userService, times(1)).getById(targetUserId);
        verify(userService, times(1)).unblockUser(targetUserId);
        verify(userService, never()).blockUser(any());
    }

    @Test
    void patchAuthenticatedNonAdminRequestToToggleUserStatus_returnsForbidden() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = patch("/admin/users/{id}/status", targetUserId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getById(any());
        verify(userService, never()).blockUser(any());
        verify(userService, never()).unblockUser(any());
    }

    @Test
    void patchUnauthenticatedRequestToToggleUserStatus_returnsForbidden() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = patch("/admin/users/{id}/status", targetUserId)
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getById(any());
        verify(userService, never()).blockUser(any());
        verify(userService, never()).unblockUser(any());
    }

    @Test
    void putRequestWithoutCsrfToken_returnsForbidden() throws Exception {

        UUID targetUserId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/users/edit/" + targetUserId)
                .param("username", "updatedUser")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).updateUser(any(), any());
    }
}
