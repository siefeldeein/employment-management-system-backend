package com.example.ems.attendance.mapper;

import com.example.ems.attendance.Attendance;
import com.example.ems.attendance.dto.AttendanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttendanceMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.fullName")
    @Mapping(target = "checkInTime", expression = "java(attendance.getCheckIn() != null ? java.time.LocalDateTime.of(attendance.getDate(), attendance.getCheckIn()) : null)")
    @Mapping(target = "checkOutTime", expression = "java(attendance.getCheckOut() != null ? java.time.LocalDateTime.of(attendance.getDate(), attendance.getCheckOut()) : null)")
    AttendanceResponse toDtoResponse(Attendance attendance);
}