# Family Tasks API

A Spring Boot backend for a shared family task board. Family members can see who is responsible for each task and whether it has been completed. Administrators manage tasks and member accounts; only the assigned member can mark a task as completed.

This is a learning and portfolio project. The current version supports one family and provides an API; a user interface and hosted deployment are planned.

## Stack

- Java 25 and Spring Boot 4.1.2
- Spring Web MVC, Spring Security and Bean Validation
- Spring Data JPA / Hibernate with PostgreSQL
- JUnit, Mockito, Spring Security Test and MockMvc
- H2 for isolated automated tests
- Maven Wrapper

## Permissions

| Action | Member | Administrator |
| --- | --- | --- |
| View all family tasks and assigned members | Yes | Yes |
| Complete a task assigned to the current user | Yes | Yes |
| Create, edit, reassign or delete tasks | No | Yes |
| List possible assignees and create member accounts | No | Yes |

Passwords are hashed with BCrypt. Member creation always assigns the `MEMBER` role. API responses do not return password hashes.

## Run locally on Windows

Prerequisites: JDK 25, PostgreSQL, and PowerShell. Maven does not need to be installed separately.

1. Create a PostgreSQL database named `family_tasks`.
2. Review `src/main/resources/application.properties`. The local defaults use `localhost:5432`, database `family_tasks`, and database user `postgres`.
3. On the **first run against an empty database**, choose the initial application password in PowerShell:

   ```powershell
   $initialSecret = Read-Host "Choose initial application password" -AsSecureString
   $env:APP_LOGIN_PASSWORD = [System.Net.NetworkCredential]::new("", $initialSecret).Password
   ```

   The application creates the initial administrator with username `dvir`. This variable is only needed to create that account; changing it later does not change the stored password.

4. From the project directory, run:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\start-server.ps1
   ```

   Enter the **PostgreSQL password** when prompted. It is separate from the application login password. The script supplies `DB_PASSWORD` only while the server runs and does not save it to a file. The execution-policy option applies to this child process only.

5. Open `http://localhost:8080/tasks` and sign in. Use Ctrl+C to stop the server.

The database must remain available while the application runs. Development schema management currently uses `spring.jpa.hibernate.ddl-auto=update`.

## API overview

All endpoints require authentication. Browser form login and HTTP Basic are available through Spring Security's default configuration. State-changing requests also require a valid CSRF token; the MockMvc tests show authenticated requests with CSRF protection.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/me` | Current user's ID, username and role |
| GET | `/tasks` | Shared family task board |
| GET | `/tasks/{id}` | Task details |
| GET | `/tasks/assignees` | Available assignees; administrators only |
| POST | `/tasks` | Create and assign a task; administrators only |
| PUT | `/tasks/{id}` | Update task details and assignee; administrators only |
| DELETE | `/tasks/{id}` | Delete a task; administrators only |
| PATCH | `/tasks/{id}/complete` | Complete a task assigned to the current user |
| POST | `/members` | Create a member account; administrators only |

Example task request (use a real member ID):

```json
{
  "name": "Shopping",
  "day": "Sunday",
  "description": "Buy milk and bread",
  "assigneeId": 2
}
```

The internal field `owner` currently represents the member assigned to perform a task. It does not restrict who can view the task.

## Tests

```powershell
.\mvnw.cmd test
```

The test suite uses mocked repositories or isolated H2 databases. It does not require a running PostgreSQL server or real database credentials. Fixed passwords in test sources are disposable test fixtures only.

Coverage includes role restrictions, assignment, shared visibility, completion permissions, BCrypt password storage, authentication, and response fields.

## Build an executable JAR

```powershell
.\mvnw.cmd package
```

The executable is written to `target/rest-service-0.0.1-SNAPSHOT.jar`. To run it directly, supply the PostgreSQL password in the same PowerShell process:

```powershell
$dbSecret = Read-Host "Enter PostgreSQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbSecret).Password
java -jar .\target\rest-service-0.0.1-SNAPSHOT.jar
```

For a new database, also set `APP_LOGIN_PASSWORD` before the first startup as described above. Rebuild the JAR after source changes. Database contents and environment variables are not part of the JAR.

## Project structure

```text
com.example.rest_service
├── RestServiceApplication
├── task       # Task entity, API, business rules and persistence
├── member     # Accounts, roles and safe response objects
├── security   # Authentication and initial administrator setup
└── exception  # Validation error handling
```

## Before hosted deployment

- Provide a user interface and account/password management flows.
- Configure HTTPS, hosted PostgreSQL, backups, and secret management.
- Replace development schema updates with managed database migrations.
- Keep the current single-family scope explicit; multiple families would require membership and data isolation.

Do not commit real passwords, tokens, environment files or database backups. Build output is excluded through `.gitignore`.
