# Student Enrollment Management System

A small Spring Boot web application for managing students and the courses they
enrol in, backed by an Oracle relational database accessed through
`JdbcTemplate` (no ORM).

> **Context.** This is a personal recreation of a university database practicum,
> a coursework-scale app, not a production system. There is deliberately no
> authentication, authorization, CSRF protection, or audit logging (see
> [Scope & security](#scope--security)). The point of the project is clean
> layering, hand-written SQL, and a test suite that pins the behaviour down.

## Screenshots

_Screenshots of the student list, the add form with validation errors, the
detail page (JOIN result), and the 404 page will be added here._

## Tech stack

| Component | Version | Note |
|---|---|---|
| Java | 17 | Temurin / OpenJDK |
| Spring Boot | 3.5.16 | Spring MVC + Thymeleaf |
| Spring JDBC (`JdbcTemplate`) | via Boot | Database access, no ORM |
| Bean Validation | via Boot | Server-side field validation |
| Oracle JDBC (`ojdbc11`) | 23.26.3.0.0 | Driver only, `runtime` scope |
| Bootstrap | 5.3.3 (CDN) | Grid and utilities |
| Maven Wrapper | → Maven 3.9.x | No separate Maven install needed |

There is no second database and no embedded/H2 fallback. The app talks to
Oracle or it does not run.

## Architecture

```
src/main/java/com/togar/studentenrollment/
├── controller/   HTTP routes only, no SQL and no business rules
│   └── GlobalExceptionHandler   maps domain exceptions to 404 / error pages
├── service/      Business rules (duplicate NIM, missing NIM) + @Transactional
├── repository/   The only place SQL is written (JdbcTemplate)
├── model/        Records mirroring the tables (Mahasiswa, MataKuliah, Irs)
├── dto/          Form-binding object + JOIN result shape
├── exception/    Domain exceptions (DuplicateNimException, MahasiswaNotFoundException)
└── config/       WebBindingConfig (global input trimming), WebMvcConfig
```

Each layer has one job: the controller translates HTTP, the service decides
whether an operation is allowed, the repository runs SQL. Nothing leaks across
those lines.

## Design decisions worth a look

**One JOIN query, not N+1.** The detail page (`GET /students/{nim}`) loads the
student and every enrolled course in a single query, assembled with a
`ResultSetExtractor` (one `MahasiswaDetail` built from many rows). A student
with 10 courses is still 1 round-trip, not 11.

**`LEFT JOIN`, not `INNER JOIN`.** A student who has enrolled in nothing must
still resolve to one row (with `NULL` course columns) so the page renders an
empty state. `INNER JOIN` would return zero rows and the app would wrongly show
a 404. `NULL` rows are then dropped while building the course list.

**Parameterized SQL everywhere.** Every external value goes to `JdbcTemplate` as
a `?` bind parameter; no string concatenation builds SQL anywhere, so there is no
injection surface. `MahasiswaRepository` documents this per method.

**`ON DELETE CASCADE` only where it is correct.** The `IRS → MAHASISWA` foreign
key cascades, so deleting a student cleans up their enrolment rows. The
`IRS → MATA_KULIAH` key deliberately does **not** cascade, because a course that
students are still taking should refuse to be deleted, not silently drop
enrolments.

**NIM is immutable.** It is the primary key and the student's identity, and
Oracle has no `ON UPDATE CASCADE`. The edit form renders NIM read-only and the
controller always takes NIM from the path, never the request body, so the
primary key can never be rewritten, even by a tampered request.

## Testing

```powershell
.\mvnw.cmd test
```

**The tests need no database.** They run on `@WebMvcTest` / plain Mockito with a
mocked service, so no `DataSource` is created and no JDBC connection is
attempted, so `.\mvnw.cmd test` passes even with `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD` unset.

| Class | Tests | Covers |
|---|---|---|
| `MahasiswaControllerTest` | 23 | Routes, redirects, HTTP status, per-field validation, POST-only delete, NIM-from-path invariant, `;jsessionid` URL regression |
| `TemplateRenderingTest` | 12 | Thymeleaf templates actually render to HTML (not just view-name matching), including the error page when the database is down |
| `MahasiswaServiceTest` | 16 | Business rules: duplicate NIM, missing NIM, total-SKS calculation, model/DTO helpers |
| **Total** | **51** | |

## Running it

**Prerequisites:** JDK 17, and a running **Oracle Database XE** (21c or 18c)
with an application user created. Full install instructions (native installer and
Docker) plus a troubleshooting table are in
[`docs/SETUP.md`](docs/SETUP.md).

1. **Create the schema.** From `src/main/resources/sql/`, run `schema.sql` then
   `seed.sql` against your Oracle user (via SQL\*Plus or SQL Developer). Both are
   safe to re-run.

2. **Provide the connection via environment variables.** No credentials are
   stored in the repo (`application.properties` only holds `${DB_URL}`
   placeholders). See [`application.properties.example`](src/main/resources/application.properties.example).

   ```powershell
   $env:DB_URL      = "jdbc:oracle:thin:@localhost:1521/XEPDB1"
   $env:DB_USERNAME = "sems_user"
   $env:DB_PASSWORD = "<your password>"
   ```

3. **Run.**

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Open <http://localhost:8080>.

## Routes

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/` | Student list |
| `GET` | `/students/new` | Add form |
| `POST` | `/students` | Create a student |
| `GET` | `/students/{nim}` | Detail + enrolled courses (one JOIN query) |
| `GET` | `/students/{nim}/edit` | Edit form (NIM read-only) |
| `POST` | `/students/{nim}/edit` | Save changes |
| `POST` | `/students/{nim}/delete` | Delete a student |

Mutations are POST-only. Opening `/students/{nim}/delete` in a browser returns
**405 Method Not Allowed**, not a deletion.

## Data model

| Table | Key columns | Notes |
|---|---|---|
| `MAHASISWA` | `NIM` (PK) | `NAMA`, `ANGKATAN` (2000-2100), `GENDER` (`L`/`P`), all `CHECK`-constrained |
| `MATA_KULIAH` | `MATKUL_ID` (PK) | `MATKUL_NAMA`, `SKS` (1-6), `HARI` (Senin-Sabtu) |
| `IRS` | `IRS_ID` (PK) | `NIM` → `MAHASISWA` `ON DELETE CASCADE`; `MATKUL_ID` → `MATA_KULIAH`; `STATUS` (`aktif`/`lulus`/`gagal`); `UNIQUE (NIM, MATKUL_ID)` |

`UNIQUE (NIM, MATKUL_ID)` stops a student enrolling in the same course twice.

## Scope & security

**Applied:**

- No credentials in code; the connection comes from environment variables.
- All SQL is parameterized; no injection surface.
- Server-side validation on every field (forms carry `novalidate` so the tested
  server path is the one that runs).
- Thymeleaf auto-escaping, so a name containing `<script>` renders as text.
- Mutations are POST-only; deletion cannot be triggered by GET or prefetch.
- Stack traces are never sent to the browser (`server.error.include-stacktrace=never`).
- `spring.sql.init.mode=never`, so running the app never alters your schema.

**Deliberately out of scope** (a real deployment would need these): authentication
and authorization, CSRF protection, rate limiting, audit logging, and data
encryption.

## License

[MIT](LICENSE) © 2026 Togar Anthony Mario Sianturi
