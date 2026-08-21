# Delegate AI Hub "Scheduled" to the AI Agent

**Date:** 2026-08-17
**Status:** Approved for planning
**Ticket:** 732

## Summary

The AI Hub owns a second, parallel implementation of "run a prompt on a cron with tools": the
**task** (`AiHubTask*`, four tables, a Quartz-backed hub scheduler, a `task_agent` subagent, a
`TASK` chat kind, seven tool callbacks, a full-page form, and three routes). The **AI Agent**
already does the same thing through shipped infrastructure — a `schedule` channel row whose cron
the Schedule component's `schedule/v1/cron` trigger fires inside the agent's generated workflow.

This spec deletes the AI Hub task subsystem outright and delegates scheduling to the AI Agent.
Nothing about scheduling remains in the AI Hub — no page, no sidebar entry, no listener, no chat
kind, no scheduler. The work that remains is on the Agents pages: a frequency picker on the agent
schedule dialog, an optional schedule block on the create-agent dialog, and a Scheduled filter plus
a per-row marker on the agents list.

## Motivation

Every capability of an AI Hub task has an AI Agent equivalent that already exists:

| AI Hub task | AI Agent |
| --- | --- |
| task `prompt` + `instructions` overlay | agent instructions + per-schedule `AiAgentChannel.parameters.prompt` |
| `ai_hub_task_tool` / `ai_hub_task_resource` | `AiAgentElement` rows (`TOOL`, `SKILL`, knowledge base) |
| `ai_hub_task_schedule` + `QuartzAgentScheduler` | `schedule` channel row + `schedule/v1/cron` trigger in the generated workflow |
| `AgentScheduleFiredEventListener` → `AiHubScheduledChatDispatcher` → LLM | ordinary workflow execution of the agent's workflow |

Keeping both means two runtimes, two schedulers, two authorization paths and two places to fix a
scheduling bug. Collapsing onto the agent also gains what the hub path never had: scheduled runs
become ordinary workflow executions — visible in Executions, subject to the plan-limit admission
gates, resumable, and covered by orphaned-job recovery.

## Decisions

1. **Nothing scheduling-related remains in the AI Hub.** The "Scheduled" sidebar entry, all three
   `/automation/ai-hub/tasks*` routes and the whole client task tree are deleted. An earlier
   iteration of this design kept a hub page listing scheduled agents; it was dropped because the
   agents list, gaining a Scheduled filter, already answers "which agents are scheduled?".
2. **No chat per scheduled fire.** With the Schedule component driving the run there is no hub turn
   to record, so producing an `AGENT_CHAT` per fire would need a new hub-side listener watching
   agent job completions and synthesising a transcript. Scheduled runs are observable in Executions
   instead, and the workflow generator already assigns each schedule a deterministic chat-memory
   conversation id (`AiAgentWorkflowGenerator.scheduleConversationId`), so continuity across fires
   exists without any hub table.
3. **The hub scheduler is deleted, not repurposed.** `AgentScheduler`, `QuartzAgentScheduler`,
   `AgentScheduleJob` and `AgentScheduleFiredEvent` exist solely for hub tasks (verified: the only
   non-test consumer is `AiHubTaskScheduleServiceImpl`). After this change the Schedule component's
   cron trigger is the only scheduling path in the product.
4. **The schedule lives on the existing `schedule` channel row.** No new table. "Scheduled agent"
   is defined as *an agent with at least one `AiAgentChannel` of type `schedule`*.
5. **The structured frequency picker is ported to the client.** `AiHubTaskSchedule`'s cadence model
   (EVERY_X_MINUTES / HOURLY / DAILY / WEEKLY / MONTHLY / CUSTOM_CRON) is reproduced in
   `AgentScheduleDialog`, which generates the cron expression client-side. `ScheduleLifecycleKind`
   (run-once, until-date, max-runs) does **not** come along — a cron trigger has no end condition,
   and inventing one would put scheduling logic back outside the component.
6. **Liquibase changelogs are deleted, not superseded.** The AI Hub is entirely unreleased
   (`git ls-tree -r --name-only v0.31.3 | grep ai/hub` returns nothing), so the task changelog files
   are removed rather than given drop-table changesets. The `aihub` directory is pulled in with
   `includeAll`, so no `master.xml` edit is needed.
7. **`AiHubChatKind.TASK` is removed**, shifting `AGENT_CHAT` from ordinal 3 to 2. This violates the
   normal append-only rule and is legal **only** because no released database holds these rows.
   `EnumOrdinalStabilityTest` is updated in the same commit.

## Cron format — the one easy thing to get wrong

`ScheduleCronTrigger` prepends a seconds field before handing the expression to Quartz:

```java
"0 " + expression   // ScheduleCronTrigger.java
```

and its property is documented as `[Minute] [Hour] [Day of Month] [Month] [Day of Week]`. So the
channel stores a **5-field** expression, while `ScheduleCronNormalizer` (being deleted) produced
**6-field** Quartz expressions. The ported generator must emit the 5-field form, and because Quartz
still parses the result, exactly one of day-of-month / day-of-week must be `?`:

