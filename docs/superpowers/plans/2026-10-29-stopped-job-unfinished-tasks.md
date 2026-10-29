# Stopped Job Unfinished Tasks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After a user stops a job, no task execution of that job starts and none is left `CREATED`/`STARTED`; the executions view shows leftovers of a user-stopped job as cancelled.

**Architecture:** A `StoppedJobTaskDispatcher` resolver placed first in every `TaskDispatcherChain` cancels (persistently, via `cancelIfUnfinished`) any `TaskExecution` dispatched for a job stopped by a user, which covers flow-control and worker dispatches alike. Suspended jobs (`STOPPED` + `jobResumeId` metadata) pass through untouched. The canceller loads only unfinished rows. The client treats a user-stopped job as ended.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Testcontainers PostgreSQL, JUnit 5; React 19, Vitest 4.

**Spec:** `docs/superpowers/specs/2026-10-29-stopped-job-unfinished-tasks-design.md`

## Global Constraints

- Worktree `/Volumes/Data/bytechef/bytechef-6797`, branch `6797`, base `upstream/master` `5d83fb359a1`. Never push or open a PR without an explicit yes.
- Commit messages: one subject line, no body, no trailers. Server `6797 <description>`, client `6797 client - <description>`, docs `6797 docs - <description>`. Commit by path.
- Suspended = `job.getStatus() == STOPPED && job.getMetadata("jobResumeId") != null`. Stopped by a user = `STOPPED` and not suspended.
- `SuspendedJobs` must match PR #6376's class text for `JOB_RESUME_ID` and `isSuspended` (package `com.bytechef.atlas.execution.domain`, module `atlas-execution-api`), Apache header, `@author Ivica Cardic`.
- Java style per `CLAUDE.md`: blank line before control statements and after variable modification, no method chaining outside the allowed list, descriptive names, test methods camelCase.
- Core paths are tested against real services and repositories (Postgres or the in-memory repositories), not Mockito. Each new test must be shown to fail with the fix removed.
- `./gradlew spotlessApply` before every server commit; `npm run format` and `npm run check` before the client commit.

## Review Focus

1. **Job deleted while a task is dispatched** (`JobSyncExecutor.deleteJob`, test runs): the gate must not throw where dispatch did not throw before. Use `jobService.fetchJob(jobId)`; an absent job resolves to `null` (pass through). Test in Task 2.
2. **Suspended job sibling dispatch:** a `STOPPED` job with `jobResumeId` must still reach the next resolver exactly as today. Test in Task 2.
3. **Resume and restart of a stopped job:** `resumeToStatusStarted` flips the job to `STARTED` before dispatching, so the gate must read the job fresh per dispatch (no caching). Test in Task 2 (stop, then resume, then dispatch reaches the next resolver).
4. **Control tasks during a stop** (`CancelControlTask` from the canceller itself): must pass the gate. Test in Task 2.
5. **Running job with `jobStatus` undefined on the client** (editor test output before the job loads): rows keep their own status. Test in Task 4.

---

### Task 1: `SuspendedJobs`

**Files:**
- Create: `server/libs/atlas/atlas-execution/atlas-execution-api/src/main/java/com/bytechef/atlas/execution/domain/SuspendedJobs.java`
- Test: `server/libs/atlas/atlas-execution/atlas-execution-api/src/test/java/com/bytechef/atlas/execution/domain/SuspendedJobsTest.java`

**Interfaces:**
- Produces: `SuspendedJobs.JOB_RESUME_ID = "jobResumeId"`, `static boolean isSuspended(Job job)`, `static boolean isStoppedByUser(Job job)` (`STOPPED && !isSuspended`).

- [ ] **Step 1: Write failing tests** `testIsSuspendedWhenStoppedWithResumeId` (true), `testIsSuspendedWhenStoppedWithoutResumeId` (false), `testIsSuspendedWhenStartedWithResumeId` (false), `testIsStoppedByUserWhenStoppedWithoutResumeId` (true), `testIsStoppedByUserWhenSuspended` (false), `testIsStoppedByUserWhenCompleted` (false). Build `Job` with `setStatus` and `setMetadata(Map.of(SuspendedJobs.JOB_RESUME_ID, "abc"))`.
- [ ] **Step 2: Run** `./gradlew :server:libs:atlas:atlas-execution:atlas-execution-api:test --tests '*SuspendedJobsTest'`. Expected: compilation failure.
- [ ] **Step 3: Implement** the final class with a private constructor.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `6797 Add SuspendedJobs to tell a suspended job from one stopped by a user`

