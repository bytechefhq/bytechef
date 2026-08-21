# Context Store Phase 17b — Incremental-sync orchestrator auto-wiring

**Status**: Draft
**Date**: 2026-05-12
**Predecessor**: Phase 17 (SPI + delegate + listener — landed via commits `6c8960e2f78`, `3fc66c076a3`, `80b403b97e4`, `7a13a81c2a5`, `6e36a9586bb`).

## Goal

Take Phase 17's plumbing — `ItemReader.SINCE_KEY` constant + `supportsIncremental()` SPI + `DataStreamJobExecutionListener` writing `lastSyncStart` into the JobExecution's `ExecutionContext` — and **automate** the orchestrator side so users don't have to hand-edit workflow YAML to opt into incremental sync. After this phase, an Airtable-backed CS/KB source that ships with `supportsIncremental() = true` should sync incrementally on the second and subsequent runs without any user action beyond picking a cadence.

## Why this is its own phase

Phase 17 deliberately stopped short of orchestrator auto-wiring per decision-log pick #5: ship the SPI clean, prove end-to-end via the IntTest's manual `JobParameter` injection, get user feedback. With Airtable in production now, three deferred concerns are ready to be addressed together:

1. **Workflow generator side** — auto-emit a `datastream.since` JobParameter expression so the user doesn't have to add it manually.
2. **Cadence-pair UI** — let users pair a frequent incremental cadence (e.g. hourly) with a rare full-replace cadence (e.g. daily) to recover from drift without paying the upstream-API cost on every run.
3. **Tombstone-derivation strategy** — incremental sync alone cannot derive tombstones (records deleted upstream stay forever). The orchestrator needs a policy choice per source.

Each of these is small in isolation but they're tightly coupled (a cadence-pair UI is meaningless without auto-wiring; a tombstone strategy choice depends on whether a periodic full-replace runs). Shipping them together avoids two half-features.

## Architecture

### Part A — `datastream.since` JobParameter auto-injection (commit 5, deferred)

> Status as of 2026-05-12: the *workflow-emit* side landed in commit 4 (the trigger JSON declares
> `jobParameters.datastream.mode`). The *runtime-consume* side — the work this section now describes — is
> the deepest open item in Phase 17b and is queued for its own session.

**Today**: `ContextStoreWorkflowGenerator` / `KnowledgeBaseSourceWorkflowGenerator` emit a single `dataStream/v1/stream` task with empty `parameters`. The reader's `open()` reads `executionContext.get(SINCE_KEY)` — but the orchestrator never sets it for auto-generated workflows, so the reader always sees `null` and does a full pull.

**After 17b**: when a schedule trigger fires, the platform looks up the source row, computes `datastream.since` from `source.lastSyncRunAt`, reads the trigger's `jobParameters` block (e.g. `datastream.mode`), and threads both into the spawned job's Spring Batch `JobParameters`. The reader sees `since` in its `ExecutionContext` and filters upstream; the destination writer reads `mode` and routes to PARTIAL / FULL_REPLACE accordingly.

#### Design choice — scheduler-side lookup vs runtime SpEL

- **Option A (chosen): scheduler-side lookup.** The trigger fires → the platform-scheduler reads the workflow's `metadata.contextStoreSourceId` / `metadata.knowledgeBaseSourceId`, looks up `source.lastSyncRunAt`, and injects the resolved value as a `datastream.since` JobParameter when creating the job. The workflow JSON stays declarative — no SpEL evaluator extension needed, and the source-lookup logic lives in one place per subsystem.
- **Option B (rejected): runtime SpEL in workflow JSON.** Emit something like `"datastream.since": "${source.lastSyncRunAt?.toEpochMilli()}"` in the task parameters. Requires extending Atlas's expression evaluator with a `source` binding scoped to CS/KB sources. More flexible but bleeds source-specific concerns into the general workflow execution path. Reject.

#### The pipeline today (what commit 5 must edit)

When a `schedule/v1/cron` trigger fires:

