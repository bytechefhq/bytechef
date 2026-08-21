# `internalOnly` Input Flag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a boolean `internalOnly` flag to a workflow input that strictly routes where it renders — `internalOnly=true` → admin `IntegrationInstanceConfigurationDialog`; `internalOnly=false` (default) → end-user `ConnectDialog`.

**Architecture:** The flag lives in the workflow-definition JSON (like the field-mapping `objectName`), parsed into `Workflow.Input`. It reaches the SDK `ConnectDialog` through the embedded `InputModel` (regenerated). On the client it rides the `WorkflowInputType` intersection + a cast in the admin dialog (no platform/automation OpenAPI regen, mirroring `objectName`). Two render filters do the routing.

**Tech Stack:** Java 25 / Spring Boot (atlas + EE embedded), React 19 + TypeScript (embedded SDK + main client), JUnit 5, Vitest, OpenAPI-generated embedded models.

**Spec:** `docs/superpowers/specs/2026-06-05-internal-only-input-flag-design.md`

**~~Deliberate deviation from spec §4.2~~ — REVERTED (the spec was right).** Tasks 1–5 followed a deviation: skip the platform `WorkflowInput` OpenAPI regen and instead read `internalOnly` on the client via the `WorkflowInputType` intersection (editor) + a localized cast (admin dialog), on the theory that this mirrored `objectName`. **The final holistic review caught that this breaks the feature on page reload:** the admin dialog and the Inputs editor both read `workflow.inputs` from the `WorkflowInput` **REST model** on a fresh load (not the definition JSON), and that model dropped `internalOnly` — so after reload the admin dialog filtered out every input. (`objectName` survives only because it's re-derived from `testValue` on save, not round-tripped through the model; `internalOnly` is a pure toggle with no other source.) **Task 6** reverts to the spec's original §4.2 approach: add `internalOnly` to the platform-configuration `WorkflowInput` schema (automation + embedded `$ref` it), regenerate the Java + all three client models, let MapStruct auto-map, and drop the localized cast. See the **Task 6** section appended after Task 5. The lesson: a client REST model is a serialization boundary; a flag that must survive reload has to live in the model, not only in a client-side intersection type.

> **Conventions:** EE files (`server/ee/**`) use the ByteChef Enterprise license header + `@version ee` Javadoc. Run `./gradlew spotlessApply` before server commits; `cd client && npm run check` before main-client commits; the SDK uses `npx vitest`/`npx tsc`/`npx eslint`. Client object keys alphabetical (ESLint `sort-keys`). Java: one blank line before control statements / after variable modification. Commit prefixes: server `732 …`, client/SDK `732 client - …`.

---

## Task 1: Server — `Workflow.Input.internalOnly` + parsing

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java` (the `Input` record ~lines 412-424, and the inputs-parsing block ~lines 164-182)
- Modify: `WorkflowConstants` (add `INTERNAL_ONLY`; also register it in `WORKFLOW_DEFINITION_CONSTANTS` if a reserved-words validator exists)
- Test: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTest.java`

- [ ] **Step 1: Write the failing test** — add to `WorkflowTest.java`:

```java
    @Test
    void testInputParsesInternalOnly() {
        Workflow workflow = new Workflow(
            "1",
            """
            {"inputs": [{"name": "apiKey", "type": "string", "internalOnly": true}]}
            """,
            Workflow.Format.JSON);

        assertTrue(workflow.getInputs()
            .getFirst()
            .internalOnly());
    }

    @Test
    void testInputInternalOnlyDefaultsFalse() {
        Workflow workflow = new Workflow(
            "1", """
            {"inputs": [{"name": "channel", "type": "string"}]}
            """, Workflow.Format.JSON);

        assertFalse(workflow.getInputs()
            .getFirst()
            .internalOnly());
    }
```

Add `import static org.junit.jupiter.api.Assertions.assertTrue;` / `assertFalse;` if missing. (`WorkflowTest` already constructs `new Workflow("1", <json>, Workflow.Format.JSON)` for the `objectName` tests — match that exact constructor.)

- [ ] **Step 2: Run, confirm FAIL**

Run: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.domain.WorkflowTest"`
Expected: FAIL — `internalOnly()` does not exist.

- [ ] **Step 3: Add the constant** — in `WorkflowConstants.java`, add (alphabetically near `INPUTS`/`LABEL`):
```java
    public static final String INTERNAL_ONLY = "internalOnly";
```
If `WORKFLOW_DEFINITION_CONSTANTS` (the reserved-words list `AbstractWorkflowMapper.validateReservedWords` checks) exists, add `INTERNAL_ONLY` to it (the `objectName` work required adding `OBJECT_NAME` there, or parsing rejects the key).

- [ ] **Step 4: Extend the `Input` record** — replace it with (adds `internalOnly` as the last component + a 6-arg convenience constructor so existing 6-arg call sites keep compiling):
```java
    public record Input(
        String name, String label, String type, boolean required,
        ComponentInputReference componentReference, String objectName, boolean internalOnly)
        implements Serializable {

        public Input(String name, String label, String type, boolean required) {
            this(name, label, type, required, null, null, false);
        }

        public Input(
            String name, String label, String type, boolean required, ComponentInputReference componentReference) {

            this(name, label, type, required, componentReference, null, false);
        }

        public Input(
            String name, String label, String type, boolean required, ComponentInputReference componentReference,
            String objectName) {

            this(name, label, type, required, componentReference, objectName, false);
        }
    }
```
In the inputs-parsing block, pass `internalOnly` as the new last argument:
```java
                        return new Input(
                            MapUtils.getRequiredString(map, WorkflowConstants.NAME),
                            MapUtils.getString(map, WorkflowConstants.LABEL),
                            MapUtils.getString(map, WorkflowConstants.TYPE, "string"),
                            MapUtils.getBoolean(map, WorkflowConstants.REQUIRED, false),
                            componentName == null
                                ? null
                                : new ComponentInputReference(
                                    componentName,
                                    MapUtils.getInteger(map, WorkflowConstants.COMPONENT_VERSION),
                                    MapUtils.getString(map, WorkflowConstants.GROUP_NAME)),
                            MapUtils.getString(map, WorkflowConstants.OBJECT_NAME),
                            MapUtils.getBoolean(map, WorkflowConstants.INTERNAL_ONLY, false));
```

- [ ] **Step 5: Run, confirm PASS** (same command as Step 2).

- [ ] **Step 6: Compile the module** — `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:compileJava` → BUILD SUCCESSFUL (the 6-arg convenience constructor preserves all existing call sites).

- [ ] **Step 7: Format + commit**
```bash
./gradlew spotlessApply
git add server/libs/atlas/atlas-configuration server/libs/platform
git commit -m "732 Add internalOnly to workflow Input"
```

---

## Task 2: Server — surface `internalOnly` in the embedded `InputModel` + mapper

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` (the `Input` schema)
- Regenerate: `.../generated/.../model/InputModel.java`
- Modify: `.../src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java` (the `map(Workflow.Input)` method)
- Modify: `.../src/test/java/.../mapper/ConnectedUserIntegrationMapperTest.java` (existing test constructor + a new assertion)

- [ ] **Step 1: Edit the OpenAPI `Input` schema** — add the `internalOnly` property (after `componentReference`, matching the existing indentation):
```yaml
      internalOnly:
        description: "If true, the input is configured in the admin IntegrationInstanceConfigurationDialog; if false (default), it is rendered in the end-user ConnectDialog."
        type: "boolean"
        default: false
```

- [ ] **Step 2: Regenerate the model**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPI`
Expected: BUILD SUCCESSFUL; `generated/.../model/InputModel.java` now has a `@Nullable Boolean internalOnly` field with `getInternalOnly()`/`setInternalOnly(...)` and a fluent `.internalOnly(...)` builder.

- [ ] **Step 3: Update the existing mapper test (constructor arity) + add an internalOnly assertion**

The existing `testFieldMappingInputMapsTypeAndObjectName` constructs `new Workflow.Input("contactMapping", "Contact Mapping", "field_mapping", false, null, "Contacts")` — that 6-arg form still compiles (Task 1 kept a 6-arg convenience constructor), so it needs no change. Add a new test:
```java
    @Test
    void testInputMapsInternalOnly() {
        Workflow.Input input = new Workflow.Input(
            "apiKey", "API Key", "string", false, null, null, true);

        InputModel model = TestMapper.INSTANCE.map(input);

        assertEquals(Boolean.TRUE, model.getInternalOnly());
    }
```
Use the SAME mapper-instance access the existing test uses (the explorer's `ConnectedUserIntegrationMapperTest` instantiates the mapper — mirror its `TestMapper.INSTANCE`/anonymous-impl pattern exactly; the `new Workflow.Input(..., true)` 7-arg constructor is the canonical one).

- [ ] **Step 4: Run, confirm FAIL** — `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test --tests "*ConnectedUserIntegrationMapperTest"` → FAIL (`getInternalOnly()` returns null; mapper doesn't set it).

- [ ] **Step 5: Update the mapper** — in `ConnectedUserIntegrationMapper.map(Workflow.Input)`:
```java
        default InputModel map(Workflow.Input input) {
            return new InputModel()
                .internalOnly(input.internalOnly())
                .label(input.label())
                .name(input.name())
                .objectName(input.objectName())
                .required(input.required())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.type())));
        }
```

- [ ] **Step 6: Run, confirm PASS** (same command as Step 4).

- [ ] **Step 7: Format + commit**
```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest
git commit -m "732 Surface internalOnly in embedded InputModel"
```

---

## Task 3: SDK — `ConnectDialog` renders only `!internalOnly` inputs

**Files:**
- Modify: `sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts` (`WorkflowInputType`)
- Modify: `.../connect-dialog/ConnectDialog.tsx` (the two input `.map` loops)
- Test: `.../connect-dialog/ConnectDialog.internalOnly.test.tsx` (create) OR extend an existing ConnectDialog test

> Work from `sdks/frontend/embedded/library/react`. Verify with `npx vitest`/`npx tsc`/`npx eslint`.

- [ ] **Step 1: Add the type** — in `types.ts`, add to `WorkflowInputType` (keep keys alphabetical):
```ts
    internalOnly?: boolean;
```
(Insert between `defaultValue?` and `name`/`objectName` per the interface's current ordering — alphabetical: `componentReference, defaultValue, internalOnly, label, name, objectName, required, type, value`. Match whatever order keeps ESLint happy; the field just needs to exist.)

- [ ] **Step 2: Write the failing test** (`ConnectDialog.internalOnly.test.tsx`):
```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import ConnectDialog from './ConnectDialog';
import {MergedWorkflowType} from './types';

const mergedWorkflows: MergedWorkflowType[] = [
    {
        enabled: true,
        inputs: [
            {label: 'Channel', name: 'channel', required: false, type: 'string'},
            {internalOnly: true, label: 'API Key', name: 'apiKey', required: false, type: 'string'},
        ],
        label: 'My Workflow',
        workflowUuid: 'wf-1',
    },
];

describe('ConnectDialog internalOnly filtering', () => {
    it('renders non-internal inputs and hides internalOnly inputs', () => {
        render(
            <ConnectDialog
                closeDialog={vi.fn()}
                handleClick={vi.fn()}
                handleWorkflowToggle={vi.fn()}
                handleWorkflowInputChange={vi.fn()}
                isOpen
                mergedWorkflows={mergedWorkflows}
                workflowsView
            />
        );

        expect(screen.getByText('Channel')).toBeInTheDocument();
        expect(screen.queryByText('API Key')).not.toBeInTheDocument();
    });
});
```
> Adjust the `<ConnectDialog .../>` props to the component's actual required props (read `DialogProps` in `ConnectDialog.tsx`; provide the minimal set so it renders the workflows view with the enabled workflow's inputs). The contract: an input with `internalOnly: true` is not rendered; a normal one is.

- [ ] **Step 3: Run, confirm FAIL** — `npx vitest run src/components/connect-dialog/ConnectDialog.internalOnly.test.tsx` → FAIL (the API Key input renders).

- [ ] **Step 4: Add the filter** — in `ConnectDialog.tsx`, in BOTH the regular-workflow loop (~line 288) and the MCP-workflow loop (~line 388), change `inputs?.map(...)` to filter first:
```tsx
                                        {inputs
                                            ?.filter((input: WorkflowInputType) => !input.internalOnly)
                                            .map((input: WorkflowInputType) => (
```
(Leave the `inputs?.length === 0` empty-state check as-is; the filter only changes the `.map` source. Apply the identical change at both loop sites.)

- [ ] **Step 5: Run, confirm PASS** (same command as Step 3). Then run the full connect-dialog suite + typecheck:
`npx vitest run src/components/connect-dialog && npx tsc --noEmit -p tsconfig.app.json && npx eslint src/components/connect-dialog/ConnectDialog.tsx src/components/connect-dialog/types.ts`
Expected: all green.

- [ ] **Step 6: Commit**
```bash
git add sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.tsx sdks/frontend/embedded/library/react/src/components/connect-dialog/ConnectDialog.internalOnly.test.tsx
git commit -m "732 client - Hide internalOnly inputs in embedded ConnectDialog"
```

---

## Task 4: Main client — `Internal only` checkbox in the Inputs editor

**Files:**
- Modify: `client/src/shared/types.ts` (`WorkflowInputType` intersection)
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx` (add the checkbox + `Checkbox` import)
- Test: `.../workflow-inputs/utils/toWorkflowDefinitionInput.test.ts` (create) and `.../WorkflowInputsEditDialog.test.tsx` (extend)

> Work from `client/`. Verify with `npm run check` (lint + typecheck + tests).

- [ ] **Step 1: Add the type** — in `client/src/shared/types.ts`, extend the intersection (keep alphabetical):
```ts
export type WorkflowInputType = WorkflowInput & {
    internalOnly?: boolean;
    objectName?: string;
    testValue?: string;
};
```

- [ ] **Step 2: Write the failing round-trip test** (`utils/toWorkflowDefinitionInput.test.ts`):
```ts
import {describe, expect, it} from 'vitest';

import {toWorkflowDefinitionInput} from './toWorkflowDefinitionInput';

describe('toWorkflowDefinitionInput', () => {
    it('preserves internalOnly and drops testValue', () => {
        const result = toWorkflowDefinitionInput({
            internalOnly: true,
            label: 'API Key',
            name: 'apiKey',
            testValue: 'x',
            type: 'string',
        });

        expect(result.internalOnly).toBe(true);
        expect('testValue' in result).toBe(false);
    });
});
```

- [ ] **Step 3: Run, confirm it PASSES already** — `cd client && npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs/utils/toWorkflowDefinitionInput.test.ts`
Expected: PASS — `toWorkflowDefinitionInput` already spreads `...rest`, so `internalOnly` survives with no code change. (This test locks that behavior so a future refactor can't silently drop it.) If it FAILS because `internalOnly` isn't on the type, Step 1 fixes the type; re-run.

- [ ] **Step 4: Write the failing dialog test** — add to `WorkflowInputsEditDialog.test.tsx`:
```tsx
    it('renders an Internal only checkbox', () => {
        render(<Harness />);

        expect(screen.getByText('Internal only')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: /internal only/i})).toBeInTheDocument();
    });
```
> If the checkbox's accessible name isn't wired to "Internal only" by default, scope the query to the label container per the file's existing test style; the contract is that an "Internal only" checkbox is present.

- [ ] **Step 5: Run, confirm FAIL** — `npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.test.tsx -t "Internal only"` → FAIL (no checkbox).

- [ ] **Step 6: Add the checkbox** — add `import {Checkbox} from '@/components/ui/checkbox';` to `WorkflowInputsEditDialog.tsx`, and add this `FormField` after the `required` FormField block (the `Checkbox` API is `checked` + `onCheckedChange`, per `CheckboxFieldRenderer.tsx`):
```tsx
                        <FormField
                            control={form.control}
                            name="internalOnly"
                            render={({field}) => (
                                <FormItem>
                                    <div className="flex items-center space-x-2">
                                        <FormControl>
                                            <Checkbox
                                                checked={!!field.value}
                                                id="internalOnly"
                                                onCheckedChange={field.onChange}
                                            />
                                        </FormControl>

                                        <FormLabel htmlFor="internalOnly">Internal only</FormLabel>
                                    </div>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
```
(`FormItem`, `FormControl`, `FormLabel`, `FormMessage`, `FormField` are already imported. The `htmlFor`/`id` pair wires the accessible name so the test's `getByRole('checkbox', {name: /internal only/i})` resolves.)

- [ ] **Step 7: Run, confirm PASS** (same command as Step 5). Then run the whole workflow-inputs suite: `npx vitest run src/pages/platform/workflow-editor/components/workflow-inputs`.

- [ ] **Step 8: Check + commit**
```bash
cd client && npm run check
git add src/shared/types.ts src/pages/platform/workflow-editor/components/workflow-inputs
git commit -m "732 client - Add Internal only checkbox to the workflow Inputs editor"
```

---

## Task 5: Main client — admin dialog renders only `internalOnly` inputs

**Files:**
- Modify: `client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` (the `inputs` derivation, ~line 44)
- Test: `.../IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx` (create) OR a focused filter unit test

- [ ] **Step 1: Write the failing test** — create `IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx`, mocking `InputConfigurationList` to capture the `inputs` prop it receives:
```tsx
import {render} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogWorkflowsStepItem from './IntegrationInstanceConfigurationDialogWorkflowsStepItem';

const inputsSpy = vi.fn();

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: (props: {inputs?: Array<{name: string}>}) => {
        inputsSpy(props.inputs);

        return null;
    },
    resolveComponentInputGroup: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowsEnabledStore', () => ({
    default: () => [vi.fn(), new Map([['w1', true]])],
}));

