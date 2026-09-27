package com.example.ems.auth;

import com.example.ems.auth.dto.AuthResponse;
import com.example.ems.auth.dto.RegisterRequest;
import com.example.ems.employee.Employee;
import com.example.ems.user.role.Role;
import com.example.ems.user.User;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.employee.EmployeeService;
import com.example.ems.user.role.RoleService;
import com.example.ems.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationFacade {

    private final UserService userService;
    private final RoleService roleService;
    private final EmployeeService employeeService;
    private final AuthService authService;

    public AuthResponse register(RegisterRequest request) {

        // 1- Check if user already exists
        if (userService.existsByUsername(request.getUsername())){
           throw new DuplicateResourceException("This username already exists");
        }

        // 2- load Employee
        Employee employee =
        employeeService.getEmployeeByEmail(request.getEmail());

        // 3- load Role
        Role role = roleService.getEmployeeRole();

        // 4- create user
        User user = userService.createUser(
        request.getUsername(),
        request.getPassword(),
        role,
        employee
        );

        return new AuthResponse(
        authService.generateToken(user)
        );
    }
}
