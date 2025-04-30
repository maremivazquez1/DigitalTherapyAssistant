package harvard.capstone.digitaltherapy.authentication.controller;

import harvard.capstone.digitaltherapy.authentication.model.ApiResponse;
import harvard.capstone.digitaltherapy.authentication.model.LoginRequest;
import harvard.capstone.digitaltherapy.authentication.service.UserLoginService;
import harvard.capstone.digitaltherapy.authentication.service.JwtTokenProvider;
import harvard.capstone.digitaltherapy.authentication.service.TokenService;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        boolean isAuthenticated = loginService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
        if (isAuthenticated) {
            // Make JWT token, store in Redis, and send with response
            String token = jwtTokenProvider.createToken(loginRequest.getUsername());
            tokenService.storeToken(token, loginRequest.getUsername(), JwtTokenProvider.TOKEN_EXPIRATION_MINUTES);
            ApiResponse response = new ApiResponse("success", "Login successful!", token);
            return ResponseEntity.ok(response);
        } else {
            ApiResponse response = new ApiResponse("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
