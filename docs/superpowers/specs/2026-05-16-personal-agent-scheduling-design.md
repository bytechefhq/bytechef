# Personal Agent Scheduling (AI Hub)

**Status:** Design
**Author:** Ivica Cardic
**Date:** 2026-05-16
**Scope:** EE (Enterprise Edition); `automation-ai-hub` module

## Problem

AI Hub personal agents today are reactive: a user picks an agent from the
sidebar, a fresh `PERSONAL_AGENT`-kind task is created, and the user drives
the conversation by hand. There is no way to have an agent run a stored
prompt on a cadence (e.g., "every weekday at 9 AM, summarize my open issues
and email me the result").

We need to let users schedule one or more "scheduled tasks" per personal
agent. Each scheduled task carries a frequency, a timezone, an optional
start date, an optional run-count cap, and a prompt. When the schedule
fires, the platform creates a new `PERSONAL_AGENT` task as if the user had
clicked the agent in the sidebar and sent the stored prompt as the first
turn.

## Non-goals (v1)

- Per-fire run-history UI (we record `last_run_at` and a Micrometer counter,
  but no detailed history table).
- Per-fire failure record table. Three consecutive failures disable the
  schedule silently (with a metric); a failure history table is deferred.
- Sharing or org-wide scheduling. A schedule is owned by the user who
  created it; workspace admins do not manage other users' schedules.
- Backfilling "missed runs" if the app was down past `nextRunAt`. Quartz's
  default `SMART_POLICY` misfire handling will fire once on recovery and
  resume from there. Catch-up loops are out of scope.
- Templates / cloning a schedule between agents.
- Time-window restrictions ("only between 9 AM and 5 PM").

## User-facing surface

A new **Schedules** tab on the personal-agent detail page lists existing
schedules and exposes a **+ New scheduled task** button. The button opens a
dialog whose layout mirrors the provided mockups:

- **Title** (text, required) — surfaced in the schedule list and copied
  into the created task's `title`.
- **Task description** (textarea, required) — the prompt sent to the agent
  as the first user message on each fire.
- **Run frequency** (select) — one of `Every X Minutes`, `Hourly`, `Daily`,
  `Weekly`, `Monthly`, `Custom (Cron)`. Selecting a value reveals the
  matching sub-fields:
  - `Every X Minutes`: integer minutes input.
  - `Hourly`: minute-of-hour (0–59).
  - `Daily`: time-of-day.
  - `Weekly`: day-of-week + time-of-day.
  - `Monthly`: day-of-month + time-of-day.
  - `Custom (Cron)`: free-form cron expression (6-field Quartz syntax —
    `seconds minutes hours day-of-month month day-of-week` — validated
    server-side).
- **Time** (time picker) — shown for `Hourly`, `Daily`, `Weekly`, `Monthly`.
- **Timezone** (searchable select of IANA zones) — required.
- **Start date** (optional date picker, default "Starts immediately").
- **Lifecycle** (radio: `Recurring` | `Number of runs`).
  - `Recurring`: no cap.
  - `Number of runs` with an optional `Max runs` integer; placeholder is
    "No limit". Empty == unbounded (semantically equivalent to `Recurring`,
    but the UI preserves the user's chosen radio).
- A live preview line at the bottom — `At 9:00 AM (GMT+2) — Next run:
  May 17, 2026, 9:00 AM` — derived from the server-computed `nextRunAt`.

The list view shows: Title, frequency summary (`Daily at 9:00 AM Europe/Zagreb`),
next-run timestamp, enabled toggle, and an ellipsis menu (Edit, Disable/
Enable, Delete).

## Architecture

### Module placement

- **Domain + service + repository + Quartz listener:**
  `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/`
  (rationale: this is automation-package, so per the memory note
  "automation-package entities keep `workspaceId` directly" — no relation
  table needed).
- **GraphQL controller extension:**
  `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/`
  (extends the existing `AiHubPersonalAgentGraphQlController`).
- **Scheduler API extension:**
  `server/libs/platform/platform-scheduler/platform-scheduler-api/` and
  `platform-scheduler-impl/`. New API surface is non-EE so the impl module
  stays Apache-licensed; the EE listener consumes the platform event.
- **Frontend:**
  `client/src/pages/automation/ai-hub/personal-agents/schedules/`.

### Data model

One new table, EE Liquibase changelog under
`server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/`:

```
ai_hub_personal_agent_schedule
  id                        BIGINT PK (auto-increment)
  ai_hub_personal_agent_id  BIGINT  NOT NULL   FK → ai_hub_personal_agent(id) ON DELETE CASCADE
  workspace_id              BIGINT  NOT NULL   -- automation-package convention
  user_id                   BIGINT  NOT NULL   -- the user the run executes as (also the owner)
  environment               INT     NOT NULL   -- Environment ordinal
  title                     VARCHAR(255) NOT NULL
  prompt                    TEXT    NOT NULL   -- the "Task description" field (mockup)
  frequency_kind            INT     NOT NULL   -- ScheduleFrequencyKind ordinal
  interval_minutes          INT          NULL  -- EVERY_X_MINUTES only
  minute_of_hour            INT          NULL  -- HOURLY only (0–59)
  time_of_day               TIME         NULL  -- DAILY/WEEKLY/MONTHLY
  day_of_week               INT          NULL  -- WEEKLY (1=Mon … 7=Sun, ISO)
  day_of_month              INT          NULL  -- MONTHLY (1–31)
  cron_expression           VARCHAR(255) NULL  -- CUSTOM_CRON only; raw input from user
  effective_cron_expression VARCHAR(255) NOT NULL -- canonicalized; what Quartz sees
  zone_id                   VARCHAR(64)  NOT NULL
  start_date                TIMESTAMP    NULL
  lifecycle_kind            INT     NOT NULL   -- ScheduleLifecycleKind ordinal
  max_runs                  INT          NULL  -- null = unbounded
  remaining_runs            INT          NULL  -- mirrors max_runs at create; decremented per fire
  consecutive_failures      INT     NOT NULL DEFAULT 0
  enabled                   BOOLEAN NOT NULL DEFAULT TRUE
  last_run_at               TIMESTAMP    NULL
  next_run_at               TIMESTAMP    NULL  -- denormalized for sidebar; recomputed per fire
  created_by                VARCHAR(50)  NOT NULL
  created_date              TIMESTAMP    NOT NULL
  last_modified_by          VARCHAR(50)  NOT NULL
  last_modified_date        TIMESTAMP    NOT NULL
  version                   INT     NOT NULL DEFAULT 0
```

Indexes:
- `idx_ai_hub_personal_agent_schedule_agent (ai_hub_personal_agent_id)` for
  the list-by-agent query.
- `idx_ai_hub_personal_agent_schedule_workspace_user (workspace_id, user_id)`
  for the per-user listing in admin views.

### Enums (INT ordinals, append-only per project convention)

```java
public enum ScheduleFrequencyKind {
    EVERY_X_MINUTES, HOURLY, DAILY, WEEKLY, MONTHLY, CUSTOM_CRON
}

public enum ScheduleLifecycleKind {
    RECURRING, NUMBER_OF_RUNS
}
```

Both get pinned mappings in `EnumOrdinalStabilityTest`.

### Server-side normalization (write path)

Before persisting, the service normalizes the input:

1. **Cron derivation** — translate structured frequency fields into a
   single 6-field Quartz cron expression stored in `effective_cron_expression`.
   `CUSTOM_CRON` validates the user-supplied expression by attempting
   `org.quartz.CronExpression.validateExpression(...)`; on success the
   expression is stored as-is. Note: Spring's
   `org.springframework.scheduling.support.CronExpression` is also
   6-field but uses `*` where Quartz requires `?` for the
   day-of-month/day-of-week mutex; we standardize on Quartz syntax for
   `effective_cron_expression` since Quartz is what executes.
2. **Lifecycle cleanup** — if `lifecycle_kind = RECURRING`, set
   `max_runs = null` and `remaining_runs = null`. If `lifecycle_kind =
   NUMBER_OF_RUNS` and `max_runs IS NULL`, also leave `remaining_runs =
   null` (Number-of-runs with No-limit collapses to unbounded).
3. **Field consistency** — clear sub-fields that don't belong to the
   chosen `frequency_kind` (e.g., wipe `day_of_week` when `frequency_kind
   = DAILY`).
4. **Computed metadata** — set `next_run_at` from
   `Trigger.getFireTimeAfter(max(now, start_date))`.

The normalized form is what the DB holds; the GraphQL response surfaces
both the raw structured fields and the derived `effective_cron_expression`
+ `next_run_at` for the preview.

### Scheduler layer

New API in `platform-scheduler-api`:

```java
public interface AgentScheduler {
    void scheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, Instant startAt);
    void rescheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, Instant startAt);
    void cancelAgentRun(long agentScheduleId);
}
```

Implementation `QuartzAgentScheduler` in `platform-scheduler-impl`:
- Reuses the existing autowired `org.quartz.Scheduler` bean (shared
  JobStore; we inherit clustering, JDBC persistence, and crash recovery
  for free).
- Quartz group: `"agent-run"`. Job and trigger name:
  `"agent-schedule-" + id`.
- Job class `AgentScheduleJob implements org.quartz.Job` publishes a
  Spring application event `AgentScheduleFiredEvent(long scheduleId)`
  inside `execute(...)`. Quartz beans live in platform; the EE listener
  consumes the event. **Rationale:** platform must not depend on EE
  types, but EE can subscribe to a platform event.
- Misfire policy: `MISFIRE_INSTRUCTION_FIRE_ONCE_NOW` (Quartz default for
  cron is `SMART_POLICY`, which becomes `FIRE_ONCE_NOW` for cron triggers
  — we set it explicitly so the behavior is contractual, not incidental).

### Execution path (EE listener)

`AgentScheduleFiredEventListener` in `automation-ai-hub-service`:

```
1. Load AiHubPersonalAgentSchedule by id.
   If missing or enabled = false → record outcome=skipped, return.

2. Verify the personal agent still exists (could have been deleted
   between Quartz fire and listener pickup).
   If missing → cancel Quartz trigger, record outcome=skipped, return.

3. Check remaining_runs:
   - null → proceed (unbounded).
   - 0    → defensive: should have been cancelled; cancel + disable now.
   - >0   → proceed; decrement happens after successful dispatch.

4. Call AiHubTaskService.createAiHubPersonalAgentChat(
        workspaceId, userId, environment, agentId, title).
   Returns a new AiHubTask with a fresh UUID threadId.

5. Post the schedule's `prompt` as the first user turn by calling the
   internal AiHub chat dispatch service directly (no HTTP loopback).
   The entry point is whichever service method the chat REST endpoint
   delegates to after auth/session resolution — `AiHubRoutingAgent` or
   a thin facade above it. The dispatch is fire-and-forget for the
   listener: the LLM response is streamed back into
   `SPRING_AI_CHAT_MEMORY` against the new `threadId` and is later
   visible when the user opens the task. The listener does NOT block on
   the LLM completing.

6. On success:
   - last_run_at = now()
   - if remaining_runs IS NOT NULL: remaining_runs -= 1
   - if remaining_runs == 0: enabled = false; agentScheduler.cancelAgentRun(id)
   - next_run_at = Trigger.getFireTimeAfter(now)
   - consecutive_failures = 0
   - counter: bytechef_ai_hub_agent_schedule_fire{outcome="success"}

7. On failure (any exception from createTask or dispatch *invocation*;
   LLM execution failures that happen async are not seen here — they
   surface in the task itself):
   - consecutive_failures += 1
   - if consecutive_failures >= 3: enabled = false;
                                   agentScheduler.cancelAgentRun(id)
   - counter: bytechef_ai_hub_agent_schedule_fire{outcome="failed"}
   - log with scheduleId, agentId, workspaceId for triage
```

`outcome="success"` therefore means "task created and dispatch
accepted", not "LLM produced an answer". A separate failure-record table
(deferred to v2 per non-goals) would be the right place to surface
async LLM failures.

The listener runs in its own short transaction. The Quartz job itself
does not hold a DB transaction across the LLM call — that would pin a
connection for seconds. The event-publish pattern lets the listener
manage its own transactional boundaries.

### Boot recovery / drift reconciliation

Quartz JDBC persistence already restores triggers across restarts.
`AiHubPersonalAgentScheduleServiceImpl#reconcileOnStartup` (annotated
`@EventListener(ApplicationReadyEvent.class)`) iterates every
`enabled = true` row and asks `agentScheduler.exists(id)` — re-registers
any drift (covers DB-restored-from-backup with stale Quartz tables).

### GraphQL surface

Extend `AiHubPersonalAgentGraphQlController` with:

```graphql
type AiHubPersonalAgentSchedule {
  id: ID!
  aiHubPersonalAgentId: ID!
  title: String!
  prompt: String!
  frequencyKind: ScheduleFrequencyKind!
  intervalMinutes: Int
  minuteOfHour: Int
  timeOfDay: String      # "HH:mm"
  dayOfWeek: Int
  dayOfMonth: Int
  cronExpression: String
  effectiveCronExpression: String!
  zoneId: String!
  startDate: String      # ISO-8601
  lifecycleKind: ScheduleLifecycleKind!
  maxRuns: Int
  remainingRuns: Int
  enabled: Boolean!
  lastRunAt: String
  nextRunAt: String
}

enum ScheduleFrequencyKind { EVERY_X_MINUTES HOURLY DAILY WEEKLY MONTHLY CUSTOM_CRON }
enum ScheduleLifecycleKind { RECURRING NUMBER_OF_RUNS }

extend type Query {
  aiHubPersonalAgentSchedules(workspaceId: ID!, agentId: ID!): [AiHubPersonalAgentSchedule!]!
  aiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!): AiHubPersonalAgentSchedule
}

extend type Mutation {
  createAiHubPersonalAgentSchedule(input: CreateAiHubPersonalAgentScheduleInput!): AiHubPersonalAgentSchedule!
  updateAiHubPersonalAgentSchedule(input: UpdateAiHubPersonalAgentScheduleInput!): AiHubPersonalAgentSchedule!
  deleteAiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!): Boolean!
  toggleAiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!, enabled: Boolean!): AiHubPersonalAgentSchedule!
}
```

Enums use SCREAMING_SNAKE_CASE per `*.graphqls` convention.

Authorization: every mutation gates on
`AiHubPersonalAgentService.findOwned(agentId, workspaceId, userId)`. A
schedule belongs to the user who created it; in v1 only the creator can
edit, toggle, or delete it. Admin-level management is out of scope.

### Frontend

New directory: `client/src/pages/automation/ai-hub/personal-agents/schedules/`

Files:
- `AiHubPersonalAgentSchedulesList.tsx` — list view; renders inside the
  agent detail page as a tab.
- `AiHubPersonalAgentScheduleDialog.tsx` — the create/edit dialog from
  the mockup. Local component state, GraphQL mutation, dialog primitive
  from `@/components/ui/dialog`.
- `AiHubPersonalAgentScheduleFrequencyFields.tsx` — small subcomponent
  that swaps the structured sub-fields based on `frequencyKind`.
- `hooks/useAiHubPersonalAgentSchedules.ts` — React Query hooks wrapping
  the generated GraphQL operations.

GraphQL operation files in `client/src/graphql/ai-hub/` (one `.graphql`
per query/mutation). Regenerate `src/shared/middleware/graphql.ts` via
`cd client && npx graphql-codegen`.

State: local `useState` per field, following `AiHubPersonalAgentForm.tsx`
precedent. No form library. Validation is server-driven; client surfaces
the error via the global `useFetchInterceptor` toast.

The "Next run" preview comes straight from server-computed `nextRunAt`
returned by the create/update mutation — no client-side cron math.

Routing: a new tab on the agent detail page rather than a separate
route. The agent detail page becomes a small `Tabs` shell with "Overview"
(current form) and "Schedules".

### Metrics

- `bytechef_ai_hub_agent_schedule_fire{outcome}` — Counter. Outcomes:
  `success`, `failed`, `skipped`.
- `bytechef_ai_hub_agent_schedule_active` — Gauge: number of enabled
  schedules. Polled once per minute via a `@Scheduled` task or recomputed
  on every create/update/delete.

Both are wired via `ObjectProvider<MeterRegistry>` so lightweight app
variants without actuator start cleanly (matching the
`bytechef_connection_create` pattern in CLAUDE.md).

### Testing strategy

**Unit:**
- `ScheduleFrequencyNormalizerTest` — covers each `frequency_kind` →
  cron expression transformation, plus invalid inputs.
- `ScheduleLifecycleNormalizerTest` — `RECURRING` wipes max_runs;
  `NUMBER_OF_RUNS` with null max_runs leaves remaining_runs null.
- `AgentScheduleFiredEventListenerTest` — every branch of the seven-step
  listener (missing schedule, disabled, missing agent, success path,
  failure path, decrement-to-zero, three-strike disable). Mocks
  `AiHubTaskService` and `AgentScheduler`.
- `EnumOrdinalStabilityTest` — add the two new enums.

**Integration (`*IntTest`):**
- `AiHubPersonalAgentScheduleServiceIntTest` — exercises create + load +
  update + delete against Testcontainers Postgres; verifies Quartz
  trigger registration and cancellation via the shared scheduler bean.
- `AgentScheduleQuartzIntTest` — registers a `0/2 * * * * ?` schedule,
  awaits two fires within ~6 seconds, asserts `last_run_at` advances
  and `remaining_runs` decrements correctly.

**Client:**
- `AiHubPersonalAgentScheduleDialog.test.tsx` — render dialog, fill
  fields per frequency kind, assert mutation called with normalized
  input. Uses `vi.hoisted` per project convention for Zustand mocks.

### Failure modes considered

- **Quartz fires after schedule deleted (race):** Step 1 of the listener
  no-ops; metric records `skipped`. Quartz trigger removal happens in
  the same transaction as the DB delete, but Quartz could already be
  mid-execute when the delete commits.
- **Personal agent deleted while schedules exist:** CASCADE deletes the
  schedule rows; the Quartz triggers become orphans until either (a) a
  fire arrives and step 2 of the listener cancels them, or (b) the boot
  reconciler runs. Acceptable.
- **LLM dispatch fails repeatedly:** three-strike auto-disable prevents
  pathological loops; the user re-enables once the upstream issue is
  fixed.
- **DB restored from backup with stale Quartz tables:** boot reconciler
  re-registers; any "extra" Quartz triggers without matching DB rows are
  swept on first fire by step 1.
- **Server clock skew:** `next_run_at` is informational, not
  authoritative. Quartz drives execution. UI shows whatever Quartz
  returned at the last write.
- **Two app instances both registering at boot:** Quartz JDBC JobStore
  serializes; the second registration becomes a no-op replace.

## Migration

No data migration. The new table is additive; existing personal-agent
rows are unaffected.

## Rollout

EE-only behavior gated by edition; CE deployments do not see the
Schedules tab. Frontend reads `useEditionStore` (existing pattern) and
hides the tab in non-EE.

No feature flag in v1 — the surface is opt-in (you only see it if you
open the new tab). If we need a kill switch later, gate the GraphQL
mutations on a config property.

## Open questions

None that block implementation. The "task description = prompt" reading
was confirmed by the existing screenshot context; if a separate
human-readable description is wanted later, add a `notes` column then.