### Task 2: Gate every dispatch of a user-stopped job

**Files:**
- Create: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/task/dispatcher/StoppedJobTaskDispatcher.java`
- Modify: `server/libs/atlas/atlas-coordinator/atlas-coordinator-config/src/main/java/com/bytechef/atlas/coordinator/config/TaskCoordinatorConfiguration.java` (`taskDispatcher()`, ~line 155)
- Modify: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/JobSyncExecutor.java` (chain assembly, ~line 197)
- Test: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/test/java/com/bytechef/atlas/coordinator/task/dispatcher/StoppedJobTaskDispatcherIntTest.java`
- Test: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/test/java/com/bytechef/atlas/coordinator/TaskCoordinatorIntTest.java` + workflow `src/test/resources/workflows/stop1.json`
- Test: `server/libs/modules/task-dispatchers/condition/src/test/java/com/bytechef/task/dispatcher/condition/ConditionTaskDispatcherStoppedJobTest.java`

**Interfaces:**
- Consumes: `SuspendedJobs.isStoppedByUser(Job)`.
- Produces: `public class StoppedJobTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver`, constructor `(JobService jobService, TaskExecutionService taskExecutionService)`; `resolve(Task)` returns `this` only for a `TaskExecution` whose job (`jobService.fetchJob`) is stopped by a user, else `null`; `dispatch(TaskExecution)` calls `taskExecutionService.cancelIfUnfinished(id)` and nothing else.

- [ ] **Step 1: Write failing gate IntTest** `StoppedJobTaskDispatcherIntTest`: Postgres, real `JobServiceImpl`/`TaskExecutionServiceImpl` over the JDBC repositories (reuse `TaskCoordinatorIntTest.CoordinatorIntTestConfiguration` via `@SpringBootTest(classes = ...)` or copy its beans). A `TaskDispatcherChain` with resolvers `[new StoppedJobTaskDispatcher(...), recordingResolver]`, where `recordingResolver` accepts every `TaskExecution` and records it. Tests:
  - `testDispatchCancelsTaskOfJobStoppedByUser`: job created and started, `setStatusToStopped`, task execution `CREATED` saved; dispatch. Assert DB row `CANCELLED`, `endDate` not null, recorder empty.
  - `testDispatchCancelsStartedFlowControlTaskOfJobStoppedByUser`: same with a `STARTED` row. Assert `CANCELLED`.
  - `testDispatchPassesTaskOfSuspendedJob`: job metadata `jobResumeId` set before stop. Assert recorder got it, row still `CREATED`.
  - `testDispatchPassesTaskOfRunningJob`: job `STARTED`. Assert recorder got it.
  - `testDispatchPassesTaskAfterStoppedJobResumes`: stop, `resumeToStatusStarted`, dispatch. Assert recorder got it.
  - `testDispatchPassesControlTask`: `new CancelControlTask(jobId, taskExecutionId)` for a stopped job reaches a recording control resolver.
  - `testDispatchPassesTaskOfDeletedJob`: task execution whose job id has no row (`fetchJob` empty). Assert recorder got it and no exception.
