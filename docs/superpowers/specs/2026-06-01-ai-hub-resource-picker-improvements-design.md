# AI Hub Resource Picker Improvements — Design

Date: 2026-06-01
Status: Approved (design)
Scope: Client only (`client/`)

## Problem

The AI Hub resource pickers — the composer `+`/`@` menu (`ResourcePickerMenu`) and the resource-panel
`+` menu (`AiHubFilePicker`) — have three issues:

1. **Not all workflows load.** The workflow list is built by looping over workspace projects capped at
   `PROJECT_LIMIT = 10`, so workflows in the 11th+ project never appear. Workflows are shown flat (with
   the project name as a right-aligned suffix) rather than grouped by project.
2. **Workflow executions don't fully load and can't be searched well.** The executions list fetches
   only the **first** REST page (20 rows) and has no working "load more" over the full set, even though
   the user has 33,527 executions across many pages. Executions also lack the "Show more" affordance
   that other sections have.
3. **Execution row layout.** The subtitle (`project · STATUS`) renders inline to the **right** of the
   name; the user wants it on a **second line beneath** the name.

## Constraints discovered

- The `Workflow` model has **no** `projectId` / `projectName` fields, so the global `getWorkflows()`
  endpoint (all workflows, no paging) cannot be grouped by project on its own. Project association is
  only available by iterating projects (`getProjectWorkflows({ projectId })`), which the current code
  already does — it just caps at 10 projects.
- The workflow-executions REST endpoint (`getWorkflowExecutionsPage`) supports `pageNumber` paging
  (fixed page size 20) and filters (status/date/project/workflow) but has **no free-text search**
  parameter in the controller or repository SQL. The `Page` response includes `number`, `size`,
  `numberOfElements`, `totalPages`, `totalElements`, `content`.

## Decisions (locked)

- **Workflows:** remove the 10-project cap, load all projects' workflows, **group by project**,
  search client-side. (No backend work; project names already available from the projects query.)
- **Executions:** server-side paging via "Show more" (`pageNumber`), search filters the **already
  loaded** rows (honest limit). No backend change.
- **Execution row:** stack name over a muted `text-xs` subtitle line.

## Architecture

### 1. Workflows: load all + group by project

Files:
- `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx`
  (`useAllWorkspaceWorkflows`, `PROJECT_LIMIT`, workflow rows in search + drilldown modes).
- `client/src/pages/automation/ai-hub/AiHubFilePicker.tsx` (its own `useAllWorkspaceWorkflows`).

Changes:
- **Remove `PROJECT_LIMIT = 10`** (or raise to effectively unbounded): the `useQueries` over projects
  iterates **all** workspace projects from `useGetWorkspaceProjectsQuery`. Project count is bounded
  (tens), each `getProjectWorkflows` is cheap and react-query-cached.
- Each workflow item carries `{ id, name, projectId, projectName, projectWorkflowId }`.
  `ResourcePickerMenu`'s `WorkflowItemI` already has `projectName`; **add `projectName`** to
  `AiHubFilePicker`'s `WorkflowItemI` (it currently omits it) so both can group.
- `projectWorkflowId` already comes from `workflow.projectWorkflowId` (fixed previously). Keep.
- **Render grouped by project:** in the Workflows section, group items by `projectName` and render a
  muted project sub-header followed by that project's workflow rows. Applies to the flat/search view.
  The existing two-level drilldown (project → workflows) remains valid and unchanged.
- **Search** matches workflow `name` + `projectName` (client-side, over the full loaded set).
- The per-section "Show more" cap (`SECTION_INITIAL_CAP = 20`, `SECTION_EXPAND_INCREMENT = 50`) stays
  as a UI guard over the now-complete list.

### 2. Workflow executions: server-side paging via "Show more"

Files:
- `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx`
  (`useGetWorkspaceProjectWorkflowExecutionsQuery`, executions rows + show-more).
- `client/src/shared/queries/automation/workflowExecutions.queries.ts` (paging hook).

Changes:
- Track a local `executionsPageNumber` state (start `0`). Fetch executions with
  `getWorkflowExecutionsPage({ id: workspaceId, environmentId, pageNumber: executionsPageNumber })`.
- **Accumulate pages:** keep a running list of fetched executions. "Show more" increments
  `executionsPageNumber`, fetches the next page, and **appends** `content` to the accumulated list.
  Use react-query with `keepPreviousData` / an accumulator (e.g. a ref or `useInfiniteQuery`-style
  merge) so prior rows aren't dropped.
- **Stop condition:** hide "Show more" when `number + 1 >= totalPages` (or accumulated length
  `>= totalElements`). Optionally show `loaded / totalElements`.
- **Search:** filters the accumulated (loaded) rows client-side by workflow label + project name.
  Document the limit in a comment: search matches only loaded pages, not the full server set.
- Executions now render the same "Show more" affordance as other sections (over server pages).

Decision on hook shape: prefer converting the executions query to support incremental page
accumulation. Either (a) `useInfiniteQuery` keyed on the filter set with `getNextPageParam` derived
from `totalPages`, or (b) a manual accumulator with `pageNumber` state + `keepPreviousData`. Pick (a)
if it fits the existing query-factory conventions; otherwise (b). Both are pure client changes.

### 3. Execution row: subtitle below name

File: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx` (execution row JSX in
both search and drilldown modes).

- Replace the inline layout:
  ```
  <HistoryIcon /> <span className="flex-1 truncate">{label}</span>
                  <span className="ml-2 text-xs text-muted-foreground">{project · STATUS}</span>
  ```
  with a stacked layout:
  ```
  <HistoryIcon />
  <div className="flex min-w-0 flex-1 flex-col">
    <span className="truncate">{label}</span>
    <span className="truncate text-xs text-muted-foreground">{project · STATUS}</span>
  </div>
  ```
- `min-w-0` on the column keeps both lines truncating instead of overflowing.

## Components / boundaries

- `useAllWorkspaceWorkflows` (both pickers) — returns the full, project-enriched workflow list; the
  render layer owns grouping. Same input/output shape, just uncapped + `projectName` everywhere.
- Executions paging — encapsulated in the executions query/accumulator; the render layer consumes a
  flat accumulated list + a `hasMore` flag.
- Execution row — presentational; layout-only change.

## Error handling

- Workflow per-project fetches that fail resolve to empty for that project (existing `useQueries`
  behavior); other projects still render. No hard failure.
- Executions paging: a failed page fetch surfaces via the existing query-error path; the already-loaded
  rows remain visible; "Show more" can be retried.
- Empty states (no workflows / no executions) render the existing "no results" messaging.

## Testing

- `ResourcePickerMenu`:
  - Workflows from **more than 10 projects** all appear (cap removed) and are **grouped by project**
    (project sub-header + rows); search matches name + project.
  - Executions: initial render shows page 0; clicking "Show more" appends page 1 (assert accumulated
    length grows and prior rows persist); "Show more" hides once `totalPages` reached; search filters
    loaded rows.
  - Execution row renders the subtitle on a **second line** (assert the stacked structure / both spans
    present beneath the name).
- `AiHubFilePicker`: `WorkflowItemI` carries `projectName`; workflows from >10 projects appear and
  group by project.

## Out of scope

- Backend free-text search for executions or workflows (explicitly deferred; client search over loaded
  pages is the chosen behavior).
- The workflow-tab-as-project-editor changes (separate spec:
  `2026-06-01-ai-hub-workflow-tab-project-editor-design.md`).
