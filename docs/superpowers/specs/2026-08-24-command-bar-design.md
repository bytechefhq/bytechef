# CommandBar design

Date: 2026-08-24
Status: approved, not implemented

## Summary

Replace the client's `GlobalSearchDialog` with a **CommandBar**: an n8n-style command palette
where every entry — a navigation target, a create action, an individual workflow — is a
**command**. Modules register commands through a client-side SPI. A command carries an ordered
list of actions executed in sequence, which is what lets one command navigate to a page and then
open a dialog that page owns.

The server already has the search half of this (`SearchAssetProvider`, ten implementations, fanned
out by `AutomationSearchFacadeImpl`). It gains a type filter so a type-scoped sub-mode queries one
provider instead of ten.

## Motivation

`GlobalSearchDialog` is 286 lines with nine hardcoded `CommandGroup` blocks, each pairing a
`SearchAssetType` with a hardcoded `navigate(path)`. It can only navigate, only to nine asset
types, and adding a tenth means editing the file. It has no commands, no nesting, and no recents.

Three consequences:

- A feature that adds a resource type must edit a shared component instead of contributing to it.
- Non-navigation actions ("create a project", "run this workflow") have nowhere to live.
- Page-specific affordances (the workflow editor's actions) cannot reach the palette at all.

## Decisions

| Decision | Choice | Rejected |
|---|---|---|
| Core model | One `CommandI`; resources are dynamically produced commands | Separate Command and Resource kinds (two render paths, two recents formats) |
| Action shape | Ordered `CommandActionType[]` run in sequence | Single async handler (cannot express navigate-then-act across a remount) |
| Nesting | Sub-modes with a stack | Flat list only (no type-scoped resource search) |
| Registration | Bootstrap `registerCommandSource` **and** `useRegisterCommands` hook | Bootstrap-only (pages can't own commands); hook-only (globals depend on a mounted component) |
| Page-owned dialogs | Navigate + one-shot intent | Global dialog host (refactors every dialog); URL search params (pollutes URLs, no non-serializable payloads) |
| Recents | localStorage per user+tenant | Server-persisted (table, migration, facade, GraphQL for a per-device concern) |
| Server change | Additive `types` filter on `automationSearch` | Client-side filtering (fans out to ten providers to use one) |

## The command model

`client/src/shared/command-bar/types.ts`

```ts
export type CommandActionType =
    | {to: string; type: 'navigate'}
    | {key: string; payload?: unknown; type: 'intent'}
    | {run: (context: CommandRunContextI) => Promise<void> | void; type: 'callback'};

export interface CommandI {
    actions?: CommandActionType[];
    children?: CommandChildrenI;
    group?: string;
    icon?: LucideIcon;
    id: string;
    keywords?: string[];
    subtitle?: string;
    title: string;
    when?: (context: CommandContextI) => boolean;
}

export interface CommandChildrenI {
    minQueryLength?: number;
    placeholder: string;
    resolve: (query: string, signal: AbortSignal) => Promise<CommandI[]>;
}

export interface CommandContextI {
    edition: string | undefined;
    featureFlags: (flag: string) => boolean;
    pathname: string;
}

export interface CommandRunContextI {
    command: CommandI;
    context: CommandContextI;
    navigate: NavigateFunction;
}
```

`CommandRunContextI` is what a `callback` action receives. It carries the command being run (so a
resolved resource command can read its own payload), the context the command was matched against,
and `navigate` — a callback often needs to route after doing its work, and reaching for the router
hook from outside React is not available to a plain function.

### Invariant: exactly one of `actions` or `children`

A command with `children` pushes a sub-mode when selected. A command with `actions` executes when
selected. A command with both, or neither, is a programming error.

TypeScript cannot express this as a discriminated union without either an artificial discriminant
field on every command literal or a union that reads badly at every call site. Instead, the check
runs in `collectCommands`, where commands materialise — a source is a lazy function of context, so
there is nothing to inspect at registration time. It throws in development and logs, then SKIPS the
command, in production: a command with neither actions nor children would otherwise render in the
palette and do nothing when clicked. The message names the offending id, which is what makes the
failure diagnosable even though it surfaces at collection rather than at registration.

### Sources

```ts
export interface CommandSourceI {
    getCommands: (context: CommandContextI) => CommandI[];
    id: string;
}
```

The context deliberately excludes the current workspace and environment. Both are reachable through
`useWorkspaceStore.getState()` / `useEnvironmentStore.getState()` at the moment a command runs, and
including them would force every source to be re-evaluated on each environment switch for the
benefit of the few commands that care.

A source is a function of context, so a source can return automation commands only on automation
routes without putting a `when` predicate on each of its commands. `when` remains for per-command
exceptions (a feature flag on one entry, a permission on another).

Sources are re-evaluated when `CommandContextI` changes — not per keystroke. Filtering the
resulting list against the typed query is cmdk's job.

## Registration

Two doors, one registry, both writing into `useCommandBarStore`.

**Global**, from a bootstrap module, mirroring the existing `shared/edition` seam:

```ts
registerCommandSource(navigationCommandSource);
```

**Page-scoped**, registered on mount and removed on unmount:

```ts
useRegisterCommands(commands, [dependencies]);
```

The hook's cleanup removes exactly the commands it added, by id. Two pages registering the same id
is a programming error; the registry warns in development and last-registration-wins.

### Sources shipped in the first cut

| Source | Kind | Contributes |
|---|---|---|
| `useRegisterNavigationCommands` | Hook, from `App.tsx` | One "Go to X" command per sidebar entry of the CURRENT surface |
| `resourceCommandSource` | Global | One children-command per searchable asset type ("Open workflow", "Open connection", …) |
| `createCommandSource` | Global | "Create project", "Create connection", "Create data table", "Create knowledge base" |
| Workflow editor | Page-scoped | Editor actions registered via `useRegisterCommands` |

### Why navigation uses the hook door

Which navigation entries are visible depends on edition, three feature flags, the AI configuration,
the context-store toggle, and the current environment — roughly forty lines of filtering that
already live in `App.tsx` and produce `filteredAutomationNavigation` / `filteredEmbeddedNavigation`.

`useRegisterNavigationCommands` is called from `App.tsx` with those already-filtered lists, so the
filter has exactly one implementation. A global source would have to either duplicate that logic or
carry all of it into `CommandContextI`.

A consequence worth stating: `App.tsx` sets its navigation to the automation list or the embedded
list by pathname, and to nothing on `/platform/*`. The palette therefore offers the navigation of the
surface you are on, and no navigation commands at all under `/platform`. That mirrors the sidebar,
which is the intent, but it means "Go to" is surface-scoped rather than global.

### Refactor: navigation arrays move out of App.tsx

`automationNavigation`, `embeddedNavigation`, and `platformNavigation` currently live as literals
in `client/src/App.tsx`. They move to `client/src/shared/navigation/navigationItems.ts`, imported
by `App.tsx` (for filtering and the document-title effect) and available to anything else that
needs the canonical list.

The sidebar's grouping behaviour is unchanged: `AppSidebar` still folds consecutive items sharing a
`group`, so group members must stay adjacent in the arrays after the move.

## Execution

```
close the palette
  → for each action, in order, awaited
    → record the command in recents
```

**The palette closes first.** A dialog opened by action 2 must not mount while a Radix modal is
mid-unmount; this repo has already been bitten by pointer-events interactions between overlay
layers.

**Sequential and awaited.** Action N+1 starts after action N resolves. The first action to throw
stops the remainder and surfaces a toast; nothing is recorded in recents. This is the whole reason
actions are a list: `navigate` must complete, and the destination page must mount, before the
intent it publishes can be claimed.

**Recents are recorded on success only**, so a command that failed does not get promoted to the top
of the palette.

### Intents

`intent` publishes `{key, payload}` into a one-shot `useCommandIntentStore`. **The dialog claims it,
not the page.** Every creation dialog in this codebase owns its own `open` state internally —
`CreateDataTableDialog` gets it from `useCreateDataTableDialog`, `ProjectDialog` from a local
`useState(!triggerNode)` — and none of them expose a controlled `open` prop. The page therefore has
no handle to open them with, but the dialog already has one:

```ts
// inside CreateDataTableDialog
useCommandIntent('dataTable.create', handleOpen);
```

This needs no prop plumbing and no change to how the dialogs are constructed. Where a page renders
the same dialog twice (an empty-state instance and a populated-state instance), `claim` clears the
intent synchronously before returning, so the second instance's effect finds nothing and only one
dialog opens.

The hook subscribes to the pending intent rather than only reading it at mount, because a command
run FROM the page it targets never remounts anything: `navigate` to the route you are already on is
a no-op, so a mount-only claim would leave the intent to expire and the command would silently do
nothing. It remains one-shot — `claim` clears synchronously — and the store is not persisted, so a
remount, a refresh, or a back-navigation still cannot re-open the dialog.

**Intents expire.** An intent still pending 15 seconds after publication is CLEARED, and logs a
development-only warning naming the key. Every claimant sits behind a data gate — the connections
dialog waits on the full component-definition list — so the lifetime has to cover a cold route chunk
plus its query, while staying far short of a session.

Both halves matter, and the expiry is the load-bearing one. An intent that merely warns and lingers
is not a pending message, it is a landmine: it waits indefinitely for the next mount of anything
using that key, on any surface. Publish "create data table", navigate away before the list page
finishes loading, and without expiry the create dialog opens unprompted the next time the user opens
an individual data table. The warning exists for the other failure — a renamed route turning a
command into one that silently does nothing.

**Claiming is opt-in, per call site.** `useCommandIntent(key, handler, enabled)` takes a third
argument, and each creation dialog exposes a `claimsCreateIntent` prop defaulting to FALSE. Only the
instances on the list page a create command actually navigates to pass `true`.

Expiry alone is not sufficient, because the dialogs are shared components. `ConnectionDialog` has
more than fifteen call sites, including a persistently-mounted create-mode instance inside the
project-deployment wizard; `CreateDataTableDialog` and `CreateKnowledgeBaseDialog` are each reused in
their resource's detail-page sidebar. A claim keyed only by intent name makes every one of those an
eligible claimant. `ProjectDialog` avoids being forced open only by accident of how its other call
sites are mounted — and instead silently swallows intents meant for the list page.

An intent needs both a lifetime and an addressee. Expiry supplies the first; the opt-in prop supplies
the second.

Intent keys are namespaced `<domain>.<verb>` (`project.create`) and are declared as constants
alongside the command that publishes them, so the publisher and claimant share one symbol.

## UI and interaction

`components/CommandBar/CommandBarDialog.tsx`, still built on cmdk, driven by the registry.

### Stack

`useCommandBarStore` holds `stack: CommandI[]`.

- **Empty stack (root):** registered commands grouped by `group`. Recent first, then domain groups,
  Navigation last.
- **Non-empty:** the parent's title renders as a static heading, the input placeholder becomes
  `children.placeholder`, and the list shows resolved children.
- `Backspace` on an empty input pops one level.
- `Esc` pops if nested, closes if at root.
- Closing the dialog clears the stack and the query.

### Resolution

`children.resolve(query, signal)` is debounced 300 ms, matching current behaviour. **Each new
keystroke aborts the previous signal.** Today's implementation has no cancellation, so a slow early
response can land after and overwrite a fast later one; the abort closes that.

`minQueryLength` defaults to 2 (today's threshold). `resourceCommandSource` sets it to 0 so an
opened sub-mode lists the newest results before anything is typed.

### Recents

Zustand with `persist` under the name `bytechef.commandBar.recents`, holding a
`Record<userId, RecentCommandI[]>`. Tenant separation is free: localStorage is partitioned by
origin, and tenants are already separated by host.

```ts
export interface RecentCommandI {
    actions: CommandActionType[];
    id: string;
    title: string;
}
```

**No icon is persisted.** A `LucideIcon` is a React component, not JSON. The UI re-resolves the icon
from the live registry by `id` and falls back to `ArrowRightIcon` when the command is no longer
registered — which is also what the n8n screenshots show for recents.

**Only commands whose actions are all `navigate` or `intent` are recorded.** A `callback` action is
a closure and cannot survive a page reload; recording one would produce a recent that silently does
nothing. Page-scoped editor commands are therefore not recorded, which is the right outcome anyway —
they are meaningless outside the page that registered them.

Capped at 5, deduped by `id`, most recent first.

A recent pointing at a deleted resource is just a stored `navigate` action; following it lands on
the application's existing not-found handling. Recents are not validated on load — doing so would
mean a fan-out of existence checks on every palette open to guard against a rare and non-destructive
outcome.

## Routing correctness

Eight of the nine `navigate()` targets in `GlobalSearchDialog` point at routes that do not exist.
This is pre-existing and is fixed **before** the CommandBar work, as its own commits, so the palette
is built on a search that works.

| Type | Current target | Actual route | Fix |
|---|---|---|---|
| Project | `/automation/projects/{id}` | `projects/:projectId/project-workflows/:projectWorkflowId` | needs `projectWorkflowId` on the result |
| Workflow | `.../projects/{projectId}/workflows/{id}` | `projects/:projectId/project-workflows/:projectWorkflowId` | wrong segment; ids already correct |
| Data table | `/automation/data-tables/{id}` | `datatables/:id` | drop the hyphen |
| Knowledge base | `/automation/knowledge-bases/{id}` | `knowledge-bases/:id` | correct already |
| Asset file | never rendered | `asset-files/:fileId` | render it; needs the GraphQL enum fix |
| KB document | `.../knowledge-bases/{kbId}/documents/{id}` | no such route | list + select intent |
| Deployment | `/automation/deployments/{id}` | `deployments` (list only) | list + select intent |
| API collection | `/automation/api-collections/{id}` | `api-platform/api-collections` (list only) | list + select intent |
| API endpoint | `.../api-collections/{cid}/endpoints/{id}` | no such route | list + select intent |

### Types with no detail route

Deployments, connections, API collections, API endpoints and knowledge-base documents are rendered
by list pages with in-page selection; there is nothing to deep-link to. Their commands navigate to
the list page and publish a select intent carrying the id:

```ts
actions: [
    {to: '/automation/connections', type: 'navigate'},
    {key: 'connection.select', payload: {id}, type: 'intent'},
]
```

**Until a page opts in to claiming, the select intent is not published at all** — the command carries
only its `navigate`. Publishing an intent nobody claims costs two things: it fires the
development-only "unclaimed" warning on the happy path, every time anyone opens one of these
resources, which trains developers to ignore the one warning that matters; and since the store holds
a single intent and `publish` overwrites, an unclaimable select intent can evict a pending create
intent. `SELECT_INTENT_KEYS` stays in place as the declared namespace, so opting a page in later
re-enables publication in one line.

A list page that has not claimed its intent still lands the user on the correct page, so the opt-in
remains incremental. Adding real detail routes for these five types is a larger piece of work
and is explicitly out of scope; the intent payload is the same `{id}` a route parameter would carry,
so converting later is a local change to one route map.

### New field: ProjectSearchResult.projectWorkflowId

Opening a project requires a project **and** a project-workflow id. `ProjectSearchResult` carries
only `(id, name, description, workspaceId)`, so `ProjectSearchAssetProvider` gains the id of the
project's first latest project-workflow, exposed as a nullable `projectWorkflowId`.

Null means the project has no workflows. Such a project cannot be opened at all through the existing
UI either, so its command navigates to `/automation/projects` rather than constructing a route that
would 404.

## Server changes

### Type filter

```graphql
automationSearch(query: String!, limit: Int, types: [SearchAssetType!]): [SearchResult!]!
```

```java
List<SearchResult<?>> search(String query, int limit, Set<SearchAssetType> types);
```

A null or empty `types` means all types, so existing callers are unaffected. The facade filters
`providers` by `getAssetType()` **before** building the `CompletableFuture` list, so a type-scoped
sub-mode spawns one task rather than ten.

The tenant propagation, `SecurityContext` re-establishment, and workspace-accessibility filtering in
`AutomationSearchFacadeImpl` are untouched.

### Bug fix: ASSET_FILE missing from the GraphQL schema

`AssetFileSearchAssetProvider` is an active `@Component` returning `AssetFileSearchResult`, and
`SearchAssetType.ASSET_FILE` exists in Java. The GraphQL schema has neither the enum value nor a
type implementing `SearchResult`, so any query matching an asset file fails interface resolution.

Add to `automation-search.graphqls`:

- `ASSET_FILE` to the `SearchAssetType` enum
- an `AssetFileSearchResult` type implementing `SearchResult`

The `SearchAssetType` enum is a GraphQL enum, not a persisted ordinal, so appending is safe. (The
repo's append-only rule applies to enums stored as INT; this one is not.)

## Rename and rollout

| Before | After |
|---|---|
| `components/GlobalSearch/GlobalSearchDialog.tsx` | `components/CommandBar/CommandBarDialog.tsx` |
| — | `shared/command-bar/` (types, registry, store, execution, intents, sources) |
| Navigation arrays in `App.tsx` | `shared/navigation/navigationItems.ts` |

- Feature flag stays `ff-2396`; Cmd+K binding and the single mount point in `App.tsx` are unchanged.
  With the flag off there is no key listener, no dialog, and no fetch — nothing observable differs from
  today. Registration is not literally suppressed: `useRegisterNavigationCommands` still runs, with an
  empty list, and the workflow editor still registers its page-scoped commands, because a hook cannot be
  called conditionally. Those sources hold no commands anyone can reach, so the off state is inert in
  behaviour if not in bookkeeping.
- The nine hardcoded `CommandGroup` blocks are **deleted, not adapted**. Their route templates move
  into `resourceCommandSource` as a `SearchAssetType -> {group, icon, route}` map: the same
  information expressed as data rather than JSX.

## Testing

Most of this subsystem is pure functions and stores, which is where the coverage goes.

**Vitest, unit:**

- registry: add, remove, duplicate-id warning, source re-evaluation on context change
- the exactly-one-of `actions`/`children` assertion
- `when` and context-based filtering
- execution: ordering, awaiting, abort-on-throw, recents recorded only on success, palette closed
  before the first action
- intents: claim once, clear after claim, unclaimed warning after the timeout
- recents: dedupe by id, cap at 5, ordering, per-user/tenant key isolation

**Vitest + React Testing Library, `CommandBarDialog`:**

- push on selecting a children-command; placeholder and heading swap
- `Backspace` on empty input pops; `Esc` pops when nested and closes at root
- a stale `resolve` is aborted and its late result never renders

Per repo convention, stores are reset in `beforeEach` and async store updates are awaited with
`waitFor` on the resulting state — never a fixed `setTimeout` delay.

**JUnit:**

- `AutomationSearchFacadeImplTest` extended: a `types` set restricts which providers are called;
  null and empty mean all; the workspace-accessibility filter still applies
- `AutomationSearchFacadeSecurityContextTest` extended rather than replaced

**Before commit:** `cd client && npm run check`; `./gradlew spotlessApply` and `./gradlew check`.

## Out of scope

- Server-persisted recents
- Deep-linkable command URLs (`?action=create`); the intent store covers the first cut, and URL
  parameters can be added later for the few commands worth sharing as links
- Embedded and platform *resource* search — `automationSearch` is automation-only today and stays
  that way; navigation commands for those surfaces still ship
- "Create workflow". A workflow belongs to a project, so the command needs a project picker: a nested
  command whose children come from project search, each child navigating to that project and
  publishing a create intent. Every piece of machinery it needs exists after the first cut, but it
  is the only create command that is not a flat two-action command, and it is not worth holding the
  rest for
- Fuzzy ranking beyond what cmdk provides
- Detail routes for deployments, connections, API collections, API endpoints and knowledge-base
  documents; the list-plus-select-intent treatment stands in for them
