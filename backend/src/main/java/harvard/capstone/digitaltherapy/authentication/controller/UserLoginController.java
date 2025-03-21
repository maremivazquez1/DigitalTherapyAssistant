package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.model.ApiResponse;
import harvard.capstone.digitaltherapy.authentication.model.LoginRequest;
import harvard.capstone.digitaltherapy.authentication.service.UserLoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserLoginController {
    @Autowired
    private UserLoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        boolean isAuthenticated = loginService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
        if (isAuthenticated) {
            ApiResponse response = new ApiResponse(
                    "success",
                    "Login successful!"
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse response = new ApiResponse(
                    "error",
                    "Invalid credentials"
            );
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

    }
}
