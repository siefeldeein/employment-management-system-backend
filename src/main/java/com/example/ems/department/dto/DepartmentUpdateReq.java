package com.example.ems.department.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentUpdateReq {

    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String name;

    @Size(max = 255,message = "Must not exceeds 255 characters")
    private String description;
}
