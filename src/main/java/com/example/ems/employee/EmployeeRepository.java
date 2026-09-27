package com.example.ems.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByFirstNameIgnoreCase(String fname);

    @Query("SELECT DISTINCT e FROM Employee e " +
    "LEFT JOIN FETCH e.department " +
    "WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
    "   OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Employee> searchByNameWithDepartment(@Param("name") String name);

    @Query("SELECT DISTINCT e FROM Employee e " +
            "LEFT JOIN FETCH e.department d " +
            "WHERE LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<Employee> findByFullNameContainingWithDepartment(@Param("fullName") String fullName);


//    @Query("SELECT DISTINCT e FROM Employee e " +
//            "LEFT JOIN FETCH e.department d " +
//            "WHERE LOWER(e.fullName) = LOWER(:fullName)")
//    Optional<Employee> findByFullNameWithDepartment(@Param("fullName") String fullName);

}