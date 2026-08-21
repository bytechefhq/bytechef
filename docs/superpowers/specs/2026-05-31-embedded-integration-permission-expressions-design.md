# Embedded Integration & Workflow Permission Expressions — Design

- **Date:** 2026-05-31
- **Status:** Approved (design)
- **Branch:** `0_732`
- **Author:** Ivica Cardic
- **Edition:** Enterprise (EE), embedded only

## Summary

Embedded integrations and their workflows should be conditionally visible to a
connected (embedded) user based on that user's attributes. We add an optional
SpEL **permission expression** to each `Integration` and each
`IntegrationWorkflow`. When an embedded integration (and its workflows) is
rendered through the embedded public REST API, each expression is evaluated
against the requesting `ConnectedUser`. If an expression evaluates to `false`,
the integration or workflow is **filtered out** (not visible). If no expression
is set, no filtering is applied.

The connected user's `metadata` map (free-form key/value pairs already stored on
`ConnectedUser`) plus a few top-level user fields form the evaluation context, so
embedding partners can gate visibility on their own per-user attributes (plan,
tier, beta opt-in, environment, etc.). Expressions are operator-only (no method
calls — see Evaluation semantics).

## Goals

- Per-integration and per-workflow visibility, evaluated live at render time.
- Expressions reference connected-user metadata and core user fields.
- Absent expression ⇒ always visible (no behavior change for existing data).
- Authoring via GraphQL mutations + existing admin UI dialogs.
- Fail closed: a broken/erroring expression hides the item rather than leaking it.

## Non-Goals

- Versioning/snapshotting the expression into `IntegrationVersion` (it lives on
  the mutable rows and is evaluated live).
- Applying these expressions outside the embedded public REST read paths (e.g.
  the internal admin list is unaffected; admins always see everything).
- A general-purpose policy/role engine. This is row-level visibility filtering,
  intentionally not modeled as Spring method security / AOP.
- CE behavior. This is EE + embedded only.

## Background / Current State

### Read path (where filtering must happen)

`IntegrationApiController` (embedded **public-rest**:
`server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/.../IntegrationApiController.java`)
exposes four endpoints, all routed through `ConnectedUserIntegrationFacade`:

| Endpoint | Auth source for externalUserId | Returns |
|---|---|---|
| `getFrontendIntegration(id)` | `SecurityUtils.fetchCurrentUserLogin()` | `IntegrationModel` (with workflows) |
| `getFrontendIntegrations()` | `SecurityUtils.fetchCurrentUserLogin()` | `List<IntegrationBasicModel>` |
| `getIntegration(externalUserId, id)` | explicit arg | `IntegrationModel` |
| `getIntegrations(externalUserId)` | explicit arg | `List<IntegrationBasicModel>` |

`ConnectedUserIntegrationFacadeImpl` resolves the `ConnectedUser`
(`connectedUserService.getConnectedUser(externalUserId, environment)`) in both
`getConnectedUserIntegration` (single) and `getConnectedUserIntegrations` (list),
then assembles `ConnectedUserIntegrationDTO`(s). This is the natural filter point
— the user (with metadata) is already in hand.

Today the controller post-processes the single-integration response with:
- `filterDisabledWorkflows(...)` — drops workflows not enabled in the instance config.
- `populateMcpData(...)` — separately gathers **MCP** tools and **MCP workflows**
  and attaches them to the model (≈70 lines of MCP service calls in the web layer).

### Connected user metadata

`ConnectedUser` (`embedded-connected-user-api`) has:
- `getMetadata(): Map<String,String>` — free-form key/value (partner-supplied; all
  values coerced to `String`).
- `getExternalId(): String`, `getEmail(): String`, `getName(): String`,
  `getEnvironment(): Environment`.

`ConnectedUserService.getConnectedUser` is **not cached** — resolving the user
twice is a real second DB round-trip plus a separate metadata query. (Drives the
"do all filtering in the facade with the single resolved user" decision below.)

### Expression evaluation

