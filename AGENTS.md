# Family Home – Agent Instructions

Permanent conventions for coding agents working in this repository. Open backlog tasks are tracked locally in `IMPROVEMENT_PLAN.md` (gitignored).

## 1. Scope & Git Hygiene
- Work **only** on the task(s) requested by the user in the current session. Do not pick up additional items from `IMPROVEMENT_PLAN.md` without asking.
- Keep existing uncommitted work safe: never run `git reset`, `git stash`, or `git checkout -- <file>` on modified files. Make small, focused edits.
- Never run `git commit` or `git push` unless explicitly asked.

## 2. Testing & Verification (Mandatory)
- Whenever you change behavior, add or update the matching tests (Java in `src/test/java/...`, frontend in `src/test/js/`).
- **Always run tests** to verify changes — never leave code or test edits unverified.
- While iterating, run targeted tests (`.\mvnw.cmd test -Dtest=ClassName` or `node .\src\test\js\<file>.cjs`).
- Before finishing, run the full Java test suite (`.\mvnw.cmd test`, ~12 seconds total) to catch cross-cutting regressions; if frontend code or API response shapes changed, also run `node .\src\test\js\vehicle-calendar.test.cjs` and `node .\src\test\js\frontend.test.cjs`.
- If the environment prevents running the full suite, run at minimum `.\mvnw.cmd test-compile` plus all affected test classes and clearly report what was not run and why.
- **Windows sandbox note:** If Maven hits offline or lock errors in a sandboxed shell, pass `-o "-Daether.syncContext.named.factory=noop"`. If Surefire fails due to Java 25 dynamic agent attach (`\\.\pipe\javatool-...`), report the failure rather than skipping verification silently.

## 3. Architecture & Conventions
- Follow package-by-feature under `com.familyhome`: `config`, `event`, `exception`, `member`, `security`, `shopping`, `task`, `vehicle`.
- Use constructor injection only (no field `@Autowired`).
- Use Java `record`s for request and response DTOs; controllers must never return JPA entities directly.
- Throw domain exceptions from `com.familyhome.exception` (never `ResponseStatusException`). API errors are serialized as RFC `ProblemDetail` via `GlobalExceptionHandler`.

## 4. Database & Flyway Migrations
- `spring.jpa.hibernate.ddl-auto` must stay `validate`.
- Never edit any already-committed Flyway migration.
- Migrations are split across **two** directories:
  - `src/main/resources/db/migration/` — vendor-neutral SQL that must run on **both** PostgreSQL and H2 tests (currently has `V1__init.sql`).
  - `src/main/resources/db/postgresql/` — PostgreSQL-only migrations skipped by H2 tests (currently has `V2__migrate_legacy_event_invitees.sql`).
- Always check **both** directories before picking the next version number `V<n>` so versions never collide on PostgreSQL (next version is `V3`).

## 5. Time & Time Zones
- All "today/now" logic must use the injected `java.time.Clock` — never call `LocalDate.now()` or `Instant.now()` without a clock.
- `TimeConfig` provides a zone-aware `Clock` (`Clock.system(appZoneId)`). Services should call `LocalDate.now(clock)` / `Instant.now(clock)` (and `clock.getZone()` when a `ZoneId` is needed) rather than hardcoding `ZoneId.of("Asia/Jerusalem")`. Date-boundary unit tests should pass a zone-aware `Clock.fixed(..., ZoneId.of("Asia/Jerusalem"))`.
- `@Scheduled` cron jobs must use `zone = "${app.time-zone:Asia/Jerusalem}"`.

## 6. Code Style & Formatting
- Spotless with `google-java-format` (`AOSP`) is configured in `pom.xml`.
- No wildcard imports (`import ...*;`) and no fully-qualified class names inside method bodies (`google-java-format` does not expand wildcard imports automatically).
- Do not run `.\mvnw.cmd spotless:apply` across unrelated files until the one-time formatting commit (`P0-1` in `IMPROVEMENT_PLAN.md`) is completed and `spotless:check` is bound to `verify`.

## 7. Localization
- The browser UI is Hebrew RTL: all user-facing frontend strings stay in Hebrew.
- Backend API error messages and `ProblemDetail` details stay in English.