const workflow = {
    id: 'w1',
    inputs: [
        {internalOnly: true, name: 'apiKey', type: 'string'},
        {internalOnly: false, name: 'channel', type: 'string'},
        {name: 'legacy', type: 'string'},
    ],
    tasks: [],
    triggers: [],
} as never;

function Harness() {
    const {control, formState, setValue} = useForm();

    return (
        <IntegrationInstanceConfigurationDialogWorkflowsStepItem
            componentName="slack"
            control={control as never}
            formState={formState as never}
            label="My Workflow"
            setValue={setValue as never}
            workflow={workflow}
            workflowIndex={0}
        />
    );
}

describe('IntegrationInstanceConfigurationDialogWorkflowsStepItem', () => {
    it('passes only internalOnly inputs to InputConfigurationList', () => {
        render(<Harness />);

        const passed = inputsSpy.mock.calls.at(-1)?.[0] as Array<{name: string}>;

        expect(passed.map((input) => input.name)).toEqual(['apiKey']);
    });
});
```
> Match the real mock surface: read the component's imports (the store hook path, `InputConfigurationList`'s export shape — it exports `resolveComponentInputGroup` too) and adjust the `vi.mock` factories accordingly. The contract: with the workflow above, `InputConfigurationList` receives only the `apiKey` input (internalOnly + not the `slack` own-component input). `channel`/`legacy` (internalOnly falsy) are excluded.

- [ ] **Step 2: Run, confirm FAIL** — `cd client && npx vitest run src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx`
Expected: FAIL — currently passes `['apiKey','channel','legacy']` (no internalOnly filter).

- [ ] **Step 3: Add the filter** — change the `inputs` derivation (the `workflow.inputs` are typed as the automation `WorkflowInput`, which has no `internalOnly` in TS, so cast):
```tsx
    // Inputs referencing the integration's own component are configured through the connect flow.
    // internalOnly inputs are configured here (the admin dialog); end-user inputs render in the ConnectDialog.
    const inputs = (workflow.inputs ?? []).filter(
        (input) =>
            input.componentReference?.componentName !== componentName &&
            (input as {internalOnly?: boolean}).internalOnly
    );
