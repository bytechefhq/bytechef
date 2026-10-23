# Lazy, paged task-execution tree for the workflow execution detail view

Date: 2026-10-21

## Why

Opening a large workflow execution in the automation execution detail view never finishes. The execution
that showed it has **112,464 task executions**: 5 top-level tasks, nesting up to 6 levels deep, one parent
with 387 children and many loops with 150 iterations each. The detail view shows a spinner forever.

`GET /workflow-executions/{id}` (`ProjectWorkflowExecutionFacadeImpl.getWorkflowExecution`) loads every task
execution of the job, converts each to a `TaskExecutionDTO`, builds the full tree with
`TaskExecutionDTO.buildHierarchy`, and for every subflow child job recursively does the same
(`toJobDTO` → `getJobTaskExecutions`). The client then walks that whole tree several times. While a run is
going, the view re-fetches the whole tree every 2 s.

## Goals

- The detail view opens quickly for any execution size: the first response is bounded by the number of
  top-level tasks, not the total number of task executions.
- Expanding a task loads one page of its direct children (or, for loops, one page of iterations). Scrolling
  to the end of what is loaded fetches the next page. This works at every nesting depth.
- The view still opens on a failed task, expanded down to it. When nothing failed, it opens on the last task
  that ran (useful for stopped and running executions).
- Everything the user sees today stays the same: the task list, input/output/error tabs, retry attempts
  (previous executions of the same task), subflow "see executions" breadcrumbs, loop iteration grouping, the
  on-error main/error branches, and the 2 s live refresh while running.

## Non-goals

- The embedded (EE) workflow execution sheet (`IntegrationWorkflowExecutionFacadeImpl` and
  `client/src/ee/pages/embedded/workflow-executions/...`). It keeps the full tree for now. It is a follow-up
  that should reuse the same server pieces.
- The workflow editor test-output panel (`WorkflowExecutionsTestOutput.tsx`); it has a separate data source.
- The executions list pages. Their `getSubflowJobTaskExecutions` only changes how it loads: the parent task
  executions of all subflow rows come from one query, and a child job whose parent task execution is missing is
  skipped with a warning, as the list already does for jobs without a project.
- The AI `WorkflowExecutionTools.getWorkflowExecution`; it only reads top-level tasks and keeps working
  with the full response (see "Compatibility").

## Anti-goals

- Changing what any existing caller of `GET /workflow-executions/{id}` receives unless it opts in.
- Losing loop iteration grouping or on-error branch grouping because of paging (for example by paging raw
  child rows and splitting an iteration across pages).
- A detail view that still loads or walks the full tree somewhere (polling, counting, failure search).

## Design

### Server

**1. Opt-in lazy mode on the root endpoint.**
`GET /workflow-executions/{id}` gets an optional query parameter, `lazy`. Without it, or with `lazy=false`, the
response is unchanged. With `lazy=true`:

- `job.taskExecutions` holds only the top-level task executions (`parent_id IS NULL`) of the root job, with
  no `children` and no `iterations`.
- Each task execution carries a new field, `childCount` (integer, optional in the schema): the number of
  direct children for a plain parent, and the number of iterations (or branches) for a loop / on-error
  dispatcher. Zero or absent means the task cannot expand. The client uses it instead of
  `children.length` / `iterations.length` to decide expandability and to show the remaining count.
- `childJob` is a shallow job: the job's own fields with empty inputs, outputs, metadata, webhooks and task
  executions, never its tree. Opening a subflow
  ("see executions") calls the lazy root endpoint with the **child job id**
  (`GET /workflow-executions/{childJobId}?lazy=true`), which returns that job's top-level tasks, its own
  `focusPath` and its own total count, so the subflow view keeps today's auto-focus and header count. A
  child job copies its parent's principal only when the parent has one (`PrincipalJobFacadeImpl.createChildJob`),
  so `getWorkflowExecution` resolves the project deployment through the root job, following
  `parentTaskExecutionId` upwards. A subflow can call a workflow of another project;
  the root deployment is then left out, so a response never pairs one project with another project's
  deployment. A cyclic parent chain fails the request instead of looping. The recursive `toJobDTO` →
  `getJobTaskExecutions` path is not used in lazy mode.