`SpelEvaluator` (`server/libs/core/evaluator/evaluator-impl`) is a sandboxed SpEL
evaluator, exposed as the `Evaluator` Spring bean via `EvaluatorConfiguration`. It
blocks `T(...)`, constructors, and bean references, and only allows a function
whitelist. The proven usage pattern is `WorkflowUtils.extractAndEvaluateCondition`:
wrap a condition as `"=" + expression`, evaluate a single-entry map against a
context map, parse the boolean result.

### Authoring surface

`embedded-configuration-graphql` already has query-only `IntegrationGraphQlController`
and `IntegrationWorkflowGraphQlController`. The admin client edits integrations via
`IntegrationDialog` and workflows via `WorkflowDialog`. Mutations elsewhere in the
embedded GraphQL modules use the standard `@MutationMapping` pattern.

## Design

### Overview

1. **Storage:** add nullable `permission_expression TEXT` to `integration` and
   `integration_workflow`.
2. **Evaluator unit:** `EmbeddedPermissionEvaluator` wraps the core `Evaluator`,
   builds the context from a `ConnectedUser`, and returns a boolean (fail closed).
3. **Filtering:** done entirely in `ConnectedUserIntegrationFacadeImpl`, using the
   single already-resolved `ConnectedUser`. **MCP workflow assembly moves from the
   controller into the facade** so regular and MCP workflows are filtered uniformly
   in one place.
4. **Authoring:** GraphQL mutations on the existing embedded-configuration GraphQL
   controllers; UI fields in the existing `IntegrationDialog` / `WorkflowDialog`.

### 1. Data model & migration

New columns (nullable, no default ⇒ existing rows = `NULL` = "no expression"):

| Table | Column | Type |
|---|---|---|
| `integration` | `permission_expression` | `TEXT` |
| `integration_workflow` | `permission_expression` | `TEXT` |

Liquibase: one new changeset file in
`embedded-configuration-service/.../changelog/embedded/configuration/`, next in the
existing `20240604183xxx` sequence (e.g.
`20240604183170_embedded_configuration_added_permission_expression.xml`) with two
`<addColumn>` changeSets. Delete any stale copy from `build/resources/` if a rename
occurs.

Domain entities:
- `Integration.java` — add `@Column("permission_expression") private String permissionExpression;`
  with getter/setter; include in `@PersistenceCreator` constructor and `toString()`.
- `IntegrationWorkflow.java` — same field, getter/setter, included in the
  `@PersistenceCreator` constructor.

DTO exposure:
- `IntegrationDTO` — add a `String permissionExpression` component, populated from
  `Integration.getPermissionExpression()` wherever `IntegrationDTO` is built (record
  component + both constructors + `Builder`). The list path
  (`getConnectedUserIntegrations`) reads it to filter at integration level.
- **Per-workflow expression resolution:** the rendered workflow list is *not* built
  from `IntegrationWorkflowDTO`; it comes from
  `IntegrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows()`
  (a `List<IntegrationInstanceConfigurationWorkflowDTO>`), each carrying a
  `workflowUuid`. The permission expression lives on `IntegrationWorkflow`
  (resolvable by uuid). So the facade builds a
  `Map<String workflowUuid, String permissionExpression>` once via
  `integrationWorkflowService.getIntegrationWorkflows(integrationId)` (key =
  `getUuidAsString()`), evaluates each config-workflow's expression by uuid, and
  rebuilds the `IntegrationInstanceConfigurationDTO` (canonical record constructor)
  with the permitted workflows only. A `null`/absent map entry ⇒ no expression ⇒
  visible. No change to `IntegrationWorkflowDTO` is required.

### 2. `EmbeddedPermissionEvaluator`

Location: `embedded-configuration-service`, package
`com.bytechef.ee.embedded.configuration.security` (a focused, single-purpose unit).

