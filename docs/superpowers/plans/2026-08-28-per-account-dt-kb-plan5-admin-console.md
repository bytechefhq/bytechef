# Embedded Admin Console for Data Tables and Knowledge Bases Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the vendor admin a page in `/embedded` that lists data tables and knowledge bases, filters them by connected user, and assigns ownership — and stop offering the two components in the integration workflow palette.

**Architecture:** Three independent slices, deliberately ordered cheapest-and-least-reversible-first: a one-line server palette change, then the GraphQL client wiring that any UI needs, then the pages themselves. The spec's "move the automation components into `shared/components/`" is **not** taken wholesale — see Task 4 and the scope note below for why, and what is done instead.

**Tech Stack:** React 19, TypeScript 6, TanStack Query, GraphQL Code Generator, TailwindCSS 4, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`

**Predecessors:** Plans 1–4, all landed on this branch. Plan 4 built the `embeddedDataTables` / `embeddedKnowledgeBases` queries and their assign mutations; this plan consumes them.

## Scope note: why the shared extraction is not step one

The spec says the embedded console is "the same page as automation, plus an owner filter", and prescribes moving the presentation components into `shared/components/{data-tables,knowledge-bases}`. Counted on this branch:

| Tree | Files |
|---|---|
| `pages/automation/datatables` (list) | 44 |
| `pages/automation/datatable` (detail grid) | 95 |
| `pages/automation/knowledge-bases` (list) | 32 |
| `pages/automation/knowledge-base` (detail) | 92 |
| **Total** | **263** |

A 263-file move is pure refactor risk against four working CE pages, and none of it is needed to answer the question the console exists to answer — who owns what. The console needs the **list** surface, not the detail grid.

So this plan builds the console against the generated hooks directly, and leaves the extraction as a named follow-up with its own justification. If the console later grows to need the grid, that is the moment the extraction pays for itself, and it can be done against a working page rather than speculatively.

## Global Constraints

- **Client style, all lint-enforced:** object keys in ascending order (`sort-keys`, not auto-fixable); named imports sorted alphabetically within `{}`; interface names end in `I` or `Props`; `useRef` variables end in `Ref`; Lucide icons imported with the `Icon` suffix; `twMerge` rather than `cn()`.
- **Hook order:** `useState` → `useRef` → store hooks → other custom hooks → derived/`useMemo`/`useCallback` → `useEffect` → `return`.
- **`npm run check` needs an explicit tool timeout of 600000** — it is auto-backgrounded at 120s otherwise.
- **Run `npm install` first** if `node_modules` predates the last rebase; a stale tree looks exactly like a botched rebase.
- **GraphQL enum values are SCREAMING_SNAKE_CASE.**
- **Commit prefixes:** client-side commits read `<ticket> client - <description>`; this branch's regrouping convention puts `---` on the first commit of a group and `-` on the rest.

---

### Task 1: Stop offering the two components in the integration palette

Server-side, one line, independent of everything else in this plan.

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/filter/IntegrationComponentDefinitionFilter.java`
- Test: `.../filter/IntegrationComponentDefinitionFilterTest.java` (create if absent)

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: nothing later tasks depend on.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testDataTableAndKnowledgeBaseAreNotOfferedInIntegrationWorkflows() {
        assertFalse(filter.filter(componentDefinition("dataTable")));
        assertFalse(filter.filter(componentDefinition("knowledgeBase")));
    }

    @Test
    void testTheFilterStillOnlySupportsEmbedded() {
        assertTrue(filter.supports(PlatformType.EMBEDDED));
        assertFalse(filter.supports(PlatformType.AUTOMATION));
    }

    @Test
    void testAnOrdinaryConnectorIsStillOffered() {
        assertTrue(filter.filter(componentDefinition("slack")));
    }
```

- [ ] **Step 2: Run it to verify the first test fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*IntegrationComponentDefinitionFilterTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log
```

- [ ] **Step 3: Add the two names**

```java
    private static final List<String> COMPONENT_NAMES = List.of(
        "apiPlatform", "dataTable", "knowledgeBase", "webhook");
```

Modify the existing list in place. A **second** `ComponentDefinitionFilter` bean for `EMBEDDED` will not work: `ComponentDefinitionServiceImpl` selects with `.filter(f -> f.supports(platformType)).findFirst()`, so an added bean is either ignored or wins by bean ordering.

Record in the commit message what this does and does not do: the filter is **listing-only**. `getComponentDefinition(name, version)` applies `componentVisibilityProviders` but not the platform filter, so a node already placed in an integration workflow still renders and still executes. This hides the two from the palette; it does not retire existing usages. The demo's integration workflows must migrate to the automation bridge separately.

- [ ] **Step 4: Run the test, format, commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/embedded/embedded-configuration
git commit -m "--- Hide data tables and knowledge bases from the integration palette"
```

---

### Task 2: Generate the client hooks

No UI yet: this task ends with typed hooks and a green `npm run check`.

**Files:**
- Modify: `client/codegen.ts`
- Create: `client/src/graphql/embedded/embeddedDataTables.graphql`
- Create: `client/src/graphql/embedded/embeddedKnowledgeBases.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`

**Interfaces:**
- Consumes: the `embeddedDataTables`, `embeddedKnowledgeBases`, `assignEmbeddedDataTableOwner` and `assignEmbeddedKnowledgeBaseOwner` operations added by Plan 4.
- Produces: `useEmbeddedDataTablesQuery`, `useEmbeddedKnowledgeBasesQuery`, `useAssignEmbeddedDataTableOwnerMutation`, `useAssignEmbeddedKnowledgeBaseOwnerMutation`.

- [ ] **Step 1: Add both schema paths to `codegen.ts`**

Alongside the existing automation data-table and knowledge-base entries:

```ts
        '../server/ee/libs/embedded/embedded-data-table-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/embedded/embedded-knowledge-base-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Write the operation documents**

