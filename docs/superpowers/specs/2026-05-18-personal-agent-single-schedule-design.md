# Personal Agent — single schedule per agent (AI Hub)

**Status:** Implemented
**Author:** Ivica Cardic
**Date:** 2026-05-18
**Scope:** EE (Enterprise Edition); `automation-ai-hub` module
**Supersedes:** `2026-05-16-personal-agent-scheduling-design.md` (the v1 multi-schedule
design; never released on `0_732`).

## Problem

The v1 scheduling design (2026-05-16) modelled scheduling as a separate
`ai_hub_personal_agent_schedule` table with a many-to-one relationship to the
agent. The user surface was a **Schedules** tab on the agent edit page with a
list view, a "+ New scheduled task" button, and a modal dialog to create or edit
each entry. Schedules were post-create only — you had to create the agent, then
open it, then open the tab, then open the dialog.

That model is wrong for the actual usage pattern of personal agents:

- A personal agent represents one *focused assistant role*. Having an agent
  run multiple parallel cron jobs blurs that single-purpose framing.
- The user has to make four navigation hops (create agent → open agent →
  Schedules tab → New scheduled task) before they can express "every weekday at
  9 AM, run this." Most users will simply never get there.
- "Multiple schedules per agent" was speculative: no concrete use case in
  the original spec justified the cost.

This redesign collapses scheduling to **at most one schedule per personal
agent**, captured **during agent creation**, on a dedicated **Schedule** tab
inside the same agent form.

## What v1 keeps

- The `ai_hub_personal_agent_schedule` table (schema preserved, except the
  agent FK becomes unique — see below).
- All Quartz wiring: `AgentScheduler` API, `QuartzAgentScheduler` impl,
  `AgentScheduleJob`, `AgentScheduleFiredEvent`,
  `AgentScheduleFiredEventListener`.
- The two enums `ScheduleFrequencyKind` and `ScheduleLifecycleKind`.
- The `ScheduleCronNormalizer` and the on-write normalization logic.
- The reconciler hook (`AiHubPersonalAgentScheduleServiceImpl#reconcileOnStartup`).
- Three-strike auto-disable on consecutive failures.
- Metrics: `bytechef_ai_hub_agent_schedule_fire{outcome}`.
- The frequency-fields subcomponent on the frontend
  (`AiHubPersonalAgentScheduleFrequencyFields.tsx`).

## What v1 loses

**Backend:**
- `findByAgent(long agentId)` (returned a `List`) — no caller needs it once
  the GraphQL surface collapses.
- The four schedule mutations (`createAiHubPersonalAgentSchedule`,
  `updateAiHubPersonalAgentSchedule`, `deleteAiHubPersonalAgentSchedule`,
  `toggleAiHubPersonalAgentSchedule`).
- The two schedule queries (`aiHubPersonalAgentSchedules`,
  `aiHubPersonalAgentSchedule`).
- The simple agent-id index (`idx_ai_hub_personal_agent_schedule_agent`) —
  replaced by a UNIQUE constraint that also serves as the lookup index.

**Frontend:**
- `AiHubPersonalAgentSchedulesList.tsx` — the list view.
- `AiHubPersonalAgentScheduleDialog.tsx` — the create/edit dialog.
- `hooks/useAiHubPersonalAgentSchedules.ts` — the React Query hooks for the
  removed mutations + queries.
- Six `.graphql` operation files under
  `client/src/graphql/ai/aihub/personal-agent/schedule/` (the create/update/
  delete/toggle/list/get operations).

## Non-goals (v2)

Carried forward from v1, still out of scope:

- Per-fire run-history UI.
- Per-fire failure record table (three-strike auto-disable remains the
  only failure surfacing).
- Sharing or org-wide scheduling.
- Backfilling missed runs (Quartz `FIRE_ONCE_NOW` misfire policy stays).
- Cron schedule templates / cloning between agents.

New for v2:

- Multiple schedules per agent. The model is strictly 1:1; if a future
  iteration revives many-schedules-per-agent, this spec is the wrong starting
  point.

## User-facing surface

The agent form (`AiHubPersonalAgentForm.tsx`) has two tabs in both create and
edit modes:

1. **Overview** — existing fields (display title, description, instructions,
   LLM provider/model, tools). No change.
