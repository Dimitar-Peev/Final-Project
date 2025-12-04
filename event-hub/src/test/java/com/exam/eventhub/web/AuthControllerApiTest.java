package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.LoginRequest;
import com.exam.eventhub.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.exam.eventhub.common.Constants.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class AuthControllerApiTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void showRegisterForm_shouldReturnRegisterViewWithEmptyForm() throws Exception {

        MockHttpServletRequestBuilder request = get("/register");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("registerRequest", instanceOf(RegisterRequest.class)));
    }

    @Test
    void showRegisterForm_whenModelContainsRegisterRequest_shouldNotAddNewAttribute() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("dimitar_p");
        registerRequest.setEmail("peev@example.com");
        registerRequest.setPassword("test123");
        registerRequest.setConfirmPassword("test123");
        registerRequest.setRole(Role.USER);

        MockHttpServletRequestBuilder request = get("/register")
                .flashAttr("registerRequest", registerRequest);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("registerRequest", registerRequest))
                .andExpect(model().attribute("registerRequest", instanceOf(RegisterRequest.class)));
    }

    @Test
    void processRegistration_withValidData_shouldRegisterUserAndRedirectToLogin() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "dimitar_p")
                .formField("email", "peev@example.com")
                .formField("password", "test123")
                .formField("confirmPassword", "test123")
                .formField("role", "USER")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService, times(1)).register(any());
    }

    @Test
    void processRegistration_withInvalidData_shouldRedirectBackToRegisterWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "")
                .formField("email", "invalid-email")
                .formField("password", "123")
                .formField("confirmPassword", "456")
                .formField("role", "USER")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("registerRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "registerRequest"))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE));

        verify(userService, never()).register(any());
    }

    @Test
    void processRegistration_withBindingErrors_shouldNotCallUserService() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "")
                .formField("email", "")
                .formField("password", "")
                .formField("confirmPassword", "")
                .formField("role", "USER")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        verify(userService, never()).register(any());
    }

    @Test
    void processRegistration_withMismatchedPasswords_shouldRedirectWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "dimitar_p")
                .formField("email", "peev@example.com")
                .formField("password", "test123")
                .formField("confirmPassword", "test1234")
                .formField("role", "USER")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(userService, never()).register(any());
    }

    @Test
    void showLoginForm_shouldReturnLoginViewWithEmptyForm() throws Exception {

        MockHttpServletRequestBuilder request = get("/login");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginRequest"))
                .andExpect(model().attribute("loginRequest", instanceOf(LoginRequest.class)));
    }

    @Test
    void showLoginForm_whenModelContainsLoginRequest_shouldNotAddNewAttribute() throws Exception {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("dimitar_p");
        loginRequest.setPassword("test123");

        MockHttpServletRequestBuilder request = get("/login")
                .flashAttr("loginRequest", loginRequest);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginRequest"))
                .andExpect(model().attribute("loginRequest", loginRequest))
                .andExpect(model().attribute("loginRequest", instanceOf(LoginRequest.class)));
    }
}

