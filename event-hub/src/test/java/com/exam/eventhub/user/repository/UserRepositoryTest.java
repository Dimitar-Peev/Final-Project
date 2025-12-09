package com.exam.eventhub.user.repository;

import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String testUserName;

    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser = new User(
                "testUser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                "1234567890",
                Role.USER);
        testUserName = testUser.getUsername();

        testUser2 = new User(
                "Dimitar",
                "mitko@test.com",
                "password123",
                "Dimitar",
                "Peev",
                "0987654321", Role.USER);
    }

    @Test
    void saveUser_returnSaved() {

        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertEquals(testUser.getUsername(), saved.getUsername());
    }

    @Test
    void getAll_returnMoreThanOneUser() {

        userRepository.save(testUser);
        userRepository.save(testUser2);

        List<User> userList = userRepository.findAll();

        assertNotNull(userList);
        assertEquals(2, userList.size());
    }

    @Test
    void shouldSaveAndFindUserById() {

        User savedUser = userRepository.save(testUser);

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(testUserName);
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void shouldFindUserByUsername() {

        userRepository.save(testUser);

        Optional<User> foundUser = userRepository.findByUsername(testUserName);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {

        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {

        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenUsernameOrEmailNotFound() {

        Optional<User> foundUser = userRepository.findByUsernameOrEmail("nonexistent", "nonexistent@example.com");

        assertThat(foundUser).isEmpty();
    }
}