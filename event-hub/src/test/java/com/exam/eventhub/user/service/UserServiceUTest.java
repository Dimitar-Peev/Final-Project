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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
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

    @Captor
    private ArgumentCaptor<List<User>> listUserCaptor;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private UserService userService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new User("testUser", "test@exam.com", "encodedPass",
                "Test", "User", "+359888111222", Role.USER);
        defaultUser.setId(UUID.randomUUID());
    }

    @Test
    void givenRepository_whenRepositoryIsEmpty_thenInitializeUsers() {

        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        userService.initData();

        verify(userRepository).saveAll(listUserCaptor.capture());

        List<User> savedUsers = listUserCaptor.getValue();
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

        when(userRepository.count()).thenReturn(5L);

        userService.initData();

        verify(userRepository).count();
        verify(userRepository, never()).saveAll(anyList());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void givenExistingUsersInDatabase_whenGetAllUsers_thenReturnThemAll() {

        List<User> userList = List.of(defaultUser, new User());
        when(userRepository.findAll()).thenReturn(userList);

        List<User> result = userService.getAll();

        assertEquals(2, result.size());
        assertEquals(defaultUser, result.get(0));
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void givenNonExistingUsername_whenRegister_thenSaveToDatabase() {

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

        User saved = userService.register(registerRequest);

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

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("new@exam.com");
        request.setPassword("12345");
        request.setRole(Role.USER);

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = userService.register(request);

        assertNotNull(saved);
        assertEquals(request.getUsername(), saved.getUsername());
        assertEquals(Role.USER, saved.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenNewUser_whenUsernameExists_thenThrowUsernameAlreadyExistsException() {

        RegisterRequest validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("newUser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setConfirmPassword("password123");
        validRegisterRequest.setRole(Role.USER);

        User existingUser = new User();
        existingUser.setUsername(validRegisterRequest.getUsername());

        when(userRepository.findByUsername(validRegisterRequest.getUsername())).thenReturn(Optional.of(existingUser));

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

        EmailAlreadyExistsException exception =
                assertThrows(EmailAlreadyExistsException.class, () -> userService.register(validRegisterRequest));
        assertTrue(exception.getMessage().contains(EMAIL_EXIST));

        verify(userRepository).findByUsername(validRegisterRequest.getUsername());
        verify(userRepository).findByEmail(validRegisterRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void givenMissingUserFromDatabase_whenUpdateUserProfile_thenExceptionIsThrown() {

        String username = "missing";
        UserEditRequest dto = new UserEditRequest();
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUserProfile(username, dto));
    }

    @Test
    void givenExistingUser_whenUpdateUserProfileWithActualEmail_thenChangeTheirProfileAndSaveToDatabase() {

        String username = "existing";

        UserEditRequest dto = new UserEditRequest();
        dto.setUsername("testUser");
        dto.setFirstName("Dimitar");
        dto.setLastName("Peev");
        dto.setEmail("peev@test.bg");
        dto.setProfileImageUrl("www.image.com");
        dto.setPhoneNumber("+359888111222");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(defaultUser));

        userService.updateUserProfile(username, dto);

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

        EmailDuplicateException exception =
                assertThrows(EmailDuplicateException.class, () -> userService.updateUserProfile(username, dto));
        assertTrue(exception.getMessage().contains(EMAIL_EXIST));
    }

    @Test
    void givenExistingUser_whenUpdateUserProfileWithSameEmail_thenChangeOtherDetailsAndSaveToDatabase() {

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

        userService.updateUserProfile(username, dto);

        assertEquals(originalEmail, user.getEmail());
        assertEquals("Dimitar", user.getFirstName());
        assertEquals("Peev", user.getLastName());
        assertEquals("www.image.com", user.getProfileImageUrl());
        assertEquals("+359888111222", user.getPhoneNumber());
        verify(userRepository, times(1)).save(user);
        verify(userRepository, times(1)).findByEmail(originalEmail);
    }

    @Test
    void givenUserWithStatusUnblocked_whenSwitchStatus_thenUserStatusBecomeBlocked() throws NoSuchFieldException, IllegalAccessException {

        defaultUser.setBlocked(false);
        when(userRepository.findById(defaultUser.getId())).thenReturn(Optional.of(defaultUser));

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        Field jdbcField = UserService.class.getDeclaredField("jdbcTemplate");
        jdbcField.setAccessible(true);
        jdbcField.set(userService, jdbcTemplate);

        userService.blockUser(defaultUser.getId());

        assertTrue(defaultUser.isBlocked());
        verify(userRepository, times(1)).save(defaultUser);
    }

    @Test
    void givenUserWithStatusBlocked_whenSwitchStatus_thenUserStatusBecomeUnblocked() {

        defaultUser.setBlocked(true);
        when(userRepository.findById(defaultUser.getId())).thenReturn(Optional.of(defaultUser));

        userService.unblockUser(defaultUser.getId());

        assertFalse(defaultUser.isBlocked());
        verify(userRepository, times(1)).save(defaultUser);
    }

    @Test
    void givenId_whenFound_thenReturnUser() {

        UUID userId = defaultUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(defaultUser));

        User result = userService.getById(userId);

        assertEquals(defaultUser, result);
    }

    @Test
    void givenId_whenNotFound_thenThrowUserNotFoundException() {

        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(userId));
    }

    @Test
    void givenUsername_whenFound_thenReturnUser() {

        String username = defaultUser.getUsername();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(defaultUser));

        User result = userService.getByUsername(username);

        assertEquals(defaultUser, result);
    }

    @Test
    void givenUsername_whenNotFound_thenThrowUserNotFoundException() {

        String username = "missing";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getByUsername(username));
    }

    @Test
    void testHasRole_returnsTrue() {
        when(userRepository.findByUsername(defaultUser.getUsername())).thenReturn(Optional.of(defaultUser));
        assertTrue(userService.hasRole(defaultUser.getUsername(), Role.USER));
    }

    @Test
    void testHasRole_returnsFalse_whenRoleIsDifferent() {
        when(userRepository.findByUsername(defaultUser.getUsername())).thenReturn(Optional.of(defaultUser));
        assertFalse(userService.hasRole(defaultUser.getUsername(), Role.ADMIN));
    }

    @Test
    void testHasRole_returnsFalse_whenUserMissing() {

        String username = "missing";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertFalse(userService.hasRole(username, Role.ADMIN));
    }

    @Test
    void updateUser_shouldUpdateAllFieldsAndSave() {

        UUID id = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setUsername("oldUsername");
        existingUser.setEmail("old@mail.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("User");
        existingUser.setPhoneNumber("0000000000");
        existingUser.setRole(Role.USER);

        User updatedUser = new User();
        updatedUser.setUsername("newUsername");
        updatedUser.setEmail("new@mail.com");
        updatedUser.setFirstName("New");
        updatedUser.setLastName("Name");
        updatedUser.setPhoneNumber("0888123456");
        updatedUser.setRole(Role.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        userService.updateUser(id, updatedUser);

        assertEquals("newUsername", existingUser.getUsername());
        assertEquals("new@mail.com", existingUser.getEmail());
        assertEquals("New", existingUser.getFirstName());
        assertEquals("Name", existingUser.getLastName());
        assertEquals("0888123456", existingUser.getPhoneNumber());
        assertEquals(Role.ADMIN, existingUser.getRole());

        verify(userRepository).save(existingUser);
    }

    @Test
    void givenUserWithDisabledNotifications_whenToggleNotifications_thenUserNotificationsIsEnabled() {

        User user = new User();
        user.setNotificationsEnabled(false);

        userService.toggleNotifications(user);

        assertTrue(user.isNotificationsEnabled());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenExistingUser_whenLoadUserByUsername_thenReturnCorrectAuthenticationMetadata() {

        String username = "Dimitar";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword("1234567890");
        user.setRole(Role.USER);
        user.setBlocked(false);
        user.setProfileImageUrl("www.image.com");

        when(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Optional.of(user));

        UserDetails authenticationMetadata = userService.loadUserByUsername(username);

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

    @Test
    void givenMissingUserFromDatabase_whenLoadUserByUsername_thenExceptionIsThrown() {

        String username = "missing";
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException exception =
                assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(username));
        assertTrue(exception.getMessage().contains("User not found: " + username));

    }
}
