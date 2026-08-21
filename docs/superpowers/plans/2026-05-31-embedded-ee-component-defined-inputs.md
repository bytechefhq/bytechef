# Component-defined Inputs in the EE Integration Instance Configuration Dialog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the EE embedded Integration Instance Configuration dialog render component-defined workflow inputs (single component properties and compound property groups, with dynamic connection-backed options where reachable) by adopting the shared `InputConfigurationList` already built for automation.

**Architecture:** Replace the EE dialog's inline 7-type input converter with the shared `InputConfigurationList` (which already resolves component references, fetches component definitions, renders groups as nested-object compound inputs, sources dynamic options from a backing node's connection, and degrades to free-text / disabled placeholder). Surface the 4 component-reference fields (`componentName`, `componentVersion`, `propertyName`, `groupName`) onto the TS `WorkflowInput` model the EE path uses via a client augmentation (runtime JSON already carries them; only TS types are missing).

**Tech Stack:** React 19 / TypeScript, react-hook-form, TanStack Query, Vitest. EE client under `client/src/ee/`.

**Related spec:** `docs/superpowers/specs/2026-05-31-embedded-ee-component-defined-inputs-design.md`

---

## Ground Truth (verified during planning)

- **Automation reference (the template to mirror):**
  `client/src/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem.tsx`
  imports `InputConfigurationList` from `@/shared/components/InputConfigurationList` and renders it (`:159-169`)
  with props: `componentConnections`, `configuredConnectionIds`, `control` (cast `as unknown as Control<FieldValues>`),
  `controlPath`, `formState` (cast `as unknown as FormState<FieldValues>`), `inputs`, `workflowId`. It derives
  `configuredConnectionIds` (`:65-67`) as:
  ```ts
  const configuredConnectionIds = componentConnections.map(
      (_componentConnection, connectionIndex) => watchedConnections?.[connectionIndex]?.connectionId
  );
  ```
  where `watchedConnections = useWatch({control, name: \`projectDeploymentWorkflows.${workflowIndex}.connections\`})`.
- **EE dialog files:**
  - `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx`
    — computes `componentConnections` by walking `workflow.tasks`/`workflow.triggers` connections, excluding the
    integration's own `componentName` (`:37-40`); renders the inline
    `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs` (`:69-74`) and a separate connections
    sub-component. Its `workflow` prop is typed `Workflow` from `@/shared/middleware/automation/configuration`.
  - `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx`
    — the inline converter to DELETE (maps 7 primitive types → `PropertyAllType`, renders shared `Properties`,
    binds `integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`).
  - Connections bind to
    `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections.${connectionIndex}.connectionId`
    (positional, aligned to `componentConnections`) — confirmed in
    `IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection.tsx:24`.
- **Shared `InputConfigurationList`** (`client/src/shared/components/InputConfigurationList.tsx`) props
  (`:31-42`): `componentConnections?`, `configuredConnectionIds?`, `control: Control<FieldValues>`,
  `controlPath: string`, `duplicateSubflowStubs?`, `formState: FormState<FieldValues>`, `inputs?: WorkflowInput[]`,
  `onOpenInputs?`, `subflowLabelMap?`, `workflowId?`. It imports `WorkflowInput` from
  `@/shared/middleware/platform/configuration`. Exports `resolveComponentInputProperty` /
  `resolveComponentInputGroup` (already unit-tested in `InputConfigurationList.test.tsx`).
- **Type gap (the reason an augmentation is needed):** the EE step item's `workflow` is the **automation**
  `Workflow`, whose `inputs` are **automation** `WorkflowInput`
  (`client/src/shared/middleware/automation/configuration/models/WorkflowInput.ts`) — only 4 fields
  (`label,name,required,type`). The **platform** `WorkflowInput` already has the 8 fields (generated). The
  **embedded** `WorkflowInput` (`client/src/ee/shared/middleware/embedded/configuration/models/WorkflowInput.ts`)
  also has only 4 fields. Runtime JSON carries the ref fields (atlas `Workflow.Input` parser + platform REST
  schema), so only the TS types are missing.
