package harvard.capstone.digitaltherapy.authentication.service;

import harvard.capstone.digitaltherapy.authentication.exception.UserAlreadyExistsException;
import harvard.capstone.digitaltherapy.authentication.model.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private PasswordEncoderService passwordEncoderService;

    @Mock
    private TypedQuery<Long> typedQuery;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    private Users testUser;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "hashedPassword123";

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword(TEST_PASSWORD);

        // Mock the query creation
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
    }

    @Test
    void registerUser_WithNewEmail_SuccessfullyRegisters() {
        // Arrange
        when(typedQuery.getSingleResult()).thenReturn(0L);
        when(passwordEncoderService.encodePassword(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

        // Act
        Users registeredUser = userRegistrationService.registerUser(testUser);

        // Assert
        assertNotNull(registeredUser);
        assertEquals(HASHED_PASSWORD, registeredUser.getPassword());
        verify(entityManager).persist(testUser);
        verify(passwordEncoderService).encodePassword(TEST_PASSWORD);
    }

    @Test
    void registerUser_WithExistingEmail_ThrowsException() {
        // Arrange
        when(typedQuery.getSingleResult()).thenReturn(1L);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userRegistrationService.registerUser(testUser);
        });

        verify(entityManager, never()).persist(any(Users.class));
        verify(passwordEncoderService, never()).encodePassword(anyString());
    }

    @Test
    void registerUser_WhenEmailCheckThrowsNoResultException_SuccessfullyRegisters() {
        // Arrange
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);
        when(passwordEncoderService.encodePassword(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

        // Act
        Users registeredUser = userRegistrationService.registerUser(testUser);

        // Assert
        assertNotNull(registeredUser);
        assertEquals(HASHED_PASSWORD, registeredUser.getPassword());
    }
}


