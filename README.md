# Employee Management System — Backend

REST API for an employee management system: employees, departments, attendance, authentication (JWT) and role-based access control.

**Frontend:** [`employment-management-system-frontend`](https://github.com/siefeldeein/employment-management-system-frontend)

## Tech Stack

| Area | Technology |
| --- | --- |
| Framework | Spring Boot 4.0.2 (Java 17, Maven Wrapper) |
| Data | Spring Data JPA (Hibernate), MySQL 8 |
| Security | Spring Security, JWT (jjwt 0.11.5) |
| Mapping | MapStruct, Lombok |
| Validation | Jakarta Bean Validation |

## Features

- **Authentication** — register + login issuing JWTs (`/api/auth/register`, `/api/auth/login`)
- **Role-based access** — `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE` via `@PreAuthorize`; department writes are ADMIN-only
- **Employees** — paginated list, name search, create / update (PATCH) / delete
- **Departments** — paginated list, ADMIN CRUD, employees-by-department view (nested DTO)
- **Attendance** — flexible search with multiple filters using JPA Specifications (`POST /api/attendance/search`, paginated)
- **Robust error handling** — `@RestControllerAdvice` returning consistent `ErrorResponse` bodies (404 / 409 / 400 + validation errors)
- **Layered architecture** — Controller → Service → Repository, with MapStruct DTO mappers

## Project Structure

```
src/main/java/com/example/ems
├── auth/                  login & register, JWT issuing
├── employee/              employee entity, service, controller, DTOs, mapper
├── department/            department entity, service, controller, DTOs, mapper
├── attendance/            attendance entity, service, controller, DTOs, specifications
├── user/                  User entity + Role (N:M), role services
├── security/              JWT filter, JwtService, CustomUserDetailsService, SecurityConfig
└── common/exception/      GlobalExceptionHandler, ErrorResponse, custom exceptions
```

## Getting Started

Prerequisites: **Java 17**, **Maven** (or use the included `./mvnw`), **MySQL 8**.

### 1. Create the database

```sql
CREATE DATABASE employee_management_system;
CREATE USER 'springstudent'@'localhost' IDENTIFIED BY 'springstudent';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'springstudent'@'localhost';
```

Tables are created automatically at startup (`ddl-auto: update`).

### 2. Seed the roles

```sql
INSERT INTO roles (id, name) VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_MANAGER'),
  (3, 'ROLE_EMPLOYEE');
```

### 3. Start the API

```bash
./mvnw spring-boot:run    # or: mvn spring-boot:run
```

Runs on `http://localhost:8080`. DB credentials, JWT secret and token expiry are in `src/main/resources/application.yaml`.

### 4. Register & login

```bash
# Register (new users get ROLE_EMPLOYEE)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jane","password":"secret123","email":"jane@example.com"}'

# Login → returns { "token": "..." }
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jane","password":"secret123"}'
```

Send the token on secured requests:

```
Authorization: Bearer <token>
```

## API Overview

| Method | Endpoint | Description | Access |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Create a user | public |
| POST | `/api/auth/login` | Login, returns JWT | public |
| GET | `/api/employees/paged?page=&size=` | Paginated employees | auth |
| GET | `/api/employees/search?name=` | Search by name | auth |
| GET | `/api/employees/{id}` | Get one employee | auth |
| POST | `/api/employees` | Create employee | auth |
| PATCH | `/api/employees/{id}` | Update employee | auth |
| DELETE | `/api/employees/{id}` | Delete employee | auth |
| GET | `/api/departments/paginated?page=&size=` | Paginated departments | auth |
| GET | `/api/departments/{id}/employees` | Department with its employees | auth |
| POST | `/api/departments` | Create department | **ADMIN** |
| PUT | `/api/departments/{id}` | Update department | **ADMIN** |
| DELETE | `/api/departments/{id}` | Delete department | **ADMIN** |
| POST | `/api/attendance/search` | Attendance search (filters + paging) | auth |

Errors are returned as: `{ "message": "...", "timestamp": "..." }`.

> **Development note:** while the React frontend is being built, authentication is temporarily relaxed (`anyRequest().permitAll()` + method security commented out in `SecurityConfig`). The JWT filter, roles and `@PreAuthorize` rules are fully implemented and will be re-enabled by uncommenting those two lines.