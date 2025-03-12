package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.model.ApiResponse;
import harvard.capstone.digitaltherapy.authentication.model.Users;
import harvard.capstone.digitaltherapy.authentication.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody Users user) {
        userService.registerUser(user);
        ApiResponse response = new ApiResponse(
                "success",
                "User registered successfully!"
        );
        return ResponseEntity.ok(response);
    }
}
