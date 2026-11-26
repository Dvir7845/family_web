# API & Permissions Reference

## Permissions Matrix

The `ADMIN` / `MEMBER` role and the boolean `driver` and `taskManager` flags are independent. Being an administrator does not automatically grant access to vehicles, and task management can be delegated to members via `taskManager=true` without granting full administrator privileges. These rules are enforced on the server (Spring Security `@PreAuthorize` with `@auth` and service-layer checks) as well as in the UI.

| Action | Member | Task Manager | Administrator |
| --- | --- | --- | --- |
| View shared tasks and list family members | Yes | Yes | Yes |
| Complete a task assigned to the current user | Yes | Yes | Yes |
| Create, edit, reassign, or delete tasks | No | Yes | Yes |
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

New accounts always receive the `MEMBER` role. Both `driver` and `taskManager` default to `false`. `/me` and `/members` expose `driver` and `taskManager` flags without returning passwords or password hashes.

---

## API Overview

All business endpoints require authentication. The login page (`/login.html`), static assets (`/assets/**`), `/csrf`, `/error`, `/actuator/health`, `/actuator/info`, and the OpenAPI/Swagger UI endpoints (`/swagger-ui/**`, `/v3/api-docs/**`) are public. Both browser form login (used by the web UI) and HTTP Basic (retained for Swagger UI, CLI/API clients, and automated tests) are supported, alongside per-username login rate limiting (5 failed attempts trigger a 5-minute cooldown). State-changing requests require a valid CSRF token.

List endpoints (`/tasks`, `/events`, `/shopping-items`) return Spring Data `Page<...>` JSON responses with `@PageableDefault(size = 50)`. Errors are returned as RFC `ProblemDetail` payloads by `GlobalExceptionHandler`.

| Method | Path | Purpose / Permission |
| --- | --- | --- |
| `GET` | `/csrf` | Obtain a CSRF token |
| `GET` | `/me` | Current user's ID, username, role, `driver`, and `taskManager` flags |
| `PATCH` | `/me/password` | Change own password using the current password |
| `GET` | `/members` | List member IDs, usernames, roles, `driver`, and `taskManager` flags; any signed-in user |
| `POST` | `/members` | Create a `MEMBER` account (`201 Created`); administrator only |
| `PATCH` | `/members/{id}/driver` | Enable or revoke driver status; administrator only |
| `PATCH` | `/members/{id}/task-manager` | Enable or revoke task manager status; administrator only |
| `POST` | `/members/{id}/promote` | Promote a member to administrator; administrator only |
| `DELETE` | `/members/{id}` | Delete another user (`204 No Content`); administrator only |
| `GET` | `/tasks` | Paginated shared task board (`Page<TaskResponse>`) |
| `GET` | `/tasks/{id}` | Task details |
| `GET` | `/tasks/statistics/weekly` | Weekly completion leaderboard (`weekStart`, `weekEnd`, member counts) |
| `GET` | `/tasks/assignees` | Available assignees; administrator or task manager only |
| `POST` | `/tasks` | Create and assign a `ONETIME` or `WEEKLY` task (`201 Created`); administrator or task manager only |
| `PUT` | `/tasks/{id}` | Update task details, schedule, and assignee (task type cannot change); administrator or task manager only |
| `DELETE` | `/tasks/{id}` | Delete a task (`204 No Content`); administrator or task manager only |
| `PATCH` | `/tasks/{id}/complete` | Complete a task assigned to the current user |
| `GET` | `/shopping-items` | Paginated shared shopping list (`Page<ShoppingItemResponse>`) |
| `GET` | `/shopping-items/{id}` | Shopping item details |
| `POST` | `/shopping-items` | Add a product (`201 Created`); any signed-in user |
| `PUT` | `/shopping-items/{id}` | Edit product/quantity and refresh `updatedAt` |
| `DELETE` | `/shopping-items/{id}` | Remove a product (`204 No Content`); any signed-in user |
| `GET` | `/events` | Paginated shared family events (`Page<EventResponse>`) |
| `GET` | `/events/{id}` | Event details |
| `POST` | `/events` | Create an event with invitees (`201 Created`); any signed-in user |
| `PUT` | `/events/{id}` | Update an event and invitees; any signed-in user |
| `DELETE` | `/events/{id}` | Delete an event (`204 No Content`); any signed-in user |
| `POST` | `/events/{eventId}/invite/{userId}` | Invite an existing user (`204 No Content`); any signed-in user |
| `GET` | `/vehicles` | List vehicles; drivers only |
| `POST` | `/vehicles` | Add a vehicle (`201 Created`); administrator who is a driver only |
| `DELETE` | `/vehicles/{id}` | Delete a vehicle and all its bookings (`204 No Content`); administrator who is a driver only |
| `GET` | `/vehicles/bookings?month=YYYY-MM` | Bookings across vehicles for the selected month; drivers only |
| `POST` | `/vehicles/bookings` | Reserve a slot (`MORNING`, `AFTERNOON`, `EVENING`) for the signed-in driver (`201 Created`) |
| `DELETE` | `/vehicles/bookings/{id}` | Cancel own booking, or another user's booking as an administrator who is a driver (`204 No Content`) |

---

## Request Examples

Use real IDs from your database. Values below are illustrative only.

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

Reserve a slot (`POST /vehicles/bookings`):

```json
{
  "vehicleId": 1,
  "date": "2099-12-10",
  "slot": "MORNING"
}
```