1. **`ScheduleTriggerJob.execute(JobExecutionContext)`** (Quartz job) — fires on schedule, publishes a `TriggerListenerEvent` carrying `WorkflowExecutionId` + the trigger's `output` payload (cron expression, timezone, fire-time). [server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/ScheduleTriggerJob.java](server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/ScheduleTriggerJob.java)
2. **`TriggerCoordinator.onTriggerListenerEvent(...)`** — receives the event, builds a `TriggerExecution`, dispatches it. Completion routes through `TriggerCompletionHandler`. [server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/TriggerCoordinator.java](server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/TriggerCoordinator.java)
3. **`TriggerCompletionHandler.handle(TriggerExecution)`** — composes the spawned job's `inputMap` (workflow inputs + trigger output under `{triggerName: ...}`), grabs the workflow's `metadataMap`, calls `principalJobFacade.createJob(JobParametersDTO, jobPrincipalId, type)`. [server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/trigger/completion/TriggerCompletionHandler.java:99-119](server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/trigger/completion/TriggerCompletionHandler.java)
4. **`JobParametersDTO`** carries `(workflowId, inputMap, metadataMap)` — three string-keyed maps. There is no first-class Spring Batch `JobParameters` slot on this DTO; the dataStream task action assembles Spring Batch parameters itself when it launches `JobLauncher.run(...)`.
5. **`DataStreamStreamActionDefinition`** (in `server/libs/modules/components/data-stream/`) — perform-time, launches the Spring Batch job. Phase 17's `ItemStreamReaderDelegate.doBeforeStep` already reads `datastream.since` off the `JobParameters` and copies into the per-step `ExecutionContext` under `ItemReader.SINCE_KEY`.

The pipeline gap: between step 3 and step 5, there's no path for the trigger's `jobParameters` block (commit 4's declaration) or a scheduler-computed `datastream.since` (this section's contribution) to reach Spring Batch's `JobLauncher.run(...)`. Commit 5 lays that path.

#### Layered design — four touch-points

##### Layer 1 — Trigger-declared JobParameters reach the platform coordinator

`TriggerCompletionHandler` already loads the workflow definition indirectly (via `JobPrincipalAccessorRegistry.getJobPrincipalAccessor(...).getWorkflowId(...)`). Extend it so that *before* calling `principalJobFacade.createJob(...)`, it:

- Loads the workflow definition (`workflowService.getWorkflow(workflowId)`).
- Finds the firing trigger by name (`triggerExecution.getName()`) — same matching `TriggerCoordinator.getWorkflowTrigger(...)` already does.
- Reads the trigger's `jobParameters` block (the Phase 17b convention introduced in commit 4): `trigger.get("jobParameters")` returns a `Map<String, ?>` or `null`.
- Threads those entries into a new field on `JobParametersDTO`, called `jobParameters` (distinct from the existing `metadataMap` — the latter is workflow-level, this is job-launch-level).

`JobParametersDTO` becomes:

```java
public record JobParametersDTO(
    String workflowId,
    Map<String, ?> inputMap,
    Map<String, ?> metadataMap,
    Map<String, ?> jobParameters       // NEW — Spring Batch JobParameter overrides
) {}
```

Backward compat: any caller that doesn't set `jobParameters` gets `Map.of()` via a compact constructor / convenience overload that defaults the new field.

##### Layer 2 — Source-row lookup contributes `datastream.since`

Add an SPI in the platform-workflow-coordinator module:

```java
package com.bytechef.platform.workflow.coordinator.trigger.jobparameter;

public interface TriggerJobParameterContributor {
    /**
     * Returns additional JobParameter overrides for the job spawned by the firing trigger. The metadataMap argument
     * carries whatever the workflow's static metadata block held (e.g. contextStoreSourceId, knowledgeBaseSourceId).
     * Returns an empty map when the contributor does not recognize the workflow.
     */
    Map<String, ?> contribute(Map<String, ?> workflowMetadataMap, WorkflowExecutionId workflowExecutionId);
}
```

Two implementations:

- `ContextStoreTriggerJobParameterContributor` (in `automation-context-store-service`):
  - Reads `metadataMap.get("contextStoreSourceId")`.
  - On hit: loads the `ContextStoreSource`, reads `lastSyncRunAt`, returns `{"datastream.since": lastSyncRunAt.toEpochMilli()}`. Returns `Map.of()` when `lastSyncRunAt == null` (first run) or the row is missing.
  - Loaded via `@Service` + `@ConditionalOnBean(ContextStoreSourceService.class)`. Module already depends on `platform-workflow-coordinator-api`.
- `KnowledgeBaseSourceTriggerJobParameterContributor` (in `automation-knowledge-base-service`): mirror.

