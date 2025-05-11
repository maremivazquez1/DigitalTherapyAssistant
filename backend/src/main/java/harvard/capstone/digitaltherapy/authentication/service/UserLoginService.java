package harvard.capstone.digitaltherapy.authentication.service;

import harvard.capstone.digitaltherapy.authentication.model.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserLoginService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoderService passwordEncoderService;

    public boolean authenticateUser(String username, String rawPassword) {
        try {
            // Fetch user from database using EntityManager
            TypedQuery<Users> query = entityManager.createQuery("SELECT u FROM Users u WHERE u.email = :username", Users.class);
            query.setParameter("username", username);
            Users user = query.getSingleResult();
            if (user != null) {
                String storedHashedPassword = user.getPassword();
                // Compare raw password with stored hashed password
                return passwordEncoderService.matches(rawPassword, storedHashedPassword);
            }
        } catch (Exception e) {
            System.out.println("User not found or error occurred: " + e.getMessage());
        }
        return false;
    }

}