- A new field `focusPath` on the root response: an ordered list of task execution ids from the top level
  down to the task the view should open on. As built, it is the earliest-created failed leaf (a task
  execution with an `error` and no children) that was not itself retried and whose top-level ancestor is the
  latest attempt of its task name. Failed leaves are read in pages of 100, ordered `created_date, id`, so the
  search never loads the whole job. If no such leaf exists, it points at the last started task, else the last
  created one. This is close to what the client's `getDeepestFailedExecution` picked, but not identical: the
  client walked depth-first, so the two can differ when concurrent branches (parallel, fork-join, each) hold
  failed leaves created out of depth-first order. Each element is
  `{taskExecutionId, iterationIndex?, childIndex?}`: `iterationIndex` under a loop (0 or 1 for the main or
  error branch of an on-error task), `childIndex` under a plain parent, so the client can open the page that
  holds the element; at most one of the two is set. A failed leaf whose ancestry or indexes cannot be resolved
  is skipped and the search moves on to the next candidate, then to the fallback. The path is empty only when
  nothing resolves, for example when the job has no task executions. The fallback does not skip superseded or
  retried attempts.
- A new field `taskExecutionCount`: the number of task executions of the job counting only the latest attempt
  of each task. It excludes failed attempts that are retried, top-level attempts superseded by a later
  attempt of the same task name (a resumed job), and the descendants of both. A failed attempt counts as
  retried when it has `status = FAILED`, an `error` and `retry_attempts < max_retries`, and a later sibling
  (same job and parent, same workflow task name) has `retry_attempts + 1`. Requiring the sibling keeps a failed
  on-error dispatcher, which has retries left but is never retried, in the count; matching the name keeps a
  failed task in the count when only a different sibling task was retried. It replaces the client's
  `countTaskExecutions` in lazy mode; full trees still use it.

**2. New paged children endpoint.**
`GET /workflow-executions/{id}/task-executions/{taskExecutionId}/children?pageNumber=&pageSize=`

- `{id}` is the workflow execution (root job) id. `{taskExecutionId}` must belong to that job or to one of its
  descendant subflow jobs, checked by `validateTaskExecutionBelongsToJob`.
- For a plain parent: a page of direct children, ordered `created_date, id` (today's order with a
  deterministic tiebreak), each with `childCount` and a shallow `childJob` where the task spawned a subflow,
  and no nested `children` or `iterations`.
- For a loop: a page of **iterations**. Each item is one iteration: the list of task executions in that
  iteration, grouped exactly as `buildHierarchy` does today (a new iteration starts when `taskNumber` resets
  to 0, in created-date order, **not** `ORDER BY task_number`, which merges iterations). `pageSize` counts
  iterations, not rows. The response also carries the total number of iterations.
- For an on-error dispatcher: its two branches (main, error), split by the `on-error-branch` task names as
  today. There are only two, so they come back in a single page.
- The response is one object with page metadata (page number, total elements, total pages) and **either**
  `children: TaskExecution[]` (plain parent) **or** `iterations: TaskExecution[][]` (loop / on-error), never
  both, mirroring how `TaskExecution` itself carries `children` or `iterations` today. A single `content`
  array cannot be typed for both cases.
- Default `pageSize` is 50, at most 200 (`MAX_PAGE_SIZE` in `LazyTaskExecutionTreeBuilder` and `maximum` in
  the spec). An out-of-range `pageNumber` or `pageSize` returns 400.

**3. Repository support and index.**
Before this change `task_execution` was indexed only on `id` and `job_id`. Every children page and every `childCount` query
filters by `parent_id`, so a new index `(parent_id, created_date, id)` is **required**, added by a Liquibase
changelog in its own commit before the code that depends on it. On PostgreSQL it is built with
`CREATE INDEX CONCURRENTLY` outside a transaction, so the upgrade does not block writes to `task_execution`;
other databases use a plain `createIndex`. The precondition looks for a valid index in the current schema,
not just an index of that name: an interrupted concurrent build leaves an invalid index behind, which the
changeset drops and builds again on the next start. The same applies to the `job(parent_task_execution_id)` index used
to attach subflow child jobs. The iteration count of a loop is the number of children with
`task_number = 0`, plus one when the first child does not start an iteration, so `childCount` for a whole page
of parents is two `GROUP BY parent_id` queries plus one query that checks the first child of every parent on
the page. Attaching child
jobs adds one more query per page. Loop pages are read by iteration-start offsets and
`created_date` ranges, so a page loads only the rows of its own iterations. The queries load full task
execution rows; there is no light projection. The in-memory repository mirrors the JDBC behaviour for tests.

**4. Schema and code generation.**
`childCount`, `focusPath` and the children page live in the platform workflow execution OpenAPI spec
(`server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-rest/platform-workflow-execution-rest-impl/openapi.yaml`)
and the automation spec
(`server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-rest/openapi.yaml`).
Regenerate with the modules' `generateOpenAPI` Gradle tasks (Spring interfaces and the TypeScript fetch
clients under `client/src/shared/middleware/...`). The embedded spec is not changed in this work.