```java
public class EmbeddedPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPermissionEvaluator.class);

    private final Evaluator evaluator;

    public EmbeddedPermissionEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public boolean evaluate(@Nullable String permissionExpression, ConnectedUser connectedUser) {
        if (permissionExpression == null || permissionExpression.isBlank()) {
            return true;
        }

        try {
            Map<String, Object> context = buildContext(connectedUser);

            Map<String, Object> evaluated = evaluator.evaluate(
                Map.of("__result", "=" + permissionExpression), context);

            return Boolean.parseBoolean(String.valueOf(evaluated.get("__result")));
        } catch (Exception exception) {
            log.warn(
                "Failed to evaluate permission expression [{}] for connected user [{}]; hiding (fail closed)",
                permissionExpression, connectedUser.getExternalId(), exception);

            return false;
        }
    }

    private Map<String, Object> buildContext(ConnectedUser connectedUser) {
        Map<String, Object> context = new HashMap<>();

        context.put("metadata", connectedUser.getMetadata());
        context.put("externalId", connectedUser.getExternalId());
        context.put("email", connectedUser.getEmail());
        context.put("name", connectedUser.getName());
        context.put("environment", connectedUser.getEnvironment().name());

        return context;
    }
}
```

Registered as a Spring bean (config in `embedded-configuration-service`, injecting
the existing `Evaluator` bean). The `Evaluator` bean comes from the core
`eval-config`; the service module already runs in the full app context.

#### Evaluation semantics

- Null/blank expression ⇒ `true` (visible; no filtering).
- Boolean-true ⇒ visible; anything else (including non-boolean) ⇒ hidden.
- Thrown error (invalid SpEL, type error) ⇒ logged WARN + hidden (**fail closed**).
- `metadata['missingKey']` returns `null` (SpEL map access does not throw), so
  `metadata['plan'] == 'pro'` yields `false` for absent keys — no special handling.
- All metadata values are strings; compare against string literals
  (`metadata['seats'] == '5'`, not `== 5`). Documented as a known sharp edge.
- `environment` is the enum **name** (e.g. `PRODUCTION`), not the ordinal.
- **Operator-only (no method calls).** The shared `SpelEvaluator` sandbox deliberately
  rejects member-method invocations (its formula-expression validator forbids the
  `.<name>(` pattern), so an expression like `email.endsWith('@acme.com')` or
  `metadata['x'].contains(...)` fails validation and therefore fails closed (hidden).
  v1 permission expressions are **operator-only**: equality/inequality (`==`, `!=`),
  comparisons (`<`, `>`, `<=`, `>=`), boolean `and`/`or`/`not`, and map access
  (`metadata['key']`). Supporting method-call syntax would require widening the shared
  sandbox whitelist — out of scope for v1, tracked as a future enhancement.

#### Example expressions

```
metadata['plan'] == 'pro'
metadata['tier'] == 'gold' or environment == 'PRODUCTION'
environment == 'PRODUCTION' and metadata['betaOptIn'] == 'true'
```

### 3. Filtering in the facade (Option A) + MCP assembly relocation

`ConnectedUserIntegrationFacadeImpl` gains the `EmbeddedPermissionEvaluator`
dependency and becomes the single source of truth for what a connected user sees.

**List** (`getConnectedUserIntegrations`):
1. Resolve `ConnectedUser` (already done).
2. For each candidate, evaluate the integration-level expression; **drop the whole
   integration** if `false`.
3. For survivors, drop any workflow whose per-workflow expression is `false`.

**Single** (`getConnectedUserIntegration`):
1. Resolve `ConnectedUser` (already done).
2. Evaluate the integration-level expression. If `false`, **throw the same
   not-found exception the facade already throws for a missing integration**, so
   the controller returns **404** — a hidden integration is indistinguishable from
   a nonexistent one.
3. Filter the workflow list by per-workflow expression before returning the DTO.

**MCP assembly relocation:** the data-gathering currently in the controller's
`populateMcpData` (MCP tools + MCP workflows) moves into the facade (or a facade
collaborator), so:
- The facade owns assembling the full integration view (regular workflows + MCP
  workflows + MCP tools) for the resolved user.
- The **same per-workflow permission filter** is applied to MCP workflows,
  closing the bypass where a workflow hidden by expression could still surface in
  the MCP list.
- The controller no longer re-resolves the `ConnectedUser` (avoids the extra DB
  round-trip + metadata query) and is reduced to model conversion.

