# Setup & Troubleshooting

Full setup for running the Student Enrollment Management System locally against a
real Oracle database, plus a troubleshooting table and the documentation
screenshot checklist. For an overview of the project, see the
[README](../README.md).

## Contents

1. [Prerequisites](#prerequisites)
2. [Step 0: Install Oracle XE](#step-0-install-oracle-xe)
3. [Step 1: Create the application user](#step-1-create-the-application-user)
4. [Step 2: Run schema.sql and seed.sql](#step-2-run-schemasql-and-seedsql)
5. [Inspecting the database](#inspecting-the-database)
6. [Step 3: Environment variables](#step-3-environment-variables)
7. [Step 4: Run the application](#step-4-run-the-application)
8. [Running the tests](#running-the-tests)
9. [Screenshot checklist](#screenshot-checklist)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

1. **JDK 17**, check with `java -version`.
2. **Oracle Database XE** (21c or 18c) running.
3. **SQL\*Plus** or **SQL Developer** to run the SQL scripts.

Maven does **not** need to be installed; the project ships the Maven Wrapper
(`mvnw` / `mvnw.cmd`).

### If `JAVA_HOME` is not set

The wrapper needs `JAVA_HOME`. Check it:

```powershell
$env:JAVA_HOME
```

If empty, set it to your JDK location:

```powershell
# Current terminal session only
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot"

# Persist it (run once, then open a new terminal)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot", "User")
```

---

## Step 0: Install Oracle XE

Skip if Oracle is already installed. Check with:

```powershell
Get-Service -Name '*Oracle*' -ErrorAction SilentlyContinue
```

No output means Oracle is not installed.

### Native installer

1. Download **Oracle Database 21c Express Edition (XE) for Windows** from
   <https://www.oracle.com/database/technologies/xe-downloads.html> (free Oracle
   account required). About 2 GB.
2. Extract, run `setup.exe`, follow the wizard. You will be asked to set a
   password for `SYS` / `SYSTEM`. **Note it down**, it is needed in Step 1.
3. Installation takes 15-30 minutes and creates two Windows services:
   `OracleServiceXE` and `OracleOraDB21Home1TNSListener`.

After installation, confirm both services run:

```powershell
Get-Service OracleServiceXE, OracleOraDB21Home1TNSListener
```

Both `Status` columns must read `Running`. If `Stopped`, run PowerShell as
Administrator and:

```powershell
Start-Service OracleServiceXE, OracleOraDB21Home1TNSListener
```

### Alternative: Oracle XE via Docker

Lighter path if you have Docker: ~755 MB download (not ~2 GB), no Oracle
account, no permanent Windows service, removable with one command.

```powershell
docker run -d --name sems-oracle -p 1521:1521 `
  -e ORACLE_PASSWORD="YourPassword" `
  -e APP_USER=sems_user `
  -e APP_USER_PASSWORD="YourPassword" `
  gvenzl/oracle-xe:21-slim
```

`APP_USER` / `APP_USER_PASSWORD` create `sems_user` automatically with
`CONNECT` + `RESOURCE` and `UNLIMITED` quota, exactly what
[Step 1](#step-1-create-the-application-user) does, so **Step 1 can be skipped**
on this path.

First-time database init takes ~5-10 minutes. Wait for:

```powershell
docker logs sems-oracle | Select-String "DATABASE IS READY TO USE"
```

Test the connection (no need for `sqlplus` on Windows; use the one inside the
container):

```powershell
docker exec -it sems-oracle sqlplus sems_user/YourPassword@localhost:1521/XEPDB1
```

The service name is `XEPDB1`, same as the XE installer, so `DB_URL` in
[Step 3](#step-3-environment-variables) does not change.

Running the SQL scripts on the Docker path, copy them in first:

```powershell
docker cp src\main\resources\sql\schema.sql sems-oracle:/tmp/schema.sql
docker cp src\main\resources\sql\seed.sql   sems-oracle:/tmp/seed.sql
docker exec sems-oracle bash -c "cd /tmp && sqlplus -S sems_user/YourPassword@localhost:1521/XEPDB1 @schema.sql"
docker exec sems-oracle bash -c "cd /tmp && sqlplus -S sems_user/YourPassword@localhost:1521/XEPDB1 @seed.sql"
```

Managing the container:

```powershell
docker stop sems-oracle     # stop, data kept
docker start sems-oracle    # start again
docker rm -f sems-oracle    # remove entirely, data included
```

> The container has no volume, so `docker rm` deletes its data too. For a
> coursework app that is convenient (just re-run `schema.sql` and `seed.sql`).
> To persist data, add `-v sems-oracle-data:/opt/oracle/oradata` to `docker run`.

### If `sqlplus` is not recognised

The installer adds `sqlplus` to `PATH`, but terminals opened before the install
do not pick that up. **Close and reopen PowerShell**, then:

```powershell
sqlplus -V
```

Still not found? Call it by full path (adjust the version):

```powershell
& "C:\app\$env:USERNAME\product\21c\dbhomeXE\bin\sqlplus.exe" -V
```

Save it as a variable and use `$sql` in place of `sqlplus` below:

```powershell
$sql = "C:\app\$env:USERNAME\product\21c\dbhomeXE\bin\sqlplus.exe"
```

---

## Step 1: Create the application user

Create a dedicated database user for the app. Do not use `SYS` or `SYSTEM`.
(Skip this step on the Docker path; `sems_user` already exists.)

Connect as administrator, minding the **quotes**:

```powershell
sqlplus "sys/YourSysPassword@localhost:1521/XEPDB1 as sysdba"
```

> The quotes are required. Without them PowerShell parses `as sysdba` as separate
> arguments and the connection fails.

Then:

```sql
CREATE USER sems_user IDENTIFIED BY YourAppPassword;
GRANT CONNECT, RESOURCE TO sems_user;
ALTER USER sems_user QUOTA UNLIMITED ON USERS;
EXIT;
```

> Replace `YourAppPassword` with your own. It must never be written into any file
> in the repository.

---

## Step 2: Run schema.sql and seed.sql

> **PowerShell users:** `@` is the splatting operator, so `@schema.sql` is parsed
> as a variable and errors with `SplattingNotPermitted`. Arguments containing `@`
> must be quoted. In `cmd.exe` this does not apply.

From the project folder:

```powershell
cd "src\main\resources\sql"

sqlplus "sems_user/YourPassword@localhost:1521/XEPDB1" "@schema.sql"
sqlplus "sems_user/YourPassword@localhost:1521/XEPDB1" "@seed.sql"

cd ..\..\..\..
```

Alternative: connect first, then run the scripts:

```powershell
sqlplus "sems_user/YourPassword@localhost:1521/XEPDB1"
```

then at the `SQL>` prompt:

```sql
@schema.sql
@seed.sql
EXIT;
```

**Expected after `schema.sql`:** three `Table created.` lines, an
`Index created.`, a `Sequence created.`, and a "Schema berhasil dibuat" banner.

**Expected after `seed.sql`:** a row-count check reading `4 / 4 / 7`
(`mahasiswa` / `mata_kuliah` / `irs`) and a sample JOIN result.

Both scripts are safe to re-run; existing tables are dropped first.

---

## Inspecting the database

### a. `lihat.sql`, quickest for documentation screenshots

`src/main/resources/sql/lihat.sql` prints all three tables, the JOIN result, row
counts, table structure (`DESC`), and the constraint list in one run, with column
widths pre-set so output does not wrap. It is **read-only**: only `SELECT` and
`DESC`.

```powershell
# Docker
docker cp src\main\resources\sql\lihat.sql sems-oracle:/tmp/lihat.sql
docker exec sems-oracle bash -c "cd /tmp && sqlplus -S sems_user/YourPassword@localhost:1521/XEPDB1 @lihat.sql"

# sqlplus on Windows
cd src\main\resources\sql
sqlplus "sems_user/YourPassword@localhost:1521/XEPDB1" "@lihat.sql"
```

### b. Interactive `sqlplus`

```powershell
docker exec -it sems-oracle sqlplus sems_user/YourPassword@localhost:1521/XEPDB1
```

```sql
SELECT * FROM mahasiswa;
EXIT;
```

If output wraps, first run:

```sql
SET LINESIZE 140
SET PAGESIZE 60
COLUMN nama FORMAT A26
```

### c. SQL Developer / DBeaver

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `1521` |
| **Service name** | `XEPDB1` |
| Username | `sems_user` |
| Password | your chosen password |

Choose **Service name**, not SID. The wrong choice gives `ORA-12514`.

---

## Step 3: Environment variables

The app stores **no** credentials in any file. Values are read from environment
variables at startup.

```powershell
# Current session only
$env:DB_URL      = "jdbc:oracle:thin:@localhost:1521/XEPDB1"
$env:DB_USERNAME = "sems_user"
$env:DB_PASSWORD = "YourPassword"
```

Persist across sessions (run once, then open a new terminal):

```powershell
[Environment]::SetEnvironmentVariable("DB_URL",      "jdbc:oracle:thin:@localhost:1521/XEPDB1", "User")
[Environment]::SetEnvironmentVariable("DB_USERNAME", "sems_user",                               "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "YourPassword",                            "User")
```

Common `DB_URL` forms:

| Situation | Value |
|---|---|
| Oracle XE 21c (PDB service name) | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| Oracle XE 11g (SID) | `jdbc:oracle:thin:@localhost:1521:XE` |
| Other host/port | `jdbc:oracle:thin:@<host>:<port>/<service_name>` |

---

## Step 4: Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080>. Stop with `Ctrl + C`.

Alternative: run from the JAR:

```powershell
.\mvnw.cmd clean package
java -jar target\student-enrollment-management-system-1.0.0.jar
```

---

## Running the tests

```powershell
.\mvnw.cmd test
```

**The tests do not need Oracle.** They use `@WebMvcTest` with a mocked service
(Mockito), so no `DataSource` is created and no JDBC connection is attempted. The
command passes even with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` completely
unset.

| Class | Tests | Covers |
|---|---|---|
| `MahasiswaControllerTest` | 23 | Routes, redirects, HTTP status, per-field validation, POST-only delete, NIM-from-path invariant, `;jsessionid` regression |
| `TemplateRenderingTest` | 12 | Thymeleaf templates render to HTML, including the error page when the database is down |
| `MahasiswaServiceTest` | 16 | Business rules: duplicate NIM, missing NIM, total-SKS calculation |
| **Total** | **51** | |

---

## Screenshot checklist

For coursework documentation and the portfolio README. Take these on **desktop**
and repeat the ones marked 📱 at mobile width (DevTools → device toolbar →
375 px).

**Database:**

- [ ] 1. SQL\*Plus output after `@schema.sql`, `Table created.` three times
- [ ] 2. SQL\*Plus output after `@seed.sql`, row counts `4 / 4 / 7`
- [ ] 3. `SELECT * FROM mahasiswa;`
- [ ] 4. Manual JOIN result for one NIM (also printed at the end of `seed.sql`)
- [ ] 5. Table structure: `DESC mahasiswa;`, `DESC mata_kuliah;`, `DESC irs;`
- [ ] 6. Constraint list:
      `SELECT constraint_name, constraint_type, table_name FROM user_constraints WHERE table_name IN ('MAHASISWA','MATA_KULIAH','IRS');`

**Application:**

- [ ] 7. 📱 `/`, student list with data
- [ ] 8. `/` when empty, empty state
- [ ] 9. 📱 Add form, blank
- [ ] 10. Add form submitted blank, errors on all four fields
- [ ] 11. Add form with a duplicate NIM, "NIM sudah terdaftar" on the NIM field
- [ ] 12. Green flash after a successful add
- [ ] 13. 📱 Detail page for a student **with** courses, JOIN table with status badges
- [ ] 14. Detail page for a student **without** courses, empty state
- [ ] 15. Edit form pre-filled (NIM read-only)
- [ ] 16. Delete confirmation dialog
- [ ] 17. List after a delete, student gone and flash shown
- [ ] 18. 404 page, open `/students/00000000`

**Code and build:**

- [ ] 19. `.\mvnw.cmd test` output, `Tests run: 51, Failures: 0, Errors: 0`
- [ ] 20. `MahasiswaRepository.java`, the `SQL_FIND_DETAIL` JOIN query
- [ ] 21. Project structure in the editor

> For #8, easiest: `DELETE FROM irs; DELETE FROM mahasiswa; COMMIT;` in
> SQL\*Plus, screenshot, then re-run `@seed.sql` to restore the data.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Error: JAVA_HOME not found in your environment` | `JAVA_HOME` not set. See [Prerequisites](#prerequisites). |
| `sqlplus : The term 'sqlplus' is not recognized` | Oracle not installed, or `PATH` not refreshed. See [Step 0](#step-0-install-oracle-xe); reopen PowerShell after installing. |
| `The splatting operator '@' cannot be used...` | PowerShell: quote the argument as `"@schema.sql"`. See [Step 2](#step-2-run-schemasql-and-seedsql). |
| App starts but pages show "Gagal mengakses database" | Expected: HikariCP connects lazily, so startup succeeds even with Oracle down. Failure shows on the first data page. Check the Oracle service and the environment variables. |
| `Could not resolve placeholder 'DB_URL'` at startup | Environment variables not set, or set in a different terminal. Set them in the same terminal, then re-run. |
| `ORA-12541: TNS:no listener` | Oracle not running. Start `OracleServiceXE` and `OracleOraDB21Home1TNSListener` via `services.msc`. |
| `ORA-01017: invalid username/password` | `DB_USERNAME` / `DB_PASSWORD` wrong. Check with `$env:DB_USERNAME`. |
| `ORA-01017` even though the password looks exactly right | The password was likely read from a file PowerShell wrote with `Out-File -Encoding utf8` or `>`, which prepends an invisible **BOM** (`EF BB BF`). Check with `Format-Hex file.txt \| Select-Object -First 1`; if it starts `EF BB BF`, rewrite with `[System.IO.File]::WriteAllText($path, $text)`, or reset the password: `ALTER USER sems_user IDENTIFIED BY "NewPassword";` |
| `ORA-12514: service name not resolved` | Wrong service name in `DB_URL`. Try `XEPDB1`, or `XE` for Oracle 11g. |
| Page shows "Gagal mengakses database" | Tables not created. Run `schema.sql` and `seed.sql`. |
| `ORA-00942: table or view does not exist` | Scripts were run as a different user than `DB_USERNAME`. Use the same user for both. |
| `SP2-0734: unknown command beginning "CONSTRAINT..."` while running `schema.sql` | SQL\*Plus treats a **blank line as end of statement**, cutting `CREATE TABLE` short. `schema.sql` sets `SET SQLBLANKLINES ON` to prevent this, so this message means you are using an old copy of the script. |
| `schema.sql` prints "Schema berhasil dibuat" but the tables are missing | Old script version kept going after an error. The current one uses `WHENEVER SQLERROR EXIT SQL.SQLCODE`. Verify with `SELECT table_name FROM user_tables;`. |
| After adding a student the browser shows 404 although the data was saved | Old-version bug: Tomcat put `;jsessionid=...` in the redirect URL on a session's first request. Fixed via `server.servlet.session.tracking-modes=cookie` + `WebMvcConfig`, locked by a regression test. |
| Plain, unstyled layout | No internet connection, so Bootstrap and Google Fonts from the CDN fail to load. Layout still works with fallback fonts. |
| Port 8080 already in use | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` |

To see the SQL actually executed with its bind parameters, uncomment the last two
lines of `application.properties`.
