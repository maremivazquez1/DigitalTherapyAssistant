package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.model.ApiResponse;
import harvard.capstone.digitaltherapy.authentication.model.LoginRequest;
import harvard.capstone.digitaltherapy.authentication.service.UserLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

//@ExtendWith(MockitoExtension.class)
class UserLoginControllerTest {

    @Mock
    private UserLoginService loginService;

    @InjectMocks
    private UserLoginController userLoginController;

    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;

    @BeforeEach
    void setUp() {
        // Initialize test data
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("test@example.com");
        validLoginRequest.setPassword("validPassword123");

        invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsername("test@example.com");
        invalidLoginRequest.setPassword("wrongPassword");
    }

 /*   @Test
    void loginUser_WithValidCredentials_ReturnsSuccessResponse() {
        // Arrange
        when(loginService.authenticateUser(validLoginRequest.getUsername(),
                                        validLoginRequest.getPassword()))
            .thenReturn(true);

        // Act
        ResponseEntity<ApiResponse> response = userLoginController.loginUser(validLoginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().getStatus());
        assertEquals("Login successful!", response.getBody().getMessage());
    }

    @Test
    void loginUser_WithInvalidCredentials_ReturnsErrorResponse() {
        // Arrange
        when(loginService.authenticateUser(invalidLoginRequest.getUsername(),
                                        invalidLoginRequest.getPassword()))
            .thenReturn(false);

        // Act
        ResponseEntity<ApiResponse> response = userLoginController.loginUser(invalidLoginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("error", response.getBody().getStatus());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }*/

    @Test
    void loginUser_WithNullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            userLoginController.loginUser(null);
        });
    }

}

