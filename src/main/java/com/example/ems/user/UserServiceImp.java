package com.example.ems.user;

import com.example.ems.employee.Employee;
import com.example.ems.user.role.Role;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(String username, String rawPassword, Role role, Employee employee) {

        if (existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);

        user.getRoles().add(role);
        user.setEmployee(employee);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
        .orElseThrow(() ->
        new ResourceNotFoundException("User not found"));
    }

    @Override
    public void linkEmployee(User user, Employee employee) {
        user.setEmployee(employee);
        userRepository.save(user);
    }
}