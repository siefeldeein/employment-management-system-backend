package com.example.ems.attendance.mapper;

import com.example.ems.attendance.Attendance;
import com.example.ems.attendance.dto.AttendanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttendanceMapper {

    @Mapping(target = "employeeId", source = "employee.id")
//    @Mapping(target = "employeeName",
//        expression = """
//            java(
//                attendance.getEmployee().getFirstName()
//                +" "+ attendance.getEmployee().getLastName()
//            )
//            """)
    //after adding method getfullname in employee entity
    @Mapping(target = "employeeName", source = "employee.fullName")
    AttendanceResponse toDtoResponse(Attendance attendance);

    List<AttendanceResponse> toResponseList(List<Attendance> attendanceList);

}
