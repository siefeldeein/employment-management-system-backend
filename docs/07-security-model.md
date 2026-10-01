# 07 - The Security Model

One page that answers "who can do what, and where is that decided?"

---

## 1. The three roles

| Role | Stored as | Meaning in this app |
|---|---|---|
| Admin | `ROLE_ADMIN` | Full control. Identical to MANAGER in behaviour. |
| Manager | `ROLE_MANAGER` | Full control. Identical to ADMIN in behaviour. |
| Employee | `ROLE_EMPLOYEE` | Own attendance only. |

The `ROLE_` prefix is **not cosmetic** - Spring Security's `hasRole('X')`
internally compares against `ROLE_X`, which is why the code says
`hasRole('EMPLOYEE')` while the database says `ROLE_EMPLOYEE`. Writing
`hasRole('ROLE_EMPLOYEE')` would silently never match.

`ADMIN` and `MANAGER` are separated only for future differentiation (audit logs,
deletion rights). Today every check is `hasAnyRole('ADMIN','MANAGER')`.

---

## 2. Where roles come from

```
DataSeeder               ->  inserts ROLE_ADMIN / ROLE_MANAGER / ROLE_EMPLOYEE into `roles`
RoleRepository.findByName
UserRegistrationFacade   ->  ALWAYS ROLE_EMPLOYEE
User.roles (join table)  ->  the account's actual roles
```

There is **no endpoint, no UI and no code path that assigns ADMIN or MANAGER**.
A fresh database has the three role rows but **zero user rows**, so the only way to
obtain a manager account is to insert one manually (SQL, see
`08-errors-and-glossary.md`) or to promote an existing account in the
`users_roles` table.

That is why `hasAnyRole('ADMIN','MANAGER')` is effectively "is not an employee".

---

## 3. The two layers of enforcement

### Layer 1 - URL level, `SecurityConfig.java:34-37`

```java
.requestMatchers("/api/auth/**", "/error").permitAll()
.anyRequest().authenticated()
```

Answers only: *is there a valid token?* It knows nothing about roles.

Note `/api/auth/**` is permitted, which includes `/api/auth/me`. That does **not**
make `/me` public: the controller requires an `Authentication` argument, so an
anonymous request still gets `401`. `permitAll` just means the filter chain does
not reject it early.

### Layer 2 - method level, `@PreAuthorize`

Answers: *does this token have a role that allows this?* Three variants are used:

| Annotation | Where |
|---|---|
| `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` on the **class** | `EmployeeController`, `DepartmentController` |
| `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` on the **method** | `AttendanceController.search` |
| `@PreAuthorize("hasRole('EMPLOYEE')")` | `AttendanceController.me` |
| SpEL ownership check | `AttendanceController.checkIn/checkOut` |

`@EnableMethodSecurity` in `SecurityConfig:22` is what activates these
annotations; without it they would be silently ignored.

Class-level is preferred because it is impossible to forget on a new method. The
attendance controller uses per-method annotations because it must mix roles.

### Layer 3 - the frontend, `usePermissions`

Hides links, hides buttons, redirects pages, disables queries. **Convenience
only.** Nothing here is a security control: deleting every `canManage` check would
still leave the backend enforcing everything.

---

## 4. Complete endpoint permission matrix

`A/M` = ADMIN or MANAGER, `E` = EMPLOYEE. Anything not listed is not a real
endpoint.

| # | Method | Path | Role | Enforced by |
|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | public | `permitAll` |
| 2 | POST | `/api/auth/register` | public | `permitAll` |
| 3 | GET | `/api/auth/me` | any authenticated | `permitAll` + controller arg |
| 4 | POST | `/api/employees` | A/M | class `@PreAuthorize` |
| 5 | GET | `/api/employees` | A/M | class |
| 6 | GET | `/api/employees/{id}` | A/M | class |
| 7 | GET | `/api/employees/paged` | A/M | class |
| 8 | GET | `/api/employees/search` | A/M | class |
| 9 | PATCH | `/api/employees/{id}` | A/M | class |
| 10 | DELETE | `/api/employees/{id}` | A/M | class |
| 11 | POST | `/api/departments` | A/M | class |
| 12 | GET | `/api/departments` | A/M | class |
| 13 | GET | `/api/departments/paginated` | A/M | class |
| 14 | GET | `/api/departments/{id}` | A/M | class |
| 15 | GET | `/api/departments/{id}/employees` | A/M | class |
| 16 | PUT | `/api/departments/{id}` | A/M | class |
| 17 | DELETE | `/api/departments/{id}` | A/M | class |
| 18 | POST | `/api/attendance/search` | A/M | method |
| 19 | GET | `/api/attendance/me` | E | method |
| 20 | POST | `/api/attendance/check-in` | A/M any, E self | SpEL |
| 21 | POST | `/api/attendance/check-out` | A/M any, E self | SpEL |

### What an `EMPLOYEE` gets, verified against a running server

| Attempt | Result |
|---|---|
| `GET /api/attendance/me` | `200`, only their own rows (17 in the test DB) |
| `POST /api/attendance/search` | `403` |
| `GET /api/employees` | `403` |
| `GET /api/departments` | `403` |
| `POST /api/attendance/check-in?employeeId=<someone else>` | `403` |
| `POST /api/attendance/check-in?employeeId=<self>` | `200` |