`TriggerCompletionHandler` gains a `List<TriggerJobParameterContributor> contributors` ctor arg (`@Autowired` list injection, ordered insertion order doesn't matter — each contributor only recognizes its own metadata key, so contributions union without conflict). The handler merges the trigger's static `jobParameters` block (Layer 1) with each contributor's contribution; later contributions can override earlier ones, but realistically each contributor writes a disjoint key set.

##### Layer 3 — Coordinator passes `jobParameters` to the Atlas job

`principalJobFacade.createJob(...)` is the boundary into the Atlas execution plane. Today it accepts a `JobParametersDTO`. **Decision (2026-05-12): the `Job` entity stays unchanged — no new `job_parameters` column on the table.** Instead, the coordinator merges the trigger's `jobParameters` block + each `TriggerJobParameterContributor`'s contribution into the existing `metadataMap` under the reserved key `__jobParameters`, defined as the constant `JobMetadataKeys.JOB_PARAMETERS` in `platform-workflow-coordinator-api`. The dataStream task action (Layer 4) reads this key back out at perform-time.

Rationale: the `Job` entity is the most-loaded table in the system, touched by hundreds of integration paths. Touching its schema for an opt-in feature that only a small fraction of jobs ever carry would force universal compile / test churn for marginal ergonomic gain. Piggyback on `metadata` and the change stays scoped to two callers.

The previous draft of this section described an explicit `JobParametersDTO.jobParameters` field that would be persisted to its own column. That field landed briefly as commit `0563f42f804` and was reverted in `a92bb1c4545` once the metadata-piggyback decision was made. The DTO stays at its pre-17b shape; the coordinator does the metadata-merge itself when constructing the DTO.

##### Layer 4 — `DataStreamStreamActionDefinition` consumes `jobParameters` at perform-time

The action already builds Spring Batch JobParameters when calling `JobLauncher.run(...)`. Extend the assembly: before launch, read `Job.metadata.get(JobMetadataKeys.JOB_PARAMETERS)`, cast to `Map<String, ?>`, and merge entries into the `JobParametersBuilder`. The Phase 17 `ItemStreamReaderDelegate.doBeforeStep` already reads `datastream.since` from `stepExecution.getJobParameters()` and copies into the per-step `ExecutionContext` under `ItemReader.SINCE_KEY` — that handoff still works unchanged.

Similar handoff for `datastream.mode`: extend the destination writer (`KnowledgeBaseItemWriter` already reads `MODE` from input parameters; pivot it to also consume `JobParameters.getString("datastream.mode")` with the input-parameter as fallback). Same for the CS `writeToReplica` cluster element.

#### Tasks for commit 5

1. **`TriggerJobParameterContributor` SPI + two impls.** ✅ Landed in `d3cc4394f11` (commit 5 Layer 1). Interface in `platform-workflow-coordinator-api`. CS impl in `automation-context-store-service`; KB impl in `automation-knowledge-base-service`. 12 unit tests across both modules cover the five fall-through branches plus the happy path.
2. **`JobMetadataKeys.JOB_PARAMETERS` reserved-key constant.** ✅ Landed alongside the metadata-piggyback decision. Defines `"__jobParameters"` as the well-known map-valued key inside `Job.metadata` where the coordinator merges trigger jobParameters + contributor outputs. No schema change to the `Job` entity.
3. **`TriggerCompletionHandler` reads trigger's `jobParameters` block + runs contributors + merges into `metadataMap`.** Compute order: copy the firing trigger's static `jobParameters` block from the workflow JSON first; then layer each contributor's response on top (later wins on key collisions); finally fold the merged map into `inputMap.metadata` under `JobMetadataKeys.JOB_PARAMETERS`. IntTest covers: a trigger declaring `jobParameters.datastream.mode = PARTIAL` spawns a job whose `metadata.__jobParameters` map contains that key; a workflow with `metadata.contextStoreSourceId` and an existing `lastSyncRunAt` spawns a job containing `datastream.since`.
4. **`DataStreamStreamActionDefinition` reads `Job.metadata.__jobParameters` and applies them on `JobLauncher.run(...)`.** Existing `ItemStreamReaderDelegate` Phase 17 plumbing stays unchanged — it reads from Spring Batch JobParameters, which now actually carry the values. New unit test: launch a synthetic Spring Batch job through the action with `Job.metadata.__jobParameters = {"datastream.since": 1234567890}` and assert the per-step ExecutionContext gets `SINCE_KEY = 1234567890`.
5. **Destination writers honor `datastream.mode` JobParameter override.** `KnowledgeBaseItemWriter` and the CS `writeToReplica` writer both gain a "check JobParameters first, then input parameters" mode selector. Mode-routing tests grow one case each.
6. **End-to-end IntTest paired-cadence sweep.** Extend `ContextStoreSyncE2EIntTest` (or write a new sibling): set up a source with `fullReplaceCadence = "@daily"`, fire the `scheduledIncrementalSync` trigger, assert the spawned job ran in PARTIAL mode with `datastream.since = previous lastSyncStart`. Then fire the `scheduledFullSync` trigger, assert FULL_REPLACE mode and `datastream.since` absent, tombstone sweep occurs.

#### Why this is a session of its own

After the metadata-piggyback decision (2026-05-12), the remaining scope crosses three modules (`platform-workflow-coordinator-impl`, the dataStream component module, the KB / CS destination writers) and one class that is widely depended on (`TriggerCompletionHandler`). The Atlas `Job` entity stays untouched — no schema change, no broad ripple. Each remaining change is small in isolation; doing them atomically without breaking existing tests still needs sequencing care.

Sequencing recommendation: Tasks 1 and 2 above are already landed (`d3cc4394f11` + the `JobMetadataKeys` constant). Tasks 3-6 in order — handler hook → action consumer → writer mode selectors → end-to-end IntTest. Each is one commit. Roughly four commits to close commit 5 out.

**Files** (remaining):

- `server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/trigger/completion/TriggerCompletionHandler.java` (load workflow def, read trigger.jobParameters, run contributors, merge into metadataMap under `JobMetadataKeys.JOB_PARAMETERS`).
- `server/libs/modules/components/data-stream/src/main/java/com/bytechef/component/datastream/DataStreamStreamActionDefinition.java` (read `Job.metadata[JobMetadataKeys.JOB_PARAMETERS]`, apply on `JobLauncher.run(...)`).
- `server/libs/modules/components/ai/vectorstore/knowledgebase/src/main/java/com/bytechef/component/ai/vectorstore/knowledgebase/destination/KnowledgeBaseItemWriter.java` (mode-selector: Spring Batch JobParameters first, input parameters fallback).
- CS destination writer mirror (location TBD when the writeToReplica writer lands its own mode-routing).

**Risks / open questions for the implementation session**:

- **Reserved-key namespace enforcement.** Today the workflow DSL doesn't reject user-authored `__`-prefixed metadata keys. An adversarial user could overwrite `__jobParameters`. Acceptable risk for an MVP — the auto-generated workflows we ship never expose user-controlled metadata writes — but worth a validator check before this surface is opened to external workflow authors. Captured in the `JobMetadataKeys` Javadoc.
- **Concurrent triggers on the same source.** If both `scheduledIncrementalSync` and `scheduledFullSync` fire at the same minute (cron alignment), both grab the same `lastSyncRunAt` via the contributor. The full run ignores `datastream.since` (incremental is opt-in per reader); the incremental run uses it. No correctness issue, just both pulling from the same upstream cursor — acceptable but worth documenting.
- **`datastream.since` jitter across distributed schedulers.** `ContextStoreSyncJobListener` writes `lastSyncRunAt` from `jobExecution.getStartTime()`. If two coordinators race a single trigger, the second's contribution may see a stale `lastSyncRunAt`. Spring Batch's job-instance uniqueness guard catches the duplicate at launch — acceptable.

### Part B — Cadence-pair UI

**Today**: each source has a single `cadence` field (`@hourly`, `@daily`, cron string, `@manual`). The workflow generator emits one trigger. Tombstone-bearing full-replace runs require manually swapping cadence values periodically — not a usable model.

**After 17b**: the source gains a second optional cadence — `fullReplaceCadence` — alongside the existing `cadence` (renamed conceptually to `incrementalCadence` in the UI). When set:

- The workflow generator emits **two** triggers: `scheduledIncrementalSync` (frequent, e.g. `@hourly`) and `scheduledFullSync` (rare, e.g. `@daily`).
- Both triggers route to the same `dataStream/v1/stream` task — they differ only by JobParameter: the rare trigger sets `datastream.mode=FULL_REPLACE` and **does not** set `datastream.since`; the frequent trigger sets `datastream.mode=PARTIAL` and lets the scheduler inject `datastream.since`.
- The DESTINATION cluster element (`contextStore/v1/writeToReplica`, `knowledgeBase/v1/writeAsDocument`) already reads `mode` and routes appropriately.

The UI extension: in the create-source dialog, the cadence field becomes a paired control: "Incremental sync every: [hourly]" + "Full re-sync every: [daily] (optional, recommended for sources without upstream change-feed)".

**Schema changes**:

- `context_store_source` and `knowledge_base_source` gain a nullable `full_replace_cadence` column (cron-string or named cadence; same shape as `cadence`).
- Existing rows have `full_replace_cadence = NULL` (single-trigger workflow, unchanged from today).

Per the "edit existing Liquibase" rule (branch unmerged), add the column to the existing init changeset in place; otherwise ship a follow-up changeset.

### Part C — Tombstone-derivation strategy

**The hole**: incremental sync only emits records the upstream filter matches (`lastModifiedTime > since`). Records the upstream system *deletes* never appear. Without a counter-strategy, deleted-upstream rows live forever in CS/KB.

**Three choices**, picked per-source on create:

- **`PERIODIC_FULL_REPLACE`** (default, recommended): pair the incremental cadence with a less-frequent full-replace cadence (Part B). The FULL_REPLACE run sees the complete current upstream set and the listener's tombstone-sweep tombstones anything missing.
- **`UPSTREAM_CHANGE_FEED`**: the source component reads a deletion stream (HubSpot CRM events, Salesforce CDC) and tombstones at write time. Requires the SOURCE cluster element to emit a `_deleted=true` record marker the DESTINATION recognizes — an SPI extension beyond Phase 17's scope. Forward-compatible: the strategy field acts as a feature gate.
- **`NONE`**: explicit opt-out. Source records are append-only; never tombstoned. Suitable for write-once event streams.

**Schema change**: add `tombstone_strategy` enum column (INT ordinal) to both `context_store_source` and `knowledge_base_source`. Default `PERIODIC_FULL_REPLACE` for new rows; existing rows backfill to `PERIODIC_FULL_REPLACE` (matches current behavior when paired with a daily full-replace).

**Enum stability** per CLAUDE.md memory: append new strategies at the end; pinned by `EnumOrdinalStabilityTest`.

## Tasks

1. **Scheduler-side JobParameter injection.** Extend the scheduler's job-creation path to read workflow metadata (`contextStoreSourceId` / `knowledgeBaseSourceId`), look up the source row, inject `datastream.since` (from `lastSyncRunAt`) when the trigger name implies incremental (`scheduledIncrementalSync` or no full-sync sibling), or omit when `scheduledFullSync`. Unit-test against a fake scheduler context.
2. **Liquibase column additions** (edit init in place per the established rule): `full_replace_cadence VARCHAR(64)` and `tombstone_strategy INT` on `context_store_source` + `knowledge_base_source`. Append `TombstoneStrategy` enum to `EnumOrdinalStabilityTest`.
3. **Domain + facade updates.** `ContextStoreSource` / `KnowledgeBaseSource` get `fullReplaceCadence` + `tombstoneStrategy` fields and getters/setters. `Create*Input` and `Update*Input` DTOs gain the two new fields. Facades persist them; workflow generation reads them.
4. **Workflow generator dual-trigger emission.** Both generators learn to emit two triggers when `fullReplaceCadence != null`. The full-trigger sets `datastream.mode = FULL_REPLACE` (and no `datastream.since`); the incremental trigger sets `datastream.mode = PARTIAL`. Generator tests update.
5. **GraphQL surface.** `CreateKnowledgeBaseSourceInput` + `UpdateKnowledgeBaseSourceInput` + `CreateContextStoreSourceInput` + `UpdateContextStoreSourceInput` gain `fullReplaceCadence: String` and `tombstoneStrategy: TombstoneStrategy` (new enum). `.graphqls` updates.
6. **UI cadence-pair control.** Update create-source dialog (`AddContextStoreSourceDialog` / `AddKnowledgeBaseSourceDialog`) to render the paired cadence picker. The full-cadence picker is disabled when `tombstoneStrategy = UPSTREAM_CHANGE_FEED` or `NONE`.
7. **IntTest.** Extend `ContextStoreSyncE2EIntTest` with a paired-cadence scenario: run incremental (sees one new record), then run full-replace (sees a deletion, tombstones it). Confirm `datastream.since` is injected on the incremental run and absent on the full run.

## Open questions

- **First incremental run after a missed full**: if the daily full-replace fails, the next incremental still uses the previous `lastSyncRunAt` — deletions accumulate until the next successful full. Acceptable, but document. A health-check could flag sources where the last full-replace is older than 2× the full cadence.
- **Cadence drift detection**: should the scheduler alert when `incrementalCadence` < `fullReplaceCadence`? Probably yes, but a runtime check, not a save-time block.
- **`UPSTREAM_CHANGE_FEED` strategy without component support**: the UI should grey out the option when the selected source component's cluster element doesn't advertise change-feed capability. Needs a new flag on the cluster element definition. Out of scope here — punt to a follow-up alongside the first change-feed pilot connector (HubSpot CRM events).

## Non-goals (17b)

- **Backfilling `UPSTREAM_CHANGE_FEED` for existing connectors** — that's per-connector SPI work; this phase just adds the strategy field as a forward-compatible enum.
- **Tombstone-after-X-cycles GC** — periodic full-replace's tombstone sweep is sufficient; deleted rows live softly until the GC sweep we'll spec separately.
- **Cross-source cadence orchestration** — each source's cadence pair is independent; no global "all sources sync at midnight" mode.

## Commit sequence

Landed (2026-05-12):

1. ✅ `202c14ba4f1 4857 Context Store Phase 17b commit 1 — add full_replace_cadence + tombstone_strategy columns`.
2. ✅ `90d69d9fc75 4857 Context Store Phase 17b commit 2 — TombstoneStrategy enum + ordinal stability pins`.
3. ✅ `9854c1a6623 4857 Context Store Phase 17b commit 3 — domain + DTO + facade plumb fullReplaceCadence + tombstoneStrategy` (also folded in the GraphQL surface for the two new fields — Schema + Create/UpdateGraphQlInput DTOs — so Task 5 from the original sequence merged into commit 3).
4. ✅ `6a841c20f3a 4857 Context Store Phase 17b commit 4 — workflow generator dual-trigger emission`.
6. ✅ `f3a3f28c3c1 4857 client - Context Store Phase 17b commit 6 — paired-cadence control on create-KB-source dialog`.

Commit 5 broken into sub-tasks (in-flight):

5a. ✅ `d3cc4394f11 — TriggerJobParameterContributor SPI + KB/CS impls` (Layer 1).
5b. ↩️ `0563f42f804 — JobParametersDTO gains jobParameters field` — landed, then reverted in `a92bb1c4545` after the 2026-05-12 metadata-piggyback decision. The `Job` entity no longer gains a new column; instead the coordinator writes the merged map into `Job.metadata` under the reserved key `__jobParameters` (constant `JobMetadataKeys.JOB_PARAMETERS` in `platform-workflow-coordinator-api`).
5c. ⏳ `CC17b 5/3 — TriggerCompletionHandler reads trigger.jobParameters + invokes contributors + merges into metadataMap` (Layer 4 of the four-layer design).
5d. ⏳ `CC17b 5/4 — DataStreamStreamActionDefinition reads Job.metadata.__jobParameters and applies on JobLauncher.run` (Layer 5).
5e. ⏳ `CC17b 5/5 — Destination writers (KB, CS) honor datastream.mode JobParameter override` (Layer 6).

Deferred to a follow-up session:

7. ⏳ **`CC17b IntTest paired-cadence end-to-end`** — depends on 5c–5e landing first. Without those the workflows emit two triggers but the destination writer ignores `datastream.mode`, so the end-to-end test can't observe the PARTIAL vs FULL_REPLACE routing it's meant to prove.

The `AddContextStoreSourceDialog` parallel client change (the CS-side of commit 6) is also queued — Phase 15 of the CS plan added the dialog and it needs the same paired-cadence + tombstone-strategy controls the KB dialog now has. Trivially small once Phase 17b commit 6's KB pattern is copied across.

## Backward compatibility

- **Existing sources** keep their single `cadence` field, `full_replace_cadence = NULL`, `tombstone_strategy = PERIODIC_FULL_REPLACE` — generator emits the same single-trigger workflow it does today.
- **The workflow JSON shape change** (single trigger vs two triggers) is generator-driven, not consumer-driven; readers/writers don't care how many triggers fire.
- **`datastream.since` injection** is additive — readers that ignore it (`supportsIncremental() = false`) keep doing full pulls.
- **`JobParametersDTO.jobParameters` field (commit 5, deferred)** ships with a `Map.of()` default via a compact constructor so every existing caller continues to compile and behave identically. The new field is only populated by `TriggerCompletionHandler` when the firing trigger declares a `jobParameters` block or a contributor returns non-empty.