### Client (automation detail view only)

- `useWorkflowExecutionDetail` requests the root in lazy mode.
- `WorkflowExecutionsAccordionItem` gets its children from a paged infinite query (React Query, keyed by
  workflow execution id + task execution id) that runs when the item is expanded. Pages hold
  `TASK_EXECUTION_CHILDREN_PAGE_SIZE` (50) entries and at most 10 pages stay loaded; earlier pages load
  backwards. The infinite-scroll trigger (an IntersectionObserver rooted at the scroll area viewport) fetches
  the next page instead of revealing more of an already-loaded array; `ITERATIONS_PAGE_SIZE` remains only for
  full trees. Children and iterations from the full response must keep working, because the
  embedded sheet and the editor test output keep passing full trees into the same component.
- Expandability uses `childCount` when present, else today's `children`/`iterations` lengths.
- Opening behaviour: `deepestFailedExecution` and the accordion `defaultValue` come from `focusPath` instead
  of `getDeepestFailedExecution` walking the tree. The items on the path are expanded and their pages loaded
  so the focused task is visible and selected. If the focused task sits beyond the first page, the view opens on
  the page that contains it and loads earlier or later pages from there.
- Retry attempts: today `groupTaskExecutionAttempts` groups attempts **only for the top-level tasks** of the
  active job; nested children and iterations are rendered one row per task execution. That stays exactly as it
  is. Paging therefore pages raw child rows (or iterations), and `childCount` is a plain count. Grouping nested
  attempts is out of scope (it would collapse `each`/`parallel`/`map` children that share one task name).
- The header's task count (`countTaskExecutions`, which walks the whole tree) comes from the server's
  `taskExecutionCount` instead.
- Polling while running: the root query keeps polling at 2 s, but it is now the small lazy response.
  Expanded children pages of a running task are refetched too (only those currently open), so new
  iterations appear, and once more when that task finishes, so its final state shows while the job keeps
  running. Finished executions do not poll.
- Subflows: "see executions" pushes the child job as today; its tree loads lazily the same way. When a
  subflow fails to load, the view offers a way back to the parent.
- A query of children pages is cached per start page, so reopening on another focused page starts there
  instead of reusing pages cached from an earlier start. Without a known index, the view loads at most the
  page limit while looking for a focused child.

## Compatibility

- Without the lazy parameter, `GET /workflow-executions/{id}` returns the same full tree as today, so
  `WorkflowExecutionTools` is not broken; a subflow whose parent task execution is missing still fails the
  request rather than being left out. `ProjectDeploymentWorkflowExecutionsSheet` (which uses the root only for
  the project version) and `useWorkflowExecutionSheet` only need top-level data and request lazy mode.
- The embedded facade and its client are untouched.
- `GET /workflow-executions/{id}/task-executions/{taskExecutionId}` now returns a shallow `childJob` for a task
  that spawned a subflow, where it used to return none. The change is additive.

## Testing

- Server: facade/IntTest coverage for lazy root (top level only, `childCount`, shallow `childJob`,
  `focusPath` for failed / stopped / completed jobs), the children endpoint for plain parents, loops
  (iteration grouping across page boundaries, `taskNumber` resets, page totals) and on-error dispatchers,
  and ownership validation (a task execution from another job is rejected). Controller tests as
  `@WebMvcTest` IntTests, following the existing pattern for this controller.
- Client: tests for lazy expansion, next-page loading, opening on `focusPath`, and that full trees passed in
  (embedded / test output) still render.
- Manual: an execution with over 100,000 task executions opens quickly and can be drilled into.

## Design decisions

- Lazy mode is opt-in by query parameter rather than a new root endpoint.
- Default page size 50.
- `focusPath` falls back to the last started task when nothing failed, and otherwise picks the earliest
  failed leaf under the latest attempt (see above).
- Retry-attempt grouping stays top-level only, as today.
- Subflows open through the lazy root endpoint with the child job id.
- The embedded sheet is a follow-up, not part of this work.

## Decided while building

- Names: the `lazy` parameter, `childCount`, `focusPath` with `{taskExecutionId, iterationIndex?, childIndex?}`
  elements, `taskExecutionCount` and the `TaskExecutionChildrenPage` response.
- Iteration boundaries, child counts, retried-attempt detection and the failed-leaf search run in SQL;
  `focusPath` indexes are counts in SQL as well. Superseded top-level attempts are found in Java from the
  top-level rows already loaded and passed to those queries.
- The view jumps straight to the page holding a deep-linked iteration or child, then loads earlier or later
  pages.
- Each index changelog is its own commit, before the code that depends on it; generated code is committed
  separately from the hand-written changes.
