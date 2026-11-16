# Family Home

A shared family home board built with Spring Boot and a Hebrew, right-to-left browser interface. The application brings together household tasks, shopping, family events, member accounts and vehicle bookings.

This is a learning and portfolio project for a single family. The frontend is served by the same application as the API and requires no frontend installation or build step. Hosted deployment is planned.

## Features

- **Tasks:** shared visibility, assignment, search, type/day/status filters and completion by the assigned member. Supports both **one-time tasks** (with a specific date and overdue indicator) and **weekly recurring tasks** (with a fixed day of the week). Administrators and designated task managers create, edit, reassign and delete tasks.
- **Shopping:** all signed-in family members can add, update and remove products. The server tracks update times and automatically removes stale items.
- **Family events:** shared events with dates, invitee selection, editing and automatic cleanup of old events. The interface displays Gregorian and Hebrew dates.
- **Member management:** administrators create accounts, enable or revoke driver status, promote members to administrators, and delete other users after confirmation.
- **Password changes:** each user can change their own password by supplying the current password. The user must sign in again afterward.
- **Vehicles:** drivers can select a vehicle and month, see who booked each day, and reserve morning, afternoon or evening slots. Only administrators who are also drivers can add or delete vehicles. Deleting a vehicle also removes all its bookings.
- **Mobile layout:** two-column navigation on phones, daily vehicle cards, stacked form fields, wrapping buttons and long text, and scrollable dialogs that fit narrow screens.

## Stack

- Java 25 and Spring Boot 4.1.2
- Spring Web MVC, Spring Security and Bean Validation
- Spring Data JPA / Hibernate with PostgreSQL
- JUnit, Mockito, Spring Security Test and MockMvc
- H2 for isolated automated tests
- Maven Wrapper
- HTML, CSS and browser JavaScript
- Node.js built-in modules for the vehicle calendar UI test; no npm dependencies

## Permissions

The `ADMIN` / `MEMBER` role and the boolean `driver` and `taskManager` flags are separate. Being an administrator does not automatically grant access to vehicles, and task management can be delegated to members via `taskManager=true` without granting full administrator privileges. These rules are enforced by the server as well as the interface.

| Action | Member | Task Manager | Administrator |
| --- | --- | --- | --- |
| View shared tasks and list family members | Yes | Yes | Yes |
| Complete a task assigned to the current user | Yes | Yes | Yes |
| Create, edit, reassign or delete tasks | No | Yes | Yes |
| Manage shared shopping items and family events | Yes | Yes | Yes |
| Change own password using the current password | Yes | Yes | Yes |
| Create member accounts | No | No | Yes |
| Enable or revoke a user's `driver` or `taskManager` status | No | No | Yes |
| Delete another user | No | No | Yes |
| Promote a member to administrator | No | No | Yes |
| Delete own account | No | No | No |
| View vehicles and monthly bookings | Drivers only | Drivers only | Drivers only |
| Reserve a vehicle slot on own behalf | Drivers only | Drivers only | Drivers only |
| Cancel own booking | Drivers only | Drivers only | Drivers only |
| Cancel another user's booking | No | No | Drivers only |
| Add or delete a vehicle | No | No | Drivers only |

New accounts always receive the `MEMBER` role. Both `driver` and `taskManager` flags default to `false` when omitted, including for existing accounts when the columns are added. `/me` and `/members` expose `driver` and `taskManager` status without returning passwords or password hashes.

## Run locally on Windows

Prerequisites: JDK 25, PostgreSQL and PowerShell. Maven does not need to be installed separately. Node.js is optional and is only needed to run the standalone JavaScript test.

The Java base package is `com.familyhome` (Java package names cannot contain hyphens). The Maven artifact and application name are `family-home`.

Open `family-home.code-workspace` from the project directory, or open the directory containing `pom.xml` directly in VS Code, so that the Maven project and Git repository are the workspace root. The physical project directory is currently named `rest-service`; it can be renamed to `family-home` once programs holding it open have been closed. Run the following commands from the directory containing `pom.xml`, `mvnw.cmd` and `start-server.ps1`.

1. Create a PostgreSQL database named `family_tasks`.
2. Review [application.properties](src/main/resources/application.properties). The local defaults use `localhost:5432`, database `family_tasks` and database user `postgres`.
3. On the **first run against an empty database**, choose the initial application password in PowerShell:

   ```powershell
   $initialSecret = Read-Host "Choose initial application password" -AsSecureString
   $env:APP_LOGIN_PASSWORD = [System.Net.NetworkCredential]::new("", $initialSecret).Password
   ```

   The application creates the initial administrator with username `dvir`. This variable is only needed to create that account; changing it later does not change the stored password. Use the interface's password change form for an existing account.

