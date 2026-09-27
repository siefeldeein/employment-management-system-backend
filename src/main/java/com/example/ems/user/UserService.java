package com.example.ems.user;

import com.example.ems.employee.Employee;
import com.example.ems.user.role.Role;

public interface UserService {

    User createUser(String username, String rawPassword, Role role, Employee employee);

    boolean existsByUsername(String username);

    User getByUsername(String username);

    void linkEmployee(User user, Employee employee);
}