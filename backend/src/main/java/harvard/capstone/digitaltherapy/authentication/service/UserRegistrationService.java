package harvard.capstone.digitaltherapy.authentication.service;

import harvard.capstone.digitaltherapy.authentication.exception.UserAlreadyExistsException;
import harvard.capstone.digitaltherapy.authentication.model.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoderService passwordEncoderService;

    @Transactional
    public Users registerUser(Users user) {
        // Validate for duplicate email
        if (isEmailExists(user.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + user.getEmail());
        }
        String hashedPassword = passwordEncoderService.encodePassword(user.getPassword());
        user.setPassword(hashedPassword);
        entityManager.persist(user); // Directly persist user
        return user;
    }

    private boolean isEmailExists(String email) {
        try {
            Long count = entityManager
                    .createQuery("SELECT COUNT(u) FROM Users u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        } catch (NoResultException e) {
            return false;
        }
    }
}