- **Existing augmentation pattern to mirror:** `client/src/shared/middleware/workflowInput.augmentation.d.ts`
  uses `declare module './platform/configuration/models/WorkflowInput'` to add subflow fields to the platform
  model. Same mechanism works per-model.
- **Codegen is not a reliable path here:** `client/src/shared/middleware/platform/configuration/openapi.yaml`
  is a **0-byte** build-time artifact; the embedded internal spec `$ref`s it for the `Workflow` schema. So do
  NOT depend on regenerating TS models to surface the fields — use augmentation (deterministic, no build).
- **Server:** input value maps (`IntegrationInstanceConfigurationWorkflow.inputs` = `MapWrapper`/`Map<String,?>`)
  already accept nested objects; embedded facade input validation is commented out (no nested-value crash).
  No server change expected for sub-project 1.

## File Structure

- **Create:** `client/src/ee/shared/middleware/embedded/workflowInput.augmentation.d.ts` — augments the TS
  `WorkflowInput` model(s) the EE path uses with the 4 reference fields. One responsibility: type surface only.
- **Modify:** `IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` — render shared
  `InputConfigurationList` instead of the inline converter; derive `configuredConnectionIds`.
- **Delete:** `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx` — folded into shared component.
- **Create:** `IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx` — component test for the new
  rendering (single property + group).

## Verify-First Checklist (do before the tasks; adjust the affected task if reality differs)

1. **Which `WorkflowInput` type does the EE step item's `workflow.inputs` resolve to?** Confirm by checking the
   import of `Workflow` in `IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` (currently
   `@/shared/middleware/automation/configuration`) and that the automation `WorkflowInput` lacks the 4 fields.
   Task 1 augments the **automation** model. If the file is changed to use the embedded or platform `Workflow`,
   augment that model instead (platform already has the fields → augmentation unnecessary in that case).
2. **Dynamic-options reachability:** confirm whether `useGetWorkflowNodeOptionsQuery`
   (`/workflows/{id}/workflow-nodes/{node}/options`, platform `WorkflowNodeOptionApi`) is callable/authorized
   from the embedded EE client for an integration's workflow with a configured connection. If reachable →
   dynamic dropdowns work for free. If not → component inputs degrade to the free-text fallback already in
   `InputConfigurationList` (no code change; note the limitation). This does not block any task.
3. **Server regen needed?** Confirm the embedded internal Java/TS models do not need regeneration for sub-project
   1 (expected: not needed — values are untyped maps; the dialog reads inputs via the automation/platform model).
   If a consumer genuinely needs the embedded internal `WorkflowInput` Java model to carry the fields, that is a
   separate, additive regen — out of scope unless a compile/test fails.

---

## Task 1: Augment the EE `WorkflowInput` TS model with the 4 reference fields

**Files:**
- Create: `client/src/ee/shared/middleware/embedded/workflowInput.augmentation.d.ts`

- [ ] **Step 1: Confirm the target model**