- [ ] **Step 2: Write failing end-to-end IntTest** in `TaskCoordinatorIntTest`: `testStopJobCancelsTasksDispatchedAfterTheStop`. Workflow `stop1.json`: two tasks, `stopper` (type `stopHelper/v1/stop`) then `next` (`randomHelper/v1/randomInt`). The `stopper` handler calls `jobService.setStatusToStopped(taskExecution.getJobId())` and returns (this reproduces an in-flight task completing after the stop, before the canceller reaches it). Start with `jobSyncExecutor.startJob(...)`, then poll `taskExecutionService.getJobTaskExecutions(jobId)` with a 10 s deadline until the `next` row is terminated. Assert no row of the job is `CREATED` or `STARTED`, and `next` is `CANCELLED`. Today `next` stays `CREATED` (the pre-send processor cancels it only in memory).
- [ ] **Step 3: Write failing flow-control test** `ConditionTaskDispatcherStoppedJobTest` (in-memory `JobServiceImpl`/`TaskExecutionServiceImpl`, real `ConditionTaskDispatcher`): chain `[StoppedJobTaskDispatcher, conditionTaskDispatcher, recordingDefault]`, job stopped by user, condition task execution `CREATED` with a true branch of one task. Dispatch. Assert the condition row is `CANCELLED` (not `STARTED`), `taskExecutionService.getJobTaskExecutions(jobId)` has no child, recorder empty.
- [ ] **Step 4: Run** `./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:testIntegration --tests '*StoppedJobTaskDispatcherIntTest' --tests '*TaskCoordinatorIntTest'` and `./gradlew :server:libs:modules:task-dispatchers:condition:test --tests '*ConditionTaskDispatcherStoppedJobTest'`. Expected: compile failure, then (after a stub class) assertion failures.
- [ ] **Step 5: Implement** `StoppedJobTaskDispatcher` and put it first in both chain assemblies (`Stream.of(stoppedJobTaskDispatcher)` concatenated before the resolver factories). In `TaskCoordinatorConfiguration` expose it as a `@Bean`.
- [ ] **Step 6: Run** Step 4 commands. Expected: PASS.
- [ ] **Step 7: Prove the tests**: remove the gate from both chains, rerun, confirm `testStopJobCancelsTasksDispatchedAfterTheStop` fails with `next` `CREATED` and the condition test fails with the condition `STARTED`; restore.
- [ ] **Step 8: Run module checks** `./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:check :server:libs:atlas:atlas-coordinator:atlas-coordinator-config:check :server:libs:platform:platform-job-sync:check :server:libs:modules:task-dispatchers:condition:check`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 9: Commit** `6797 Cancel every task dispatched for a job stopped by a user instead of starting it`

### Task 3: Canceller loads only unfinished task executions

