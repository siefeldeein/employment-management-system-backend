package com.example.ems.attendance;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.dto.AttendanceSearchReq;
import com.example.ems.attendance.enums.AttendanceStatus;
import com.example.ems.attendance.mapper.AttendanceMapper;
import com.example.ems.attendance.specification.AttendanceSpecification;
import com.example.ems.common.exception.InvalidInputException;
import com.example.ems.employee.Employee;
import com.example.ems.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImp implements AttendanceService{

    private static final LocalTime LATE_CUTOFF = LocalTime.of(9, 0);
    private static final double HALF_DAY_MIN_HOURS = 4.0;

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final EmployeeRepository employeeRepository;

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

    @Override
    @Transactional
    public AttendanceResponse checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInputException("Employee not found"));

        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndDate(employeeId, today)
                .orElseGet(() -> {
                    Attendance newAttendance = new Attendance();
                    newAttendance.setEmployee(employee);
                    newAttendance.setDate(today);
                    return newAttendance;
                });

        if (attendance.getCheckIn() != null) {
            throw new InvalidInputException("Employee already checked in today");
        }

        attendance.setCheckIn(LocalTime.now());
        attendance.setCheckOut(null);
        attendance.setTotalHours(null);
        attendance.setStatus(AttendanceStatus.INCOMPLETE);

        return attendanceMapper.toDtoResponse(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new InvalidInputException("Employee not found");
        }

        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new InvalidInputException("Employee has not checked in today"));

        if (attendance.getCheckIn() == null) {
            throw new InvalidInputException("Employee has not checked in today");
        }

        LocalTime checkOut = LocalTime.now();
        attendance.setCheckOut(checkOut);
        double totalHours = Math.round(
                Duration.between(attendance.getCheckIn(), checkOut).toMinutes() / 60.0 * 100.0) / 100.0;
        attendance.setTotalHours(totalHours);
        attendance.setStatus(computeStatus(attendance.getCheckIn(), checkOut, totalHours));

        return attendanceMapper.toDtoResponse(attendanceRepository.save(attendance));
    }

    private AttendanceStatus computeStatus(LocalTime checkIn, LocalTime checkOut, double totalHours) {
        if (totalHours < HALF_DAY_MIN_HOURS) {
            return AttendanceStatus.HALF_DAY;
        }
        if (checkIn.isAfter(LATE_CUTOFF)) {
            return AttendanceStatus.LATE;
        }
        return AttendanceStatus.PRESENT;
    }
}