Run: `grep -n "componentName" client/src/shared/middleware/workflowInput.augmentation.d.ts`
Expected: the existing global augmentation already adds the 4 fields to the automation AND platform
`WorkflowInput` models (so the dialog's `workflow.inputs` is already typed). Then:
Run: `grep -nE "componentName|propertyName|groupName" client/src/ee/shared/middleware/embedded/configuration/models/WorkflowInput.ts`
Expected: no matches — the **embedded** model (used by the save payload) is the only gap → augment it.

- [ ] **Step 2: Create the augmentation**

Mirror `client/src/shared/middleware/workflowInput.augmentation.d.ts`. Augment BOTH the automation model (used
by the dialog's `workflow.inputs`) and the embedded model (used by the save payload
`IntegrationInstanceConfigurationWorkflow`), so the whole EE path is typed. Use relative module paths from this
file's location (`client/src/ee/shared/middleware/embedded/`):

```ts
/* eslint-disable */

declare module '@/shared/middleware/automation/configuration/models/WorkflowInput' {
    interface WorkflowInput {
        componentName?: string;
        componentVersion?: number;
        groupName?: string;
        propertyName?: string;
    }
}

declare module './configuration/models/WorkflowInput' {
    interface WorkflowInput {
        componentName?: string;
        componentVersion?: number;
        groupName?: string;
        propertyName?: string;
    }
}
```

> VERIFIED: the existing `client/src/shared/middleware/workflowInput.augmentation.d.ts` uses the `@/...`
> module-specifier form (`declare module '@/shared/middleware/automation/configuration/models/WorkflowInput'`)
> and already augments BOTH the automation and platform models with these exact 4 fields (plus subflow fields).
> So the automation/platform models the dialog reads are ALREADY augmented globally by that file. That means the
> automation `declare module` block in this new file is REDUNDANT — the gap is only the **embedded** model
> (`client/src/ee/shared/middleware/embedded/configuration/models/WorkflowInput.ts`, confirmed 4 fields only),
> which the save payload (`IntegrationInstanceConfigurationWorkflow`) references. Reduce this file to just the
> embedded augmentation using the `@/...` form to match the existing convention:
>
> ```ts
> /* eslint-disable */
>
> declare module '@/shared/middleware/embedded/configuration/models/WorkflowInput' {
>     interface WorkflowInput {
>         componentName?: string;
>         componentVersion?: number;
>         groupName?: string;
>         propertyName?: string;
>     }
> }
>
> export {};
> ```
>
> If typecheck (Step 3) shows the dialog's `workflow.inputs` (automation model) still lacks the fields despite
> the global augmentation, ALSO add the automation `declare module` block — but try embedded-only first.

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: exit 0. (No consumer uses the new fields yet; this only adds optional members.)

- [ ] **Step 4: Commit**

```bash
git add client/src/ee/shared/middleware/embedded/workflowInput.augmentation.d.ts
git commit -m "732 client - Augment EE WorkflowInput model with component-reference fields

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Render the shared `InputConfigurationList` in the EE step item

**Files:**
- Modify: `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx`

- [ ] **Step 1: Add imports**

Add to the import block:
```ts
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
```
(Merge with the existing `react-hook-form` import; it currently imports `Control, FormState, UseFormSetValue`.)
Remove the import of `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs`.

- [ ] **Step 2: Derive `configuredConnectionIds` from the form**

Inside the component, after `componentConnections` is computed (`:37-40`), add:
```ts
    const watchedConnections = useWatch({
        control,
        name: `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections`,
    });

    const configuredConnectionIds = componentConnections.map(
        (_componentConnection, connectionIndex) => watchedConnections?.[connectionIndex]?.connectionId
    );
```

- [ ] **Step 3: Replace the inline inputs renderer with the shared component**

Replace the Inputs `<li>` block (the one rendering
`<IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs .../>`, `:66-75`) with:
```tsx
                    <li className="flex flex-col gap-3">
                        <Label className="text-base font-semibold">Inputs</Label>

                        <InputConfigurationList
                            componentConnections={componentConnections}
                            configuredConnectionIds={configuredConnectionIds}
                            control={control as unknown as Control<FieldValues>}
                            controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`}
                            formState={formState as unknown as FormState<FieldValues>}
                            inputs={workflow.inputs}
                            workflowId={workflow.id}
                        />
                    </li>
```
Keep the existing Connections `<li>` unchanged.

> `workflow.inputs` is `WorkflowInput[]` from the automation model (augmented in Task 1), structurally
> compatible with the platform `WorkflowInput[]` the shared component expects. If the compiler rejects the
> assignment due to the two distinct (but identical) `WorkflowInput` declarations, cast:
> `inputs={workflow.inputs as unknown as WorkflowInput[]}` and import the platform `WorkflowInput` type — but
> try without the cast first.

- [ ] **Step 4: Typecheck**

Run: `cd client && npm run typecheck`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx
git commit -m "732 client - Render shared InputConfigurationList in EE integration instance dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Delete the inline converter

**Files:**
- Delete: `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx`

- [ ] **Step 1: Confirm no other importers**

Run: `grep -rln "IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs" client/src --include=*.ts --include=*.tsx | grep -v "/IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx$"`
Expected: no output (only the file itself, already de-referenced by Task 2).

- [ ] **Step 2: Delete the file**

```bash
git rm client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx
```

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: exit 0 (nothing references the deleted file).

- [ ] **Step 4: Commit**

```bash
git commit -m "732 client - Remove EE inline workflow inputs converter superseded by InputConfigurationList

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Component test for the EE step item

**Files:**
- Create: `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx`

- [ ] **Step 1: Inspect the automation test harness to mirror**

Run: `sed -n '1,60p' client/src/shared/components/InputConfigurationList.test.tsx`
Expected: see how `resolveComponentInputProperty`/`resolveComponentInputGroup` are unit-tested and how
`useGetComponentDefinitionQuery` / query hooks are mocked (CLAUDE.md: use `vi.hoisted` for store/query mocks).
Reuse the same mock style.

- [ ] **Step 2: Write the test (RED)**

Write a test that renders `IntegrationInstanceConfigurationDialogWorkflowsStepItem` inside a react-hook-form
wrapper and a QueryClientProvider, with a workflow whose `inputs` contains (a) a single component-reference
input (`{name:'channel', componentName:'slack', componentVersion:1, propertyName:'channelId'}`) and (b) a group
reference (`{name:'sheet', componentName:'googleSheets', componentVersion:1, groupName:'sheetSelection'}`); mock
`useGetComponentDefinitionQuery` (from `@/shared/queries/platform/componentDefinitions.queries`) via `vi.hoisted`
to return definitions exposing `properties:[{name:'channelId',label:'Channel',type:'STRING',controlType:'SELECT'}]`
and `propertyGroups:[{name:'sheetSelection',label:'Sheet',properties:[{name:'spreadsheetId',label:'Spreadsheet',
type:'STRING',controlType:'SELECT'}]}]`. Assert the single property renders with label "Channel" and the group
renders its collapsible label "Sheet". Set `showWorkflowToggle={false}` so inputs render without toggling.

```tsx
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

const {useGetComponentDefinitionQueryMock} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinition: () => ['componentDefinition']},
    useGetComponentDefinitionQuery: useGetComponentDefinitionQueryMock,
}));