**Files:**
- Modify: `server/libs/atlas/atlas-execution/atlas-execution-repository/atlas-execution-repository-api/src/main/java/com/bytechef/atlas/execution/repository/TaskExecutionRepository.java`
- Modify: `.../atlas-execution-repository-jdbc/src/main/java/com/bytechef/atlas/execution/repository/jdbc/JdbcTaskExecutionRepository.java`
- Modify: `.../atlas-execution-repository-memory/src/main/java/com/bytechef/atlas/execution/repository/memory/InMemoryTaskExecutionRepository.java`
- Modify: `server/libs/atlas/atlas-execution/atlas-execution-api/src/main/java/com/bytechef/atlas/execution/service/TaskExecutionService.java`, `.../atlas-execution-service/.../TaskExecutionServiceImpl.java`
- Modify: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/task/UnfinishedTaskExecutionCanceller.java`
- Test: `JdbcTaskExecutionRepositoryIntTest`, `InMemoryTaskExecutionRepositoryTest`, `UnfinishedTaskExecutionCancellerTest`

**Interfaces:**
- Produces: `List<TaskExecution> findAllUnfinishedByJobId(long jobId)` on the repository (`@Query("SELECT * FROM task_execution WHERE job_id = :jobId AND status IN (0, 1) ORDER BY id")`, matching the ordinal-integer style of the existing `status = 3` query); `List<TaskExecution> getJobUnfinishedTaskExecutions(long jobId)` on the service.

- [ ] **Step 1: Write failing tests** `testFindAllUnfinishedByJobId` in both repository tests: one job with `CREATED`, `STARTED`, `COMPLETED`, `FAILED`, `CANCELLED` rows plus a `STARTED` row of another job; assert exactly the two unfinished ids of the first job, ascending.
- [ ] **Step 2: Run** `./gradlew :server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc:testIntegration --tests '*JdbcTaskExecutionRepositoryIntTest'` and the memory module `test`. Expected: compile failure.
- [ ] **Step 3: Implement** the repository method (JDBC query, in-memory filter), the service method, and switch `UnfinishedTaskExecutionCanceller.cancel` to it (drop the now-redundant terminated check only if every caller path still passes its test). Update `UnfinishedTaskExecutionCancellerTest` stubs to the new service call.
- [ ] **Step 4: Run** Step 2 commands plus `./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:test :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:testIntegration`. Expected: PASS (the Task 2 IntTests still pass).
- [ ] **Step 5: Run checks** on the touched modules (`check`). Expected: BUILD SUCCESSFUL.
- [ ] **Step 6: Commit** `6797 Load only unfinished task executions when cancelling a stopped or failed job`

### Task 4: Client shows leftovers of a user-stopped job as cancelled

**Files:**
- Modify: `client/src/shared/components/workflow-executions/util/workflowExecution-utils.tsx`
- Modify: `client/src/shared/components/workflow-executions/WorkflowTaskExecutionItem.tsx`, `WorkflowExecutionsAccordionItem.tsx`, `TaskExecutionChildrenList.tsx`, `LazyTaskExecutionChildren.tsx`
- Modify: `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetContent.tsx`, `client/src/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput.tsx`
- Test: `client/src/shared/components/workflow-executions/tests/workflowExecution-utils.test.tsx`, `WorkflowTaskExecutionItem.test.tsx`, `WorkflowExecutionsAccordionItem.test.tsx`

**Interfaces:**
- Produces: `export function isJobEnded(job?: Job): boolean` (`COMPLETED`, `FAILED`, or `STOPPED` with `job.metadata?.jobResumeId == null`); `getTaskExecutionDisplayStatus(status: ExecutionStatusType, jobEnded?: boolean): ExecutionStatusType`; prop `jobEnded?: boolean` replaces `jobStatus` on `WorkflowTaskExecutionItem`, `WorkflowExecutionsAccordionItem`, `TaskExecutionChildrenList`, `LazyTaskExecutionChildren`; the two pages pass `jobEnded={isJobEnded(job)}`.

- [ ] **Step 1: Write failing tests** in `workflowExecution-utils.test.tsx`: `isJobEnded` true for `{status: 'STOPPED'}`, false for `{metadata: {jobResumeId: 'abc'}, status: 'STOPPED'}`, true for `COMPLETED`/`FAILED`, false for `STARTED` and `undefined`; `getTaskExecutionDisplayStatus('STARTED', true)` → `'CANCELLED'`, `('CREATED', true)` → `'CANCELLED'`, `('STARTED', false)` → `'STARTED'`, `('STARTED', undefined)` → `'STARTED'`, `('COMPLETED', true)` → `'COMPLETED'`. In `WorkflowTaskExecutionItem.test.tsx`: a `STARTED` task without `endDate` and `jobEnded` renders no `animate-spin` icon and shows `-` (not `0ms`); with `jobEnded` false it still spins. Update existing tests in `WorkflowExecutionsAccordionItem.test.tsx` from `jobStatus` to `jobEnded`, adding one for a stopped-by-user job and one for a suspended job. Keys sorted per the sort-keys rule.
- [ ] **Step 2: Run** `cd client && npx vitest run src/shared/components/workflow-executions`. Expected: FAIL.
- [ ] **Step 3: Implement** `isJobEnded`, the new `getTaskExecutionDisplayStatus` signature, the prop rename through the four components, and `-` for a missing duration when `jobEnded`.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Prove** by reverting `isJobEnded` to `COMPLETED || FAILED`: the stopped-by-user tests fail; restore.
- [ ] **Step 6: Run** `cd client && npm run format && npm run check`. Expected: lint, typecheck, and all tests pass.
- [ ] **Step 7: Commit** `6797 client - Show unfinished tasks of a job stopped by a user as cancelled`

### Task 5: Docs and final verification

- [ ] **Step 1: Commit the spec and this plan** `6797 docs - Add the stopped job unfinished tasks design and plan`
- [ ] **Step 2: Manual check** against `bytechef_test`: run this branch's server on :9555 and client, open jobs 2858 and 3291 in Load Test; the leftover rows show the cancelled icon and `-`, no spinners. Start a new run of the Load Test workflow, stop it mid-loop, and confirm with `SELECT status, count(*) FROM bytechef_000001.task_execution WHERE job_id = <id> GROUP BY status` that no row is `0` or `1`.
