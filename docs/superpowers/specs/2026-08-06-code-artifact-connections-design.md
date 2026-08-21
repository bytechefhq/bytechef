# Declared Connections for Code Workflows and Custom Components — Design

Date: 2026-08-06
Status: Approved (design; decisions resolved at review, 2026-08-06)

Builds on `2026-08-05-code-perform-context-design.md` (implemented), which gave code workflow tasks
`perform(context)` with `context.component.<name>.<action>(input, connectionName)` and script custom
components `perform(inputParameters, connectionParameters, context)`.

## Problem

Connections are invisible in code-defined artifacts:

1. **Code workflows** — the generated task JSON is an opaque `codeWorkflow/v1/perform` node, so
   nothing enumerates which components (and hence which connections) the code will invoke. The
   standard "required connections" machinery (editor wiring, `WorkflowTestConfiguration`,
   `ProjectDeploymentWorkflowConnection`) sees a workflow that requires nothing; a missing
   connection surfaces only at runtime.
2. **The by-name store fallback is a stub** — `CodeWorkflowConnectionResolver` is an interface with
   no registered bean, so today a `connectionName` resolves ONLY against the task's (in practice
   empty) forwarded `componentConnections` map. The perform-context docs describe the intended
   contract, not current behavior.
3. **`environmentId` is dropped** — `CodeWorkflowTaskContext` passes `null` where the script
   component's `ScriptComponentActionInvoker` forwards `jobContextAware.getEnvironmentId()` into
   `executePerformForPolyglot`.
4. **Script custom components cannot declare a connection** — the single-file contract has no
   `connection` member, so the registered `ComponentDefinition` declares none, the editor never
   offers wiring, and the `connectionParameters` argument delivered since the perform-context work
   is always empty. (Java classloader custom components can declare connections via `ComponentDsl`;
   the Espresso describe path reports `connection` as unsupported.)

## Design

### A. Code workflows — SDK-declared connections

Tasks declare the connections they use; the declaration is emitted into the generated workflow
definition, making requirements introspectable and pluggable into the standard wiring machinery.

- **Java SDK** (`workflow-api`): `TaskDefinition` gains
  `Optional<List<? extends ConnectionRequirement>> getConnections()`;
  `WorkflowDsl.task(...).connections(connection(componentName, name), ...)` builds them. The
  `name` doubles as (a) the key the perform code passes as `connectionName` and (b) the default
  Connections-page name used for by-name auto-wiring.
- **Script contract** (JS/Python/Ruby): an optional task member
  `connections: [{componentName, name}]`, read by both loader engines during `load(...)` alongside
  name/label.
- **Definition emission**: `CodeWorkflowContainerFacadeImpl.toArrayNode` writes the declarations
  into the task JSON (sibling of the still-deferred `getParameters()` emission), e.g.
  `"connections": [{"componentName": "slack", "name": "slack-prod"}]`.
