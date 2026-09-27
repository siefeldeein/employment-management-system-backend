package com.example.ems.attendance;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.dto.AttendanceSearchReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceService {

    Page<AttendanceResponse> searchAttendance(AttendanceSearchReq req, Pageable pageable);


}
