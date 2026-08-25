# Custom Variables (workspace-wide and embedded organization-wide)

**Date:** 2026-08-17
**Status:** Draft — written autonomously; decisions flagged in "Decisions to confirm" are the ones most
worth overruling before planning starts
**Ticket:** 732
**Reference model:** n8n custom variables (`$vars.<name>`, string values, admin-managed, read-only in
workflows) — <https://docs.n8n.io/build/code-in-n8n/define-custom-variables>

## Summary

Add **variables**: named string values that an admin defines once in a settings page and that every
workflow in that scope can reference as `${vars.NAME}` — in the visual editor via a new **Variables**
section of the data-pill panel (next to Inputs), in code workflows via `TaskContext.input().get("vars")`,
and anywhere else `${...}` expressions are evaluated. Two scopes, two settings pages:

| Surface | Scope | Settings page | Who edits |
| --- | --- | --- | --- |
| Automation | one set per **workspace** per environment | `/automation/settings/variables` (Current Workspace group) | workspace admin (`VARIABLE_MANAGE`) |
| Embedded | one set per **organization** (= tenant) per environment | `/embedded/settings/variables` | tenant `ROLE_ADMIN` |

Storage reuses the existing `property` table (`Property implements CredentialSecret`): **one `Property` row
per variable**, key `variable.<NAME>`, `Scope.WORKSPACE`/`workspaceId` for automation and the so-far-unused
`Scope.EMBEDDED`/`null` for embedded, `environment` set. No new table, no Liquibase changelog.

Variables are **read-only from workflows** and are **snapshotted into the job at creation** under the
reserved input key `vars`, so a run sees one consistent set for its whole lifetime and workers need no
database access to resolve them.

## Motivation

Today the only way to share a value across workflows (an API base URL, a tenant slug, a region, a
default sender address) is to paste it into every node, or to fake it with a per-workflow input that
every deployment must then supply. n8n, Make and Pipedream all ship a first-class "variables" primitive
for exactly this; ByteChef has the storage (`Property`) and the expression engine already — what is
missing is the entity, the two settings pages, and threading a `vars` map into evaluation.

## Non-goals (v1)

- **Secrets.** Variables are plain configuration. They are shown in clear in the settings page and in the
  data-pill panel, and their values land in `Job.inputs` (visible on the execution detail page). Storage
  happens to be encrypted at rest because `Property.value` always is; that is not a security claim.
  A `secret` flag (masked in UI, redacted from `Job.inputs`) is a natural follow-up — `Property` already
  routes through the credential-store SPI — but is out of scope.
