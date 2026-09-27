package com.example.ems.user.role;

import com.example.ems.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImp implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role getEmployeeRole() {
        return roleRepository.findByName("ROLE_EMPLOYEE")
        .orElseThrow(() ->
        new ResourceNotFoundException("ROLE_EMPLOYEE not found"));
    }

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
        .orElseThrow(() ->
        new ResourceNotFoundException("Role not found: " + name));
    }
}