package com.example.ems.employee.mapper;

import com.example.ems.employee.Employee;
import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EmployeeMapper {

    @Mapping(target = "department", ignore = true)
    Employee toEntity(EmployeeCreateRequest req);

    @Mapping(target = "department", ignore = true)
    void updateEmployee(EmployeeUpdateRequest updateReq, @MappingTarget Employee emp);

    EmployeeResponse toDtoResponse(Employee emp);

}
