# Shared Data Table and Knowledge Base Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the automation and embedded surfaces render the same data table and knowledge base list components, parameterized by a scope, instead of the console carrying a thin duplicate.

**Architecture:** The blocker is not file location, it is that the two GraphQL types differ. `DataTable` carries `columns` and `lastModifiedDate`; `EmbeddedDataTable` carries `ownerId` and neither. So the payloads are aligned first (the data is already in `DataTableInfo`; it simply was not exposed), then the presentation components move to `shared/components/`, then a scope union replaces the workspace store read inside the data hook. Automation keeps its tags; embedded has none, and the hook returns an empty tag set rather than pretending otherwise.

**Tech Stack:** React 19, TypeScript 6, TanStack Query, GraphQL Code Generator, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`, "Admin UI and shared extraction".

**Predecessors:** Plans 1–5. Plan 5 Task 4 deferred this and named the trigger; this plan is that work brought forward.

## Global Constraints

- **`shared/components/`, not `ee/shared/components/`.** The automation pages are CE, and no CE page imports from `@/ee/` anywhere in the client. The variables precedent sits under `ee/shared` only because both its consumers are EE.
- **No registry.** `variablesApi.ts` uses register/get because CE needs a no-op fallback for a feature it lacks; that constraint is absent here — the components are CE and every generated hook lands in the shared `graphql.ts`. A scope prop suffices.
- **Hooks cannot be called conditionally.** The scope branch selects between two queries by `enabled`, never by an `if` around a hook.
- **Client style is lint-enforced:** ascending object keys, alphabetically sorted named imports and JSX props, interfaces ending `I`/`Props`, `Icon`-suffixed Lucide imports, `twMerge` over `cn()`.
- **`npm run check` needs an explicit 600000ms tool timeout.**
- **Commit prefixes:** `<ticket> client - <description>` for client commits; `---` opens a group, `-` continues.

## The scope union

```ts
export type DataTableScopeType =
    | {type: 'WORKSPACE'; workspaceId: number}
    | {type: 'EMBEDDED'; ownerId?: number};
