# 08 - Errors, Flows, Glossary, and Known Gaps

The last document: how failures are reported, the full list of flows, every term
defined, and an honest list of what is still missing.

---

## 1. `ErrorResponse` - one shape for every failure

```java
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private String path;                 // which endpoint failed
    private Map<String,String> errors;   // field-level validation errors
}
```

Example (`400`):

```json
{
  "status": 400,
  "message": "Validation failed: 1 error(s)",
  "timeStamp": "2026-01-15T10:22:31.482",
  "path": "/api/auth/register",
  "errors": { "password": "Password size must be minimum of 6" }
}
```

Every response is built by one private helper, so the shape is guaranteed:

```java
private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
        HttpServletRequest request, Map<String,String> errors){
    ErrorResponse e = new ErrorResponse(status.value(), message, LocalDateTime.now(),
            request.getRequestURI(), errors);
    return new ResponseEntity<>(e, status);
}
```

`request.getRequestURI()` is why every error knows its own endpoint - handy when
debugging. `errors` is `null` for non-validation errors, which serialises as
`"errors": null` rather than being omitted.

---

## 2. The handler table

`@RestControllerAdvice` on `GlobalExceptionHandler` means these apply to **every**
controller without any per-controller code.

| Thrown | Status | Body `message` |
|---|---|---|
| `ResourceNotFoundException` | **404** | the exception's own message |
| `InvalidInputException` | **400** | the exception's own message |
| `MethodArgumentNotValidException` | **400** | `"Validation failed: N error(s)"` + per-field map |
| `DuplicateResourceException` | **409** | the exception's own message |
| `HttpMessageNotReadableException` | **400** | `"Malformed request body"` |
| `DataIntegrityViolationException` | **409** | `"Database Constraint Violation: <root cause>"` |
| `AccessDeniedException` | **403** | `"Forbidden: insufficient permissions"` |
| `BadCredentialsException` | **401** | `"Invalid username or password"` |
| anything else (`Exception`) | **500** | `"An unexpected error occurred. Please try again later."` |

The generic `500` handler logs the stack trace (`log.error("Unexpected error at
{}:", request.getRequestURI(), ex)`) and returns a **fixed** message. That is
intentional: never echo an exception message to a client.

