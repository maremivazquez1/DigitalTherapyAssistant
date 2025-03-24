package harvard.capstone.digitaltherapy.authentication.service;

import harvard.capstone.digitaltherapy.authentication.model.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserLoginServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private PasswordEncoderService passwordEncoderService;

    @Mock
    private TypedQuery<Users> typedQuery;

    @InjectMocks
    private UserLoginService userLoginService;

    private static final String TEST_USERNAME = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @BeforeEach
    void setUp() {
        when(entityManager.createQuery(any(String.class), eq(Users.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("username"), any())).thenReturn(typedQuery);
    }

    @Test
    void authenticateUser_ValidCredentials_ReturnsTrue() {
        // Arrange
        Users mockUser = new Users();
        mockUser.setEmail(TEST_USERNAME);
        mockUser.setPassword(HASHED_PASSWORD);

        when(typedQuery.getSingleResult()).thenReturn(mockUser);
        when(passwordEncoderService.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        // Act
        boolean result = userLoginService.authenticateUser(TEST_USERNAME, TEST_PASSWORD);

        // Assert
        assertTrue(result);
    }

    @Test
    void authenticateUser_InvalidPassword_ReturnsFalse() {
        // Arrange
        Users mockUser = new Users();
        mockUser.setEmail(TEST_USERNAME);
        mockUser.setPassword(HASHED_PASSWORD);

        when(typedQuery.getSingleResult()).thenReturn(mockUser);
        when(passwordEncoderService.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        // Act
        boolean result = userLoginService.authenticateUser(TEST_USERNAME, TEST_PASSWORD);

        // Assert
        assertFalse(result);
    }

    @Test
    void authenticateUser_UserNotFound_ReturnsFalse() {
        // Arrange
        when(typedQuery.getSingleResult()).thenThrow(new jakarta.persistence.NoResultException());

        // Act
        boolean result = userLoginService.authenticateUser(TEST_USERNAME, TEST_PASSWORD);

        // Assert
        assertFalse(result);
    }
}