```

- [ ] **Step 4: Run, confirm PASS** (same command as Step 2).

- [ ] **Step 5: Check + commit**
```bash
cd client && npm run check
git add src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.test.tsx
git commit -m "732 client - Show only internalOnly inputs in the integration instance config dialog"
```

---

## Task 6: Thread `internalOnly` through the `WorkflowInput` REST model (corrects the §4.2 deviation)

**Why:** The admin dialog and the Inputs editor read `workflow.inputs` from the generated `WorkflowInput` REST model on a fresh page load, not from the definition JSON. Tasks 1–5 never put `internalOnly` on that model, so after reload the admin filter dropped every input. Fix: add `internalOnly` to the platform `WorkflowInput` schema (the single source — `automation` and `embedded` specs `$ref` it) and regenerate.

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml` (the `WorkflowInput` schema — the `automation` and `embedded` specs `$ref` this one).
- Regenerate (committed): the platform server `WorkflowInputModel.java` + the three client `WorkflowInput.ts` models (`shared/middleware/platform/configuration`, `shared/middleware/automation/configuration`, `ee/shared/middleware/embedded/configuration`).
- Modify: `client/src/ee/pages/.../IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` (drop the `(input as {internalOnly?: boolean})` cast — the type now has the field).
- Modify: `client/src/shared/types.ts` (remove the now-redundant `internalOnly?` from the `WorkflowInputType` intersection — the base model provides it).

