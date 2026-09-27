# Employment Management System (EMS)

A Spring Boot 3 backend system for managing employees, departments, attendance, and user roles with JWT-based authentication.

## 🛠️ Built With
- **Backend**: Spring Boot, Spring Security, Spring Data JPA, Hibernate
- **Security**: JWT, BCrypt
- **Database**: MySQL 8
- **API**: RESTful, Swagger/OpenAPI
- **Tools**: Maven, Git, Postman, IntelliJ

## ✨ Features
- Secure login & role-based access (ADMIN, MANAGER, EMPLOYEE)
- Full CRUD for Employees, Departments, and Attendance
- Input validation using Bean Validation (JSR-380)
- DTOs for clean API contracts
- Global exception handling
- Profile-based config (dev/test/prod)

## 🚀 Getting Started
1. Create MySQL DB: \`CREATE DATABASE HR_System CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\`
2. Update \`application.yml\` with your DB credentials
3. Run: \`./mvnw spring-boot:run\`
4. API Docs: http://localhost:8080/swagger-ui.html

## 📄 License
MIT
EOF
