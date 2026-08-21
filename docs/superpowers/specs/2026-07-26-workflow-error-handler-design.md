# Workflow error handler (error workflow + Error Trigger)

**Status:** Designed, not implemented.

Brings n8n's *error workflow* / *Error Trigger* pair
(<https://docs.n8n.io/build/flow-logic/handle-errors-gracefully>) to ByteChef automation: when a
workflow run ends `FAILED`, run a designated workflow that receives the failed run's details.

## Why this, and only this

ByteChef already covers most of that n8n page. The audit that produced this spec found:

| n8n mechanism | ByteChef today |
|---|---|
| Retry on fail | `TaskExecution.retryAttempts` / `retryDelay` / `retryDelayFactor` |
| Continue using error output | `on-error` task dispatcher (`mainBranch` + `onErrorBranch`) |
| *(no n8n equivalent)* | `WorkflowTask.finalize` — a `finally` block |
| Alerting on failure | `JOB_FAILED` notifications (email/webhook/Slack), EE workflow alert rules |
| Execution review | execution history |
| **Error workflow** | **missing** |
| **Error Trigger** | **missing** |
| Stop And Error | missing — deliberately out of scope, see below |

Only the error-workflow/Error-Trigger pair is a genuine gap, and they are one feature because the
trigger is how the error workflow starts.

### Layering against the `on-error` dispatcher

The two compose without overlap, which is what makes this feature small:

- **`on-error` dispatcher** — *intra*-workflow catch. The error is handled, the error branch runs,
  the job ends `COMPLETED`. No error workflow fires.
- **Error workflow** — *inter*-workflow catch. Fires only when the job ends `FAILED`, i.e. the error
  escaped every `on-error` branch.
- **`finalize`** — still the `finally`, unaffected by either.

There is no precedence rule to invent and no per-task "continue on fail" setting to add. A future
proposal for such a setting should be measured against the `on-error` dispatcher first; it is likely
duplication rather than a gap.

## Configuration

Two nullable columns, no relation tables (per
`2026-07-25-workspace-relation-table-convention-revision.md`):

- `project.error_project_workflow_id` — project-wide default
- `project_workflow.error_project_workflow_id` — per-workflow override
- `project_workflow.error_workflow_disabled` (boolean) — explicit opt-out

Resolution at failure time: **workflow override → project default → none**. The separate boolean
exists because `null` already means "inherit"; overloading it would make opting out impossible.

**Target:** a `projectWorkflowId` (stable across workflow versions) **in the same project**.
Cross-project handlers are out of scope for v1: the handler must be deployed and enabled in the same
environment as the failing run, and cross-project turns that into a matrix. Config is already
per-project, so "one handler per project" covers the common case.

**Validated at configuration time, not at failure time** — a broken reference must not surface as a
second failure while the first is being handled:

- the referenced workflow exists in the project
- it contains a `workflow/newWorkflowError` trigger
- it is not the workflow being configured

**Scope:** automation only. Embedded integrations have no project and the config surface is
project-shaped. Not a parity gap to be closed silently later — it is a deliberate v1 boundary.

**Environment:** the handler runs in the failed run's environment, under the same principal.

## The Error Trigger

A new trigger on the existing `workflow` component, beside `newWorkflowCall`:
**`workflow/newWorkflowError`**. Reusing the component means no new module and no new registry entry.
Its presence is the gate the config validator checks, mirroring the existing rule that a workflow is
MCP-exposable only if it carries `workflow/newWorkflowCall`.

The trigger declares an output schema so the editor renders data pills. The run is started directly
through `PrincipalJobFacade`, not over HTTP, so the trigger is a schema and a marker rather than a
listener.

### Payload

```
execution:
  jobId
  url                     // link to the failed run's execution detail page
  error: { message, stackTrace }
  lastTaskExecuted        // failing task name + type; null if the run failed before any task
  mode                    // webhook / schedule / manual / api
  autoRecoveryAttempts    // how many times this run was already auto-recovered after a crash
workflow:
  projectId, projectWorkflowId, workflowId, label
environment
```

Three deliberate departures from n8n:

- **`environment`** — ByteChef runs one workflow across environments; a handler that cannot tell
  production from staging is close to useless.
- **`autoRecoveryAttempts`, not `retryOf`** — n8n creates a NEW execution on retry and points
  `retryOf` at the old one; ByteChef resumes a job IN PLACE (`resumeToStatusStarted` reuses the same
  id), so no prior job exists to reference. An earlier draft specified `resumeOf` as "the prior job
  id", which was unimplementable for exactly that reason. The field now carries the count that
  `OrphanedJobRecoveryMonitor` actually writes.
- **One shape, not two** — n8n emits a structurally different payload when the trigger itself fails.
  Here `lastTaskExecuted` is simply null, and handlers test for it.

**No task inputs or outputs**, matching n8n. The `jobId` is the handle; a handler needing more fetches
it through existing APIs. That keeps copying run data into a second execution history an explicit,
auditable choice.

## Dispatch

`ErrorWorkflowJobStatusApplicationEventListener` in `platform-coordinator`, mirroring
`NotificationJobStatusApplicationEventListener`, at `@Order(300)` — after the cost listener (100) and
workflow alerts (200), so dispatch never delays alerting. Both are configured, alerts fire first.

`platform-coordinator` gains dependencies on `platform-workflow-execution-api` and
`automation-configuration-api` — **interfaces only**. Depending on `automation-configuration-service`
instead would put a second set of `ProjectService` / `ProjectDeploymentService` /
`ProjectWorkflowService` beans on the deliberately datasource-less `coordinator-app`, which already
binds those interfaces from `automation-configuration-remote-client`, and the app would fail to boot.

**The feature is monolith-only.** An earlier draft of this spec claimed it worked in both topologies,
reasoning from `RemotePrincipalJobFacadeClient.createJob` being a real REST call rather than a stub.
That reasoning was incomplete: dispatch is only the last step, and resolution comes first.
`RemoteProjectWorkflowServiceClient` is entirely `UnsupportedOperationException` stubs, so in a
distributed deployment the resolver cannot look up the failing workflow at all. This is the same
limitation, for the same reason, as orphaned-job recovery.

Because the listener is fail-open, that would otherwise mean swallowing an exception on every failed
job, forever, in a distributed deployment. The listener therefore detects the unsupported operation,
logs it once, records it as a distinct metric outcome, and skips — so the limitation reads as a
capability gap rather than a recurring error. Lifting it means implementing
`getWorkflowProjectWorkflow` and `getProjectWorkflow` on that remote client plus the matching
`configuration-app` endpoints.

On `JobStatusApplicationEvent` with status `FAILED`:

1. **Recursion guard first.** If the job's metadata carries `errorHandlerFor`, stop.
2. Resolve project and workflow from the job's principal.
3. Resolve handler: workflow override → project default → skip. Skip when disabled, missing, or not
   deployed in this environment.
4. Defensive self-reference check — a workflow can be re-pointed after config-time validation.
5. Submit through `PrincipalJobFacade.createJob` with metadata `errorHandlerFor=<failedJobId>`, in the
   failed run's environment and tenant.

### Safety

**Recursion cap is the guard this feature cannot ship without.** An error-workflow run that itself
fails does not dispatch another, because it carries `errorHandlerFor`. Chains cap at depth 1, matching
n8n. Without it, one broken handler spawns jobs forever.

**Fail-open throughout.** A dispatch failure logs a warning and touches neither the failed job's
status nor the rest of the event fan-out. Handling an error must never manufacture a second one.

**Admission gates are not bypassed.** The handler job goes through the normal `createJob` path, so
plan rate limits, the concurrency gate and the monthly cost cap all apply. That is the real storm
control: a bad deploy failing 5,000 runs is bounded by the plan gate rather than becoming an unbounded
job storm. Rejections are counted, not silently dropped.

**Metric:** `bytechef_error_workflow_dispatch{outcome=dispatched|skipped_no_config|skipped_recursion|
skipped_not_deployed|rejected|failed}`, wired via `ObjectProvider<MeterRegistry>` so it no-ops without
a registry.

### Known limits

- **Failure storms are bounded but not deduplicated.** 5,000 failures produce up to 5,000 handler
  runs, capped by plan limits. Per-project dedup or throttling needs a windowing policy that is a
  product decision; the plan gate already prevents the pathological case.
- **Depth-1 chains only.** A handler's own failure is invisible to error workflows. It is still
  visible to `JOB_FAILED` notifications and alert rules, which is the correct escalation path.

## Testing

- **Unit** (`ErrorWorkflowJobStatusApplicationEventListenerTest`): recursion guard trips on
  `errorHandlerFor`; resolution falls back override → project → none; explicit disable beats an
  inherited default; self-reference rejected; every failure path stays fail-open. Note the documented
  Mockito trap — unstubbed wrapper-returning methods yield `0`, not `null`, and this listener branches
  on nullable ids, so stub `thenReturn(null)` explicitly.
- **Config validation:** reference resolves in-project, target carries the trigger, target is not self.
- **Integration** (`ErrorWorkflowIntTest`, Testcontainers): a failing workflow dispatches the handler
  in the same environment with `errorHandlerFor` set. **Write the negative twin first** — a failing
  error workflow must not spawn another. That test is what keeps an infinite job loop out of
  production.
- **Contract pinning:** assert the payload's exact field names. The payload is a public contract the
  moment a user builds a handler against it.
- **Snapshot regeneration:** adding a trigger regenerates `workflow_v1.json`. Delete the stale copy
  from **both** `src/test/resources/definition/` and `build/resources/test/definition/`, or the test
  compares against the build-output copy and passes wrongly.

## Out of scope

- **Stop And Error node** — a component action that fails a run with a custom message. Genuinely
  missing and genuinely independent; deferred so this spec stays one feature.
- **Per-task continue-on-fail setting** — likely duplicates the `on-error` dispatcher.
- **Cross-project and embedded handlers.**
- **Failure-storm dedup.**