```graphql
query EmbeddedDataTables($environmentId: ID!, $ownerId: ID) {
    embeddedDataTables(environmentId: $environmentId, ownerId: $ownerId) {
        id
        baseName
        description
    }
}

mutation AssignEmbeddedDataTableOwner($input: AssignDataTableOwnerInput!) {
    assignEmbeddedDataTableOwner(input: $input)
}
```

and the knowledge base pair, with `id`, `name`, `description`.

- [ ] **Step 3: Regenerate and check**

```bash
cd client && npx graphql-codegen
```

Then, with an explicit 600000ms tool timeout:

```bash
cd client && npm run check
```

- [ ] **Step 4: Commit operations and generated file separately**

The repo convention keeps them apart so a regeneration diff stays readable.

```bash
git add client/codegen.ts client/src/graphql/embedded
git commit -m "- client - Add the embedded data table and knowledge base operations"
git add client/src/shared/middleware/graphql.ts
git commit -m "- client - Regenerate the GraphQL client"
```

---

### Task 3: The console pages

Two pages under `client/src/ee/pages/embedded/`, each a thin shell over the generated hooks. Model the chrome on `client/src/ee/pages/embedded/connected-users/ConnectedUsers.tsx` — read it first and mirror its `LayoutContainer`/`Header` usage rather than inventing a layout.

**Files:**
- Create: `client/src/ee/pages/embedded/data-tables/EmbeddedDataTables.tsx`
- Create: `client/src/ee/pages/embedded/data-tables/components/EmbeddedDataTableList.tsx`
- Create: `client/src/ee/pages/embedded/data-tables/components/AssignOwnerDialog.tsx`
- Create: the knowledge base equivalents under `.../knowledge-bases/`
- Modify: `client/src/App.tsx` (routes and the embedded nav)
- Test: one `.test.tsx` per page

**Interfaces:**
- Consumes: the four hooks from Task 2, and `useGetConnectedUsersQuery` (or whatever the connected-users page uses — check) to populate the owner filter.
- Produces: routes `/embedded/data-tables` and `/embedded/knowledge-bases`.

- [ ] **Step 1: Read the precedent**

`ConnectedUsers.tsx` and its `components/`. Note how it obtains the environment and how its list rows are laid out — the list row rhythm is shared across all sixteen `*ListItem` components and is documented in CLAUDE.md: first row `min-h-8`, an 8px gap, second row `min-h-7`. Getting it wrong offsets this list against its neighbours and the failure is invisible in the file you edited.

- [ ] **Step 2: Write the failing page test**

```tsx
it('lists every table when no owner is selected', async () => {
    render(<EmbeddedDataTables />);

    expect(await screen.findByText('conversations')).toBeInTheDocument();
});

it('refetches scoped to the owner when one is picked', async () => {
    // assert the query variables carry ownerId
});
```

Mock the generated hooks with `vi.hoisted(() => ({...}))` — `vi.mock` factories hoist above module-scope `const`s, so referencing one directly throws `Cannot access X before initialization`.

- [ ] **Step 3: Build the list page**

An `EnvironmentSelect`, an owner `Select` populated from the connected users, and a list. Each row shows the resource name, its description, and its current owner — "Shared" when unowned, since that is the state the runtime rule treats as everyone's.

- [ ] **Step 4: Build the assign dialog**

A `Select` of connected users plus an explicit "Shared (vendor)" option that sends `ownerId: null`. That option is the whole reason assignment is nullable; a dialog that can only assign and never unassign strands a table with an account forever.

- [ ] **Step 5: Wire the routes and nav**

Add both under the embedded nav. Consecutive items sharing a `group` fold into one labelled `SidebarGroup` at the first item's position — so keep the two adjacent in the nav array, or they render as two separate sections with the same label.

- [ ] **Step 6: Check and commit**

```bash
cd client && npm run check
```

```bash
git add client/src
git commit -m "- client - Add the embedded data table and knowledge base console"
```

---

### Task 4: Deferred — the shared extraction

**Not done in this plan, and this section records why so the decision is not re-litigated from scratch.**

The spec prescribes moving the automation presentation components into `shared/components/{data-tables,knowledge-bases}` and threading a scope union:

```ts
export type DataTableScopeType =
    | {type: 'WORKSPACE'; workspaceId: number}
    | {type: 'EMBEDDED'; ownerId?: number};
```

That remains the right end state, and the spec's reasoning about placement holds: `shared/components/`, not `ee/shared/components/`, because the automation pages are CE and no CE page imports from `@/ee/` anywhere in the client. A scope prop suffices; no registry is needed, because unlike `variablesApi.ts` there is no CE feature gap to fall back over — the components are CE and the generated hooks all land in the shared `graphql.ts`.

What makes it a separate piece of work is the size: 263 files across the four page trees, 187 of them in the two detail pages the console does not use. Doing it here would mean a large refactor of four working CE pages in service of a page that does not yet exist.

**The trigger to do it:** the first time the console needs the detail grid — row inspection, or a per-row owner view. At that point the extraction is paying for a concrete need and can be verified against a working console.

---

## Deliberately not in this plan

- **No row-level owner UI.** The console assigns tables and knowledge bases; rows take their owner from whoever writes them at runtime.
- **No connected-user-facing console.** The audience is the vendor admin, per the spec, and the membership resolver denies connected users these resource types — pinned by a test in Plan 4.
- **No migration of the demo's integration workflows** to the automation bridge. Task 1 hides the components from the palette; existing placed nodes keep rendering and executing, so that migration is independent and safe to do later.
