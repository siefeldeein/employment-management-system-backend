package com.example.ems.attendance.specification;

import com.example.ems.attendance.Attendance;
import com.example.ems.attendance.enums.AttendanceStatus;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

@NoArgsConstructor
public class AttendanceSpecification {

    public static Specification<Attendance> hasEmployeeId(Long employeeId){
        return (root, query, cb) ->

                cb.equal( root.get("employee").get("id"), employeeId );
    }

    public static Specification<Attendance> hasStatus(AttendanceStatus status){
        return (root, query, cb) ->
                cb.equal( root.get("status"), status );
    }

    public static Specification<Attendance> dateBetween(LocalDate startDate, LocalDate endDate){
        return (root, query, cb) ->
                cb.between( root.get("date"), startDate, endDate );
    }

    public static Specification<Attendance> hasDepartmentId(Long departmentId) {
        return (root, query, cb) ->
                cb.equal( root.get("employee").get("department").get("id"), departmentId );
    }
}

