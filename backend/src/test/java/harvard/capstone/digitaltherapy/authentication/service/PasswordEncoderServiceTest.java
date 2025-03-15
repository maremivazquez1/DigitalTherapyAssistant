package harvard.capstone.digitaltherapy.authentication.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderServiceTest {

    private PasswordEncoderService passwordEncoderService;
    private static final String RAW_PASSWORD = "testPassword123";

    @BeforeEach
    void setUp() {
        passwordEncoderService = new PasswordEncoderService();
    }

    @Test
    void encode_ValidPassword_ReturnsHashedPassword() {
        // Act
        String hashedPassword = passwordEncoderService.encodePassword(RAW_PASSWORD);

        // Assert
        assertNotNull(hashedPassword);
        assertNotEquals(RAW_PASSWORD, hashedPassword);
        assertTrue(new BCryptPasswordEncoder().matches(RAW_PASSWORD, hashedPassword));
    }

    @Test
    void matches_CorrectPassword_ReturnsTrue() {
        // Arrange
        String hashedPassword = passwordEncoderService.encodePassword(RAW_PASSWORD);

        // Act
        boolean result = passwordEncoderService.matches(RAW_PASSWORD, hashedPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void matches_IncorrectPassword_ReturnsFalse() {
        // Arrange
        String hashedPassword = passwordEncoderService.encodePassword(RAW_PASSWORD);
        String wrongPassword = "wrongPassword123";

        // Act
        boolean result = passwordEncoderService.matches(wrongPassword, hashedPassword);

        // Assert
        assertFalse(result);
    }
}

