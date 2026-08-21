# MCP Server card layout — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the MCP Server page so each component and each workflow group renders as a bordered collapsible card (mirroring AI Hub Connectors `ConnectorRow`), with leaf tools/workflows as full-width rows instead of badges — no behavioural change, no per-tool toggle.

**Architecture:** Each component/workflow-group component is refactored to own a `Collapsible` bordered card: a header row (chevron, icon, title + version badge, last-modified, existing dropdown menu) and a `CollapsibleContent` holding the leaf items as rows. The leaf-item components change from `Badge` to a row layout. All existing hooks, dialogs, popovers, and handlers are preserved verbatim. Automation and embedded have parallel copies; both change.

**Tech Stack:** React 19 + TypeScript, shadcn `Collapsible`, Tailwind (twMerge), lucide icons.

## Global Constraints

- Commit format `<ticket> client - <desc>`. Automation tasks (1–2) → ticket **2445**; embedded tasks (3–4) → ticket **2446**. Each task is its own commit. Footer on every commit:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- FRESH commits only (never `--amend`; the user commits in parallel on `0_732`). Stage only the files the task changes.
- **No behavioural change:** preserve every existing handler, dialog, popover, hook, prop, and action. The ONLY changes are markup/className. Do NOT add a per-tool toggle.
- Match these exact AI Hub `ConnectorRow` classes:
  - Card: `Collapsible` with `group rounded-md border border-border`.
  - Header row: `flex items-center gap-2.5 px-3 py-2.5`.
  - Chevron button: `shrink-0 text-muted-foreground hover:text-foreground`; `ChevronDownIcon`/`ChevronRightIcon` `className="size-4"`.
  - Icon: `size-6 shrink-0` (component/workflow icon).
  - Title block: `flex min-w-0 flex-1 flex-col`; title `truncate text-sm font-medium`; description `truncate text-xs text-muted-foreground`.
  - Expanded section: `flex flex-col gap-1 border-t border-border px-3 py-2 pl-10`.
  - Leaf row: `flex items-center gap-2 py-0.5`.
- Client conventions: object keys / named imports / JSX props alphabetical; interface names end `I`/`Props`; lucide icons with `Icon` suffix; `twMerge` not `cn()`; no short/cryptic names.
- Cards default to **expanded** (`useState(true)`) so tools/workflows stay visible as they are today (current lists render leaf items inline). Collapsing is the new affordance.
- Verify per task with `npx tsc --noEmit` (no NEW errors from changed files) and `npx eslint <changed files>` (clean). Global `npm run check` is currently red on PRE-EXISTING unrelated `AiHubConnectors.tsx` lint errors — ignore those. This work is presentational; if a changed file has a `*.test.tsx`, update it and keep it green.

---

## File structure

**Task 1 (2445) — automation component cards:**
- Modify `client/src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem.tsx` (becomes the card)
- Modify `.../mcp-component-list/McpComponentToolList.tsx` (rows container)
- Modify `.../mcp-component-list/McpComponentToolListItem.tsx` (badge → row)
- Modify `.../mcp-component-list/McpComponentList.tsx` (stack cards; drop the separate tool-list sibling)

**Task 2 (2445) — automation workflow cards:**
- Modify `.../mcp-project-workflow-list/McpProjectListItem.tsx` (card)
- Modify `.../mcp-project-workflow-list/McpProjectWorkflowList.tsx` (rows container)
- Modify `.../mcp-project-workflow-list/McpProjectWorkflowListItem.tsx` (badge → row)
- Modify `.../mcp-project-workflow-list/McpProjectList.tsx` (stack cards; drop the `pl-6` sibling wrapper)

**Task 3 (2446) — embedded component cards:** the embedded copies under
`client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/` (same four files).

**Task 4 (2446) — embedded workflow cards:**
`client/src/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/`
(`McpIntegrationInstanceConfigurationWorkflowList.tsx` + `…ListItem.tsx`) and the embedded project/group
list + item that host them (discover exact names while implementing).

---

## Task 1: Automation component cards

**Files:** the four `mcp-component-list/` files above. Work from `/Volumes/Data/bytechef/bytechef/client`.

