package com.example.ems.attendance;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.dto.AttendanceSearchReq;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/search")
    public ResponseEntity<Page<AttendanceResponse>> searchAttendance(
            @RequestBody AttendanceSearchReq req, Pageable pageable){

        return ResponseEntity.ok(attendanceService.searchAttendance(req, pageable));
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@RequestParam Long employeeId){
        return ResponseEntity.ok(attendanceService.checkIn(employeeId));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(@RequestParam Long employeeId){
        return ResponseEntity.ok(attendanceService.checkOut(employeeId));
    }
}
