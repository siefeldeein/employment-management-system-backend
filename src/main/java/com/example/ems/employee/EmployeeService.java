package com.example.ems.employee;

import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {

    // Create
    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    // Read
    EmployeeResponse getEmployeeById(Long id);
    Employee getEmployeeByEmail(String email);
    List<EmployeeResponse> getAllEmployees();
    List<EmployeeResponse> getEmployeesByFullNameContaining(String name);

    // Update
    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    // Delete
    void deleteEmployee(Long id);

    Page<EmployeeResponse> getAllEmployees(Pageable pageable);
}