The MCP services currently injected into the controller
(`McpComponentService`, `McpToolService`, `McpIntegrationInstanceConfigurationService`,
`McpIntegrationInstanceConfigurationWorkflowService`,
`McpIntegrationInstanceToolService`, `McpServerService`,
`IntegrationInstanceConfigurationWorkflowService`,
`IntegrationInstanceWorkflowService`, `IntegrationWorkflowService`,
`ClusterElementDefinitionService`, `WorkflowService`) move (as needed) into the
facade or a dedicated MCP-assembly collaborator in `embedded-configuration-service`.
This is the main refactor cost and is accepted in exchange for one filtering site.

> Note: the controller produces REST `*Model` objects today via MapStruct
> (`ConnectedUserIntegrationMapper`) + `ConversionService`. The relocation keeps DTO
> assembly in the facade and **model conversion in the controller**: the facade
> returns the filtered, MCP-inclusive `ConnectedUserIntegrationDTO`, and the
> controller maps DTO → model.
>
> **New service-layer DTO carriers** (no web models leak into the service layer) —
> added as nested records on `ConnectedUserIntegrationDTO`:
> - `mcpTools: List<McpToolInfo>` where `McpToolInfo(String name, String description)`
>   (integration-level MCP tools; description sourced from `ClusterElementDefinition`).
> - `mcpWorkflows: List<McpWorkflowInfo>` where
>   `McpWorkflowInfo(String label, String description, List<WorkflowInputInfo> inputs, String workflowUuid)`
>   and `WorkflowInputInfo(String name, String label, boolean required, String type)`.
> - Per instance, `ConnectedUserIntegrationInstance` gains
>   `mcpTools: List<McpInstanceToolInfo>` and `mcpWorkflows: List<McpWorkflowInfo>`
>   (the per-instance MCP tool carrier mirrors the existing instance-tool model fields).
>
> The MapStruct mapper (`ConnectedUserIntegrationMapper`) stops `ignore`-ing
> `mcpTools`/`mcpWorkflows` and instead maps these new DTO carriers to the existing
> `McpToolModel` / `IntegrationWorkflowModel` / `McpIntegrationInstanceToolModel` /
> `IntegrationInstanceWorkflowModel` web models. The **per-workflow permission filter
> is applied while the facade builds `mcpWorkflows`**, so a workflow hidden by its
> expression never reaches the MCP list.

### 4. Authoring path

**Server (GraphQL, `embedded-configuration-graphql`):**
- `integration.graphqls`: add `permissionExpression: String` to `type Integration`;
  add `extend type Mutation { updateIntegrationPermissionExpression(id: ID!, permissionExpression: String): Integration }`.
- `integration-workflow.graphqls`: add `permissionExpression: String` to
  `type IntegrationWorkflow`; add
  `extend type Mutation { updateIntegrationWorkflowPermissionExpression(integrationWorkflowId: ID!, permissionExpression: String): IntegrationWorkflow }`.
- Controllers gain `@MutationMapping` methods delegating to new
  service/facade methods that set the column
  (`IntegrationService`/`IntegrationFacade`, `IntegrationWorkflowService`/`IntegrationWorkflowFacade`).
- Keep `@ConditionalOnCoordinator` + `@ConditionalOnEEVersion` (already present).

**Client:**
- New operations in `client/src/graphql/embedded/configuration/`:
  `updateIntegrationPermissionExpression.graphql`,
  `updateIntegrationWorkflowPermissionExpression.graphql`; extend
  `integrationById.graphql` / `integrationWorkflowsByIntegrationId.graphql` to
  select `permissionExpression`. Run `npx graphql-codegen`.
- `IntegrationDialog`: add a "Permission expression" `Textarea` (integration level),
  persisted via the new mutation on save.
- `WorkflowDialog`: add a "Permission expression" `Textarea` (per workflow),
  persisted via the new mutation on save.
- Follow client conventions: `twMerge` (not `cn`), `Textarea`, alphabetical import
  destructure + sort-keys, hook ordering, interface names ending in `I`/`Props`.
  Run `npm run check` before commit.

## Error Handling

