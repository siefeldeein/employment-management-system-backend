package com.example.ems.department.mapper;

import com.example.ems.department.Department;
import com.example.ems.department.dto.DepartmentCreateReq;
import com.example.ems.department.dto.DepartmentDetailsResponse;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateReq;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)

public interface DepartmentMapper {

    Department toEntity(DepartmentCreateReq req);

    void updateDepartment(DepartmentUpdateReq updateReq, @MappingTarget Department department);

    DepartmentResponse toDtoResponse(Department department);

    DepartmentDetailsResponse toDtoResponseDetails(Department department);
}