2. **Schedule** — new tab. Renders the schedule fields **inline** (no list,
   no dialog).

The Schedule tab layout, top to bottom:

- **Enabled** (`Switch`) labelled "Run this agent on a schedule." Default
  `false` for new agents. Hydrated from existing row's `enabled` field for
  edit mode. Controls **Quartz registration**, not row existence — see
  *Save semantics* below for the full matrix.
- **Title** (text, required when Enabled = true) — surfaced in run logs and
  copied into the created task's `title`.
- **Task description** (textarea, required when Enabled = true) — the prompt
  sent as the first user message on each fire.
- **Run frequency** (select) — same six options as v1: `Every X Minutes`,
  `Hourly`, `Daily`, `Weekly`, `Monthly`, `Custom (Cron)`.
- Frequency-specific sub-fields (interval, minute-of-hour, time-of-day,
  day-of-week, day-of-month, cron expression) — reuses
  `AiHubPersonalAgentScheduleFrequencyFields.tsx` from v1.
- **Timezone** (searchable IANA select), defaulting to
  `Intl.DateTimeFormat().resolvedOptions().timeZone`.
- **Start date** (optional date picker, default "Starts immediately").
- **Lifecycle** (`Recurring` | `Number of runs`), with the v1 `Max runs`
  follow-up for `Number of runs`.
- A live preview line showing the server-computed `nextRunAt` (only after
  first save; absent on the create form).
- **Remove schedule** (link button, edit mode only, visible only when a
  schedule row exists). Deletes the row entirely and cancels its Quartz
  trigger. Clears the form back to "no schedule" state with Enabled = false.

**Fields stay visible regardless of the Enabled toggle.** This matches the
explicit user preference and avoids the "where did my fields go" surprise
when toggling the switch in edit mode.

### Removed UI affordances

- The **Schedules** (plural) tab is gone.
- The "+ New scheduled task" button is gone.
- The per-row ellipsis menu (Edit, Disable/Enable, Delete) is gone.
- The list view is gone — the only place a schedule is visible is the
  agent's own Schedule tab.

The Delete affordance is replaced by the "Remove schedule" link inside the
Schedule tab.

## Architecture

### Data model

`ai_hub_personal_agent_schedule` retains every column from v1. **Two changes
in the v1 init migration (`20260516000001_ai_hub_personal_agent_schedule_init.xml`),
edited in place** since v1 is unreleased on `0_732`:

1. Drop the simple index
   `idx_ai_hub_personal_agent_schedule_agent (ai_hub_personal_agent_id)`.
2. Add a unique constraint
   `uniq_ai_hub_personal_agent_schedule_agent (ai_hub_personal_agent_id)`
   — both enforces the 1:1 invariant and serves as the lookup index.

The composite index
`idx_ai_hub_personal_agent_schedule_workspace_user (workspace_id, user_id)`
is kept. It serves the (deferred but plausible) admin-listing query of "all
schedules a user owns in workspace X."

Per CLAUDE.md lessons from the workspace-relation refactor, the rename is
applied in the **init file**, not a follow-up `ALTER INDEX` migration —
follow-ups don't survive init edits. CI environments need a one-time
`./gradlew clean` so stale resources under `build/resources/` are evicted.

No data migration. v1 was never released; existing dev environments wipe
their schedule rows by running `docker compose ... down -v` (the standard
local-reset path).

### Service layer

`AiHubPersonalAgentScheduleServiceImpl` (existing) gets:

- **New method:** `Optional<AiHubPersonalAgentSchedule> findByAgentId(long agentId)`
  — backed by a repository derived query
  `findByAiHubPersonalAgentId(long)`.
- **New facade method:**
  `@Nullable AiHubPersonalAgentSchedule upsertOrDelete(long agentId, long workspaceId, long userId, Environment environment, @Nullable ScheduleInput input)`.
  - `input == null` → `findByAgentId` → if present, call existing `delete`;
    return null.
  - `input != null` → `findByAgentId` → if present, apply input and call
    existing `update`; otherwise build a new entity and call existing
    `create`.

`create(...)` and `update(...)` keep their current signatures (they're still
useful from boot reconcile and from internal flows). `toggle(...)` and the
list-by-agent method `findByAgent(...)` are deleted — no caller remains.

