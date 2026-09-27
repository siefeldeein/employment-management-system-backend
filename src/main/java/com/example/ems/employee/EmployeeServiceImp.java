package com.example.ems.employee;

import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import com.example.ems.department.Department;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.department.DepartmentRepository;
import com.example.ems.employee.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeServiceImp implements EmployeeService {

    // 1- Dependencies
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final DepartmentRepository departmentRepository;

    // 2- Create
    @Override
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        // Check existence
        if(employeeRepository.existsByEmailIgnoreCase(request.getEmail().trim())){
            throw new DuplicateResourceException("Email already exists");
        }

        Employee employee = employeeMapper.toEntity(request);

        // Check Department
        Department department = departmentRepository.findByNameIgnoreCase(request.getDepartmentName().trim())
        .orElseThrow(()-> new ResourceNotFoundException("Department Not Found: " + request.getDepartmentName()));

        employee.setDepartment(department);
        Employee saved = employeeRepository.save(employee);

        return employeeMapper.toDtoResponse(saved);
    }

    // 3- Read
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Employee Not Found"));

        return employeeMapper.toDtoResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeByEmail(String email){
        Employee employee = employeeRepository.findByEmailIgnoreCase(email).orElseThrow(()-> new ResourceNotFoundException("Employee Not Found"));

        return employee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByFullNameContaining(String name) {
        List<Employee> employees = employeeRepository.findByFullNameContainingWithDepartment(name);

        return employees.stream().map(employeeMapper::toDtoResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream().map(employeeMapper::toDtoResponse).collect(Collectors.toList());
    }

    @Override
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        // page has already built-in map func.
        return employeeRepository.findAll(pageable).map(employeeMapper::toDtoResponse);
    }

    // 4- Update
    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        // Check existence
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Check that email is being edited and not already exists
        if (request.getEmail() != null &&
        !employee.getEmail().equalsIgnoreCase(request.getEmail()) &&
        employeeRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // check the Department Written right if it's edited
        Department department = null;
        if (request.getDepartmentName() != null) {
            department = departmentRepository.findByNameIgnoreCase(request.getDepartmentName())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        }

        employeeMapper.updateEmployee(request, employee);

        // Apply relationship updates
        if (department != null) {
            employee.setDepartment(department);
        }

        return employeeMapper.toDtoResponse(employee);
    }

    // 4- Delete
    @Override
    public void deleteEmployee(Long id) {
        Employee emp = employeeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        employeeRepository.delete(emp);

    }


}
