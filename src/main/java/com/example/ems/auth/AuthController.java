package com.example.ems.auth;

import com.example.ems.auth.dto.AuthResponse;
import com.example.ems.auth.dto.LogInRequest;
import com.example.ems.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRegistrationFacade userRegistrationFacade;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LogInRequest logInRequest){

        return ResponseEntity.ok(authService.login(logInRequest));
    }

   @PostMapping("/register")
   public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest){

       return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userRegistrationFacade.register(registerRequest));
   }
}