// Mock the node-options query so dynamic SELECTs don't hit the network.
vi.mock('@/shared/queries/platform/workflowNodeOptions.queries', () => ({
    WorkflowNodeOptionKeys: {workflowNodeOptions: ['workflowNodeOptions']},
    useGetWorkflowNodeOptionsQuery: () => ({data: [], isLoading: false}),
}));

import IntegrationInstanceConfigurationDialogWorkflowsStepItem from './IntegrationInstanceConfigurationDialogWorkflowsStepItem';

const componentDefinitionsByName: Record<string, unknown> = {
    googleSheets: {
        name: 'googleSheets',
        properties: [],
        propertyGroups: [
            {
                label: 'Sheet',
                name: 'sheetSelection',
                properties: [
                    {controlType: 'SELECT', label: 'Spreadsheet', name: 'spreadsheetId', type: 'STRING'},
                ],
            },
        ],
        version: 1,
    },
    slack: {
        name: 'slack',
        properties: [{controlType: 'SELECT', label: 'Channel', name: 'channelId', type: 'STRING'}],
        propertyGroups: [],
        version: 1,
    },
};

const Wrapper = () => {
    const {control, formState} = useForm({
        defaultValues: {integrationInstanceConfigurationWorkflows: [{connections: [], inputs: {}}]},
    });

    const workflow = {
        id: 'wf1',
        inputs: [
            {componentName: 'slack', componentVersion: 1, name: 'channel', propertyName: 'channelId'},
            {componentName: 'googleSheets', componentVersion: 1, groupName: 'sheetSelection', name: 'sheet'},
        ],
        tasks: [],
        triggers: [],
    };

    return (
        <IntegrationInstanceConfigurationDialogWorkflowsStepItem
            componentName="someApp"
            control={control as never}
            formState={formState as never}
            label="Workflow"
            setValue={(() => {}) as never}
            workflow={workflow as never}
            workflowIndex={0}
        />
    );
};