4. Start the server:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\start-server.ps1
   ```

   Enter the **PostgreSQL password** when prompted. It is separate from the application login password. The script supplies `DB_PASSWORD` only while the server runs and does not save it to a file. The execution-policy option applies to this child process only.

5. Open `http://localhost:8080/` and sign in. Use Ctrl+C to stop the server.

### Enable vehicle access

1. Sign in as an administrator and open **בני המשפחה** (family members).
2. Select **נהג / נהגת** for the users who should have vehicle access. The administrator must also be marked as a driver to use vehicle management.
3. Open **מי משתמש ברכב** (vehicle usage). An administrator who is a driver can add a vehicle.
4. Select a vehicle and month, then choose a free morning, afternoon or evening slot. The reservation is made for the signed-in user.

The database must remain available while the application runs. Development schema management uses `spring.jpa.hibernate.ddl-auto=update`; on the next startup it adds the driver column and vehicle tables to an existing database. Restart the server after backend changes, rebuild any executable JAR in use, and refresh the browser to load updated frontend assets.

## Browser interface

The interface lives in [src/main/resources/static](src/main/resources/static). It includes task, shopping, event and driver-only vehicle tabs, administrator member management and a password change button for every user.

- Desktop vehicle usage is displayed as a monthly grid. At phone widths up to 580px, it becomes a list of daily cards with the weekday and three booking buttons.
- On phones, navigation uses two columns. Tables become cards, form fields stack, action buttons wrap and long names/descriptions stay within their containers.
- On tablets up to 820px, navigation and account controls use a compact header. On wider screens the sidebar layout is retained.
- The browser supplies CSRF tokens for login, logout and changes. Passwords and tokens are not saved in browser storage.
- Data is loaded from the API. Use the refresh button to see changes made by other family members.
- Shopping update times and the displayed current date use the `Asia/Jerusalem` timezone.

## Accounts and passwords

Administrators can mark new users as drivers during account creation or change the flag for existing users. The family member management screen shows each user's role and offers **הפיכה למנהל** (promote to administrator) for members. Promotion requires confirmation and expires the promoted user's registered sessions after commit; they must sign in again to load their administrator permissions. Repeating promotion for an existing administrator has no effect. Driver status is unchanged. Usernames contain 3–30 English letters, digits or underscores. Created accounts use passwords of 8–72 characters, subject to BCrypt's 72-byte UTF-8 limit.

Every user can open **שינוי סיסמה** (change password), enter the current password and confirm a new password. The new password must contain 8–72 characters and at most 72 UTF-8 bytes. Passwords are hashed with BCrypt. After a successful change, the current browser session is logged out and other registered sessions expire, requiring a new login.

Deleting another user requires administrator access and confirmation in the interface. The deletion:

- Preserves the user's tasks but removes their assignee.
- Removes the user from event invitee lists while preserving the events.
- Cancels the user's vehicle bookings while preserving vehicles.
- Deletes the account and expires its registered sessions.

Self-deletion is blocked. Session expiration is applied after the database transaction commits; other registered sessions are rejected on their next request.

## Vehicle booking rules

- Vehicle access requires `driver=true`, including for administrators.
- Only an administrator who is a driver can add a vehicle, using a name or identifying label of up to 100 characters.
- The same permission is required to delete the selected vehicle. The interface asks for confirmation; deletion removes all bookings for that vehicle, including bookings outside the selected month, while preserving other vehicles and their bookings.
- Each vehicle has three slots per date: `MORNING`, `AFTERNOON` and `EVENING`.
- A database unique constraint prevents two bookings for the same vehicle, date and slot, including competing requests. A conflicting reservation returns HTTP `409`.
- Reservations belong to the signed-in user; clients cannot choose another booking owner.
- Dates before today in `Asia/Jerusalem` are rejected. There are no separate clock-time cutoffs for today's slots.
- A driver can cancel their own booking. An administrator who is a driver can also cancel another user's booking.

## Automatic cleanup

- **Tasks:**
  - **One-time tasks:** a daily job at midnight in `Asia/Jerusalem` removes completed `ONETIME` tasks. Open one-time tasks remain on the board until completed.
  - **Weekly tasks:** a weekly job on Sunday at midnight in `Asia/Jerusalem` resets the completion status (`completed`, `completedAt`, `completedByUsername`) of all `WEEKLY` tasks for the new week.
