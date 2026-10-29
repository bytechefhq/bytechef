# Stopped Job Unfinished Tasks

Issue: [#6797](https://github.com/bytechefhq/bytechef/issues/6797)

## Intent

When a user stops a running job, some of its task executions stay `STARTED` or `CREATED` forever, and the workflow executions view shows them with a running spinner and `0ms`.

Observed on job 3291 (load test, 112,844 task executions, nested loops with a condition inside):

| Time | Event |
|---|---|
| `20:59:08.856` | `onStopJobEvent` sets the job `STOPPED` |
| `20:59:08.880` | an in-flight `postgresql_notes` completes; `completeIfNotCancelled` succeeds because the canceller has not reached it yet |
| `20:59:09.056` | `loop_rows` moves on and dispatches `condition_cancelled`, which marks itself `STARTED` and creates `mathHelper_total` |
| `20:59:18.5` | `UnfinishedTaskExecutionCanceller` cancels the loops, from a snapshot that predates `condition_cancelled` |

End state: `condition_cancelled` `STARTED`, `mathHelper_total` `CREATED`, both forever.

## Cause

1. **Flow-control dispatches skip the stop check.** `StopJobTaskDispatcherPreSendProcessor` runs only inside `DefaultTaskDispatcher`, which handles worker tasks. Condition, loop, branch, fork-join, each, parallel, subflow and the others resolve to their own dispatcher in `TaskDispatcherChain` and never look at the job status, so they start and create children after the stop.
2. **The stop check does not persist.** When `StopJobTaskDispatcherPreSendProcessor` sees a stopped job, it sets `CANCELLED` and an end date on the in-memory `TaskExecution`, and `DefaultTaskDispatcher` drops it. The row stays `CREATED`.
3. **The canceller is slow on large jobs.** `UnfinishedTaskExecutionCanceller.cancel` loads every task execution of the job (`getJobTaskExecutions`) to find the few unfinished ones. On 112k rows this took about 10 s, which is what made the race in (1) likely rather than rare.
4. **The client cannot tell a user stop from a suspend.** `getTaskExecutionDisplayStatus` shows unfinished tasks as cancelled only for `COMPLETED` and `FAILED` jobs. `STOPPED` was excluded in `e0ee986d13d` (6423) because a suspended job is also `STOPPED`, and its open tasks are real.

## Goals

- After a user stops a job, no task execution of that job starts, and every unfinished one ends `CANCELLED` in the database.
- A suspended job (`STOPPED` with `jobResumeId` metadata) behaves exactly as today on the server.
- The executions view shows unfinished tasks of a user-stopped job as cancelled, including rows left behind before this fix. A suspended job keeps its running tasks as running.

## Non-goals

- Changing how suspended jobs treat sibling branches. That is PR #6376 (`SuspendedJobs.canRunTasks`), which this design stays compatible with.
- Repairing old rows in the database. The client display covers them; a data migration is not worth it for a display-only symptom.
- Making `onStopJobEvent` transactional (the existing `TODO @Transactional`).

## Design

### Telling a user stop from a suspend (server)

Add `SuspendedJobs` to `atlas-execution-api` (`com.bytechef.atlas.execution.domain`) with `JOB_RESUME_ID = "jobResumeId"` and `isSuspended(Job)`: `STOPPED` and `JOB_RESUME_ID` metadata present. This is the same class, constant and method as on PR #6376, so when that PR rebases onto master the files collide as add/add and resolve by taking #6376's fuller version.

A job is **stopped by a user** when it is `STOPPED` and not `isSuspended`.

### Gate every dispatch of a user-stopped job

Add `StoppedJobTaskDispatcher` to `atlas-coordinator-impl` (`task.dispatcher` package), implementing both `TaskDispatcher<TaskExecution>` and `TaskDispatcherResolver`:

- `resolve(task)`: returns itself when the task is a `TaskExecution` whose job is stopped by a user; otherwise `null`, so the chain moves on. Control tasks (`CancelControlTask` and the rest) are not `TaskExecution`s and pass through.
- `dispatch(taskExecution)`: calls `taskExecutionService.cancelIfUnfinished(id)`, which locks the row, sets `CANCELLED` with an end date, and saves it. Nothing is dispatched.

It goes **first** in both places that assemble a `TaskDispatcherChain`:

- `TaskCoordinatorConfiguration.taskDispatcher()`
- `JobSyncExecutor` (the API-platform sync path and the editor's Test button)

Being first, it catches flow-control and worker dispatches alike, so cause (1) and cause (2) are fixed in one place.

`StopJobTaskDispatcherPreSendProcessor` stays. For a user-stopped job it is no longer reached; for a suspended job it keeps today's behavior (worker tasks dropped in memory), which #6376 changes on its own terms.

**Why this closes the race.** Every task execution is created and then dispatched, and each dispatch now reads the job status:

- If the dispatch reads the job before the stop commits, the row already exists, so the canceller's snapshot (taken after the stop) contains it and cancels it. If a flow-control dispatcher then writes `STARTED`, `TaskExecutionServiceImpl.update` keeps the terminated status (it only copies the start date onto a terminated row).
- If the dispatch reads the job after the stop commits, the gate cancels the row itself.

A worker task that passed the gate before the stop is cancelled by the canceller, and its completion is dropped by `completeIfNotCancelled`, as today.

**Cost.** One `jobService.getJob(jobId)` by primary key per dispatch. Worker tasks of a running job now read the job twice (gate and pre-send processor); this is a primary-key read and was judged not worth sharing.

### Load only unfinished task executions in the canceller

Add `TaskExecutionRepository.findAllUnfinishedByJobId(long jobId)` (JDBC query on `status IN (0, 1)`, i.e. `CREATED` and `STARTED`, and an in-memory implementation) and `TaskExecutionService.getJobUnfinishedTaskExecutions(long jobId)`. `UnfinishedTaskExecutionCanceller.cancel` uses it instead of loading the whole job.

This is no longer needed for correctness, but it shortens the stop from seconds to milliseconds on large jobs, and the same canceller runs when a job fails.

### Client display

In `workflowExecution-utils.tsx`:

- `getTaskExecutionDisplayStatus(status, job)` takes the job (status and metadata) instead of only the status. A job has ended when it is `COMPLETED`, `FAILED`, or `STOPPED` without `metadata.jobResumeId`. An unfinished task of an ended job shows as `CANCELLED`.
- The `jobStatus` prop that `WorkflowExecutionsAccordionItem` → `TaskExecutionChildrenList` → `WorkflowTaskExecutionItem` already pass down becomes a `jobEnded` boolean computed once at the top, so the metadata check lives in one function and the children do not need the job.
- A task with no end date shows `-` instead of `0ms` when the job has ended.

## Testing

Server (DB-backed, per the no-mocks-for-core-paths rule):

- `StoppedJobTaskDispatcherIntTest` (Postgres): a task of a user-stopped job is cancelled in the database and not passed on; tasks of suspended, running and resumed jobs and control tasks pass through.
- `LoopTaskDispatcherStoppedJobIntTest` (real `LoopTaskDispatcher` and `LoopTaskCompletionHandler`): a child completes after its job was stopped (the in-flight race) and the next child ends `CANCELLED`; a loop dispatched after the stop is cancelled, not started, and creates no children. Top-level tasks need no such test: `JobExecutor.execute` already refuses to run the next task of a job that is not `STARTED`.
- Each is shown to fail with the gate removed.

The gate reads the job with `getJob`, not `fetchJob`: in the EE microservices `RemoteJobServiceClient.fetchJob` is unsupported, while `getJob` is what `StopJobTaskDispatcherPreSendProcessor` already calls on every worker dispatch. `getJobUnfinishedTaskExecutions` gets a remote endpoint, because `coordinator-app` reads task executions through `RemoteTaskExecutionServiceClient`.
- `JdbcTaskExecutionRepositoryIntTest` and `InMemoryTaskExecutionRepositoryTest` cover `findAllByJobIdAndStatusIn`.
- `UnfinishedTaskExecutionCancellerTest` updated for the new service call.

Client:

- `workflowExecution-utils.test.tsx`: user-stopped job maps `STARTED`/`CREATED` to `CANCELLED`; suspended job keeps them; completed and failed unchanged.
- `WorkflowTaskExecutionItem.test.tsx`: `-` instead of `0ms` for an ended job.

## Commits

Each a single subject line, server and client separate:

1. `6797 Add SuspendedJobs to tell a suspended job from one stopped by a user`
2. `6797 Cancel every task dispatched for a job stopped by a user instead of starting it`
3. `6797 Load only unfinished task executions when cancelling a stopped or failed job`
4. `6797 client - Show unfinished tasks of a job stopped by a user as cancelled`
5. `6797 docs - Add the stopped job unfinished tasks design`
