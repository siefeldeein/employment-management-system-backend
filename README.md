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

- **Authentication** — register + login issuing JWTs, plus `GET /api/auth/me` to restore the session on reload
- **Role-based access** — `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE` enforced with `@PreAuthorize`; employee and department features are manager-only, employees get `GET /api/attendance/me` and self-scoped clock-in/out
- **Employees** — paginated list, name search, create / update (PATCH) / delete
- **Departments** — paginated list, create / update / delete, employees-by-department view (nested DTO)
- **Attendance** — check-in / check-out with automatic status calculation, flexible search using JPA Specifications, and a self-service endpoint for employees
- **Robust error handling** — `@RestControllerAdvice` returning consistent `ErrorResponse` bodies (400 / 401 / 403 / 404 / 409)
- **Layered architecture** — Controller → Service → Repository, with MapStruct DTO mappers

## Documentation

Full code-level walkthrough of every flow: [`docs/`](docs/) — start with
[`docs/00-README.md`](docs/00-README.md).

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

### 2. Start the API

```bash
./mvnw spring-boot:run    # or: mvn spring-boot:run
```

Runs on `http://localhost:8080`. DB credentials, JWT secret and token expiry are in
`src/main/resources/application.yaml`.

On the first run `DataSeeder` inserts the three roles (`ROLE_ADMIN`,
`ROLE_MANAGER`, `ROLE_EMPLOYEE`), three departments, five employees, and two
weeks of attendance rows. Do **not** insert the roles by hand before starting —
the seeder only seeds when the `roles` table is empty, and inserting rows first
would make it skip the departments and employees.

### 3. Create an account

The seeder creates no user accounts, and registration always produces
`ROLE_EMPLOYEE`. To reach the manager-only screens, register first, then promote
that account in the database:

```bash
# Register (new users get ROLE_EMPLOYEE); the email must match a seeded employee
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jane","password":"secret123","email":"jane.smith@example.com"}'

# Login → returns { "token": "..." }
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jane","password":"secret123"}'
```

Send the token on secured requests:

```
Authorization: Bearer <token>
```

To reach the manager-only screens, promote the account in the database, then log
out and back in (roles are re-read from the database on every request, but the
frontend needs a fresh `GET /api/auth/me`):

```sql
USE employee_management_system;
INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'jane' AND r.name = 'ROLE_MANAGER';
```

## API Overview

| Method | Endpoint | Description | Access |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Create a user (always `ROLE_EMPLOYEE`) | public |
| POST | `/api/auth/login` | Login, returns JWT | public |
| GET | `/api/auth/me` | Current username, email, roles, employeeId | authenticated |
| GET | `/api/employees` | All employees | **A/M** |
| GET | `/api/employees/paged?page=&size=` | Paginated employees | **A/M** |
| GET | `/api/employees/search?name=` | Search by name | **A/M** |
| GET | `/api/employees/{id}` | Get one employee | **A/M** |
| POST | `/api/employees` | Create employee | **A/M** |
| PATCH | `/api/employees/{id}` | Update employee | **A/M** |
| DELETE | `/api/employees/{id}` | Delete employee | **A/M** |
| GET | `/api/departments` | All departments | **A/M** |
| GET | `/api/departments/paginated?page=&size=` | Paginated departments | **A/M** |
| GET | `/api/departments/{id}` | Get one department | **A/M** |
| GET | `/api/departments/{id}/employees` | Department with its employees | **A/M** |
| POST | `/api/departments` | Create department | **A/M** |
| PUT | `/api/departments/{id}` | Update department | **A/M** |
| DELETE | `/api/departments/{id}` | Delete department | **A/M** |
| POST | `/api/attendance/search` | Attendance search (filters + paging) | **A/M** |
| GET | `/api/attendance/me` | Own attendance only | **EMPLOYEE** |
| POST | `/api/attendance/check-in?employeeId=` | Check in (employees: own id only) | **A/M** any, **EMPLOYEE** self |
| POST | `/api/attendance/check-out?employeeId=` | Check out (employees: own id only) | **A/M** any, **EMPLOYEE** self |

**A/M** = `ROLE_ADMIN` or `ROLE_MANAGER`.

Errors are returned as:

```json
{
  "status": 400,
  "message": "Validation failed: 1 error(s)",
  "timeStamp": "2026-01-15T10:22:31.482",
  "path": "/api/auth/register",
  "errors": { "password": "Password size must be minimum of 6" }
}
```

> **Note:** authentication is fully enabled — `anyRequest().authenticated()` plus
> `@EnableMethodSecurity` and the `@PreAuthorize` rules above. The `DataSeeder`
> creates roles, departments, employees and attendance, but **no user accounts**,
> so on a fresh database you must create one yourself: register (which always
> yields `ROLE_EMPLOYEE`) and then promote the account with
> `INSERT INTO users_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username = '...' AND r.name = 'ROLE_MANAGER';`.
> See [`docs/08-errors-and-glossary.md`](docs/08-errors-and-glossary.md) section 5.