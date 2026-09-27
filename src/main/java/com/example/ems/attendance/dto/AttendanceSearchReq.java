package com.example.ems.attendance.dto;

import com.example.ems.attendance.enums.AttendanceStatus;

import java.time.LocalDate;

public record AttendanceSearchReq(
        Long employeeId,
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate,
        AttendanceStatus status
) { }
