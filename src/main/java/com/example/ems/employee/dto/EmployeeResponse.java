package com.example.ems.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate hireDate;
    private BigDecimal salary;

    private DepartmentSummary department;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepartmentSummary{
        private Long id;
        private String name;

    }


}