| Kind | Fields | Generated expression |
| --- | --- | --- |
| EVERY_X_MINUTES | `intervalMinutes` 1..59 | `0/{n} * * * ?` |
| HOURLY | `minuteOfHour` 0..59 | `{m} * * * ?` |
| DAILY | `timeOfDay` | `{min} {hour} * * ?` |
| WEEKLY | `timeOfDay`, `dayOfWeek` (ISO 1..7) | `{min} {hour} ? * {quartzDay}` |
| MONTHLY | `timeOfDay`, `dayOfMonth` 1..31 | `{min} {hour} {dom} * ?` |
| CUSTOM_CRON | `cronExpression` | the trimmed input, verbatim |

ISO→Quartz day conversion is `quartzDay = (isoDayOfWeek % 7) + 1` (ISO 1=Mon…7=Sun, Quartz
1=Sun…7=Sat), carried over unchanged from `ScheduleCronNormalizer.weekly`.

## Removal inventory

### Server — AI Hub (`server/ee/libs/ai/ai-hub/`)

- Package `com.bytechef.ee.ai.hub.task`, whole: `AiHubTask`, `AiHubTaskTool`, `AiHubTaskResource`,
  `AiHubTaskSchedule`, `AiHubTaskResourceKind`, `ScheduleFrequencyKind`, `ScheduleLifecycleKind`,
  `AiHubTaskService(+Impl)`, `AiHubTaskScheduleService(+Impl)`, `WorkspaceAiHubTaskService(+Impl)`,
  the four repositories, `TaskSaveValidator`, `TaskScheduleValidator`, `ScheduleCronNormalizer`,
  `AgentScheduleFiredEventListener`, and their tests.
- `AiHubScheduledChatDispatcher` (`…hub.agent`) — its only caller is the fired-event listener.
- Tool callbacks: `CreateAiHubTaskToolCallback`, `UpdateAiHubTaskToolCallback`,
  `DeleteAiHubTaskToolCallback`, `CloneAiHubTaskToolCallback`, `ListAiHubTasksToolCallback`,
  `OpenAiHubTaskTabToolCallback`, `SetAiHubTaskScheduleToolCallback`, `TaskScheduleToolSupport`,
  plus their registrations in `AiHubConfiguration` and the `task` branch in
  `OpenResourceTabToolCallback`.
- Subagent: `TaskSubAgentConfiguration`, `prompt_task_agent.txt`, `AiHubAgentType.TASK_AGENT`, its
  entry in `AiHubSubAgentMcpContributorConfiguration`, and references in the parent agents' prompts.
- Chat surface: `AiHubChatKind.TASK`; `AiHubChat.aiHubTaskId` and the `ai_hub_task_id` column;
  `AiHubChatService.createAiHubTaskChat` (+ impl); `AiHubRoutingAgent.applyAiHubTaskOverlay`;
  `AiHubSpringAIAgent.appendAiHubTaskContext`; the `aiHubTaskInstructions` / `aiHubTaskTitle` keys
  in `AiHubStateKeys`; `AiHubSpringAIAgentTaskContextTest`.
- GraphQL: `AiHubTaskGraphQlController`, `ai-hub-task.graphqls`, `ai-hub-task-schedule.graphqls`,
  and the task fields on `ai-hub-chat.graphqls`.
- Audit: the `AI_HUB_TASK_*` values in `AiHubAuditEvent`.
- Liquibase, under `changelog/automation/aihub/` and `aihub_execution/`: the `*ai_hub_task*` files
  and `20260504000002_ai_hub_chat_add_task_id.xml`.

### Server — platform

- `platform-scheduler-api`: `AgentScheduler`, `AgentScheduleFiredEvent`.
- `platform-scheduler-impl`: `QuartzAgentScheduler`, `AgentScheduleJob`, `QuartzAgentSchedulerIntTest`.

### Client

- `client/src/ee/pages/automation/ai-hub/tasks/` — all 17 files including `schedules/` and tests.
- Routes `ai-hub/tasks`, `ai-hub/tasks/new`, `ai-hub/tasks/:taskId/edit` and the two lazy imports in
  `routes.tsx`.
- The "Scheduled" entry and `isOnAiHubTasks` logic in `AiHubChatsSidebar.tsx`; the
  `useAiHubTaskQuery` import in `AiHubPanel.tsx`.
- Task GraphQL operations under `client/src/graphql/`, regenerating `shared/middleware/graphql.ts`.

`AiHubTaskScheduleFrequencyFields.tsx` and the `validateSchedule` rules in
`AiHubTaskScheduleSection.tsx` are deleted from the hub **after** their logic is reproduced on the
agent side (see below) — port first, then delete, so the picker is never absent from both places.

## What gets built

All of it on the Agents pages; no server change beyond the deletions.

### 1. Frequency picker on `AgentScheduleDialog`

Today the dialog collects `name`, `expression` (raw cron), `timezone`, `prompt`. It gains a
frequency selector and the per-cadence fields, mirroring `AiHubTaskScheduleFrequencyFields`. On
submit it writes to `AiAgentChannel.parameters`:

