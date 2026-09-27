package com.example.ems.employee;

import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // 1- CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(@Valid @RequestBody EmployeeCreateRequest request){
        return employeeService.createEmployee(request);
    }

    // 2- GET ALL
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(){
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // 3- GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id){
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // 4- Pagination
    @GetMapping("/paged")
    public ResponseEntity<Page<EmployeeResponse>> getEmployeesPaged(Pageable pageable){
        return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
    }

    // 5- SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> searchEmployees(@RequestParam String name){
        return ResponseEntity.ok(employeeService.getEmployeesByFullNameContaining(name));
    }

    // 6- UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request){
        return ResponseEntity.ok(employeeService.updateEmployee(id,request));
    }

    // 7- DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id){
        employeeService.deleteEmployee(id);
    }
}
