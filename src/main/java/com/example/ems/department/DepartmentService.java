package com.example.ems.department;

import com.example.ems.department.dto.DepartmentCreateReq;
import com.example.ems.department.dto.DepartmentDetailsResponse;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DepartmentService {

    //Create
    DepartmentResponse createDepartment(DepartmentCreateReq createReq);

    //Read
    DepartmentResponse getDepartmentById(Long id);
    DepartmentDetailsResponse getDepartmentByIdWithEmployees(Long id);
    DepartmentResponse getDepartmentByName(String name);


    List<DepartmentResponse> getAllDepartments();
    Page<DepartmentResponse> getAllDepartments(Pageable pageable);

    //Update
    DepartmentResponse updateDepartment(Long id, DepartmentUpdateReq updateReq);

    void deleteDepartment(Long id);

}
