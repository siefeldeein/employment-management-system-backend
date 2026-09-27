package com.example.ems.auth;

import com.example.ems.auth.dto.AuthResponse;
import com.example.ems.auth.dto.LogInRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    AuthResponse login(LogInRequest request);

    String generateToken(UserDetails userDetails);

//    AuthResponse register(RegisterRequest registerRequest);

}