### Three custom exceptions, all identical

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message){ super(message); }
}
```

`ResourceNotFoundException`, `InvalidInputException` and
`DuplicateResourceException` are byte-for-byte the same shape - each exists so the
**handler** can map it to a different status. Because they extend
`RuntimeException`, no `throws` clause is needed anywhere and no method is forced
to declare them.

The naming convention across the codebase:

| Exception | Used for | Status |
|---|---|---|
| `ResourceNotFoundException` | "that record does not exist" | 404 |
| `InvalidInputException` | "you sent something impossible" | 400 |
| `DuplicateResourceException` | "it already exists" | 409 |

**Inconsistency to know about:** attendance uses `InvalidInputException("Employee
not found")`, which produces **400** where the other features produce **404** for
the same situation.

### Errors that bypass the advice

The two security responses are written by `SecurityConfig`, not by the advice,
because they are produced by the servlet filter chain *before* any controller:

| Response | Written by | Status | Body |
|---|---|---|---|
| no/invalid token | `authenticationEntryPoint` | 401 | `{"status":401,"message":"Unauthorized: invalid or missing token"}` |
| missing role | `accessDeniedHandler` | 403 | `{"status":403,"message":"Forbidden: insufficient permissions"}` |

They have a **different, much smaller body shape** - no `timeStamp`, no `path`.
`api()` only reads `message`, so the UI behaves identically, but if you ever
assert on the error shape in a test you must handle both forms.

Also missing from the advice: `HttpMediaTypeNotSupportedException` (wrong
`Content-Type`), `MethodArgumentTypeMismatchException` (`/employees/abc`), and
`MissingServletRequestParameterException` (missing required param). All three fall
into the generic `500` handler, which is why a malformed request returns `500`
rather than `400`/``415`. Those are genuine gaps.

---

## 3. The 21 endpoints, grouped into 18 flows

A **flow** is what a user accomplishes. This is the map of the whole application.

### Auth (4 flows, 3 endpoints)

| # | Flow | Steps | Endpoints |
|---|---|---|---|
| 1 | **Log in** | type credentials -> `POST /login` -> store token -> `GET /me` -> navigate `/` | 2 |
| 2 | **Register** | type username/password/employee email -> `POST /register` -> token -> `/me` -> navigate `/` | 2 |
| 3 | **Restore session on reload** | token found -> `GET /me` -> render app; or `401` -> logout -> `/login` | 1 |
| 4 | **Log out** | click Logout -> clear query cache -> clear token -> navigate `/login` | 0 |

### Employees (5 flows, 7 endpoints)

| # | Flow | Steps | Endpoints |
|---|---|---|---|
| 5 | **Browse the list** | open `/employees` -> `GET /employees/paged` -> Prev/Next -> `GET /employees/paged?page=n` | 1 |
| 6 | **Search by name** | type name -> Submit -> `GET /employees/search?name=` | 1 |
| 7 | **Create** | Add Employee -> modal -> choose department (`GET /departments`) -> `POST /employees` -> invalidate | 2 |
| 8 | **Update** | Edit -> modal -> `PATCH /employees/{id}` -> invalidate | 1 |
| 9 | **Delete** | Delete -> confirm -> `DELETE /employees/{id}` (204) -> invalidate | 1 |

### Departments (5 flows, 7 endpoints)

| # | Flow | Steps | Endpoints |
|---|---|---|---|
| 10 | **Browse the list** | open `/departments` -> `GET /departments/paginated` -> paginate | 1 |
| 11 | **Create** | Add Department -> modal -> `POST /departments` -> invalidate | 1 |
| 12 | **Update** | Edit -> modal -> `PUT /departments/{id}` -> invalidate | 1 |
| 13 | **Delete** | Delete -> confirm -> `DELETE /departments/{id}` (204) | 1 |
| 14 | **View details** | View -> modal opens -> `GET /departments/{id}/employees` -> table | 1 |

### Attendance (3 flows, 4 endpoints)

| # | Flow | Steps | Endpoints |
|---|---|---|---|
| 15 | **Check in** | select self (or pick an employee) -> Check In -> `POST /check-in` -> invalidate `attendance`, `attendance-today`, `dashboard-attendance` | 1 + 1-2 refetches |
| 16 | **Check out** | Check Out -> `POST /check-out` -> status computed -> same invalidations | 1 + refetches |
| 17 | **View attendance** | employees: `GET /attendance/me`; managers: filters -> Apply -> `POST /attendance/search`; paginate | 1-2 |

### Dashboard (1 flow, 3 endpoints)

| # | Flow | Steps | Endpoints |
|---|---|---|---|
| 18 | **See the overview** | open `/` -> `GET /employees`, `GET /departments`, `POST /attendance/search` (empty filter, size 1000) -> count client-side | 3 |

Note flows 7 and 18 issue **3 endpoints for one user goal**. That is the clearest
illustration of why "endpoint" and "flow" are different words.

---

## 4. Where every status code comes from

| Status | Trigger in this app |
|---|---|
| `200 OK` | any successful read, update, check-in, check-out, login |
| `201 Created` | `POST /employees`, `POST /departments`, `POST /auth/register` |
| `204 No Content` | `DELETE /employees/{id}`, `DELETE /departments/{id}` (empty body) |
| `400 Bad Request` | bean-validation failure, malformed JSON, empty department name, department still has employees, check-in twice, check-out with no check-in, unknown attendance employee id |
| `401 Unauthorized` | missing/expired/invalid token, wrong username or password |
| `403 Forbidden` | authenticated but wrong role, or an employee targeting someone else |
| `404 Not Found` | unknown employee id, unknown department name, unknown email at registration, missing `ROLE_EMPLOYEE` row |
| `409 Conflict` | duplicate username, duplicate email, duplicate department name, DB constraint violation (e.g. deleting an employee who still has a login) |
| `500 Internal Server Error` | anything unhandled - and, unfortunately, unsupported content types and unparseable path variables |

---

## 5. Known gaps, in priority order

### Blocking for a fresh install

**1. No admin/manager account can be created.**
`DataSeeder` inserts roles, departments, employees and attendance - but **no
`User` rows**. Registration always assigns `ROLE_EMPLOYEE`. So on a new database
you cannot reach any manager-only screen.

Two ways out:

*Register as an employee, then promote that row:*

```sql
USE employee_management_system;
INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'jane' AND r.name = 'ROLE_MANAGER';
```

*Or insert an account from scratch with a BCrypt hash of the password:*

```sql
USE employee_management_system;
INSERT INTO users (username, password, enabled, created_at)
VALUES ('admin', '<bcrypt-hash>', TRUE, NOW());

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
```

`User` also implements `UserDetails` without `@Transient` on `isEnabled`, and
`enabled` is a primitive `boolean`, so leaving it out yields `false` and login
fails with "Invalid username or password".

To generate a hash in a Spring Boot project:

```java
new BCryptPasswordEncoder().encode("secret123")
```

**2. `README.md` is out of date.** It claims department writes are ADMIN-only
(they are A/M), shows the access column as plain `auth`, and - worst - states
that *"authentication is temporarily relaxed (`anyRequest().permitAll()`)"*. That
is no longer true: `anyRequest().authenticated()` with the full `@PreAuthorize`
set is live. It also documents the error shape as `{"message", "timestamp"}`
instead of the real `ErrorResponse`.

### Correctness issues worth fixing

| Issue | Where | Effect |
|---|---|---|
| A single date bound is silently ignored | `AttendanceServiceImp` line 63 (needs both) | picking only "From" or only "To" filters nothing |
| `new Date().toISOString()` is UTC | `AttendancePage:36` | in non-UTC timezones the frontend's "today" can differ from the server's `LocalDate.now()` |
| Negative `totalHours` for overnight shifts | `checkOut` | an overnight shift is reported `HALF_DAY` |
| Department referenced by **name** in employee APIs | `EmployeeCreateRequest.departmentName` | renaming a department breaks clients; an id would be the fix |
| Search is unpaged and unescaped | `EmployeeRepository.findByFullNameContainingWithDepartment`, `http.js:38` | unbounded result set; a name with `&` breaks the query string |
| `""` passes `@Size` on `DepartmentUpdateReq` | `DepartmentUpdateReq` | a blank description can overwrite a `nullable = false` column |
| Unsupported content types return `500` | no handler for `HttpMediaTypeNotSupportedException` | should be `415`/`400` |
| `totalHours` is write-only | never in `AttendanceResponse` | computed, stored, never displayed |
| `/me` has no `employee != null` guard | `AttendanceController:47` | an `EMPLOYEE` account with no employee row would NPE -> `500` |

### Production readiness

| Issue | Fix |
|---|---|
| `jwt.secret` and DB password committed | environment variables |
| `ddl-auto: update` | Flyway/Liquibase + `validate` |
| `show-sql: true` | off in production |
| 24-hour tokens, no revocation, no refresh | short TTL + refresh token |
| Public registration | invite codes or admin-created accounts |
| No rate limiting on `/login` | throttling |
| Only `EmsApplicationTests` exists | real `@WebMvcTest`/`@SpringBootTest` coverage of the role matrix |
| `vercel.json` points at `YOUR-EMS-BACKEND.onrender.com` | real backend URL, plus an SPA fallback rewrite |
| Frontend `min="0"` allows salary `0`; backend `@DecimalMin(inclusive=false)` rejects it | align the two |

---

## 6. Glossary

| Term | Meaning in this project |
|---|---|
| `Employee` | an HR record (name, email, salary, hire date, department). **Not** a login. |
| `User` | a login account (username, BCrypt password, roles). Links to at most one `Employee`. |
| `Role` | a row in `roles`; the string is `ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_EMPLOYEE`. |
| authority / `GrantedAuthority` | Spring Security's view of a role, e.g. `ROLE_ADMIN`. Produced by `User.getAuthorities()`. |
| `canManage` | frontend boolean: "is ADMIN or MANAGER". Drives nav, guards and `enabled`. |
| JWT | a signed, self-contained token: header + claims + HMAC signature. No server session. |
| `SecurityContext` | the per-request holder of the authenticated principal. |
| principal | the authenticated object - here, the `User` entity. |
| `AuthenticationManager` | Spring component that verifies username + password against `UserDetailsService` + `PasswordEncoder`. |
| BCrypt | salted, deliberately slow password hashing. |
| `@PreAuthorize` | SpEL-based method authorization; requires `@EnableMethodSecurity`. |
| `permitAll` | the URL needs no authentication. It does **not** mean "public data". |
| DTO | data transfer object - the request/response shape, decoupled from the entity. |
| Entity | the `@Entity` class that maps a database table. |
| Mapper (MapStruct) | compile-time generated Entity<->DTO converter. |
| `Page<T>` | Spring Data's paginated result: `content`, `totalPages`, `totalElements`, `number`, `size`. |
| `Pageable` | the request-side pagination object: `page`, `size`, `sort`. |
| `Specification` | Spring Data's Criteria-API predicate; attendance composes them with `allOf`. |
| `@Transactional` | run the method in one DB transaction; roll back on exception. |
| dirty checking | Hibernate auto-`UPDATE`s a managed entity's changed fields - why update/delete methods never call `save`. |
| `FetchType.LAZY` | load the association on first access, only inside an open transaction. |
| `LEFT JOIN FETCH` | fetch an association in the same query - required to map it safely. |
| `CommandLineRunner` | Spring's startup hook; `DataSeeder` uses it. |
| `ddl-auto: update` | create/migrate tables from the entities at startup. |
| Zustand | client-state store; `create`, `set`, `get`, selector subscriptions. |
| TanStack Query | server-state library: caching, `useQuery`, `useMutation`, invalidation. |
| query key | the cache identity of a query; a prefix match is used for invalidation. |
| `Outlet` | the placeholder a layout route renders its child route into. |
| `<Navigate replace>` | redirect that **replaces** the history entry, avoiding Back-button loops. |
| `enabled` | TanStack Query flag: do not fire this query yet. |
| `placeholderData` | keep showing the previous data while a new request is in flight. |
| `isLoading` vs `isFetching` | "no data yet" vs "a request is running (maybe a refetch)". |
| `invalidateQueries` | mark cache entries stale and refetch the mounted ones. |
| proxy | Vite's dev-time `/api` -> `localhost:8080` forwarding; the reason there is no CORS config. |
| SpEL | Spring Expression Language - the language inside `@PreAuthorize` strings. |
| `record` | immutable Java data class; accessors have no `get` prefix. |

---

## 7. Where to go next

* **To run it:** follow `README.md` sections 1-4 for the database and roles, but
  ignore the "security is relaxed" note at the bottom - it is out of date. Then
  create a manager account with the SQL in section 5.1.
* **To change a rule:** business rules live in the `*ServiceImp` classes; access
  rules live in `@PreAuthorize` annotations; validation rules live in the DTOs.
* **To add an endpoint:** controller -> service interface -> service impl ->
  repository method -> (if new fields) DTO + mapper. Then add the `http.js`
  wrapper and invalidate the right query key from the mutation.
* **To verify a change:** `.\mvnw.cmd clean package -DskipTests`, `npx oxlint src`,
  `npm run build`, then re-check the role matrix from `07-security-model.md`
  section 4 with a manager token and an employee token.