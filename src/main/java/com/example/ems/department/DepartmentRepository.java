package com.example.ems.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface DepartmentRepository extends JpaRepository<Department, Long> {

    public Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
    
    @Query("SELECT DISTINCT d FROM Department d " +
            "LEFT JOIN FETCH d.employeeList e " +
            "WHERE d.id = :id")
    Optional<Department> findWithEmployeesById(@Param("id") Long id);


//    boolean existsById(Long id);

}