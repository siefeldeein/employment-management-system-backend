package com.example.ems.department.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDetailsResponse {
    private Long id;
    private String name;
    private String description;

    // Nested: Only lives here.
    private List<EmployeeBrief> employeeList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeBrief {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate hireDate;
        private BigDecimal salary;
    }
}