**Interfaces:**
- `McpComponentListItem` keeps props `{mcpComponent: McpComponent; mcpServer: McpServer}` and now ALSO renders the tools inside its card (using `mcpComponent.componentName/componentVersion/connectionId/mcpTools/id`), so `McpComponentList` no longer renders `McpComponentToolList` as a sibling.
- `McpComponentToolListItem` keeps its props `{componentName, componentVersion, connectionId, mcpTool}`.

- [ ] **Step 1: Convert the tool item from badge to row** — `McpComponentToolListItem.tsx`

Replace the `<Badge ...>` (the `PopoverAnchor` child) with a row `<div>`, keeping the Popover, the `useMcpActivePopover`/delete hook, the Configure and Delete buttons, and the `McpComponentToolPropertiesPopover`/`DeleteAlertDialog` exactly as-is. New anchor markup:

```tsx
<PopoverAnchor asChild>
    <div className="flex items-center gap-2 py-0.5">
        <div className="flex min-w-0 flex-1 flex-col">
            <span className="truncate text-sm font-medium">{mcpTool.title || mcpTool.name}</span>

            {mcpTool.description && (
                <span className="truncate text-xs text-muted-foreground">{mcpTool.description}</span>
            )}
        </div>

        <Button
            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
            icon={<BoltIcon className="size-4" />}
            onClick={() => openPopover(popoverId)}
            size="iconSm"
            title="Configure"
            variant="ghost"
        />

        <Button
            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            icon={<XIcon className="size-4" />}
            onClick={() => setShowDeleteDialog(true)}
            size="iconSm"
            title="Delete"
            variant="ghost"
        />
    </div>
</PopoverAnchor>
```

`mcpTool.description` may not exist on the type — if TypeScript flags it, drop the description block (tools may not carry a description here). Remove the now-unused `Badge` import.

- [ ] **Step 2: Make the tool list the rows container** — `McpComponentToolList.tsx`

Change the populated branch's wrapper from `flex flex-wrap gap-2 py-2 pl-6` to the card's expanded-section layout, and keep the empty-state branch (Edit Tools) unchanged except for indentation:

```tsx
return tools.length > 0 ? (
    <div className="flex flex-col gap-1">
        {tools.map((tool) => (
            <McpComponentToolListItem
                componentName={componentName}
                componentVersion={componentVersion}
                connectionId={connectionId}
                key={tool.name}
                mcpTool={tool}
            />
        ))}
    </div>
) : (
    // ...unchanged empty-state block (drop the pl-6 if present)...
);
```

(The `border-t ... px-3 py-2 pl-10` wrapper lives in the card from Step 3, so this container is just the inner stack.)

- [ ] **Step 3: Turn the component item into a collapsible card** — `McpComponentListItem.tsx`

Wrap the whole thing in a `Collapsible` card. Add imports: `Collapsible, CollapsibleContent, CollapsibleTrigger` from `@/components/ui/collapsible`; `ChevronDownIcon, ChevronRightIcon` from `lucide-react`; `useState` from `react`; and `McpComponentToolList` from `./McpComponentToolList`. Add local `const [expanded, setExpanded] = useState(true);`. New return:

```tsx
return (
    <Collapsible className="group rounded-md border border-border" onOpenChange={setExpanded} open={expanded}>
        <div className="flex items-center gap-2.5 px-3 py-2.5">
            <CollapsibleTrigger asChild>
                <button
                    aria-label={expanded ? 'Hide tools' : 'Show tools'}
                    className="shrink-0 text-muted-foreground hover:text-foreground"
                    type="button"
                >
                    {expanded ? <ChevronDownIcon className="size-4" /> : <ChevronRightIcon className="size-4" />}
                </button>
            </CollapsibleTrigger>

            {componentDefinition?.icon ? (
                <InlineSVG className="size-6 shrink-0" src={componentDefinition.icon} />
            ) : (
                <ComponentIcon className="size-6 shrink-0 text-content-neutral-secondary" />
            )}

            <button
                className="flex min-w-0 flex-1 cursor-pointer items-center gap-2 text-left"
                onClick={() => setShowEditDialog(true)}
                type="button"
            >
                <span className="truncate text-sm font-medium">
                    {mcpComponent.title || mcpComponent.componentName}
                </span>
            </button>

            <Tooltip>
                <TooltipTrigger asChild>
                    <Badge
                        label={`v${mcpComponent.componentVersion}`}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </TooltipTrigger>

                <TooltipContent>Component Version</TooltipContent>
            </Tooltip>

            <Tooltip>
                <TooltipTrigger className="flex items-center text-xs text-content-neutral-secondary">
                    {mcpComponent.lastModifiedDate
                        ? `Modified at ${new Date(mcpComponent.lastModifiedDate).toLocaleDateString()} ${new Date(mcpComponent.lastModifiedDate).toLocaleTimeString()}`
                        : '-'}
                </TooltipTrigger>

                <TooltipContent>Last Updated Date</TooltipContent>
            </Tooltip>

            <McpComponentListItemDropdownMenu
                mcpComponent={mcpComponent}
                onEditClick={() => setShowEditDialog(true)}
            />
        </div>

        <CollapsibleContent>
            <div className="border-t border-border px-3 py-2 pl-10">
                <McpComponentToolList
                    componentName={mcpComponent.componentName}
                    componentVersion={mcpComponent.componentVersion}
                    connectionId={mcpComponent.connectionId}
                    mcpComponent={mcpComponent}
                    mcpServerId={mcpServer.id!}
                    mcpTools={mcpComponent.mcpTools}
                />
            </div>
        </CollapsibleContent>

        <McpComponentDialog
            mcpComponent={mcpComponent}
            mcpServerId={mcpServer.id}
            onOpenChange={setShowEditDialog}
            open={showEditDialog}
        />
    </Collapsible>
);
```