- **Per-project / per-integration variables** (n8n's project-scoped tier) and a tenant-wide "global"
  tier that workspaces override. `Property.Scope.PROJECT`/`INTEGRATION`/`PLATFORM` exist, so this is
  additive later; v1 has exactly one tier per surface, no precedence rules.
- **Cross-environment inheritance.** A variable belongs to one environment (see decision 4). "Copy to
  environment" is a UI follow-up.
- **Writing variables from a workflow.** n8n's variables are read-only too. A `variables` component
  with a `set` action would be a separate feature with its own consistency story.
- **Typed values.** Strings only, like n8n. Numbers/booleans/JSON are obtained in expressions with the
  existing casts (`=int(vars.RETRIES)`, `=toMap(vars.CONFIG)`).
- **`fromAi(...)` arguments** (evaluated against an empty context today), pre/post/finalize sub-task
  contexts (also empty today), and the AI Hub / copilot tool surface (no "list variables" tool). Each
  is noted where relevant; none is changed by this work.
- **Distributed EE parity.** `PropertyService` is unavailable in `execution-app` / `webhook-app` /
  `coordinator-app` (`RemotePropertyServiceClient` throws on every method). Resolution is fail-open there,
  so variables are **monolith-only** in v1 — the same posture as the error-workflow handler and
  orphaned-job recovery. Follow-up: implement the read path on the remote client + a
  `/remote/property-service` controller.

## Decisions to confirm

These were made without a human in the loop; each has a cheap alternative if you disagree.

1. **EE on both sides.** Every existing "Current Workspace" settings entry (Users, Git Configuration,
   API Keys, AI Hub Connectors, AI Agents) is `<EEVersion>`-gated and embedded is EE by nature, so
   variables follow. If you want automation variables in CE: move `platform-variable-*` under
   `server/libs/platform/`, drop `@ConditionalOnEEVersion` and the `<EEVersion>` wrapper on the
   automation route, and register the client seam unconditionally. Everything else is unchanged.
2. **One `Property` row per variable** (key `variable.<NAME>`) rather than one row per scope holding a
   `{name: value}` map. Per-row gives audit columns per variable, optimistic locking per variable
   instead of last-write-wins across the whole set, and free future use of `Property.enabled` /
   the credential-store routing for a `secret` flag. Cost: `PropertyService` needs one new
   list-by-key-prefix method (there is no list-by-scope today), and with an external credential store
   configured every variable read is one store round-trip (see "Performance").
3. **Values are seeded into `Job.inputs` under `vars` at job creation** rather than resolved lazily at
   evaluation time. It is the only seam outside `server/libs/atlas/` (which must stay
   platform-agnostic) that reaches the JOB context, it is exactly how `__triggerName` is delivered,
   it gives run-consistent snapshot semantics, and it works with workers that have no property access.
   Consequence: `vars` appears among the job's inputs on the execution page, and a value edited mid-run
   does not affect that run.
4. **Environment-scoped, no fallback.** A variable is defined per (scope, environment). Connections,
   API keys, AI-provider keys, signing keys and — most relevantly — the editor's **test configuration
   inputs** are all per-environment, and the settings pages already carry the `EnvironmentSelect`
   header. CE exposes only `DEVELOPMENT`, so CE users never see the extra dimension. Alternative:
   environment-agnostic (`environment = null`), which is one fewer argument everywhere but leaves the
   "API URL differs per env" case to naming conventions (`API_URL_PROD`).
5. **`vars` is a reserved name** for workflow inputs and node names (both editions), enforced by the
   save-time guards in `WorkflowValidatorFacade` and by the client's input/node rename validation.
   `__vars` would need no reservation but reads badly (`${__vars.X}`); n8n users expect `vars`.
6. **Names are identifiers**: `^[A-Za-z_][A-Za-z0-9_]{0,49}$` (n8n's alphabet, but a leading digit is
   rejected because `${vars.1abc}` is not a valid SpEL property path). Values: max **4096** chars (n8n:
   1000). Case-sensitive, unique per (scope, environment).

## Design

### Domain and storage

New EE modules under `server/ee/libs/platform/platform-variable/` (mirrors `platform-ai-guardrails`,
which is likewise a `Property`-backed EE platform feature):

- `platform-variable-api`
  - `Variable` record: `id`, `name`, `value`, `environmentId`, audit fields (`createdBy`, `createdDate`,
    `lastModifiedBy`, `lastModifiedDate`).
  - `VariableScope` record `(VariableScopeType type, @Nullable Long workspaceId)` with factories
    `workspace(long)` and `embedded()`; `VariableScopeType {WORKSPACE, EMBEDDED}`.
  - `VariableService`:
    ```java
    List<Variable> getVariables(VariableScope scope, long environmentId);
    Map<String, String> getVariableMap(VariableScope scope, long environmentId);   // name -> value
    Variable create(VariableScope scope, long environmentId, String name, String value);
    Variable update(VariableScope scope, long environmentId, long id, String name, String value);
    void delete(VariableScope scope, long environmentId, long id);
    ```
    `update`/`delete` verify the row's key prefix, scope, scopeId and environment match the requested
    scope; a mismatch throws the same not-found error as a missing id (ids must not be usable to reach
    another workspace's rows).
  - `VariableNameValidator` (static, unconditional — the `TaskScheduleValidator` posture): name regex,
    length caps, and the "must not be blank" checks; shared by the service and unit-tested directly.
  - `VariableScopeProvider` SPI (one implementation per `PlatformType`, see below):
    ```java
    PlatformType getType();
    Optional<VariableScope> getVariableScope(long jobPrincipalId);
    Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId); // empty if not owned by this type
    ```
  - `VariableErrorType` (`ErrorType` enum): `VARIABLE_NAME_INVALID`, `VARIABLE_VALUE_TOO_LONG`,
    `VARIABLE_NAME_ALREADY_EXISTS`, `VARIABLE_NOT_FOUND`.
- `platform-variable-service`
  - `VariableServiceImpl` (`@Service @Transactional @ConditionalOnEEVersion`) over `PropertyService`:
    key = `Variable.KEY_PREFIX + name` (`"variable."`), value map `{"value": <string>}`,
    `Scope.WORKSPACE`/`workspaceId` or `Scope.EMBEDDED`/`null`, `environmentId` always set.
    Create = `PropertyService.save` after a uniqueness check (`fetchProperty` on the would-be key);
    rename = delete old key + save new key inside the same transaction; delete = `PropertyService.delete`.
  - `WorkflowVariablesResolverImpl implements WorkflowVariablesResolver` (CE SPI, next section):
    dispatches on `PlatformType` to the matching `VariableScopeProvider`, takes the environment from
    `JobPrincipalAccessorRegistry` (`getEnvironmentId(jobPrincipalId)`) for principals and from the
    argument for workflows, then calls `getVariableMap`. **Fail-open**: any exception (including the
    remote client's `UnsupportedOperationException`) → empty map + one WARN per JVM (`AtomicBoolean`),
    same idiom as the embedded bridge's remote-client degradation.
  - `VariablePermissionScope {VARIABLE_VIEW → VIEWER, VARIABLE_MANAGE → ADMIN}` + provider — declared in
    `automation-configuration-service/security/scope/` alongside the other scope enums (the
    `AiGatewayPermissionScope` precedent for a non-automation feature).
- `platform-variable-graphql` — the automation controller (below).

`PropertyService` (CE, `platform-configuration-api`) gains one method, implemented with a derived
repository query on `PropertyRepository`:

```java
List<Property> getPropertiesByKeyPrefix(String keyPrefix, Scope scope, @Nullable Long scopeId, @Nullable Long environmentId);
```

(`findAllByKeyStartingWithAndScopeAndScopeIdAndEnvironment` / `...AndScopeIdIsNullAndEnvironment` —
values populated through the existing credential-store dispatch, exactly like `getProperties`.)
`RemotePropertyServiceClient` gets the matching throwing stub.

`Property.Scope.EMBEDDED` (ordinal 2) is used for the first time; ordinals are unchanged.

### Scope providers (edition-owned)

- `ProjectVariableScopeProvider` — EE `automation-configuration-service`
  (`server/ee/libs/automation/automation-configuration/automation-configuration-service`, next to
  the EE `EnvironmentServiceImpl`): `getVariableScope(projectDeploymentId)` → `ProjectDeployment` →
  `Project.workspaceId` → `VariableScope.workspace(...)`; `getVariableScopeByWorkflowId` →
  `ProjectWorkflowService.fetchProjectWorkflow` → same. Embedded's automation-bridge catalog projects
  carry `Workspace.DEFAULT_WORKSPACE_ID` and therefore read the default workspace's variables — noted,
  accepted.
- `IntegrationVariableScopeProvider` — `embedded-configuration-instance-impl` (already the home of
  `IntegrationJobPrincipalAccessor`): both methods return `VariableScope.embedded()` when the principal
  / workflow exists on the embedded side, empty otherwise.

`platform-variable-service` therefore depends on neither automation nor embedded modules; the
providers point up at it, keeping the platform → edition direction one-way.

### Runtime: the `vars` job input

CE `platform-api` gains:

- `JobInputConstants.VARIABLES_INPUT = "vars"` next to `TRIGGER_NAME_INPUT`.
- SPI `com.bytechef.platform.variable.WorkflowVariablesResolver`:
  ```java
  Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type);
  Map<String, String> resolveForWorkflow(String workflowId, long environmentId);
  ```
  Consumers hold an `ObjectProvider<WorkflowVariablesResolver>`; no bean (CE) → they do not add a `vars`
  key at all. With the bean present (EE) they always add it, empty map included, so `${vars}` is
  well-defined in EE.

Seeding sites (all CE, all outside atlas):

1. `PrincipalJobFacadeImpl` — the four job-creating methods (`createJob`, `createJobWithoutDispatch`,
   `createChildJob`, `createPrincipalLinkedJob`) call one private helper that copies the
   `JobParametersDTO` with `inputs + {vars: resolved}` (a resolved map **overwrites** any caller-supplied
   `vars` — the name is reserved). For the two "linked" variants the principal is the one already looked
   up. This covers deployments, webhooks, schedules, sync webhook/MCP/A2A, agent sub-workflows and
   subflows.
2. `TestWorkflowExecutorImpl.getWorkflowTestParameters` — editor test runs go through `JobSyncExecutor`,
   not the principal facade; seed via `resolveForWorkflow(workflowId, environmentId)` there.
3. Editor-side preview evaluations. ~18 facade sites in `platform-configuration-service` build their
   context as `MapUtils.concat(testConfigurationInputs, previousOutputs)`. Introduce
   `WorkflowEvaluationInputsResolver` (CE, `platform-configuration-service`) with
   `Map<String, Object> getEvaluationInputs(String workflowId, long environmentId)` = test-configuration
   inputs + `vars`, and replace each site's `workflowTestConfigurationService.getWorkflowTestConfigurationInputs(...)`
   call with it. Sites: `WorkflowNodeOutputFacadeImpl` (×3), `WorkflowNodeTestOutputFacadeImpl` (×5),
   `WorkflowNodeParameterFacadeImpl` (×2), `WorkflowNodeScriptFacadeImpl` (×2), `WorkflowNodeDynamicPropertiesFacadeImpl` (×2),
   `WorkflowNodeOptionFacadeImpl` (×2), `WorkflowNodeDescriptionFacadeImpl` (×2), `WebhookTriggerTestFacadeImpl` (×2).
   The plan enumerates them with line references; the resolver is the single point that knows about `vars`.

Nothing else changes: `SpelEvaluator`'s accessor whitelist already admits `${vars.NAME}` /
`${vars['NAME']}`; a missing variable behaves like any missing key today (the expression is left as its
literal text; n8n yields `undefined`). Code workflows get `vars` for free via `=#root`
(`TaskContext.input().get("vars")`); the script component receives it through whatever `${vars.X}` the
user maps into its `input`. `DefaultTaskCompletionHandler` copies the JOB context forward on every task
completion, so `vars` survives the whole run and crash-resume.

Reserved-name enforcement: `WorkflowValidatorFacade.validateNoReservedInputNames` /
`validateNoReservedNodeNames` additionally reject a name equal to `vars` (new error messages, same
`RESERVED_*` error types). Pre-existing workflows with a node named `vars` continue to run but can no
longer be saved until renamed — acceptable; none is expected.

### GraphQL

Two thin controllers, one service. Automation, in `platform-variable-graphql`
(`workspace-variable.graphqls`):

```graphql
type Variable { id: ID!, name: String!, value: String!, environmentId: ID!,
                createdBy: String, createdDate: String, lastModifiedBy: String, lastModifiedDate: String }
input VariableInput { name: String!, value: String! }

extend type Query    { workspaceVariables(workspaceId: ID!, environmentId: ID!): [Variable!]! }
extend type Mutation {
    createWorkspaceVariable(workspaceId: ID!, environmentId: ID!, input: VariableInput!): Variable!
    updateWorkspaceVariable(workspaceId: ID!, environmentId: ID!, id: ID!, input: VariableInput!): Variable!
    deleteWorkspaceVariable(workspaceId: ID!, environmentId: ID!, id: ID!): Boolean!
}
```

`@PreAuthorize`: read `hasPermission(#workspaceId, 'Workspace', 'VARIABLE_VIEW')`, mutations
`hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')` — on the controller, since (as with
guardrails and workspace notifications) no facade owns the check and the service is also called by the
runtime resolver with no security context. `@Controller @ConditionalOnEEVersion @ConditionalOnCoordinator`.

Embedded, in the existing EE `embedded-configuration-graphql` module (`embedded-variable.graphqls`):
`embeddedVariables(environmentId)`, `createEmbeddedVariable(environmentId, input)`,
`updateEmbeddedVariable(environmentId, id, input)`, `deleteEmbeddedVariable(environmentId, id)` —
read `isAuthenticated()`, mutations `hasAuthority('ROLE_ADMIN')` (the A2A/guardrails posture for
tenant-level admin settings). Both controllers add their `.graphqls` path to `client/codegen.ts`.

Errors surface through the standard `ConfigurationException`→GraphQL error mapping and reach the user via
the global fetch-interceptor toast; no per-mutation `onError` beyond form reset on name conflicts.

### Client

**Settings pages (EE).** One shared table+dialog component parameterized by hooks, the
`ApiKeysProvider`/`ApiKeysContent` shape:

- `client/src/ee/shared/components/variables/` — `VariablesContent` (header with title, description
  and `EnvironmentSelect`; table columns **Name | Value | Reference | Last modified | ⋯**, where
  Reference is a copyable `${vars.NAME}` chip; empty state), `VariableDialog` (react-hook-form + zod:
  name regex/length, value length; create and edit share it), `VariableDeleteDialog`, and a
  `VariablesProvider` context that receives `{useVariablesQuery, useCreate…, useUpdate…, useDelete…}`
  from the page.
- `client/src/ee/pages/settings/automation/variables/Variables.tsx` — wires the `workspaceVariables`
  hooks with `useWorkspaceStore.currentWorkspaceId` + `useEnvironmentStore.currentEnvironmentId`.
  Route `variables` in `currentWorkspaceSettingsRoutes` (`PrivateRoute` ADMIN **or** USER — a workspace
  admin need not be a tenant admin, the `workspace-users` precedent; the mutations are gated
  server-side and the page hides the create/edit controls when `useMyWorkspaceScopesQuery` does not
  include `VARIABLE_MANAGE`, exactly as `WorkspaceUsers` does with `WORKSPACE_MEMBER_MANAGE`), nav
  item **Variables** under "Current Workspace". No path collision: `platformSettingsRoutes` has no
  `variables`.
- `client/src/ee/pages/settings/embedded/variables/Variables.tsx` — wires the `embeddedVariables`
  hooks; route `variables` in the embedded settings children (`PrivateRoute` ADMIN), nav item
  **Variables** after "API Keys".

**Editor data-pill panel (CE code, EE data via the edition seam).**

- `client/src/shared/edition/variables/variablesApi.ts` — registry in the `projectGitApi` pattern:
  `useWorkflowVariablesQuery(scope: {type: 'WORKSPACE' | 'EMBEDDED'; workspaceId?: number}, environmentId, enabled)`
  → `{data: VariableI[] | undefined}`; the CE default returns `undefined`; the EE bootstrap in
  `main.tsx` registers the generated hooks.
- `pages/platform/workflow-editor/hooks/useWorkflowVariables.ts` — derives the scope from
  `usePlatformTypeStore.currentType` (+ `useWorkspaceStore.currentWorkspaceId` for automation) and
  `useEnvironmentStore.currentEnvironmentId`, calls the seam. React-query dedupes across callers.
- `datapills/DataPillPanelBodyVariablesItem.tsx` — a **Variables** accordion item rendered right after
  Inputs (`VariableIcon`), one child pill per variable: `<DataPill workflowNodeName="vars"
  property={{name, type: 'STRING'}} path={name} sampleOutput={value} …/>` so `buildMentionId` yields
  `vars.NAME`; shows "No variables defined." with a link to the settings page when empty; **respects
  `dataPillFilterQuery`** (the Inputs item does not — fix that in passing, it is a one-prop change).
  Rendered only when the seam returns data (so CE shows nothing).
- The three flat-pill producers (`useWorkflowNodeDetailsPanel.ts`, `useDataStreamDataPills.ts`,
  `useAiAgentTestDataPills.ts`) append `{id: 'vars.NAME', nodeName: 'vars', value: 'vars.NAME'}` per
  variable, so `PropertyMentionNodeView` treats the chip as available. The triplicated inputs block is
  extracted into a small `getWorkflowInputAndVariableDataPills(inputs, variables)` helper while touching it.
- `getDataPillIconSource.ts` — `vars` prefix → the variable icon.
- Reserved name: `useWorkflowInputs.ts` (input name) and the node-rename validation reject `vars` with a
  message; mirrors the server guard.

Client-side rules to remember: `Icon`-suffixed lucide imports, `Ref`-suffixed refs, sort-keys, interface
names ending in `I`/`Props`, `twMerge` not `cn`, hook ordering, `EnvironmentSelect` from the shared header
components.

### Error handling

| Case | Behaviour |
| --- | --- |
| Invalid name / too-long value | `ConfigurationException(VARIABLE_NAME_INVALID / VARIABLE_VALUE_TOO_LONG)` → 4xx GraphQL error → toast; dialog keeps state |
| Duplicate name in scope+environment | `VARIABLE_NAME_ALREADY_EXISTS`; for workspace-scoped rows also guarded by `uk_property_key_scope_scope_id_environment` if two admins race — the second commit fails, surfaced as the same error. Embedded rows have a NULL `scope_id`, which Postgres treats as distinct per row, so that constraint does not fire there — the embedded duplicate check is service-level (`VariableServiceImpl`'s pre-check) only, and a genuine race can still create two rows |
| Id from another scope | `VARIABLE_NOT_FOUND` (indistinguishable from a missing id) |
| Property service unavailable at job creation (`execution-app`) | if the resolver bean exists but the store call fails, empty `vars`, WARN once per JVM, job proceeds. `execution-app` does not carry the resolver module at all, so in practice no resolver bean exists there — the same empty-`vars`/job-proceeds outcome occurs, but nothing is attempted and no WARN is logged |
| `${vars.X}` with unknown `X` | expression left as literal text (existing missing-key behaviour) |
| Concurrent edits of the **same** variable | last write wins at the row level (`PropertyService.save` re-reads before writing); accepted |

### Performance

Reads happen once per job creation and once per editor query — a single `LIKE 'variable.%'` query on
an indexed `(key, scope, scope_id, environment)` tuple plus one AES decrypt per row. With an **external
credential store** (Vault / AWS) configured, `PropertyServiceImpl.resolveTargetStore` routes every
`Property` write there and `populateAll` does one `getSecret` per row on read; N variables = N store
calls per job creation. Documented limitation; the fix if it bites is a short-TTL Caffeine cache in
`WorkflowVariablesResolverImpl` (the guardrails-advisor precedent), deliberately not added in v1 to keep
edits immediately visible in editor test runs.

### Testing

- `VariableNameValidatorTest` — regex edge cases (leading digit, dash, unicode, 50/51 chars, blank).
- `VariableServiceTest` (Mockito over `PropertyService`) — key/scope/environment mapping for both scope
  types, create-conflict, rename = delete+save, cross-scope id rejected, `getVariableMap` shape.
- `PropertyServiceImplTest` — new prefix method dispatches to the right finder and populates values.
- `WorkflowVariablesResolverTest` — provider dispatch by type, environment from accessor registry,
  fail-open on exception (WARN once), CE (no bean) path in `PrincipalJobFacadeImplTest`: no `vars` key.
- `PrincipalJobFacadeImplTest` / `TestWorkflowExecutorImplTest` — `vars` present in the created job's
  inputs, caller-supplied `vars` overwritten.
- `WorkflowEvaluationInputsResolverTest` — inputs + vars merged; absent bean → inputs only.
- `WorkflowValidatorFacadeTest` — `vars` rejected as input name and node name.
- GraphQL controller tests (`@GraphQlTest`-style, as `AiGuardrailsWorkspaceSettingsGraphQlControllerTest`)
  for authorization expressions and argument mapping, both controllers.
- IntTest: `VariableServiceIntTest` (Testcontainers PG) — round-trip through the real `property` table
  including the unique-constraint race and `Scope.EMBEDDED`/null scopeId rows.
- Client: `VariablesContent.test.tsx` (render, empty state, dialog validation),
  `DataPillPanelBodyVariablesItem.test.tsx` (pills built, filter applied, hidden when seam empty),
  `getWorkflowInputAndVariableDataPills.test.ts`, route/nav presence in the existing `Settings` tests.

### Documentation

- `docs/content/docs/automation/variables.mdx` (usage: settings page, `${vars.NAME}`, environments,
  reserved name, code-workflow access) with a short embedded note; follow the docs' released-version /
  coming-soon banner convention.
- CLAUDE.md: a "Variables" subsection (storage shape, the `vars` job-input seam and its fail-open,
  reserved name, EE placement).

## Decisions log

- **Why not resolve lazily in the evaluator?** The `Evaluator` is a stateless core bean with no job in
  hand; giving it a job-aware accessor would either put platform lookups into `evaluator-impl` or a
  thread-local into `atlas`. Seeding at creation keeps atlas untouched and gives snapshot semantics.
- **Why not `JobPrincipalAccessor.getWorkspaceId`?** It would collapse four duplicated
  principal→workspace walks (guardrails, workspace prompt, asset-file, job ownership) and is worth doing,
  but it cannot express the embedded case or the by-workflow-id case, both of which this feature needs.
  A follow-up can have `ProjectVariableScopeProvider` reuse it.
- **Why two controllers instead of `variables(scope, workspaceId?)`?** Different authorization
  expressions, different editions of the surrounding modules, and an unambiguous schema; the shared
  service means the duplication is ~40 lines of mapping.
- **Why not `workspaceVariables` in CE `platform-configuration-graphql`?** All workspace-scoped settings
  surfaces are EE; splitting the module by edition would only matter if decision 1 flips.
