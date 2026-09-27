package com.example.ems.department;

import com.example.ems.employee.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter @Setter
@AllArgsConstructor
@Entity
@Table(name = "departments")
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    
    @Column(name = "description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "department",
        fetch = FetchType.LAZY
    )
    private List<Employee> employeeList = new ArrayList<>();



}