The DB-level UNIQUE constraint is the authoritative 1:1 enforcement. The
service catches any `DuplicateKeyException`/`DataIntegrityViolationException`
and re-throws as `IllegalStateException` so callers see a typed error
instead of a JDBC stack.

### GraphQL surface

The schedule type and the two enums stay; the per-schedule queries and
mutations are removed.

```graphql
type AiHubPersonalAgent {
    # ...existing fields unchanged...
    """
    Optional one-to-one schedule. Null when the user has not enabled
    scheduling for this agent. Read-only field — mutate via
    setAiHubPersonalAgentSchedule.
    """
    schedule: AiHubPersonalAgentSchedule
}

extend type Mutation {
    """
    Upserts or deletes the agent's single schedule.
    - input.schedule != null  → upsert (insert if absent, update if present).
    - input.schedule == null  → delete any existing schedule + cancel Quartz.
    Returns the agent with its (possibly null) schedule field populated.
    """
    setAiHubPersonalAgentSchedule(input: SetAiHubPersonalAgentScheduleInput!): AiHubPersonalAgent!
}

input SetAiHubPersonalAgentScheduleInput {
    workspaceId: ID!
    aiHubPersonalAgentId: ID!
    """
    Null clears the schedule. Non-null upserts.
    """
    schedule: AiHubPersonalAgentScheduleInput
}

input AiHubPersonalAgentScheduleInput {
    enabled: Boolean!
    title: String!
    prompt: String!
    frequencyKind: ScheduleFrequencyKind!
    intervalMinutes: Int
    minuteOfHour: Int
    timeOfDay: String      # "HH:mm"
    dayOfWeek: Int
    dayOfMonth: Int
    cronExpression: String
    zoneId: String!
    startDate: String      # ISO-8601
    lifecycleKind: ScheduleLifecycleKind!
    maxRuns: Int
}
```

The `AiHubPersonalAgentSchedule` output type from v1 is unchanged. The two
enums (`ScheduleFrequencyKind`, `ScheduleLifecycleKind`) keep their existing
ordinals — they're already pinned in `EnumOrdinalStabilityTest`.

**Removed from the v1 schema file
`ai-hub-personal-agent-schedule.graphqls`:** the input types
`CreateAiHubPersonalAgentScheduleInput` / `UpdateAiHubPersonalAgentScheduleInput`
and the four mutations + two queries. The file collapses to just the type
definition + enums; the new input type and `setAiHubPersonalAgentSchedule`
mutation are added to `ai-hub-personal-agent.graphqls` (since the agent now
owns the schedule on its payload).

Authorization: `setAiHubPersonalAgentSchedule` gates on
`AiHubPersonalAgentService.findOwned(agentId, workspaceId, userId)` (the
same check the existing mutations use). The schedule's `userId` is forced
to the caller; clients cannot impersonate.

### Agent creation flow

`AiHubPersonalAgentForm.tsx` "Create agent" handler issues two mutations
sequentially:

1. `createAiHubPersonalAgent(...)` → returns the new agent id.
2. **If** the Schedule tab's `Enabled` is true, OR the tab was visited and
   the user filled in non-default values:
   `setAiHubPersonalAgentSchedule({ aiHubPersonalAgentId: <new id>, schedule: { ... } })`.

Sequential, not bundled into one mutation. Two reasons:

- Tool attaches already follow the same sequential pattern in
  `AiHubPersonalAgentForm.tsx`; bundling would diverge.
- The schedule needs the agent's id, which doesn't exist until step 1
  commits.

Failure modes:

- Step 1 fails → toast error, no agent created, no step 2.
- Step 1 succeeds, step 2 fails → toast "Agent created, but schedule
  failed to save: <reason>. Edit the agent to retry." The agent is **not**
  rolled back — matches the v1 tool-attach partial-failure handling.

### Edit flow

On the edit page, the Schedule tab is hydrated from `agent.schedule` (the
new field on the agent payload). Saving the agent issues mutations only
when something changed:

- `updateAiHubPersonalAgent(...)` if any Overview field changed.
- `setAiHubPersonalAgentSchedule(..., schedule: { ... })` if any Schedule
  tab field changed (including the Enabled toggle).

