package com.example.ems.common;

import com.example.ems.attendance.Attendance;
import com.example.ems.attendance.AttendanceRepository;
import com.example.ems.attendance.enums.AttendanceStatus;
import com.example.ems.department.Department;
import com.example.ems.department.DepartmentRepository;
import com.example.ems.employee.Employee;
import com.example.ems.employee.EmployeeRepository;
import com.example.ems.user.role.Role;
import com.example.ems.user.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            List<Department> departments = seedRolesAndDepartments();
            List<Employee> employees = seedEmployees(departments);
            seedAttendance(employees);
            return;
        }
        // Roles already exist (existing/local DB): top up demo attendance
        // when the table is empty so the Attendance page has data to show.
        if (attendanceRepository.count() == 0) {
            List<Employee> employees = employeeRepository.findAll();
            if (!employees.isEmpty()) {
                seedAttendance(employees);
            }
        }
    }

    private List<Department> seedRolesAndDepartments() {
        roleRepository.saveAll(List.of(
                role("ROLE_ADMIN"),
                role("ROLE_MANAGER"),
                role("ROLE_EMPLOYEE")
        ));

        Department engineering = department("Engineering", "Software development team");
        Department hr = department("HR", "Human Resources");
        Department finance = department("Finance", "Accounting and finance operations");
        departmentRepository.saveAll(List.of(engineering, hr, finance));
        return List.of(engineering, hr, finance);
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private Department department(String name, String description) {
        Department department = new Department();
        department.setName(name);
        department.setDescription(description);
        return department;
    }

    private List<Employee> seedEmployees(List<Department> departments) {
        List<Employee> employees = List.of(
                employee("Jane", "Smith", "jane.smith@example.com", "555-0101",
                        "2023-05-01", "65000", departments.get(0)),
                employee("John", "Doe", "john.doe@example.com", "555-0102",
                        "2022-08-15", "58000", departments.get(0)),
                employee("Alex", "Brown", "alex.brown@example.com", "555-0103",
                        "2024-02-10", "52000", departments.get(1)),
                employee("Maria", "Garcia", "maria.garcia@example.com", "555-0104",
                        "2023-11-20", "47000", departments.get(1)),
                employee("Omar", "Haddad", "omar.haddad@example.com", "555-0105",
                        "2024-06-03", "54000", departments.get(2))
        );
        employeeRepository.saveAll(employees);
        return employees;
    }

    private Employee employee(String firstName, String lastName, String email, String phone,
                              String hireDate, String salary, Department department) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setHireDate(LocalDate.parse(hireDate));
        employee.setSalary(new BigDecimal(salary));
        employee.setDepartment(department);
        return employee;
    }

    private void seedAttendance(List<Employee> employees) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < employees.size(); i++) {
            Employee employee = employees.get(i);
            for (int day = 1; day <= 4; day++) {
                Attendance attendance = new Attendance();
                attendance.setDate(today.minusDays(day));
                attendance.setCheckIn(LocalTime.of(9, 0));
                attendance.setCheckOut(LocalTime.of(17, 30));
                attendance.setTotalHours(8.5);
                attendance.setStatus(i % 3 == 0 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
                attendance.setEmployee(employee);
                attendanceRepository.save(attendance);
            }
        }
    }
}