- Expression evaluation errors are caught in `EmbeddedPermissionEvaluator`, logged
  at WARN with the expression + connected-user externalId, and treated as `false`
  (fail closed).
- Hidden single integration → HTTP 404. **Note on the 404 mechanism:** rather than
  relying on the shared platform `ExceptionTranslator` (`@RestControllerAdvice`)
  having a not-found→404 mapping, we make the behavior self-contained: the facade
  throws a dedicated unchecked `EmbeddedIntegrationNotVisibleException` for a hidden
  integration, and the embedded public-rest `IntegrationApiController` catches it in
  the two single-integration endpoints and returns `ResponseEntity.notFound().build()`.
  This keeps the visibility *decision* in the facade and the *HTTP concern* in the
  web layer, and does not depend on the platform advice's exception mappings. The
  404 does not leak whether the integration is hidden vs. absent.
- GraphQL mutations validate nothing beyond persistence in v1 (no save-time SpEL
  validation); a malformed expression simply fails closed at render. (Save-time
  validation is a possible later enhancement — see Open Questions.)

## Testing Strategy

- **`EmbeddedPermissionEvaluatorTest`** (unit): null/blank ⇒ true; explicit
  true/false; missing metadata key ⇒ false; invalid SpEL ⇒ false (+ log);
  user-field expressions (`email.endsWith`, `environment == 'PRODUCTION'`);
  non-boolean result ⇒ false.
- **Facade tests** (`ConnectedUserIntegrationFacade*IntTest`): integration-level
  false omits from list; visible integration drops only hidden workflows; MCP
  workflows filtered identically; single hidden integration ⇒ not-found/404.
- **Migration**: exercised by IntTest Spring context startup.
- **GraphQL**: mutation persists the column (follow existing embedded GraphQL test
  pattern).
- Naming: unit tests end `Test`, integration tests end `IntTest`; camelCase test
  method names.

## Security Considerations

- SpEL runs in the existing sandbox (`SpelEvaluator`): no `T(...)`, constructors,
  or bean refs; whitelist-only functions. Expressions are authored by trusted
  admins via the EE admin GraphQL surface, not by connected end users.
- Fail-closed evaluation prevents a broken expression from exposing a gated
  integration/workflow.
- 404 (not 403) on hidden single integration avoids confirming existence of gated
  integrations to embedded callers.

## Affected Modules / Files (indicative)

**Server**
- `embedded-configuration-api`: `domain/Integration.java`,
  `domain/IntegrationWorkflow.java`, `dto/IntegrationDTO.java`
  (`dto/IntegrationWorkflowDTO.java` already exposes the domain object).
- `embedded-configuration-service`:
  `security/EmbeddedPermissionEvaluator.java` (new) + bean config;
  `facade/ConnectedUserIntegrationFacadeImpl.java` (filtering + MCP assembly);
  `facade/IntegrationFacadeImpl.java` / `IntegrationWorkflowFacadeImpl.java`
  (set-expression methods); Liquibase changelog (new file).
- `embedded-configuration-public-rest`:
  `IntegrationApiController.java` (slimmed; MCP gathering removed; conversion only).
- `embedded-configuration-graphql`:
  `*.graphqls` (+ fields, + mutations), `IntegrationGraphQlController.java`,
  `IntegrationWorkflowGraphQlController.java`.

**Client**
- `client/src/graphql/embedded/configuration/*.graphql` (new + edited) → regenerate
  `graphql.ts`.
- `IntegrationDialog.tsx`, `WorkflowDialog.tsx`.

## Open Questions / Future Enhancements

- **Save-time SpEL validation** in the GraphQL mutations (reject syntactically
  invalid expressions before persistence). Deferred from v1; render-time fail-closed
  covers correctness. Easy follow-up.
- **Reusing the evaluator for embedded copilot** allow-listing (sibling work on
  `0_732` already gates copilot on enabled integrations) — out of scope here but the
  facade-centered design makes it a natural future consumer.
- **Caching `ConnectedUser`** to reduce repeated lookups across embedded calls —
  orthogonal optimization, not required by this feature.