Both are independent; either can fail without invalidating the other. The
"Save changes" button is the single visible action — the form figures out
which mutations to send.

The "Remove schedule" link sends `setAiHubPersonalAgentSchedule(..., schedule: null)`
as a discrete action, separate from "Save changes." It confirms via the
standard destructive-action confirm dialog before firing.

### Save semantics

The Schedule tab exposes two distinct user actions:

- The **Enabled** toggle controls **Quartz registration only**. It maps
  directly to the row's `enabled` column. Flipping it from on → off
  preserves the row (and its `consecutive_failures`, `last_run_at`,
  `next_run_at` history); flipping back on resumes Quartz with the
  current fields.
- The **Remove schedule** link (edit mode only, present only when a row
  exists) deletes the row entirely and cancels Quartz.

The combined matrix of (Save action) × (existing row):

| Action          | Existing row? | Mutation effect                                  |
|-----------------|---------------|--------------------------------------------------|
| Save, Enabled=on  | no          | INSERT row enabled=true, register Quartz         |
| Save, Enabled=on  | yes         | UPDATE row enabled=true, ensure Quartz registered|
| Save, Enabled=off | no          | No-op (no mutation sent; nothing to persist)     |
| Save, Enabled=off | yes         | UPDATE row enabled=false, cancel Quartz          |
| Remove schedule | yes           | DELETE row, cancel Quartz                        |
| Remove schedule | no            | (not reachable — link is hidden)                 |

This preserves auto-disable failure context: the three-strike auto-disable
flips `enabled = false` server-side, the user opens the form and sees a
disabled schedule with its history intact, and can either re-enable it
(after fixing the upstream issue) or hit "Remove schedule" to clear it.

The "Save, Enabled=off, no existing row" case is the create-mode default:
the user opens the new-agent form, fills in only the Overview tab, and
hits "Create agent." The schedule mutation is **not** sent — saving zero
state should not poke the server.

### Frontend file changes

**Delete:**
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts`
- `client/src/graphql/ai/aihub/personal-agent/schedule/createAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/updateAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/deleteAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/toggleAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedules.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedule.graphql`

**Keep:**
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleFrequencyFields.tsx`
  — moves into the new tab as-is.

**Add:**
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.tsx`
  — the inline schedule editor. Props: `value`, `onChange`, `disabled`. Local
  state mirrors the v1 dialog's state shape (renamed `existing` → `value`).
- `client/src/graphql/ai/aihub/personal-agent/schedule/setAiHubPersonalAgentSchedule.graphql`
  — the single new mutation.
- Add `schedule { ... }` to the existing
  `AiHubPersonalAgentFieldsFragment` (or equivalent) so it rides along on
  every agent read.

**Edit:**
- `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`
  — rename the second tab from "Schedules" to "Schedule"; make it visible in
  both create and edit modes (currently `isEditMode` only); replace the
  `<AiHubPersonalAgentSchedulesList ... />` render with
  `<AiHubPersonalAgentScheduleTab value={schedule} onChange={setSchedule} />`;
  extend the create + update submit handlers with the optional
  `setAiHubPersonalAgentSchedule` step.

After GraphQL edits: `cd client && npx graphql-codegen` to regenerate
`src/shared/middleware/graphql.ts`.

### Backend file changes

**Edit:**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml`
  — swap the simple index for a UNIQUE constraint on
  `ai_hub_personal_agent_id`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls`
  — strip mutations + queries + input types; keep only the type and enums.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls`
  — add `schedule: AiHubPersonalAgentSchedule` on the agent type; add the
  new input type and `setAiHubPersonalAgentSchedule` mutation.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java`
  — replace the four schedule mutations + two queries with a single
  `setAiHubPersonalAgentSchedule` mutation; add a `@SchemaMapping` resolver
  on `AiHubPersonalAgent.schedule` that calls
  `scheduleService.findByAgentId(agent.id)`.
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java`
  — drop `findByAgent(long)` and `toggle(...)`; add `findByAgentId(long)`
  and `upsertOrDelete(...)`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`
  — implement the two new methods; delete the two removed methods.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java`
  — add `Optional<AiHubPersonalAgentSchedule> findByAiHubPersonalAgentId(long)`;
  drop `findByAiHubPersonalAgentIdOrderByCreatedDateDesc(long)`.

