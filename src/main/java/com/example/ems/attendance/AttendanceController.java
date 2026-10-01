package com.example.ems.attendance;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.dto.AttendanceSearchReq;
import com.example.ems.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance")
// Security model:
// - ADMIN/MANAGER: everything (search any employee, clock anyone)
// - EMPLOYEE: only their own attendance, via GET /me and self-scoped check-in/out
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 1- SEARCH (ADMIN/MANAGER only)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/search")
    public ResponseEntity<Page<AttendanceResponse>> searchAttendance(
            @RequestBody(required = false) AttendanceSearchReq req, Pageable pageable){

        return ResponseEntity.ok(attendanceService.searchAttendance(req, pageable));
    }

    // 2- MY OWN ATTENDANCE (the employee's only read endpoint)
    // employeeId is never accepted from the client - it is always taken from the token,
    // so an employee can never widen the scope by tampering with the request.
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me")
    public ResponseEntity<Page<AttendanceResponse>> myAttendance(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable){

        AttendanceSearchReq req = new AttendanceSearchReq(
                user.getEmployee().getId(), null, startDate, endDate, null);
        return ResponseEntity.ok(attendanceService.searchAttendance(req, pageable));
    }

    // 3- CHECK IN / OUT
    // ADMIN/MANAGER may clock anyone; an EMPLOYEE only when employeeId is their own.
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or " +
            "hasRole('EMPLOYEE') and authentication.principal.employee != null " +
            "and #employeeId == authentication.principal.employee.id")
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@RequestParam Long employeeId){
        return ResponseEntity.ok(attendanceService.checkIn(employeeId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or " +
            "hasRole('EMPLOYEE') and authentication.principal.employee != null " +
            "and #employeeId == authentication.principal.employee.id")
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(@RequestParam Long employeeId){
        return ResponseEntity.ok(attendanceService.checkOut(employeeId));
    }
}