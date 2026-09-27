package com.example.ems.auth;

import com.example.ems.auth.dto.AuthResponse;
import com.example.ems.auth.dto.LogInRequest;
import com.example.ems.user.role.RoleRepository;
import com.example.ems.user.UserRepository;
import com.example.ems.security.CustomUserDetailsService;
import com.example.ems.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImp implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LogInRequest logInRequest) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(logInRequest.getUsername(), logInRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        String token = generateToken(userDetails);

        return new AuthResponse(token);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return jwtService.generateToken(userDetails);
    }

//    @Override
//    public AuthResponse register(RegisterRequest registerRequest) {
//
//        // 1- Check if user already exists
//        if (userRepository.existsByUsernameIgnoreCase(registerRequest.getUsername())){
//            throw new DuplicateResourceException("this username already exists");
//        }
//
//        // 2- Assign a Role
//        Role role = roleRepository.findByName("ROLE_EMPLOYEE")
//        .orElseThrow(()-> new ResourceNotFoundException("Role Not Found"));
//
//        // 3- Build user
//        User user = new User();
//
//        user.setUsername(registerRequest.getUsername());
//        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
//        user.setEnabled(true);
//        user.getRoles().add(role);
//
//        // link user to employee
//        // service
//        // 4- Save
//        userRepository.save(user);
//
//
//        String token = jwtService.generateToken(customUserDetailsService.loadUserByUsername(registerRequest.getUsername()));
//
//        return new AuthResponse(token);
//    }
}