describe('IntegrationInstanceConfigurationDialogWorkflowsStepItem', () => {
    it('renders component-defined single property and group inputs', () => {
        useGetComponentDefinitionQueryMock.mockImplementation(({componentName}: {componentName: string}) => ({
            data: componentDefinitionsByName[componentName],
        }));

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        render(
            <QueryClientProvider client={queryClient}>
                <Wrapper />
            </QueryClientProvider>
        );

        expect(screen.getByText('Channel')).toBeInTheDocument();
        expect(screen.getByText('Sheet')).toBeInTheDocument();
    });
});
```

> Adapt mock shapes to the real exports of the two query modules (open each file and match the named exports the
> component imports — `InputConfigurationList` uses `useQueries` with `ComponentDefinitionApi`/`ComponentDefinitionKeys`
> AND `useGetWorkflowNodeOptionsQuery`; mock whatever it actually calls so no network/`useEnvironmentStore`
> access throws). If `InputConfigurationList` calls `useEnvironmentStore`, the test wrapper must provide it or it
> must be mocked. Inspect `InputConfigurationList.tsx` imports and mock each external dependency it touches at
> render. The RED run will reveal missing mocks; add them until the two assertions are the only failures, then
> make them pass.

- [ ] **Step 3: Run (RED → GREEN)**

Run: `cd client && npm run test -- IntegrationInstanceConfigurationDialogWorkflowsStepItem`
Expected: initially failing while you wire mocks; then PASS with both assertions green.

- [ ] **Step 4: Commit**

```bash
git add client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx
git commit -m "732 client - Test component-defined inputs render in EE integration instance dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Full client check**

Run: `cd client && npm run check`
Expected: exit 0 (prettier + eslint `--max-warnings=0` + tsc + vitest all pass). Fix any sort-keys / interface
naming (`I`/`Props`) / import-order issues manually (ESLint `--fix` does NOT fix sort-keys).

- [ ] **Step 2: Dynamic-options reachability note (verify-first #2)**

Manually confirm whether the embedded EE dialog can load live options for a component-reference input whose
backing node has a configured connection (open the dialog for an integration that uses a component with a
connection-backed dynamic property; observe whether the dropdown populates). Record the outcome:
- reachable → dynamic options work; no further action.
- not reachable → component inputs show the free-text fallback; note that an embedded options route is deferred
  to sub-project 2. Either way, no code change in this plan.

- [ ] **Step 3: Server regen sanity (verify-first #3)**

Run: `grep -rn "componentName" client/src/ee/shared/middleware/embedded/configuration/models/WorkflowInput.ts || echo "embedded generated model unchanged (expected; augmentation supplies types)"`
Expected: the generated embedded model is unchanged; the augmentation supplies the types. No server module
needs recompilation for this sub-project (values are untyped maps). If `npm run check` and the manual check both
pass, sub-project 1 is complete.

---

## Testing Strategy

- **Reused unit tests:** `resolveComponentInputProperty` / `resolveComponentInputGroup` are already covered in
  `client/src/shared/components/InputConfigurationList.test.tsx` — no duplication.
- **New component test:** Task 4 verifies the EE step item renders a single component property and a compound
  group via the shared component.
- **Full gate:** `npm run check` (Task 5).
- **Manual:** dynamic-options reachability in the embedded context (Task 5 Step 2).

## Rollback Plan

All changes are additive and isolated to the EE client. Reverting Task 2's commit restores the inline converter;
re-adding the deleted file (Task 3) and removing the augmentation (Task 1) fully backs out. No server changes, no
DB migrations (inputs are a JSON map). The shared `InputConfigurationList` is unchanged by this plan, so
automation is unaffected.