- [ ] **Step 1:** Add to the platform `WorkflowInput` schema (keep properties alphabetical):
```yaml
        internalOnly:
          description: "If true, the input is configured in the admin integration instance configuration; if false (default), it is rendered in the end-user connect dialog."
          type: "boolean"
          default: false
```
- [ ] **Step 2:** Regenerate all three modules (each `generateOpenAPI` produces Java + its client TS):
```bash
./gradlew \
 :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:generateOpenAPI \
 :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:generateOpenAPI \
 :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:generateOpenAPI
```
- [ ] **Step 3:** Verify `internalOnly` landed in all three client `WorkflowInput.ts` (`FromJSONTyped` parse) and the server `WorkflowInputModel.java`. Compile the server so MapStruct regenerates the mappers; confirm `IntegrationWorkflowDTOToWorkflowModelMapperImpl` now sets `internalOnly` (MapStruct auto-maps same-named properties — no mapper edit needed).
- [ ] **Step 4:** Simplify the admin filter to `input.internalOnly` (drop the cast). Remove the redundant `internalOnly?` from the `WorkflowInputType` intersection.
- [ ] **Step 5:** `cd client && npx tsc --noEmit` + run the workflow-inputs and integration-instance-configurations vitest suites — green.
- [ ] **Step 6:** Commit (server prefix; includes regenerated Java + client models — generated-timestamp churn across the three modules is expected, per repo convention).