- **Shopping:** the server sets `updatedAt` on creation and update. Items not updated for at least seven days are removed when the scheduled job first runs after startup, then with a 24-hour delay between runs. Viewing a list does not refresh update times.
- **Events:** a daily job at midnight in `Asia/Jerusalem` removes events dated more than 30 days before today. Events exactly 30 days old remain until a later run.

## API overview

All business endpoints require authentication. The login page, frontend assets and `/csrf` are public. Browser form login and HTTP Basic are supported. State-changing requests require a valid CSRF token; the browser interface handles this automatically. The MockMvc tests demonstrate authenticated requests and CSRF usage.

| Method | Path | Purpose / permission |
| --- | --- | --- |
| GET | `/csrf` | Obtain a CSRF token |
| GET | `/me` | Current user's ID, username, role, `driver` and `taskManager` flags |
| PATCH | `/me/password` | Change own password using the current password |
| GET | `/members` | List member IDs, usernames, roles, `driver` and `taskManager` flags; any signed-in user |
| POST | `/members` | Create a `MEMBER` account; administrator only |
| PATCH | `/members/{id}/driver` | Enable or revoke driver status; administrator only |
| PATCH | `/members/{id}/task-manager` | Enable or revoke task manager status; administrator only |
| POST | `/members/{id}/promote` | Promote a member to administrator; administrator only |
| DELETE | `/members/{id}` | Delete another user; administrator only |
| GET | `/tasks` | Shared task board |
| GET | `/tasks/{id}` | Task details |
| GET | `/tasks/assignees` | Available assignees; administrator or task manager only |
| POST | `/tasks` | Create and assign a `ONETIME` or `WEEKLY` task; administrator or task manager only |
| PUT | `/tasks/{id}` | Update task details, schedule and assignee (task type cannot change); administrator or task manager only |
| DELETE | `/tasks/{id}` | Delete a task; administrator or task manager only |
| PATCH | `/tasks/{id}/complete` | Complete a task assigned to the current user |
| GET | `/shopping-items` | Shared shopping list |
| GET | `/shopping-items/{id}` | Shopping item details |
| POST | `/shopping-items` | Add a product; any signed-in user |
| PUT | `/shopping-items/{id}` | Edit product/quantity and refresh its update time |
| DELETE | `/shopping-items/{id}` | Remove a product; any signed-in user |
| GET | `/events` | Shared family events |
| GET | `/events/{id}` | Event details |
| POST | `/events` | Create an event with invitees; any signed-in user |
| PUT | `/events/{id}` | Update an event and invitees; any signed-in user |
| DELETE | `/events/{id}` | Delete an event; any signed-in user |
| POST | `/events/{eventId}/invite/{userId}` | Invite an existing user; any signed-in user |
| GET | `/vehicles` | List vehicles; drivers only |
| POST | `/vehicles` | Add a vehicle; administrator who is a driver only |
| DELETE | `/vehicles/{id}` | Delete a vehicle and all its bookings; administrator who is a driver only |
| GET | `/vehicles/bookings?month=YYYY-MM` | Bookings across vehicles for the selected month; drivers only |
| POST | `/vehicles/bookings` | Reserve a slot for the signed-in driver |
| DELETE | `/vehicles/bookings/{id}` | Cancel own booking, or another user's booking as an administrator who is a driver |

### Request examples

Use real IDs from your database. The values below are illustrative, not application credentials.

Create a member (`POST /members`):

```json
{
  "username": "family_member",
  "password": "ExampleOnly123!",
  "driver": true,
  "taskManager": false
}
```

Enable driver access (`PATCH /members/{id}/driver`):

```json
{
  "driver": true
}
```

Enable task manager access (`PATCH /members/{id}/task-manager`):

```json
{
  "taskManager": true
}
```

Change own password (`PATCH /me/password`):

```json
{
  "currentPassword": "ExampleOnly123!",
  "newPassword": "AnotherExampleOnly123!"
}
```

Create a weekly task (`POST /tasks`):

```json
{
  "name": "Shopping",
  "type": "WEEKLY",
  "day": "SUNDAY",
  "description": "Buy milk and bread",
  "assigneeId": 2
}
```

Create a one-time task (`POST /tasks`):

```json
{
  "name": "Fix sink",
  "type": "ONETIME",
  "taskDate": "2099-12-10",
  "description": "Call plumber",
  "assigneeId": 2
}
```