**Tests — edit:**
- `AiHubPersonalAgentScheduleServiceImplTest` — replace
  multi-schedule-per-agent assertions with upsert/delete coverage. Verify
  the upsert path goes through `create` when no row exists and `update`
  when one does. Verify `upsertOrDelete(... null)` deletes and cancels
  Quartz.
- `AiHubPersonalAgentGraphQlControllerTest` — drop the four removed
  mutations and the two queries; add `setAiHubPersonalAgentSchedule`
  happy-path + delete-path + auth-failure coverage; add a test that
  `aiHubPersonalAgent.schedule` returns the row when one exists and null
  otherwise.
- `AgentScheduleFiredEventListenerTest` — unchanged (the listener API
  doesn't move).
- `ScheduleCronNormalizerTest` — unchanged.

**Tests — add:**
- `AiHubPersonalAgentScheduleUniqueConstraintIntTest` — insert a schedule
  for agent A, then attempt a second insert for the same agent; assert
  `DataIntegrityViolationException`. Uses the standard Testcontainers
  Postgres harness.

**Client tests — edit:**
- Delete `AiHubPersonalAgentScheduleDialog.test.tsx` (the dialog is gone).
- Add `AiHubPersonalAgentScheduleTab.test.tsx` — renders the tab, toggles
  Enabled, fills frequency fields per kind, asserts `onChange` is invoked
  with the expected shape. Follows the existing `vi.hoisted` pattern from
  `AiHubPersonalAgentsList.test.tsx`.

### Metrics

- `bytechef_ai_hub_agent_schedule_fire{outcome}` — unchanged. Still counts
  `success`, `failed`, `skipped`.
- The active-schedule gauge from the v1 spec was never actually
  implemented; it stays not-implemented for v2.

### Failure modes considered

- **UNIQUE constraint race during agent create:** two browser tabs both
  call `createAiHubPersonalAgent` → both call
  `setAiHubPersonalAgentSchedule`. The second `INSERT` fails with
  `DataIntegrityViolationException`; the service catches and re-throws as
  `IllegalStateException("Schedule already exists for this agent")`. Client
  surfaces the toast; user reloads and sees the winning schedule. No data
  corruption.
- **Schedule outlives agent:** ON DELETE CASCADE on the FK is unchanged
  from v1; deleting the agent removes the schedule row. Quartz trigger
  orphans are handled by the listener's "agent missing → cancel" branch
  (v1 step 2) or by the boot reconciler.
- **Toggling Enabled = false in edit mode:** the form sends
  `setAiHubPersonalAgentSchedule({ schedule: { enabled: false, ... } })`;
  the service updates the row and cancels Quartz. Row history
  (`consecutive_failures`, `last_run_at`) is preserved. Flipping back to
  Enabled = true restores Quartz registration with the current fields.
- **Removing a schedule via "Remove schedule" link:** the form sends
  `setAiHubPersonalAgentSchedule({ schedule: null })`; the service deletes
  the row and cancels Quartz. The next page load shows the agent without a
  schedule. Creating a new schedule afterwards starts fresh
  (`consecutive_failures = 0`). The destructive-action confirm dialog
  guards against accidents.
- **Stale Quartz trigger from v1 multi-schedule data:** v1 unreleased, but
  any dev environment that ran v1 will have schedule rows. The UNIQUE
  constraint will fail to apply on init if two rows exist for the same
  agent; CI environments need `docker compose -f server/docker-compose.dev.infra.yml down -v`
  + `./gradlew clean` before pulling the v2 branch. Documented in the
  rollout section.

## Migration

No production data migration (v1 unreleased). Local dev environments need:

```bash
docker compose -f server/docker-compose.dev.infra.yml down -v
./gradlew clean
./gradlew -p server/apps/server-app bootRun
```

The Liquibase init file change replays cleanly against a fresh DB.

## Rollout

EE-only. The Schedule tab visibility follows the existing edition gate (`useEditionStore`); CE deployments don't see it. No feature flag — the surface is opt-in (you only see the tab inside the agent form), and the v1 surface it replaces never shipped, so there's no behavior to deprecate gracefully.

## Open questions

None that block implementation.
