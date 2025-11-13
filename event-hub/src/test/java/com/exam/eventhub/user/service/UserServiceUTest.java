package com.exam.eventhub.user.service;

import com.exam.eventhub.exception.EmailAlreadyExistsException;
import com.exam.eventhub.exception.EmailDuplicateException;
import com.exam.eventhub.exception.UserNotFoundException;
import com.exam.eventhub.exception.UsernameAlreadyExistsException;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.repository.UserRepository;
import com.exam.eventhub.web.dto.RegisterRequest;
import com.exam.eventhub.web.dto.UserEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.EMAIL_EXIST;
import static com.exam.eventhub.common.Constants.USERNAME_EXIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new User("testUser", "test@exam.com", "encodedPass",
                "Test", "User", "+359888111222", Role.USER);
        defaultUser.setId(UUID.randomUUID());
    }

    // ---------- initData() ----------
    @Test
    void givenRepository_whenRepositoryIsEmpty_thenInitializeUsers() {
        // Given
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.initData();

        // Then
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());

        List<User> savedUsers = captor.getValue();
        assertEquals(3, savedUsers.size());

        User admin = savedUsers.get(0);
        assertEquals("admin", admin.getUsername());
        assertEquals("admin@eventhub.com", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("Admin", admin.getFirstName());
        assertEquals("User", admin.getLastName());

        User organizer = savedUsers.get(1);
        assertEquals("organizer", organizer.getUsername());
        assertEquals("organizer1@eventhub.com", organizer.getEmail());
        assertEquals(Role.EVENT_ORGANIZER, organizer.getRole());

        User user = savedUsers.get(2);
        assertEquals("user", user.getUsername());
        assertEquals("user1@eventhub.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());

        verify(userRepository).count();
        verify(passwordEncoder, times(3)).encode(anyString());
    }

    @Test
    void givenRepository_whenRepositoryIsNotEmpty_thenNotInitializeUsers() {

        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        userService.initData();

        // Then
        verify(userRepository).count();
        verify(userRepository, never()).saveAll(anyList());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ---------- getAll() ----------
    @Test
    void givenExistingUsersInDatabase_whenGetAllUsers_thenReturnThemAll() {

        // Given
        List<User> userList = List.of(defaultUser, new User());
        when(userRepository.findAll()).thenReturn(userList);

        // When
        List<User> result = userService.getAll();

        // Then
        assertEquals(2, result.size());
        assertEquals(defaultUser, result.get(0));
        verify(userRepository, times(1)).findAll();
    }

    // ---------- register() ----------
    @Test
    void givenNonExistingUsername_whenRegister_thenSaveToDatabase() {

        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newUser");
        registerRequest.setEmail("new@exam.com");
        registerRequest.setPassword("12345");
        registerRequest.setRole(Role.USER);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword("encodedPass");
        user.setBlocked(false);
        user.setRole(registerRequest.getRole());
        user.setBlocked(false);

        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPass");
        when(userRepository.save(any())).thenReturn(user);

        // When
        User saved = userService.register(registerRequest);

        // Then
        assertNotNull(saved);
        assertEquals("newUser", saved.getUsername());
        assertEquals("new@exam.com", saved.getEmail());
        assertEquals("encodedPass", saved.getPassword());
        assertEquals(Role.USER, saved.getRole());
        assertFalse(saved.isBlocked());
        verify(userRepository, times(1)).findByUsername(registerRequest.getUsername());
        verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenNewUser_whenUsernameAndEmailNotExist_thenSaveToDatabase() {

        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("new@exam.com");
        request.setPassword("12345");
        request.setRole(Role.USER);

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User saved = userService.register(request);

        // Then
        assertNotNull(saved);
        assertEquals(request.getUsername(), saved.getUsername());
        assertEquals(Role.USER, saved.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenNewUser_whenUsernameExists_thenThrowUsernameAlreadyExistsException() {
        // Given
        RegisterRequest validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("newUser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setConfirmPassword("password123");
        validRegisterRequest.setRole(Role.USER);

        User existingUser = new User();
        existingUser.setUsername(validRegisterRequest.getUsername());

        when(userRepository.findByUsername(validRegisterRequest.getUsername())).thenReturn(Optional.of(existingUser));

        // When & Then
        UsernameAlreadyExistsException exception =
                assertThrows(UsernameAlreadyExistsException.class, () -> userService.register(validRegisterRequest));
        assertTrue(exception.getMessage().contains(USERNAME_EXIST.formatted(validRegisterRequest.getUsername())));

        verify(userRepository).findByUsername(validRegisterRequest.getUsername());
        verify(userRepository, never()).findByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenNewUser_whenEmailExists_thenThrowEmailAlreadyExistsException() {
        // Given
        RegisterRequest validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("newUser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setConfirmPassword("password123");
        validRegisterRequest.setRole(Role.USER);

        User existingUser = new User();
        existingUser.setEmail(validRegisterRequest.getEmail());

        when(userRepository.findByUsername(validRegisterRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(validRegisterRequest.getEmail())).thenReturn(Optional.of(existingUser));

        // When & Then
        EmailAlreadyExistsException exception =
                assertThrows(EmailAlreadyExistsException.class, () -> userService.register(validRegisterRequest));
        assertTrue(exception.getMessage().contains(EMAIL_EXIST));

        verify(userRepository).findByUsername(validRegisterRequest.getUsername());
        verify(userRepository).findByEmail(validRegisterRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ---------- updateUserProfile() ----------
    @Test
    void givenMissingUserFromDatabase_whenUpdateUserProfile_thenExceptionIsThrown() {

        // Given
        String username = "missing";
        UserEditRequest dto = new UserEditRequest();
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.updateUserProfile(username, dto));
    }

    @Test
    void givenExistingUser_whenUpdateUserProfileWithActualEmail_thenChangeTheirProfileAndSaveToDatabase() {

        // Given
        String username = "existing";

        UserEditRequest dto = new UserEditRequest();
        dto.setUsername("testUser");
        dto.setFirstName("Dimitar");
        dto.setLastName("Peev");
        dto.setEmail("peev@test.bg");
        dto.setProfileImageUrl("www.image.com");
        dto.setPhoneNumber("+359888111222");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(defaultUser));

        // When
        userService.updateUserProfile(username, dto);

        // Then
        assertEquals("peev@test.bg", defaultUser.getEmail());
        assertEquals("Dimitar", defaultUser.getFirstName());
        assertEquals("Peev", defaultUser.getLastName());
        assertEquals("www.image.com", defaultUser.getProfileImageUrl());
        assertEquals("+359888111222", defaultUser.getPhoneNumber());
        verify(userRepository, times(1)).findByEmail(defaultUser.getEmail());
        verify(userRepository, times(1)).save(defaultUser);
    }

    @Test
    void givenExistingUser_whenUpdateUserProfileWithExistingEmail_thenExceptionIsThrown() {

        // Given
        String username = "existing";

        UserEditRequest dto = new UserEditRequest();
        dto.setUsername("testUser");
        dto.setFirstName("Dimitar");
        dto.setLastName("Peev");
        dto.setEmail("peev@test.bg");
        dto.setProfileImageUrl("www.image.com");
        dto.setPhoneNumber("+359888111222");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(defaultUser));
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new User()));

        // When & Then
        EmailDuplicateException exception =
                assertThrows(EmailDuplicateException.class, () -> userService.updateUserProfile(username, dto));
        assertTrue(exception.getMessage().contains(EMAIL_EXIST));
    }

    @Test
    void givenExistingUser_whenUpdateUserProfileWithSameEmail_thenChangeOtherDetailsAndSaveToDatabase() {

        // Given
        String username = "existing";
        String originalEmail = "peev@abv.bg";

        UserEditRequest dto = new UserEditRequest();
        dto.setUsername("testUser");
        dto.setFirstName("Dimitar");
        dto.setLastName("Peev");
        dto.setEmail(originalEmail);
        dto.setProfileImageUrl("www.image.com");
        dto.setPhoneNumber("+359888111222");

        User user = new User();
        user.setEmail(originalEmail);
        user.setFirstName("OldFirstName");
        user.setLastName("OldLastName");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(originalEmail)).thenReturn(Optional.of(user));

        // When
        userService.updateUserProfile(username, dto);

        // Then
        assertEquals(originalEmail, user.getEmail());
        assertEquals("Dimitar", user.getFirstName());
        assertEquals("Peev", user.getLastName());
        assertEquals("www.image.com", user.getProfileImageUrl());
        assertEquals("+359888111222", user.getPhoneNumber());
        verify(userRepository, times(1)).save(user);
        verify(userRepository, times(1)).findByEmail(originalEmail);
    }

    // ---------- blockUser() ----------
    @Test
    void givenUserWithStatusUnblocked_whenSwitchStatus_thenUserStatusBecomeBlocked() {

        // Given
        defaultUser.setBlocked(false);
        when(userRepository.findById(defaultUser.getId())).thenReturn(Optional.of(defaultUser));

        // When
        userService.blockUser(defaultUser.getId());

        // Then
        assertTrue(defaultUser.isBlocked());
        verify(userRepository, times(1)).save(defaultUser);
    }

    // ---------- unblockUser() ----------
    @Test
    void givenUserWithStatusBlocked_whenSwitchStatus_thenUserStatusBecomeUnblocked() {

        // Given
        defaultUser.setBlocked(true);
        when(userRepository.findById(defaultUser.getId())).thenReturn(Optional.of(defaultUser));

        // When
        userService.unblockUser(defaultUser.getId());

        // Then
        assertFalse(defaultUser.isBlocked());
        verify(userRepository, times(1)).save(defaultUser);
    }

    // ---------- getById() ----------
    @Test
    void givenId_whenFound_thenReturnUser() {

        // Given
        UUID userId = defaultUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(defaultUser));

        // When
        User result = userService.getById(userId);

        // Then
        assertEquals(defaultUser, result);
    }

    @Test
    void givenId_whenNotFound_thenThrowUserNotFoundException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.getById(userId));
    }

    // ---------- getByUsername() ----------
    @Test
    void givenUsername_whenFound_thenReturnUser() {
        // Given
        String username = defaultUser.getUsername();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(defaultUser));

        // When
        User result = userService.getByUsername(username);

        // Then
        assertEquals(defaultUser, result);
    }

    @Test
    void givenUsername_whenNotFound_thenThrowUserNotFoundException() {

        // Given
        String username = "missing";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.getByUsername(username));
    }

    // ---------- loadUserByUsername() ----------
    // Test 1: When user exists - then return new AuthenticationMetadata
    @Test
    void givenExistingUser_whenLoadUserByUsername_thenReturnCorrectAuthenticationMetadata() {

        // Given
        String username = "Dimitar";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword("1234567890");
        user.setRole(Role.USER);
        user.setBlocked(false);
        user.setProfileImageUrl("www.image.com");

        when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(user));

        // When
        UserDetails authenticationMetadata = userService.loadUserByUsername(username);

        // Then
        assertInstanceOf(AuthenticationMetadata.class, authenticationMetadata);
        AuthenticationMetadata result = (AuthenticationMetadata) authenticationMetadata;
        assertEquals(user.getId(), result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(user.getPassword(), result.getPassword());
        assertEquals(user.getRole(), result.getRole());
        assertEquals(user.isBlocked(), result.isBlocked());
        assertEquals(user.getProfileImageUrl(), result.getProfileImageUrl());
        assertThat(result.getAuthorities()).hasSize(1);
        assertEquals("ROLE_USER", result.getAuthorities().iterator().next().getAuthority());
    }

    // Test 2: When User does not exist - then throws exception
    @Test
    void givenMissingUserFromDatabase_whenLoadUserByUsername_thenExceptionIsThrown() {

        // Given
        String username = "missing";
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception =
                assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(username));
        assertTrue(exception.getMessage().contains("User not found: " + username));

    }

    // ---------- toggleNotifications() ----------
    @Test
    void givenUserWithDisabledNotifications_whenToggleNotifications_thenUserNotificationsIsEnabled() {

        // Given
        User user = new User();
        user.setNotificationsEnabled(false);

        // When
        userService.toggleNotifications(user);

        // Then
        assertTrue(user.isNotificationsEnabled());
        verify(userRepository, times(1)).save(user);
    }
}
