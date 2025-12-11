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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Slf4j
@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private static final String ENTITY_NAME = "User";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public void initData() {
        if (userRepository.count() == 0) {
            initializeUsers();
        }
    }

    private void initializeUsers() {
        log.info("Initializing users...");
        List<User> defaultUsers = new ArrayList<>();

        User admin = new User("admin", "admin@eventhub.com",
                passwordEncoder.encode("admin123"), "Admin", "User",
                "+359888123456", Role.ADMIN);
        admin.setProfileImageUrl("https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001877.png");

        User organizer = new User("organizer", "organizer1@eventhub.com",
                passwordEncoder.encode("org123"), "John", "Organizer",
                "+359888234567", Role.EVENT_ORGANIZER);
        organizer.setProfileImageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTT1L6cWADa5B3WbXiuw_UPN4Qqv9cGe0uGjg&s");

        User user = new User("user", "user1@eventhub.com",
                passwordEncoder.encode("user123"), "Alice", "Johnson",
                "+359888456789", Role.USER);
        user.setProfileImageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR8ZCQyuw7OApiZwQKCieLNBsPXahE2m5-o6A&s");

        defaultUsers.add(admin);
        defaultUsers.add(organizer);
        defaultUsers.add(user);

        this.userRepository.saveAll(defaultUsers);

        log.info("Users initialized!");
    }

    @Cacheable("users")
    public List<User> getAll() {
        return this.userRepository.findAll();
    }

    @CacheEvict(value = "users", allEntries = true)
    public User register(RegisterRequest registerRequest) {
        String name = registerRequest.getUsername();
        log.info("Creating user: {}", name);

        Optional<User> byUsername = userRepository.findByUsername(registerRequest.getUsername());
        if (byUsername.isPresent()) {
            throw new UsernameAlreadyExistsException(USERNAME_EXIST.formatted(name));
        }

        Optional<User> byEmail = userRepository.findByEmail(registerRequest.getEmail());
        if (byEmail.isPresent()) {
            throw new EmailAlreadyExistsException(EMAIL_EXIST);
        }

        User user = create(registerRequest);

        User saved = userRepository.save(user);

        log.info("User [{}] (ID: [{}]) was successfully added.", saved.getUsername(), saved.getId());

        return saved;
    }

    private User create(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole());
        user.setBlocked(false);
        return user;
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateUserProfile(String username, UserEditRequest updatedData) {
        User user = getByUsername(username);

        boolean emailChanged = !user.getEmail().equals(updatedData.getEmail());
        Optional<User> userByEmail = userRepository.findByEmail(updatedData.getEmail());

        if (emailChanged && userByEmail.isPresent()) {
            throw new EmailDuplicateException(EMAIL_EXIST);
        }

        user.setEmail(updatedData.getEmail());
        user.setFirstName(updatedData.getFirstName());
        user.setLastName(updatedData.getLastName());
        user.setPhoneNumber(updatedData.getPhoneNumber());
        user.setProfileImageUrl(updatedData.getProfileImageUrl());

        this.userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void blockUser(UUID id) {
        User user = getById(id);
        user.setBlocked(true);
        this.userRepository.save(user);

        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", user.getUsername());
        log.info("Deleted remember-me tokens for blocked user {}", user.getUsername());
    }

    @CacheEvict(value = "users", allEntries = true)
    public void unblockUser(UUID id) {
        User user = getById(id);
        user.setBlocked(false);
        this.userRepository.save(user);
    }

    public User getById(UUID id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(ID_NOT_FOUND.formatted(ENTITY_NAME, id)));
    }

    public User getByUsername(String name) {
        return this.userRepository.findByUsername(name)
                .orElseThrow(() -> new UserNotFoundException("User with username [%s] was not found.".formatted(name)));
    }

    public User getByUsernameOrEmail(String usernameOrEmail) {
        return this.userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    public boolean hasRole(String username, Role role) {
        return this.userRepository.findByUsername(username)
                .map(u -> u.getRole() == role)
                .orElse(false);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @CacheEvict(value = "users", allEntries = true)
    public void updateUser(UUID id, User updatedUser) {
        User user = getById(id);

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setPhoneNumber(updatedUser.getPhoneNumber());
        user.setRole(updatedUser.getRole());

        this.userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public boolean toggleNotifications(User user) {
        user.setNotificationsEnabled(!user.isNotificationsEnabled());
        this.userRepository.save(user);
        return user.isNotificationsEnabled();
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        User user = getByUsernameOrEmail(usernameOrEmail);

        return new AuthenticationMetadata(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.isBlocked(), user.getProfileImageUrl());
    }
}