```

`ownerId` absent means "all owners" — the vendor's console-wide view. The same shape, renamed, covers knowledge bases.

---

### Task 1: Align the embedded payloads with the automation ones

Until the two types agree, no component can render both.

**Files:**
- Modify: `server/ee/libs/embedded/embedded-data-table-graphql/.../EmbeddedDataTableGraphQlController.java` and its `.graphqls`
- Modify: `server/ee/libs/embedded/embedded-knowledge-base-graphql/.../EmbeddedKnowledgeBaseGraphQlController.java` and its `.graphqls`
- Modify: `client/src/graphql/embedded/configuration/*.graphql`, then regenerate

**Interfaces:**
- Produces: `EmbeddedDataTable` with `columns` and `lastModifiedDate` alongside `ownerId`; `EmbeddedKnowledgeBase` with `createdDate` and `lastModifiedDate`.

- [ ] **Step 1: Add the fields server-side**

`DataTableInfo` already carries `columns()` and `lastModifiedDate()` — the embedded controller was simply not mapping them. Mirror `DataTableGraphQlController`'s `DataTableColumn` record, including its base64 column id, so the two payloads are interchangeable rather than merely similar.

- [ ] **Step 2: Extend the schemas and operations, regenerate**

```bash
cd client && npx graphql-codegen
```

- [ ] **Step 3: Compile and check**

```bash
./gradlew compileJava compileTestJava --continue
cd client && npm run check
```

- [ ] **Step 4: Commit**

```bash
git commit -m "--- Return the same data table shape from the embedded query"
```

---

### Task 2: Move the data table list components to shared

Mechanical, but do it with `git mv` so the history survives the rename, then rewrite the import paths.

**Files:**
- Move: all of `client/src/pages/automation/datatables/components/` and `stores/` to `client/src/shared/components/data-tables/`
- Modify: `client/src/pages/automation/datatables/DataTables.tsx` (imports only, for now)

**Interfaces:**
- Consumes: nothing new.
- Produces: `@/shared/components/data-tables/*` as the import path for every moved file.

- [ ] **Step 1: Move with git mv**

```bash
mkdir -p client/src/shared/components/data-tables
git mv client/src/pages/automation/datatables/components client/src/shared/components/data-tables/components
git mv client/src/pages/automation/datatables/stores client/src/shared/components/data-tables/stores
```

- [ ] **Step 2: Rewrite every import of the old path**

```bash
grep -rl "@/pages/automation/datatables/" client/src | xargs sed -i '' 's|@/pages/automation/datatables/|@/shared/components/data-tables/|g'
```

Then put `DataTables.tsx`'s own self-reference back — it stays a page, so any import OF it (routes, tests) must keep pointing at the page path.

- [ ] **Step 3: Check**

```bash
cd client && npm run check
```

Expect this to be the step that surfaces stragglers. The check is the verification; there is no separate test to write, because every moved test moved with its subject.

- [ ] **Step 4: Commit**

```bash
git commit -m "- client - Move the data table list components to shared"
```

---

### Task 3: Give the data hook a scope

**Files:**
- Create: `client/src/shared/components/data-tables/types.ts`
- Modify: `client/src/shared/components/data-tables/components/hooks/useDataTables.ts`
- Modify: `client/src/pages/automation/datatables/DataTables.tsx`
- Test: `client/src/shared/components/data-tables/components/hooks/tests/useDataTables.test.tsx` (moved in Task 2; extend it)

**Interfaces:**
- Produces: `useDataTables(scope: DataTableScopeType)`.

- [ ] **Step 1: Write the failing tests**

```tsx
it('reads the workspace query under a workspace scope', () => {
    renderHook(() => useDataTables({type: 'WORKSPACE', workspaceId: 1049}));

    expect(useDataTablesQueryMock).toHaveBeenCalledWith(
        expect.objectContaining({workspaceId: '1049'}),
        expect.objectContaining({enabled: true})
    );
    expect(useEmbeddedDataTablesQueryMock).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({enabled: false})
    );
});

it('returns no tags under an embedded scope, rather than the workspace ones', () => {
    const {result} = renderHook(() => useDataTables({type: 'EMBEDDED'}));

    expect(result.current.allTags).toEqual([]);
});
```

The second test is the one that matters: tags are workspace-scoped, and quietly returning a workspace's tags to the console would be a cross-scope leak in the UI.

- [ ] **Step 2: Branch the hook on scope**

Both queries are always called, each with `enabled` set from the scope — hooks cannot be called conditionally. The workspace store read moves out of the hook and into the automation page, which is what makes the hook usable from `/embedded`.

- [ ] **Step 3: Pass the scope from the automation page**

```tsx
const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

const {...} = useDataTables({type: 'WORKSPACE', workspaceId});
```

- [ ] **Step 4: Check and commit**

```bash
git commit -m "- client - Scope the data table list hook"
```

---

### Task 4: The same two steps for knowledge bases

Repeat Tasks 2 and 3 against `client/src/pages/automation/knowledge-bases/`, moving to `client/src/shared/components/knowledge-bases/`. Written out separately rather than by reference, because the two trees are read separately and the knowledge base one has no tag surface to preserve.

- [ ] **Step 1: `git mv` the components and stores**
- [ ] **Step 2: Rewrite the import paths**
- [ ] **Step 3: Add the scope union and branch the hook**
- [ ] **Step 4: `npm run check`, commit**

---

### Task 5: Put the console on the shared components

**Files:**
- Modify: `client/src/ee/pages/embedded/data-tables/EmbeddedDataTables.tsx`
- Modify: `client/src/ee/pages/embedded/knowledge-bases/EmbeddedKnowledgeBases.tsx`
- Delete: the two thin list components Plan 5 wrote, once nothing imports them

**Interfaces:**
- Consumes: the shared components and scoped hooks from Tasks 2–4.

- [ ] **Step 1: Swap the list for the shared one, keeping the owner filter and the row assigner**
- [ ] **Step 2: Delete the duplicates**

Verify nothing still imports them before deleting, rather than assuming:

```bash
grep -rn "EmbeddedDataTableList\|EmbeddedKnowledgeBaseList" client/src
```

- [ ] **Step 3: Check and commit**

```bash
git commit -m "- client - Put the embedded console on the shared components"
```

---

## Deliberately not in this plan

- **The detail trees stay put.** `pages/automation/datatable` (95 files) and `pages/automation/knowledge-base` (92) are the grid and the document viewer. The console lists and assigns; it does not open a table. Moving them would be the same speculative refactor this plan exists to avoid, one level down.
- **`Embeddable*` is not collapsed.** `EmbeddableDataTable` and `EmbeddableKnowledgeBase` belong to the detail trees, so they follow those, not this.
- **No tag surface for embedded.** Tags are workspace-scoped; giving the console its own would be a feature, not an extraction.
