# Configure MCP tools while adding them

Date: 2026-11-11
Scope: client + server, automation and embedded (EE) MCP servers

## Intent

Today an MCP tool is added in one dialog and configured in another. You tick tools in "Select Tools from
<Component>" (or toggle workflows on in "Add Workflows"), save, and then open each tool's gear popover
("Edit <Tool> Tool") to set Tool Name, Tool Description and input values. The user wants that configuration
available **at the moment a tool is selected**, in the same dialog, so adding a tool and setting it up is one
step.

A second, related ask: tools often have many inputs, and the common case for an MCP tool is "let the model fill
everything". Clicking the per-field AI button on each input one by one is tedious, so the form gets a single
**"Let AI fill all"** action.

## Goals

1. **Component tools, add flow.** In "Select Tools from <Component>", ticking a tool expands that tool's settings
   form inline, directly below its row (accordion-style). Unticking collapses it. The form is the same one the
   gear popover shows today: Tool Name, Tool Description, then the tool's input properties, with the same
   display conditions, formula toggles, per-field AI buttons and "Show Advanced Properties".
2. **Component tools, edit flow.** "Edit Tools for <Component>" (the same dialog opened in edit mode from a
   component row's **Edit** menu item or the **Edit Tools** empty-state button) behaves the same way, with each
   ticked tool's form **prefilled from its saved parameters**. Update persists what the forms hold.
3. **Workflow tools.** In "Add Workflows" (and "Edit Workflows"), each toggled-on workflow card gets a third
   **Tool** tab next to Connections and Inputs. It holds the same form as the workflow gear popover
   ("Edit <workflow> Tool"): optional Tool Name, optional Tool Description, and the fields defined by the
   workflow's **New Workflow Call** trigger input schema (e.g. "Message"). These schema fields are the only
   workflow values the AI can set. The **Inputs** tab is the project/integration's workflow inputs: set by the
   user, **never by the AI**, and untouched by this work (no AI buttons, no "Let AI fill all" there).
4. **"Let AI fill all".** A small button (sparkle icon + label) on the right, directly below Tool Description, in
   every place this form appears: the inline component forms, the workflow Tool tab, and the existing gear
   popovers (component and workflow, automation and embedded).
5. **Embedded parity.** Everything above applies equally to the embedded MCP component dialog and the embedded
   "integration instance configuration" workflow dialog.

## Non-goals

- No change to which tools exist, how tools are enabled/disabled, or tool authorization.
- No change to the Connections or Inputs tabs of the workflow cards.
- No new Tool tab in the shared workflow card outside MCP: the A2A server workflow dialog and the project
  deployment / integration instance configuration dialogs also use these components and must look exactly as
  they do today.

## Anti-goals (would be a failure even if it technically works)

- **Configuration is never required to save.** Save/Update/Add must work without touching any form, exactly as
  today. Required inputs (e.g. "First Name*") do not block Save; they can be filled now or later via the gear.
- **"Let AI fill all" never overwrites anything the user entered.** It fills **only empty fields**.
- The inline form must not drift from the gear popover. One form implementation, used in both places.
- Editing a component must not lose saved tool settings (see "Existing bug" below).

## Current state (what exists)

### Component tools

- Dialog: `client/src/shared/components/mcp-server/McpComponentDialogShell.tsx` (shared shell),
  `client/src/pages/automation/mcp-servers/components/mcp-component-dialog/` and the EE copy under
  `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/`
  (`McpComponentDialog`, `McpComponentDialogToolSelectionStep`, hooks `useMcpComponentDialog`,
  `useMcpComponentDialogToolSelectionStep`).
- Gear popover: `client/src/pages/platform/mcp-servers/components/McpComponentToolPropertiesPopover.tsx` with
  hook `client/src/pages/platform/mcp-servers/hooks/useMcpComponentToolPropertiesPopover.ts` and
  `useMcpToolFormDisplayConditions`. It loads properties via `useClusterElementDefinitionQuery`
  (clusterElementName = tool name), seeds defaults from property default values, merges saved parameters, and
  converts `''` <-> `null` (`toFormParameters` / `toStoredParameters`). It renders `Properties` with `toolsMode`
  inside a `ClusterElementProvider`. It already takes an `embedded` flag that picks the mutation.
- Save: `createMcpComponentWithTools` / `updateMcpComponentWithTools` already accept
  `tools: [{name, parameters}]`. The dialog currently always sends `parameters: {}`.
  **No server change is needed for component tools.**

### Existing bug fixed by this work

`McpComponentServiceImpl.update` deletes every tool of the component and recreates them from the input,
preserving only `enabled`. Because the dialog sends `parameters: {}`, clicking **Update** in "Edit Tools for X"
wipes every tool's saved Tool Name, Tool Description and input values. Goal 2 (prefill + send) fixes this. The
server behaviour stays as is.

### Workflow tools

- Automation dialog: `client/src/pages/automation/mcp-servers/components/McpProjectWorkflowDialog.tsx`, using
  `ServerProjectWorkflowsList` / `useServerProjectWorkflowsForm` (`client/src/pages/automation/server-workflows/`)
  and `ServerWorkflowsDialogContent`. The cards render through
  `client/src/shared/components/workflow-configuration/WorkflowConfigurationCard.tsx` (Connections/Inputs tabs).
  `ServerProjectWorkflowsList` is also used by `A2aServerWorkflowDialog`, and `WorkflowConfigurationCard` by the
  project deployment, integration instance configuration and provided-values dialogs.
- Embedded dialog: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.tsx`.
- Gear popovers: `McpProjectWorkflowPropertiesPopover` (+ `useMcpProjectWorkflowPropertiesPopover`) and the EE
  `McpIntegrationInstanceConfigurationWorkflowPropertiesPopover`.
- Properties come from the server: `mcpProjectWorkflowProperties(mcpProjectWorkflowId)` in
  `McpProjectWorkflowGraphQlController` (automation) and
  `mcpIntegrationInstanceConfigurationWorkflowProperties(...)` (EE). Both need an **existing** tool id; they
  resolve the workflow, take its callable trigger (`McpWorkflowUtils.getCallableTrigger`) and return
  `ToolPropertyUtils.toolNameProperty("workflow")`, `toolDescriptionProperty("workflow")`, plus the input schema
  properties.
- Save: `createMcpProject` / `updateMcpProject` (and the EE `create/updateMcpIntegrationInstanceConfiguration`)
  take `workflowConfigurations: [WorkflowConfigurationInput]` = `{workflowId, connections, inputs}`. There is
  **no field for tool parameters**. Parameters are only written afterwards via `updateMcpProjectWorkflow(id,
  {parameters})` -> `mcpProjectWorkflowService.updateParameters`.
- `McpProjectFacadeImpl.updateMcpProject` keeps existing workflow tools (matched by workflow id) and only
  creates/deletes the difference, so workflow parameters are not wiped on update today.

## Decided (by the user)

- Component tools: inline expansion under the ticked row (not a gear, split pane or extra step).
- Configuration optional at selection time; Save never blocked by it.
- Workflow tools: a new **Tool** tab on the card, alongside Connections and Inputs.
- Applies to embedded too.
- "Let AI fill all": placed right-aligned below Tool Description; present while adding and in the gear popovers;
  fills **only empty fields**. A field showing its untouched built-in default is not empty.
- Newly added tools start **AI-filled by default** (section 5), saved even if the form is never opened.
- Untouched built-in defaults are **not saved**.
- Workflow Inputs tab is never AI-settable; only New Workflow Call schema fields are.
- Every `fromAi` for a field with options carries the full options list, dynamic options included.
- Fix the Edit Tools settings wipe (automation + embedded) and the EE version-update wipe (section 6).

## Design

### 1. One shared tool form

Extract the body of the two gear popovers into reusable form components, so popover, inline row and Tool tab
render the same thing:

- `McpComponentToolForm`: given component name/version, tool name, connection id/required, and a
  react-hook-form instance (or value + onChange), renders the properties with display conditions and the
  "Let AI fill all" button. `McpComponentToolPropertiesPopover` becomes popover chrome + this form + its
  existing save.
- `McpWorkflowToolForm`: same for workflow tools, given a property list.

The `''` <-> `null` conversion and default-value seeding move with the form (shared helpers), so inline values
are stored exactly like popover values.

### 2. Component dialog (automation + embedded)

- Selection state goes from "list of tool names" to "list of tools with their parameter values" (e.g.
  `selectedTools: {name, parameters}[]` or a `Record<toolName, parameters>` beside the selection). Each ticked
  row renders `McpComponentToolForm` below it. Unticking discards that tool's values. Re-ticking starts from
  defaults (or saved values in edit mode).
- "Select All Tools" expands every row. That's acceptable; each form is collapsed again by unticking.
- Edit mode: initial values come from `mcpComponent.mcpTools[].parameters` (via `toFormParameters`).
- Save/Update sends `tools: [{name, parameters: toStoredParameters(values)}]`.
- The connection selected at the top of the dialog feeds each form's `ClusterElementProvider` `connectionId`,
  so dynamic options/lookups behave as in the popover.
- Validation: forms are not validated on Save (anti-goal: never block).

### 3. Workflow dialogs (automation + embedded)

**Server (automation and EE):**

- New query that returns the tool properties for a workflow that is not yet an MCP tool. Automation:
  `mcpProjectWorkflowPropertiesByWorkflowId(projectId: ID!, projectVersion: Int!, workflowId: String!): [Property]`
  (exact name is the builder's call). EE: the equivalent keyed by integration/integration version/workflow.
  Implement by extracting the body of the existing `mcpProjectWorkflowProperties` (from "resolve the Workflow"
  onwards) into a shared method, and make the existing query call it. Authorization: same checks as the
  existing properties query / the create mutation, placed on the facade per repo convention.
- Add optional `parameters: Map` to `WorkflowConfigurationInput` (it is shared platform input; the field is
  ignored by non-MCP callers), or an MCP-specific `toolParameters` list on `CreateMcpProjectInput` /
  `UpdateMcpProjectInput` and the EE inputs. Builder's call; prefer the option that does not change behaviour
  for project deployments and integration instance configurations.
- `McpProjectFacadeImpl.createMcpProject` / `updateMcpProject` (and EE counterparts): after creating a new
  workflow tool, write its parameters (new tools always carry them, see section 5); on update, write
  parameters for existing tools when provided. When an existing tool's configuration carries no parameters,
  leave the stored ones untouched.
- Regenerate client GraphQL types (`npx graphql-codegen`), in their own commit.

**Client:**

- `WorkflowConfigurationCard` gets an optional third tab (e.g. a `toolContent` prop). When absent, the card is
  unchanged (A2A, deployments, integration instance configuration, provided values).
- In the MCP workflow dialogs only, pass the Tool tab content: `McpWorkflowToolForm` fed by the new query and
  bound to the dialog form under the workflow's id. Values are sent with the save mutation.
- Edit mode of the workflow dialog: prefill the Tool tab from the existing `McpProjectWorkflow.parameters`.

### 4. "Let AI fill all"

- Placement: right-aligned, directly under Tool Description, sparkle icon + "Let AI fill all".
- Behaviour, per field in the current form:
  - Only fields that **show a per-field AI button today** are eligible (the same condition that renders that
    button in `Property` / `useProperty`). Object/array containers without an AI button (Headers, Query
    Parameters, Body) are skipped; Tool Name and Tool Description are skipped.
  - Only **visible** fields: skip fields hidden by display conditions or behind "Show Advanced Properties".
  - Only **empty** fields (undefined, null, `''`). Never overwrite a typed value, a formula, or an existing
    `=fromAi(...)`.
  - The value written is exactly what that field's AI button would write.
- To guarantee that, move the `fromAiExpression` builder out of `useProperty`
  (`client/src/pages/platform/workflow-editor/components/properties/hooks/useProperty.ts`, ~line 407) into a
  pure helper (inputs: name, arrayName, type, description, defaultValue, options, required). `useProperty` and
  the bulk action both call it.
- "Empty" means undefined, null or `''`. A field showing its built-in property default (e.g. Response Format =
  JSON) counts as **filled** and is skipped (user decision).
- In the workflow Tool tab, eligible fields are the New Workflow Call schema fields only.

### 5. AI-filled by default for newly added tools (user decision)

Today nothing seeds `=fromAi(...)`: the "Automatically defined by the model" label appears only when the stored
value already starts with `=fromAi(` (`getControlledToolFieldState`, `Property.tsx`). A freshly added tool
therefore exposes no inputs to the model (`AutomationMcpToolFacade` builds the input schema from stored `fromAi`
values only), and `McpProjectWorkflowMappingValidator` refuses to enable the server while a required schema
input is null.

New rule: when a tool is **newly added** (component tool ticked, or workflow toggled on, in add or edit mode),
its form starts with every field that "Let AI fill all" would fill already set to that field's `=fromAi(...)`.
Same eligibility rule (has an AI button, visible against the initial values, empty after defaults are seeded).
"Let AI fill all" then mostly restores fields the user cleared.

Consequences:

- The seeded values must be **saved even if the user never opens the form / Tool tab**. So for every newly
  added tool the client loads its properties and sends its parameters on save. Do not rely on a lazily mounted
  tab's form; compute the seeded parameters from the property list directly (the pure `fromAi` helper makes
  this possible).
- Tools that **already exist** (edit mode) are not re-seeded: their saved parameters are shown and sent as
  they are, even when empty.
- **Untouched built-in defaults are not saved** (user decision). The payload is: saved values (edit mode) +
  `fromAi` seeds + whatever the user changed. Seeding the form with property defaults for display is fine,
  but values equal to an untouched seeded default are left out of the payload, so the form must track which
  fields the user changed (react-hook-form dirty state), not just compare values.
- This also replaces the server rule "no parameters → leave stored ones untouched" for **new** tools: new tools
  always carry parameters. For existing tools the rule stands.
- Server-side seeding is not required; the client sends the seeded values. (Builder may add a server fallback,
  but it must produce the same expressions.)

### 6. Bug fixes in scope (user decision)

Both are fixed as part of this work, ideally as their own commits/PR ahead of the feature so they can ship
independently:

1. **Edit Tools wipes tool settings (automation + embedded).** "Edit Tools for <Component>" -> Update re-sends
   every tool with `parameters: {}` and `McpComponentServiceImpl.update` recreates the tools, losing their
   saved Tool Name, Tool Description and inputs. Minimal fix: the dialog sends each kept tool's saved
   parameters (`mcpComponent.mcpTools[].parameters`) instead of `{}`. The inline prefilled forms (Goal 2) then
   build on that. Server may additionally carry over parameters by tool name when the input omits them
   (builder's call), but the client fix must stand on its own.
2. **EE version update wipes workflow tool settings.** `updateMcpIntegrationInstanceConfigurationVersion`
   (`McpIntegrationInstanceConfigurationFacade` / its impl, called from
   `McpIntegrationInstanceConfigurationGraphQlController`) deletes and recreates the MCP workflow rows, wiping
   their parameters. Fix: carry each workflow's parameters (and enabled state) over to the recreated row,
   matched by workflow (UUID / reference code, whichever identifies the same workflow across integration
   versions; builder to confirm). Workflows that no longer exist in the new version are dropped as today.
   Schema fields that no longer exist in the new version's trigger schema are dropped from the carried
   parameters. (My call.)

Each fix gets a regression test that fails before it.
- **Options are mandatory** (user decision): a field with options must get a `fromAi` that carries its full
  `options` list, or the model can produce invalid values. For fields with static options this comes from the
  property definition. For fields whose options load from the server (`optionsDataSource`), the bulk action
  and the default seeding (section 5) must **fetch the options first**, with the same query and inputs the
  field's own dropdown uses (`useClusterElementOptionsQuery` / its fetcher, with component, tool name, property
  path, current input parameters and connection id).
  - If the options cannot be loaded (e.g. the field needs a connection and none is selected yet, the lookup
    depends on another empty field, or the request fails), that field is **left empty**: never write a
    `fromAi` without its options. (My call.)
  - When the connection changes in the component dialog, fields seeded from a dynamic options lookup are
    re-seeded only if the user has not touched them. (My call.)
  - Seeding therefore becomes asynchronous; Save waits for pending seeding of newly added tools (with a short
    spinner on the Save button), it does not save half-seeded parameters. (My call.)
- If nothing is eligible, the button is disabled (or hidden; builder's call).

## Constraints

- Follow CLAUDE.md client rules: sorted object keys, interface naming (`...I` / `...Props`), `twMerge`,
  `fieldset` for grouping, hook ordering, `Icon`-suffixed lucide imports, no IIFEs in JSX.
- Server: facades carry `@PreAuthorize`; services never call services; EE files get the EE header and
  `@version ee`.
- GraphQL schema changes need regenerated client types; generated files in their own commit.
- Tests: unit tests for the extracted `fromAi` helper and "Let AI fill all" eligibility (empty/visible/has-AI);
  component dialog tests for expand/collapse, prefill in edit mode, and the saved payload; workflow dialog tests
  for the Tool tab presence (MCP only) and payload; `WorkflowConfigurationCard` test that the third tab is absent
  by default. Server: controller IntTests for the new query; facade tests for parameters on create/update.

## Left to the builder

- Exact component/hook names and file placement (shared form likely under `client/src/pages/platform/mcp-servers/`).
- Shape of selection state in the component dialog.
- The GraphQL field/argument names, and whether parameters ride on `WorkflowConfigurationInput` or on an
  MCP-specific input.
- Whether "Let AI fill all" is disabled or hidden when nothing is eligible.
- Commit/PR split (suggested: server query + input; client codegen; shared form + "Let AI fill all"; component
  dialog; workflow dialogs).
