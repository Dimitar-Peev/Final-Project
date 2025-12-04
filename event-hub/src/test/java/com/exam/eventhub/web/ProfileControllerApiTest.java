package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.UserEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.ApiHelper.createMockUser;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class ProfileControllerApiTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
    }

    @Test
    void getAuthenticatedRequestToProfile_returnsProfileView() throws Exception {

        User mockUser = createMockUser();
        when(userService.getByUsername("testUser")).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = get("/profile")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/profile-menu"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", mockUser));

        verify(userService, times(1)).getByUsername("testUser");
    }

    @Test
    void getAuthenticatedRequestToEditProfile_returnsEditProfileView() throws Exception {

        User mockUser = createMockUser();
        when(userService.getByUsername("testUser")).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = get("/profile/edit")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/edit-profile"))
                .andExpect(model().attributeExists("user"));

        verify(userService, times(1)).getByUsername("testUser");
    }

    @Test
    void getEditProfileWithExistingUserInModel_doesNotOverrideUserAttribute() throws Exception {

        User mockUser = createMockUser();
        when(userService.getByUsername("testUser")).thenReturn(mockUser);

        UserEditRequest existingUserRequest = new UserEditRequest();
        existingUserRequest.setUsername("existingUser");
        existingUserRequest.setEmail("existing@example.com");

        MockHttpServletRequestBuilder request = get("/profile/edit")
                .with(user(principal))
                .flashAttr("user", existingUserRequest);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/edit-profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", existingUserRequest))
                .andExpect(model().attribute("user", instanceOf(UserEditRequest.class)));

        verify(userService, times(1)).getByUsername("testUser");
    }

    @Test
    void putValidProfileUpdate_updatesProfileAndRedirectsToProfile() throws Exception {

        User mockUser = createMockUser();
        when(userService.getByUsername("testUser")).thenReturn(mockUser);

        MockHttpServletRequestBuilder request = put("/profile")
                .param("id", UUID.randomUUID().toString())
                .param("username", "testUser")
                .formField("email", "test@example.com")
                .formField("firstName", "Test")
                .formField("lastName", "User")
                .formField("phoneNumber", "+359123456789")
                .formField("profileImageUrl", "https://example.com/profile.jpg")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, UPDATE_SUCCESSFUL.formatted("Profile")));

        verify(userService, times(1)).updateUserProfile(eq("testUser"), any(UserEditRequest.class));
        verify(userService, times(1)).getByUsername("testUser");
    }

    @Test
    void putInvalidProfileUpdate_redirectsToEditWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = put("/profile")
                .param("id", UUID.randomUUID().toString())
                .param("username", "testUser")
                .formField("email", "invalid-email")
                .formField("firstName", "")
                .formField("lastName", "")
                .formField("phoneNumber", "+3591234567890")
                .formField("profileImageUrl", "invalid-url")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("profile/edit"))
                .andExpect(flash().attributeExists("user"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "user"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE));

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    void getUnauthenticatedRequestToProfile_returnsInternalServerError() throws Exception {

        MockHttpServletRequestBuilder request = get("/profile");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is5xxServerError());

        verify(userService, never()).getByUsername(any());
    }

    @Test
    void getUnauthenticatedRequestToEditProfile_returnsInternalServerError() throws Exception {

        MockHttpServletRequestBuilder request = get("/profile/edit");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is5xxServerError());

        verify(userService, never()).getByUsername(any());
    }

    @Test
    void putUnauthenticatedProfileUpdate_returnsInternalServerError() throws Exception {

        MockHttpServletRequestBuilder request = put("/profile")
                .param("username", "testUser")
                .param("email", "test@example.com")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is5xxServerError());

        verify(userService, never()).updateUserProfile(any(), any());
    }
}
