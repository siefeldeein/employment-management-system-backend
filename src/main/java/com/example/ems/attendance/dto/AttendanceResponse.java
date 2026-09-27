package com.example.ems.attendance.dto;

import com.example.ems.attendance.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id, Long employeeId, String employeeName,
        LocalDate date, LocalDateTime checkInTime, LocalDateTime checkOutTime,
        AttendanceStatus status
) {}