Keep the existing `useMcpComponentListItem` hook usage and the `McpComponentDialog`. The component icon now uses `size-6` (was `size-4`). `ComponentIcon` and `InlineSVG` imports stay.

- [ ] **Step 4: Stack cards; drop the sibling tool list** — `McpComponentList.tsx`

In the populated branch, render only `McpComponentListItem` per component (it now renders its own tools), wrapped in a vertical stack. Replace the `<div className="py-1 pl-4">` + `Fragment`(item + toolList) structure with:

```tsx
<div className="flex flex-col gap-1.5 py-2">
    {sortedComponents.map((mcpComponent) => (
        <McpComponentListItem key={mcpComponent!.id} mcpComponent={mcpComponent!} mcpServer={mcpServer} />
    ))}
</div>
```

Remove the now-unused `McpComponentToolList` and `Fragment` imports. Keep the loading skeleton and the empty-state branch unchanged.

- [ ] **Step 5: Verify**

Run: `npx tsc --noEmit` (no new errors from these files) and `npx eslint src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentList.tsx src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem.tsx src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolList.tsx src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx` (clean).
Manual visual QA (controller/user): the automation MCP server's Tools tab shows each component as a bordered card with a chevron; tools render as rows (not badges) with Configure + Delete on the right; expand/collapse works; Configure popover and Delete still work; no toggle.

- [ ] **Step 6: Commit**

```bash
git add src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentList.tsx \
        src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem.tsx \
        src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolList.tsx \
        src/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx
git commit -m "2445 client - Render MCP components as cards with tool rows

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Automation workflow cards

**Files:** the four `mcp-project-workflow-list/` files. Same card pattern as Task 1, applied to projects/workflows.

- [ ] **Step 1: Convert the workflow item from badge to row** — `McpProjectWorkflowListItem.tsx`

Replace the `<Badge className="max-w-96 ...">` (the `PopoverAnchor` child) with a row `<div>`, keeping the Popover, `useMcpProjectWorkflowBadge` hook, all three buttons (Configure / Edit / Delete), `McpProjectWorkflowPropertiesPopover`, `DeleteAlertDialog`, and `ProjectDeploymentEditWorkflowDialog` exactly as-is:

```tsx
<PopoverAnchor asChild>
    <div className="flex items-center gap-2 py-0.5">
        <span className="min-w-0 flex-1 truncate text-sm font-medium">{workflowLabel}</span>

        <Button
            aria-label="Configure"
            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
            icon={<BoltIcon className="size-4" />}
            onClick={() => openPopover(popoverId)}
            size="iconSm"
            title="Configure"
            variant="ghost"
        />

        <Button
            aria-label="Edit"
            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
            icon={<PencilIcon className="size-4" />}
            onClick={() => setShowEditWorkflowDialog(true)}
            size="iconSm"
            title="Edit"
            variant="ghost"
        />

        <Button
            aria-label="Delete"
            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
            icon={<XIcon className="size-4" />}
            onClick={() => setShowDeleteDialog(true)}
            size="iconSm"
            title="Delete"
            variant="ghost"
        />
    </div>
