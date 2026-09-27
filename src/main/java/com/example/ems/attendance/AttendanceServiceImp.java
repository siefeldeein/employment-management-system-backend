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

@Service
@RequiredArgsConstructor
public class AttendanceServiceImp implements AttendanceService{

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    public Page<AttendanceResponse> searchAttendance(AttendanceSearchReq req, Pageable pageable){

        Specification<Attendance> spec = Specification.where((Specification<Attendance>) null);
        if(req.employeeId()!=null){
            spec = spec.and( AttendanceSpecification.hasEmployeeId(req.employeeId()) );
        }

        if(req.departmentId()!=null){
            spec = spec.and( AttendanceSpecification.hasDepartmentId(req.departmentId()) );
        }
        if(req.status()!=null){
            spec = spec.and( AttendanceSpecification.hasStatus(req.status()) );
        }
        if(req.startDate()!=null && req.endDate()!=null){
            spec = spec.and( AttendanceSpecification.dateBetween(req.startDate(), req.endDate()) );
        }

        return attendanceRepository.findAll(spec, pageable).map(attendanceMapper::toDtoResponse);
    }

}