- **Consumption (per decision 2 — self-wiring, no override)**: declarations do NOT integrate with
  `WorkflowTestConfiguration`/`ProjectDeploymentWorkflowConnection`. They serve (a) introspection —
  "this workflow requires connections named X, Y" is readable from the definition (client
  surfacing of that list is a follow-up, not part of this design's server scope) — and (b)
  validation seams (e.g. a deploy/publish-time warning when a declared name has no store
  connection in the target environment). At runtime the declared name resolves through the
  by-name store resolver (section B); the forwarded `componentConnections` map remains checked
  first for compatibility but is not the vehicle for declarations.
- Declarations are advisory, not a sandbox: an undeclared `context.component` call still executes
  (matching the script component's posture).

### B. Connection resolution — implement the store fallback + environment threading

Prerequisite for A being pleasant and completion of the perform-context v1 contract:

- An EE `@Component` implementing `CodeWorkflowConnectionResolver`: resolves the current
  environment (thread-local `EnvironmentContext` bound on execution boundaries) and workspace,
  queries connections by name, filters by matching component, maps to `ComponentConnection`.
  Registered so `CodeWorkflowTaskExecutor`'s existing `ObjectProvider` picks it up.
- `CodeWorkflowTaskContext` forwards `environmentId` into `executePerformForPolyglot` instead of
  `null`, mirroring `ScriptComponentActionInvoker`.
- Resolution order stays: task-wired `componentConnections` first, then store-by-name.

### C. Custom components — declarative `connection` member (script contract)

The single-file contract gains an optional `connection` member the loader translates into a real
`ConnectionDefinition` on the registered component:

```
connection: {
    baseUri: "https://api.example.com",          // optional; context.http resolves relative URLs
    authorizations: [
        {type: "BEARER_TOKEN"},
        {                                        // OAuth2 is declarative too — the platform owns the flow
            type: "OAUTH2_AUTHORIZATION_CODE",
            authorizationUrl: "https://example.com/oauth/authorize",
            tokenUrl: "https://example.com/oauth/token",
            refreshUrl: "https://example.com/oauth/token",   // optional
            scopes: ["read", "write"]
        }
    ],
    properties: [ {name, type, label, required, ...} ]  // same property maps as action properties
}
```

- With a declared connection, the standard machinery works unchanged: the Connections page creates
  connections of this component type, the editor offers per-task wiring, and the already-plumbed
  `connectionParameters` perform argument carries the values.
- **v1 scope — all authorization types**: API_KEY, BEARER_TOKEN, BASIC_AUTH, CUSTOM, and the
  OAuth2 grant types (AUTHORIZATION_CODE, AUTHORIZATION_CODE_PKCE, CLIENT_CREDENTIALS). For the
  common static case the guest script never executes during an OAuth flow: the loader reads the
  declaration host-side and materializes a normal `ConnectionDefinition` whose url/scope seams are
  constant-returning functions — from there `platform-oauth2` runs the
  authorize/callback/token-exchange/refresh machinery exactly as it does for built-in components.
- **Function-valued seams (v1)**: `authorizationUrl`, `tokenUrl`, and `refreshUrl` accept EITHER a
  string constant OR a guest function `(connectionParameters) => string`; an authorization may also
  declare `apply: (connectionParameters) => ({headers: {...}, queryParameters: {...}})` to override
  the authorization type's default request decoration. Function values are detected by type at
  load. A function-valued seam is materialized as a host wrapper that re-evaluates the script in a
  fresh strict-sandbox context per invocation (the same pattern the perform path uses), navigates
  to the declared function, executes it with `connectionParameters` marshalled via
  `copyToGuestValue`, and validates the result shape (string for URL seams, a map with optional
  `headers`/`queryParameters` string-multimap members for `apply`; anything else fails with a
  clear error naming the seam). The URL seams are cold-path (authorization flows only). `apply` is
  hot-path — it decorates EVERY outbound request made with the connection — so a guest `apply`
  costs one script re-evaluation per request; that cost is strictly opt-in (no declared `apply` =
  the platform default at zero added cost) and is the author's trade-off, documented in the
  contract. Covers region/tenant-dependent URLs
  (`(connectionParameters) => "https://" + connectionParameters.region + ".example.com/oauth/token"`)
  and custom signing schemes.
- **Espresso Java path**: `describeComponent` learns to serialize a declared connection's static
  shape instead of flagging `connection` unsupported (same property-map JSON it already uses for
  action properties); unsupported remains the answer for connection definitions carrying real
  lambdas (dynamic URLs, custom apply/refresh) — functions cannot cross the guest boundary as data.
  This is a Java-in-Espresso limitation, not a limit of the script contract.
- Templates, docs (`custom-components.mdx` single-file contract table), and the custom-component
  build prompt gain the `connection` member.

### Non-goals

- No static analysis of perform bodies to infer component usage.
- No enforcement that `context.component` calls stay within declared connections.
- No bespoke token-acquisition/refresh LOGIC seams (the DSL's `acquire`/custom refresh functions)
  for script custom components in v1 — standard OAuth2 refresh via `refreshUrl` (constant or
  function) is covered by the platform's default machinery; only the rare fully-custom token
  lifecycle stays Java-jar territory until demand appears. The same re-eval pattern extends to
  them if needed.
- Workflow-inputs plumbing into task perform stays deferred (unchanged from the perform-context
  design).

## Testing

- SDK: `connections(...)` DSL round-trip; emission test on `CodeWorkflowContainerFacadeImpl`
  (task JSON carries declarations).
- Loader engines: script contract `connections` member parsed for JS/Python/Ruby; absent member
  keeps current shape (regression pin).
- Resolver: environment-scoped by-name resolution happy path + wrong-component name miss;
  `CodeWorkflowTaskContext` dispatch carries `environmentId` (mock verify).
- Custom components: script `connection` member → registered `ConnectionDefinition` (auth type,
  properties, baseUri); perform receives wired `connectionParameters`; legacy no-connection source
  unchanged; Espresso `describeComponent` serializes the declared connection. Function-valued URL
  seams: a guest `tokenUrl(connectionParameters)` receives the marshalled parameters and its
  string result reaches the platform seam; a guest `apply` returning
  `{headers: {"Authorization": [...]}}` decorates the outbound request and overrides the type
  default; string constants still work; a wrong-shaped result fails naming the seam; the
  functions execute in the strict sandbox.

## Decisions (resolved at review, 2026-08-06)

1. **Optional `componentVersion` on declarations** — `connection(componentName, name)` stays the
   short form (latest version, matching `getComponentDefinition(name, null)`); an overload
   `connection(componentName, componentVersion, name)` (script: an optional `componentVersion`
   member) pins the version for dispatch. Emitted into the task JSON when present.
2. **Self-wiring by name, no override** — SUPERSEDED 2026-08-06 (see Revision below). The
   decision was taken on incomplete information: the platform already had a connection-declaration
   seam, which makes explicit wiring cheaper AND better behaved than by-name self-wiring.
3. **Custom component connection versioning follows platform `connectionVersion` pinning** —
   existing connections stay pinned to the connection version they were created with; a changed
   connection shape means the author bumps the connection version, same as built-in components.
4. **Implementation order: B → A → C**, each phase landing independently.

## Implementation notes (2026-08-06)

Implemented in full (phases B, A, C), with these concretizations:

- The save/deploy warning lives in `CodeWorkflowContainerFacadeImpl.create/update` — the shared
  chokepoint both editor saves and deploy paths converge on — via an optional
  `ObjectProvider<ConnectionService>`; no bean = validation silently skipped. Existence is checked
  across ALL environments (the environment binding only exists at run time).
- `StoreCodeWorkflowConnectionResolver` resolves ACTIVE connections only — a name matching a
  revoked/pending connection behaves as a miss so the caller's connection-does-not-exist error
  fires instead of dispatching with dead credentials.
- `CodeWorkflowTaskContext` reads `environmentId` via the `instanceof ActionContextAware` idiom and
  carries the `PlatformType` from the executor; both flow into the resolver AND into
  `executePerformForPolyglot` (previously `null`).
- Custom component connection-level `properties` attach to each declared authorization (an
  authorization's own `properties` member wins); properties with NO authorizations get an implicit
  `CUSTOM` authorization as carrier, since the DSL hangs connection properties on authorizations.
- Espresso `describeComponent` extracts seam constants by invoking the functions with null
  arguments — a lambda that reads its parameters (dynamic URL) throws, and the WHOLE connection is
  reported `unsupported` (conservative; per-seam salvage was not worth the complexity). Custom
  `apply`/`acquire` on the Java-jar Espresso path are likewise unsupported.
- The Espresso engine's host-side `toConnectionDefinition` rebuild and the guest-side serializer
  are pinned by plain-JVM tests; the end-to-end Espresso boot tests remain CI-only (platform
  guard).

## Revision (2026-08-06): use the platform's ComponentConnectionFactory SPI

The original design (and decision 2) had code workflow tasks self-wire connections by name against
the connection store, via a bespoke `CodeWorkflowConnectionResolver`. That reinvented an existing
platform seam, discovered during review:

- A workflow node's `extensions.connections` map (`{<connectionKey>: {componentName,
componentVersion, authorizationRequired?}}`) is read by `ComponentConnection.of(...)` through the
  `ComponentConnectionFactory` / `ComponentConnectionFactoryResolver` chain
  (`ComponentConnectionFacadeImpl`). The Script component plugs into it via
  `ScriptComponentConnectionFactory` for exactly the same reason a code workflow task needs it:
  its connections cannot be derived from the component definition.
- That chain feeds the editor's connection UI, `WorkflowTestConfiguration`,
  `ProjectDeploymentWorkflowConnection`, and ultimately the perform action's
  `componentConnections` map — which `CodeWorkflowTaskContext` already consulted first.

**Implemented instead:** emit declarations into `extensions.connections` keyed by the declared name
(unpinned `componentVersion` resolves to the component's latest at save time) and add
`CodeWorkflowComponentConnectionFactory`. Deleted: `CodeWorkflowConnectionResolver`, its store
implementation, and the save/deploy warning that checked store-wide name existence.

**What this fixes**, beyond being less code: the by-name lookup had no workspace scoping (it
searched the whole tenant) and no defence against duplicate connection names (nothing enforces
uniqueness; `findFirst()` picked nondeterministically). Both problems disappear with explicit
wiring. It also removes the need for a bespoke "required connections" UI — the existing
**Test Configuration** dialog renders one row per declared connection; the code workflow source
editor gained a header button opening it.