</PopoverAnchor>
```

Remove the unused `Badge` import.

- [ ] **Step 2: Make the workflow list the rows container** — `McpProjectWorkflowList.tsx`

Change the wrapper from `flex flex-wrap gap-2 py-2` to `flex flex-col gap-1`:

```tsx
return (
    <div className="flex flex-col gap-1">
        {workflows.map((workflow) => (
            <McpProjectWorkflowListItem key={workflow.id} mcpProjectWorkflow={workflow} />
        ))}
    </div>
);
```

- [ ] **Step 3: Turn the project item into a collapsible card** — `McpProjectListItem.tsx`

Wrap in a `Collapsible` card mirroring Task 1 Step 3. Add imports (`Collapsible`/`CollapsibleContent`/`CollapsibleTrigger`, `ChevronDownIcon`/`ChevronRightIcon`, `useState`, and `McpProjectWorkflowList` from `./McpProjectWorkflowList`). Add `const [expanded, setExpanded] = useState(true);`. The card header keeps the `WorkflowIcon` (now `size-6`), the title (`mcpProject.project?.name || \`Project ${mcpProject.projectDeploymentId}\``), the version badge (when `mcpProject.projectVersion`), last-modified, and `McpProjectListItemDropdownMenu` (unchanged props). The `CollapsibleContent` wraps:

```tsx
<CollapsibleContent>
    <div className="border-t border-border px-3 py-2 pl-10">
        <McpProjectWorkflowList mcpProjectWorkflows={mcpProject.mcpProjectWorkflows} />
    </div>
</CollapsibleContent>
```

Keep both existing dialogs (`McpProjectWorkflowDialog`, `ProjectDeploymentDialog`) and the `useMcpProjectListItem` hook. Use the same header markup shape as Task 1 Step 3 (chevron `CollapsibleTrigger`, `size-6` icon, title `truncate text-sm font-medium` in a clickable button if the current item is clickable — the current project item is NOT clickable to open a dialog, so render the title as a plain `<span className="min-w-0 flex-1 truncate text-sm font-medium">`).

- [ ] **Step 4: Stack cards; drop the `pl-6` sibling** — `McpProjectList.tsx`

`McpProjectListItem` now renders its own workflows, so render only the item per project in a stack:

```tsx
<div className="flex flex-col gap-1.5 py-2">
    {mcpProjects.map((mcpProject) => (
        <McpProjectListItem key={mcpProject.id} mcpProject={mcpProject} />
    ))}
</div>
```

But `McpProjectListItem` does not currently receive the workflows separately — it reads `mcpProject.mcpProjectWorkflows` (already on `mcpProject`), so no new prop is needed. Remove the now-unused `McpProjectWorkflowList` and `Fragment` imports from `McpProjectList.tsx`. Keep the loading skeleton and empty-state branch unchanged.

- [ ] **Step 5: Verify** — `npx tsc --noEmit` clean for these files; `npx eslint` clean on the four `mcp-project-workflow-list/` files. Manual QA: projects render as bordered cards; workflows as rows with Configure/Edit/Delete; expand/collapse works; all dialogs still open.

- [ ] **Step 6: Commit**