The internal task field `owner` is omitted from JSON (`@JsonIgnore`); responses expose `assigneeId` and `assigneeUsername` instead. It does not restrict who can view the task.

Create an event (`POST /events`):

```json
{
  "description": "Family dinner",
  "eventDate": "2099-12-10",
  "invitedUserIds": [2]
}
```

Add a vehicle (`POST /vehicles`):

```json
{
  "name": "Family car"
}
```

Reserve a slot (`POST /vehicles/bookings`); choose today or a future date:

```json
{
  "vehicleId": 1,
  "date": "2099-12-10",
  "slot": "MORNING"
}
```

## Tests and validation

Run the Java test suite:

```powershell
.\mvnw.cmd test
```

The suite uses mocked repositories or isolated H2 databases. It does not require a running PostgreSQL server or real database credentials. Fixed passwords in test sources are disposable test fixtures only.

Coverage includes authentication, CSRF, task assignment/completion permissions, shared shopping and events, cleanup rules, account creation/deletion, BCrypt storage, current-password validation, session expiration, driver access, monthly bookings, cancellation permissions, duplicate-slot rejection, vehicle deletion with booking cleanup, and administrator promotion with session expiration.

With Node.js installed, run the standalone vehicle calendar UI test from the project directory:

```powershell
node .\src\test\js\vehicle-calendar.test.cjs
```

This test uses Node.js built-in modules and a simulated DOM to check leap-year February, free/booked slots and administrator controls. It does not render CSS.

The latest implementation checks passed all **61 Java tests** and the vehicle calendar JavaScript test. Mobile layout was additionally checked in headless Chrome using local sample data: **72 layout measurements** across **320, 375, 390, 430, 580, 768, 1024 and 1440px**, with no page-level or dialog horizontal overflow, followed by a visual check of the phone layout. Those browser checks were a local validation exercise; generated previews and measurements are in ignored `target/`, not a committed browser test suite.

## Build an executable JAR

```powershell
.\mvnw.cmd package
```

The executable is written to `target/family-home-0.0.1-SNAPSHOT.jar`. To run it directly, supply the PostgreSQL password in the same PowerShell process:

```powershell
$dbSecret = Read-Host "Enter PostgreSQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbSecret).Password
java -jar .\target\family-home-0.0.1-SNAPSHOT.jar
```

For a new database, also set `APP_LOGIN_PASSWORD` before the first startup as described above. Rebuild the JAR after source changes. Database contents and environment variables are not part of the JAR.

## Project structure

```text
src/
├── main/
│   ├── java/com/familyhome/
│   │   ├── FamilyHomeApplication.java
│   │   ├── task/       # Shared tasks, assignment and completion
│   │   ├── shopping/   # Shopping items and expiry cleanup
│   │   ├── event/      # Events, invitees and daily cleanup
│   │   ├── vehicle/    # Vehicles, booking slots and driver restrictions
│   │   ├── member/     # Accounts, passwords, deletion and driver management
│   │   ├── security/   # Login, CSRF, session registry and initial administrator
│   │   └── exception/  # Validation error handling
│   └── resources/
│       ├── application.properties
│       └── static/     # Hebrew RTL pages, JavaScript, CSS and icons
└── test/
    ├── java/           # Unit, repository, API and security tests
    └── js/             # Standalone vehicle calendar UI test
```

## Before hosted deployment

- Configure HTTPS, hosted PostgreSQL, backups and secret management.
- Replace development schema updates with managed database migrations.
- Add a forgotten-password recovery flow; authenticated password changes already exist.
- The session registry is currently in memory in one application instance. Shared session storage and expiration coordination would be needed for multiple server instances.
- Keep the single-family scope explicit; multiple families would require membership and data isolation.

Do not commit real passwords, tokens, environment files or database backups. Build output, local preview files under `target/` and runtime logs are excluded through `.gitignore`.

## Weekly task leaderboard

The task tab shows a weekly bar chart for every current family member, including zero counts. Gold bars and a trophy mark every member tied for the highest positive count. Weeks run Sunday through Saturday in Asia/Jerusalem. `GET /tasks/statistics/weekly` requires authentication and returns `weekStart`, `weekEnd` and member counts.

Completion now stores `completedAt` and `completedByUsername` on the task. Repeated completion preserves the first timestamp and performer; reassignment does not transfer credit. Existing completed tasks without a recorded completion date are excluded. Counts use tasks still present in the database; deleting a task removes it from these statistics, and deleted accounts are not listed. Hibernate adds the new nullable columns on startup with the existing development schema configuration.