## Final verification

- [ ] **Server:** `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:test` — green.
- [ ] **SDK:** from `sdks/frontend/embedded/library/react`, `npx vitest run src/components/connect-dialog` + `npx tsc --noEmit -p tsconfig.app.json` — green.
- [ ] **Client:** `cd client && npm run check` — lint/typecheck/tests green.
- [ ] **End-to-end sanity (manual, requires running stack):** author a workflow with two inputs — one with **Internal only** checked, one unchecked. Confirm the checked input appears only in the admin `IntegrationInstanceConfigurationDialog` and the unchecked one only in the end-user `ConnectDialog`.
- [ ] **Value-flow verification (spec §7):** confirm the workflow-execution path merges `IntegrationInstanceConfiguration` input values with `IntegrationInstance` input values — i.e. an admin-set `internalOnly` value actually reaches the run. If it does NOT, file a follow-up (threading `internalOnly` values into execution is out of scope for this rendering-routing plan). Search the execution facade that builds a workflow job's inputs for a connected user (e.g. `ConnectedUserIntegrationInstance*Facade`).

---

## Notes for the executor

- **No platform/automation OpenAPI regen.** Client `internalOnly` rides the `WorkflowInputType` intersection (Task 4) and a cast in the admin dialog (Task 5) — same approach as the field-mapping `objectName`. Only the embedded `InputModel` is regenerated (Task 2). Do NOT hand-edit `generated/` files; edit `openapi.yaml` + run `generateOpenAPI`.
- **`Workflow.Input` arity (Task 1):** the new 6-arg convenience constructor preserves every existing `new Input(...)` call site (4/5/6-arg); the canonical constructor is now 7-arg. After Task 1 the whole project still compiles.
- **EE headers:** any new EE file (the `internalOnly` mapper test additions live in existing EE files) keeps the Enterprise header + `@version ee`.
