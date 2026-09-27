package com.example.ems.attendance.dto;

import java.time.LocalDateTime;

public record AttendanceUpdateReq(
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) { }