---

## 5. The ownership check in full

```java
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') or " +
        "hasRole('EMPLOYEE') and " +
        "authentication.principal.employee != null and " +
        "#employeeId == authentication.principal.employee.id")
```

| Token | Meaning |
|---|---|
| `hasAnyRole('ADMIN','MANAGER')` | managers may act on anyone |
| `#employeeId` | the method parameter, injected from the request |
| `authentication.principal` | the `User` entity placed in the context by the JWT filter |
| `authentication.principal.employee` | the linked `Employee`, or `null` |
| `authentication.principal.employee.id` | the id the token proves you own |

`and` binds tighter than `or`, so this reads
`(ADMIN or MANAGER) or (EMPLOYEE and employee != null and #employeeId == ownId)`.

The `employee != null` clause is what stops a manager-only account (no employee
row) that somehow also had `ROLE_EMPLOYEE` from causing a null dereference inside
the SpEL evaluation.

The same idea, done the safe way, appears on `/me`: the employee id is **not a
parameter at all** - it is read from `user.getEmployee().getId()`. Not accepting it
is stronger than validating it.

---

## 6. Token handling: what is strong and what is not

**Strong**

* Passwords are BCrypt-hashed (`BCryptPasswordEncoder`); the raw password is
  never stored or logged.
* The signature is verified on every request; a tampered token fails the parse and
  the filter degrades to anonymous -> `401`.
* Roles are re-read from the database on **every** request by the JWT filter, so
  the token's own `roles` claim is never trusted. Revoking a role takes effect on
  the caller's next request.
* CSRF is disabled *correctly*: there are no cookies, so there is nothing for a
  third-party site to ride on. The token is attached manually by JS.
* `Stateless` sessions - no server-side session to hijack.
* `Logout` clears the token from the client and the query cache.

**Weak / worth knowing**

| Issue | Consequence | Where to fix |
|---|---|---|
| `jwt.expiration: 86400000` (24h) | a stolen token is usable for a day | shorter TTL + refresh tokens |
| Logout does not revoke the token | a copied token stays valid until expiry | a denylist or short TTL |
| `jwt.secret` is committed in `application.yaml` | in production this key must be injected, not committed | environment variable / secret manager |
| `spring.datasource.password` committed | same problem | environment variable |
| `ddl-auto: update` in production | schema is mutated at startup by the app | Flyway/Liquibase migrations, `validate` |
| `/api/auth/register` is public | anyone with a known employee email can claim an account | invite codes, or admin-created accounts only |
| `/error` is `permitAll` | the error page is reachable without a token | acceptable; it renders no data |
| No rate limiting on `/login` | password guessing is unthrottled | add a limiter or a delay |
| `getAuthorities()` derives from a `Set` | multi-role ordering is not guaranteed by Spring, but `getCurrentUser` sorts for display | already handled where it matters |

---

## 7. Error responses are safe by design

```
401  {"status":401,"message":"Unauthorized: invalid or missing token"}        SecurityConfig entryPoint
403  {"status":403,"message":"Forbidden: insufficient permissions"}          SecurityConfig accessDeniedHandler
```

Both are written by the filter chain as **hardcoded strings** - they never
interpolate anything from the request, so an error message cannot become an
information leak.

Method-level denials (`@PreAuthorize`) throw `AccessDeniedException`, which is
handled by `GlobalExceptionHandler` and produces the same message, just through
the exception handler instead of the handler. The frontend only reads
`errorData.message`, so both paths look identical to the UI.

The generic `500` handler logs the stack trace server-side and returns
`"An unexpected error occurred. Please try again later."` - **never** the
exception message. That is why a `LazyInitializationException` or an NPE shows up
in the UI as a generic message and only in the console as a stack trace.

`BadCredentialsException` maps to `401 "Invalid username or password"`, which does
not reveal whether the username exists.

---

## 8. How the frontend mirrors the model

| Backend rule | Frontend mirror |
|---|---|
| `/employees`, `/departments`, `/` need A/M | `links` array `canManage: true` + `<Navigate to="/attendance">` in three pages + `enabled: canManage` |
| `/attendance/search` needs A/M | `canManage ? searchAttendance(...) : fetchMyAttendance(...)` |
| `/attendance/me` needs E | the same ternary; a manager never calls `/me` |
| employee may only clock self | `effectiveClockEmployeeId` derived from `user.employeeId`; the server double-checks |

Because both sides read the same source of truth (`user.roles` from
`/api/auth/me`), the UI cannot disagree with the API. If a role changes, the next
`loadUser()` updates `canManage` and the UI follows.

---

## 9. What to say if asked "is this app secure?"

A defensible answer:

> Authentication is JWT-based, stateless, with BCrypt password hashing and
> per-request database role resolution. Authorization is enforced **server-side**,
> with class-level `@PreAuthorize` on the employee and department controllers,
> a dedicated `/attendance/me` read endpoint for employees, and a SpEL ownership
> check on clock-in/out. The frontend only hides UI. The remaining gaps are
> production configuration (committed secrets, `ddl-auto: update`, no migrations),
> the absence of token revocation and refresh, and a public registration endpoint
> that is mitigated by requiring a matching employee email.