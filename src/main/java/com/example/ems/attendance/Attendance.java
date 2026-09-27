package com.example.ems.attendance;

import com.example.ems.employee.Employee;
import com.example.ems.attendance.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Entity
@Table(name = "attendances",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"employee_id", "date"})
        })
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_hours")
    private Double totalHours;

    @Enumerated(EnumType.STRING) // ← This maps PRESENT → 'PRESENT' in DB
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "employee_id",
        nullable = false
    )
    private Employee employee;




}
