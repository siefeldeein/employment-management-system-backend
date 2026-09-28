package com.example.ems.attendance;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.dto.AttendanceSearchReq;
import com.example.ems.attendance.mapper.AttendanceMapper;
import com.example.ems.attendance.specification.AttendanceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImp implements AttendanceService{

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    // NOTE: readOnly tx keeps the Hibernate session open so the LAZY
    // Attendance.employee proxy can be loaded by the mapper below.
    // Without it the session closes after findAll() and the mapper
    // throws LazyInitializationException -> 500 "unexpected error".
    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> searchAttendance(AttendanceSearchReq req, Pageable pageable){

        // NOTE: build an empty filter list and combine with allOf().
        // Specification.where(null) is FORBIDDEN in Spring Data JPA 4
        // and throws "Specification must not be null".
        List<Specification<Attendance>> filters = new ArrayList<>();
        if(req.employeeId()!=null){
            filters.add(AttendanceSpecification.hasEmployeeId(req.employeeId()));
        }

        if(req.departmentId()!=null){
            filters.add(AttendanceSpecification.hasDepartmentId(req.departmentId()));
        }
        if(req.status()!=null){
            filters.add(AttendanceSpecification.hasStatus(req.status()));
        }
        if(req.startDate()!=null && req.endDate()!=null){
            filters.add(AttendanceSpecification.dateBetween(req.startDate(), req.endDate()));
        }

        Specification<Attendance> spec = Specification.allOf(filters);

        return attendanceRepository.findAll(spec, pageable).map(attendanceMapper::toDtoResponse);
    }

}
