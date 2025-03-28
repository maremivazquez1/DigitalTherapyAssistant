package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.model.ApiResponse;
import harvard.capstone.digitaltherapy.authentication.model.Users;
import harvard.capstone.digitaltherapy.authentication.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationControllerTest {

    @Mock
    private UserRegistrationService userService;

    @InjectMocks
    private UserRegistrationController registrationController;

    private Users validUser;

    @BeforeEach
    void setUp() {
        validUser = new Users();
        validUser.setEmail("test@example.com");
        validUser.setPassword("Password123!");
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setPhone("1234567890");
    }

    @Test
    void registerUser_WithValidRequest_ReturnsSuccessResponse() {
        // Act
        ResponseEntity<ApiResponse> response = registrationController.registerUser(validUser);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().getStatus());
        assertEquals("User registered successfully!", response.getBody().getMessage());
        verify(userService).registerUser(validUser);
    }

    @Test
    void registerUser_WithNullRequest_ThrowsException() {
        // Arrange
        doThrow(new IllegalArgumentException()).when(userService).registerUser(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                registrationController.registerUser(null)
        );
        verify(userService).registerUser(null);
    }
}