- `expression` — the **generated 5-field cron**, which is what `schedule/v1/cron` reads;
- `frequencyKind` plus whichever of `intervalMinutes` / `minuteOfHour` / `timeOfDay` /
  `dayOfWeek` / `dayOfMonth` / `cronExpression` that kind owns;
- `name`, `prompt`, `timezone` as today.

Storing both the projection and its source keeps the picker reversible: reopening a schedule shows
"Daily at 09:00" rather than reverse-engineering a cron string. This is safe because
`AiAgentWorkflowGenerator` already filters `parameters` down to declared trigger properties,
explicitly excluding `prompt` and `name` — extra picker keys ride along untouched. A row created
before this change (or edited by hand) has no `frequencyKind`; the dialog opens such a row in
CUSTOM_CRON mode with its `expression` in the cron field.

Validation reproduces `validateSchedule`'s rules per kind, plus the existing requirement that
`prompt` and cron be non-blank.

### 2. Optional schedule block on `AgentDialog` (create mode)

The Agents-list "New Agent" dialog keeps Title and Description and gains a collapsed, **optional**
schedule block using the same fields as (1). Filled in, the save runs `createAiAgent` and then
`addAiAgentChannel(channelType: "schedule")` before navigating to the new agent; left empty, the
behaviour is exactly today's. Defining a schedule here is equivalent to adding one afterwards on the
detail page's `AgentScheduleCard` — the same channel row either way. No agent is ever *required* to
have a schedule; chat-only and webhook-only agents are unaffected.

The block appears only when creating. Edit mode (`agent` prop present) still edits title and
description, because an existing agent may have several schedules and the detail card owns them.

### 3. Scheduled filter and list marker

- `aiAgents.graphql` selects `channels { channelType parameters }`. The field already exists on the
  `AiAgent` GraphQL type (`aiAgent.graphql` selects it), so no schema change — only codegen.
- `AgentsLeftSidebarNav` renders a **Scheduled** item directly under **All Agents** when
  `filterMode` is set, linking to `?filter=scheduled`.
- `Agents.tsx` reads `searchParams.get('filter')` alongside the existing `agentId` / `tagId`
  filters and narrows `filteredAgents` to agents with ≥1 `schedule` channel.
  `AgentsFilterTitle` gains the corresponding label.
- `AgentListItem` renders a `CalendarClockIcon` badge beside the title of a scheduled agent,
  tooltipped with its schedules' names and cron expressions.

## Data and migration

No migration. `ai_hub_task`, `ai_hub_task_tool`, `ai_hub_task_resource`, `ai_hub_task_schedule` and
`ai_hub_chat.ai_hub_task_id` have never existed in a release, so their changelogs are deleted and
new databases simply never create them. Existing local dev databases are reconciled with
`scripts/dev/sync-local-schema-after-collapse.sh`, which patches both the schema drift and the stale
changelog md5sums.

Any locally-created task rows are lost. That is intended: a task is not convertible to an agent
without inventing a model for its tools and resources, and no deployment holds production tasks.

## Testing

- **Deletions** are verified by compilation plus `./gradlew check`: the removed types have no
  remaining referents. `EnumOrdinalStabilityTest` is updated for the new `AiHubChatKind` ordinals,
  and `AiHubChatRepositoryIntTest` / `AiHubChatServiceTest` lose their TASK cases.
- **Cron generation** gets a unit test per frequency kind asserting the exact 5-field string,
  including the ISO→Quartz weekday conversion and the `?` placement — the table above is the oracle.
  A round-trip test asserts that generating from picker fields and reopening yields the same picker
  state.
- **`AgentScheduleDialog`** tests: each kind renders its own fields; validation blocks submit with a
  missing cadence field; a row without `frequencyKind` opens as CUSTOM_CRON.
- **`AgentDialog`** tests: create with a schedule issues `createAiAgent` then `addAiAgentChannel`;
  create without one issues only `createAiAgent`; edit mode renders no schedule block.
- **Filter and marker** tests: `?filter=scheduled` narrows the list; an agent with no schedule
  channel renders no badge; the sidebar marks Scheduled current.
- Liquibase changes are verified by an existing AI Hub `*IntTest`, which builds the schema from
  scratch under Testcontainers — stronger evidence than a `bootRun` under the `liquibase` profile,
  which applies nothing.

## Risks

- **Ordinal shift on `AiHubChatKind`.** Safe only under the unreleased premise, which was verified
  against `v0.31.3` and must be re-verified if this lands after a release cut.
- **Losing lifecycle semantics.** Tasks could run once or until a date; agent schedules run forever.
  If that need returns it belongs in the Schedule component as a trigger property, not in a caller.
- **A cron typo now fails later.** The hub validated with Quartz's `CronExpression` at save time;
  the client picker generates valid expressions for five of six kinds, but CUSTOM_CRON input is only
  syntactically checked client-side and a bad expression surfaces at trigger-registration time on
  publish. Acceptable — it is the same behaviour every other Schedule-component workflow has today.