```bash
git add src/pages/automation/mcp-servers/components/mcp-project-workflow-list/McpProjectList.tsx \
        src/pages/automation/mcp-servers/components/mcp-project-workflow-list/McpProjectListItem.tsx \
        src/pages/automation/mcp-servers/components/mcp-project-workflow-list/McpProjectWorkflowList.tsx \
        src/pages/automation/mcp-servers/components/mcp-project-workflow-list/McpProjectWorkflowListItem.tsx
git commit -m "2445 client - Render MCP workflows as cards with workflow rows

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Embedded component cards (ticket 2446)

**Files:** the embedded copies under `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-list/` — `McpComponentList.tsx`, `McpComponentListItem.tsx`, `McpComponentToolList.tsx`, `McpComponentToolListItem.tsx`.

Apply the SAME transformation as Task 1, using the SAME target JSX (card + tool rows from Task 1 Steps 1–4). Differences to respect:
- Use the embedded files' existing imports, hooks, dialogs, and `McpComponentToolPropertiesPopover` (the embedded versions) — do NOT swap in automation modules.
- The embedded `McpComponentToolList` currently wraps tools in `flex flex-wrap gap-2 py-2` (parent adds `pl-6`); after the change, the `pl-10`/`border-t` lives in the card (`McpComponentListItem`'s `CollapsibleContent`), and `McpComponentToolList` returns `flex flex-col gap-1`.
- Confirm the embedded `McpComponentListItem` props/hook names; preserve them.

- [ ] **Step 1:** Tool item badge → row (embedded `McpComponentToolListItem.tsx`) — same row JSX as Task 1 Step 1, embedded imports.
- [ ] **Step 2:** Tool list → `flex flex-col gap-1` container (embedded `McpComponentToolList.tsx`).
- [ ] **Step 3:** Component item → `Collapsible` card (embedded `McpComponentListItem.tsx`) — same card JSX as Task 1 Step 3, embedded hook/dialog/popover.
- [ ] **Step 4:** Stack cards, drop sibling tool list (embedded `McpComponentList.tsx`) — same as Task 1 Step 4.
- [ ] **Step 5: Verify** — `npx tsc --noEmit` clean for these files; `npx eslint` clean on the four embedded files. Manual QA on the embedded MCP page: same card/row result as automation.
- [ ] **Step 6: Commit**

```bash
git add src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentList.tsx \
        src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentListItem.tsx \
        src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolList.tsx \
        src/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolListItem.tsx
git commit -m "2446 client - Render embedded MCP components as cards with tool rows

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Embedded workflow cards (ticket 2446)

**Files:** under `client/src/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/` — `McpIntegrationInstanceConfigurationWorkflowList.tsx`, `McpIntegrationInstanceConfigurationWorkflowListItem.tsx`, and the embedded list + item that host them (discover exact filenames; mirror automation's `McpProjectList` / `McpProjectListItem` roles).

Apply the SAME transformation as Task 2 (workflow item badge → row; workflow list → `flex flex-col gap-1` container; group item → `Collapsible` card; stack cards). Respect embedded specifics:
- The leaf item `McpIntegrationInstanceConfigurationWorkflowListItem` keeps its existing Configure / Edit / Delete actions and its popover/dialog/hook (the embedded integration-instance-configuration variants) — same row JSX as Task 2 Step 1.
- The host group item (integration-instance-configuration list item) keeps its existing version badge, last-modified, and dropdown menu; wrap it in a `Collapsible` card with the `CollapsibleContent` holding the workflow rows container.
- If the embedded structure renders the workflow list as a sibling (like automation's `pl-6` wrapper), fold it into the card's `CollapsibleContent` and remove the sibling wrapper.

- [ ] **Step 1:** Discover the embedded host list + item filenames (`ls` the directory; check `McpServerToolsContent`/equivalent for what renders the workflow list). Read them.
- [ ] **Step 2:** Workflow item badge → row (`McpIntegrationInstanceConfigurationWorkflowListItem.tsx`) — Task 2 Step 1 row JSX, embedded imports.
- [ ] **Step 3:** Workflow list → `flex flex-col gap-1` container (`McpIntegrationInstanceConfigurationWorkflowList.tsx`).
- [ ] **Step 4:** Group item → `Collapsible` card; stack cards in the host list. Preserve all embedded handlers/dialogs.
- [ ] **Step 5: Verify** — `npx tsc --noEmit` clean; `npx eslint` clean on all changed embedded files. Manual QA on the embedded MCP page workflows section.
- [ ] **Step 6: Commit** (stage exactly the embedded files you changed):

```bash
git commit -m "2446 client - Render embedded MCP workflows as cards with workflow rows

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-review notes

- Spec coverage: component cards (auto T1 / embedded T3), workflow cards (auto T2 / embedded T4); no toggle on tools (rows omit any `Switch`); behaviour preserved (all hooks/dialogs/popovers kept). Container spacing handled in each Step 4.
- Type/name consistency: `expanded`/`setExpanded`, `Collapsible`/`CollapsibleContent`/`CollapsibleTrigger`, `ChevronDownIcon`/`ChevronRightIcon`, and the card/row class strings are identical across all four tasks.
- Risk: `mcpTool.description` / icon availability may differ by type — each task's Step notes the fallback (drop the block if the field is absent). Embedded Task 4 requires a discovery step because the host list/item filenames weren't enumerated here.
