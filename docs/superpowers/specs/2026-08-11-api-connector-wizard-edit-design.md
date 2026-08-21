# API Connector wizard editing and spec-as-source-of-truth

Date: 2026-08-11
Status: implemented (phases 1–3 shipped)
Ticket: #207

## Context

API connectors are stored as one `api_connector` row plus two gzip'd blobs in file storage:

- `specification` → `api_connectors/specifications/specification.yaml` — the OpenAPI document
- `definition` → `api_connectors/definitions/definition.json` — a serialized `ComponentDefinition`

The spec YAML is already the single source of truth: endpoints are **not** stored as rows
(`ApiConnectorEndpoint`'s `@Table` is commented out); `ApiConnectorFacadeImpl` re-parses the stored
spec on every read and synthesizes endpoint ids from SHA-256 of `path:method`. The definition is
produced by `OpenApiGenerator`, which runs the CLI's `ComponentInitOpenApiGenerator`: JavaPoet
generates component sources, javac compiles them at runtime, the handler is class-loaded once, its
`getDefinition()` is serialized to JSON, and the sources/classes are discarded. The runtime never
executes generated code — `ApiConnectorDynamicComponentHandlerRegistry` deserializes
`definition.json` and replays `action.metadata.{method,path}` + property locations through
`OpenApiClientUtils.execute`.

All three creation flows (manual wizard, spec upload, AI-from-docs) converge on the
`importOpenApiSpecification` GraphQL mutation with upsert-by-name semantics.

## Phase 1 — correctness fixes (shipped)

- `ApiConnectorServiceImpl.update` never copied the regenerated `definition` (nor `title`; the
  `icon` line was a self-assignment), so every re-import kept serving stale endpoints at runtime.
  Update now carries definition/description/icon/name/specification/title. Pinned by
  `ApiConnectorServiceTest`.
- The `icon` field of `ImportOpenApiSpecificationInput` / `GenerateFromDocumentationInput` was
  accepted by the schema and dropped by the controller; it now flows through the facade
  (null = keep existing on update).
- Blob lifecycle: replaced spec/definition blobs are deleted best-effort after a successful update
  (URL-compared — every store writes a new physical blob); `deleteApiConnector` moved to the facade
  so deleting a connector releases both blobs.
- The `apiConnectors` list query re-parsed every stored spec strictly (`IllegalArgumentException`
  on any parser warning, NPE on missing `paths`), so one bad connector broke the whole list with a
  GraphQL error. Endpoint extraction is now fail-soft (lenient parse, empty endpoint list on
  failure, warn log); the import path stays strict because it feeds code generation.
- Removed dead schema fields `startDiscoverEndpoints`, `startGenerateForEndpoints`,
  `endpointDiscoveryStatus` and their orphaned types — declared with no controller mapping and no
  client caller, so any invocation produced an unmapped-field GraphQL error.

## Phase 2 — wizard editing and continuity (shipped)

One client-side endpoint model (`EndpointDefinitionI`) with a bidirectional mapping to OpenAPI:

- `generateOpenApiSpec` (review step) — endpoints → spec. When the store carries a
  `baseSpecification`, generation merges into it (replaces `paths`/`servers` only), so top-level
  sections the wizard does not model (`components`, `securitySchemes`, `info`) survive.
- `parseSpecificationForWizard` (`specification-utils.ts`) — spec → endpoints, the inverse.
  Request/response schemas ride as opaque JSON strings, so `$ref`s round-trip untouched.
  Known-lossy by design: methods outside DELETE/GET/PATCH/POST/PUT and cookie parameters are
  dropped from the visual model; the per-endpoint YAML editor remains the escape hatch.

Flows:

- **Edit** (`api-connectors/:apiConnectorId/edit`): opens the manual wizard hydrated from the
  stored spec; name is locked because import upserts by name (a rename would fork the connector);
  the raw-YAML dialog remains as "Edit YAML".
- **Import**: `Import File → Define Endpoints → Review` — the uploaded spec hydrates the endpoint
  editor instead of jumping straight to Review.
- **AI**: `Basic Info → Select Endpoints → Define Endpoints → Review` — the selection-filtered
  spec becomes the base; save sends the regenerated spec (no save-time filtering anymore).

All three converge on manual-mode review + `importOpenApiSpecification`, i.e. upload/AI are now
"manual wizard with a pre-filled store".

## Phase 3 — direct spec→ComponentDefinition mapper (shipped)

The javac step was pure ceremony: the compiled handler was instantiated once, only to serialize
`getDefinition()`. `OpenApiComponentDefinitionFactory` (configuration-service, generator package)
now builds the `ModifiableComponentDefinition` directly from the parsed `OpenAPI` object — a
faithful port of the CLI generator's definition semantics (properties with `QUERY|PATH|HEADER|BODY`
locations, action metadata, `$ref`/`allOf` handling including the shared-PROPERTIES labeling quirk,
connection/authorization families, output schemas, sample-output conversion) — and serializes it
with the same Jackson configuration. The CLI itself is untouched; `bytechef component init` still
generates code. `OpenApiGenerator` (the runtime compile wrapper) has no production caller anymore
but is retained as the golden reference.

Equivalence is pinned by `OpenApiComponentDefinitionFactoryTest`: for specs the legacy pipeline can
process, mapper JSON must equal generator JSON as parsed trees. Function-valued fields
(`baseUri`, `authorizationUrl`, `tokenUrl`, `scopes`) serialize as `{}`/null either way, so
attaching equivalent lambdas reproduces the stored bytes exactly.

**The golden tests exposed that the legacy pipeline was already broken for two spec classes:**

1. **Specs without a `components` section** — `compileComponentHandlerSource` unconditionally
   passes `<X>Connection.java` to javac, but no connection source is generated without
   securitySchemes/servers-in-components; javac aborts (`file not found`), the class dir stays
   empty, and import dies with `ClassNotFoundException` → GraphQL error. This is the exact shape
   the manual wizard emits, i.e. wizard saves failed at import. The mapper handles these specs.
2. **OAuth2 flows that always attach scopes** (clientCredentials/implicit/password, or
   authorizationCode with scopes) — the generator emits a `Map.of(...)` scopes lambda, but the
   bundled `component-api-1.0.jar` resource predates the SDK change of `ScopesFunction` to return
   `Map<String, Boolean>` (it expects `List<String>`), so the generated code no longer compiles.
   The mapper compiles against the live SDK and handles these specs; both classes are pinned by
   mapper-only tests.

Residual divergences, all deliberate: the ByteChef-internal `x-dynamic-options` /
`x-dynamic-properties` extensions are ignored (their functions referenced always-empty generated
stubs and don't serialize anyway); crash paths of the generator (NPEs on malformed specs, cookie
parameters, multi-server option emission) become clean `IllegalArgumentException`s or sensible
output instead of uncompilable code.

## Phase 4 — follow-up hardening (shipped)

- **Rename-safe, version-checked editing**: `ApiConnectorFacade.updateApiConnector(id, name, icon,
  specification, version)` regenerates the definition by id — a rename is rejected when another
  connector holds the name (`API_CONNECTOR_NAME_ALREADY_EXISTS`), and a non-null `version` must
  match the stored row (`API_CONNECTOR_VERSION_CONFLICT`), so concurrent edits are rejected
  instead of silently overwritten. The GraphQL `updateApiConnector` mutation routes through this
  path when the input carries a `specification` (metadata-only updates keep the old path); the
  edit wizard saves through it with the loaded row version, and the name field is unlocked. The
  dead `createApiConnector` mutation (its create path asserted a definition it could never have)
  was removed.
- **By-id query returns content**: the `apiConnector(id)` GraphQL query previously resolved the
  raw entity, whose `specification`/`definition` are file-storage references, so the edit wizard
  had nothing to hydrate from. It now resolves through the facade as `ApiConnectorDTO` with the
  blob contents read, like the list query always did.
- **Endpoint dialog crash fixed**: `EndpointForm` rendered shadcn `FormField`s inside a plain
  `<form>` without the `Form` (react-hook-form provider) wrapper — every Add/Edit Endpoint click
  crashed with "Cannot destructure property 'getFieldState' of 'useFormContext(...)' as it is
  null". Pre-existing on the branch; found by the live smoke test.
- **Operation extras preserved**: regeneration copies `tags`, `security`, `deprecated`, `servers`,
  `externalDocs`, `callbacks`, and `x-*` extension keys from the base specification's matching
  path+method onto the rebuilt operation, so wizard edits no longer strip them (an endpoint whose
  path or method changes still loses them).
- **HEAD support end to end**: the schema/domain `HttpMethod` enums, the endpoint listing, the
  client parser, and the method dropdown all handle HEAD (the definition pipeline always did).
- **Legacy generator demoted to test scope**: `OpenApiGenerator` and the bundled jars moved to
  `src/test`, and the `cli:commands:component:init:openapi` dependency became `testImplementation`
  — the production classpath no longer carries the CLI generator.

Verified live (2026-08-11): manual wizard save (previously crashed at import), edit-wizard
hydration + rename + endpoint addition via `updateApiConnector`, stale-version rejection,
connector list with pre-existing data, dynamic component resolution through
`ComponentDefinitionReader` on a mapper-produced definition, and facade delete.

## Phase 5 — remaining gap closure (shipped)

- **Parameter schemas survive editing**: `ParameterDefinitionI` carries the original parameter
  schema as opaque JSON; regeneration (`buildParameterSchema`) reuses it verbatim while the edited
  type still matches, so `format`/`enum`/`default` survive untouched parameters, and a retyped
  parameter falls back to a plain `{type}`. Applied in the review-step generator and the
  per-endpoint YAML editor; parameter `example` is now emitted too.
- **Historical orphan sweep**: `deleteOrphanedApiConnectorFiles` (admin GraphQL mutation →
  `ApiConnectorFacade.deleteOrphanedFiles`) lists both storage directories via the
  `FileStorageService.getFileEntries` API and deletes blobs no row references. Deliberately
  manual — an import in flight stores blobs before its row commits, so an automatic sweep could
  race it.
- **Name uniqueness enforced at the service**: `ApiConnectorServiceImpl.update` rejects taking a
  name held by another row (`API_CONNECTOR_NAME_ALREADY_EXISTS`), covering the metadata-only
  GraphQL update path in addition to the facade's own check.

## Deferred / known gaps

None. The bundled test-scope jars were refreshed to the current SDK (they only gate what the
legacy reference can compile — the compiled classes link against the live SDK through the parent
classloader, so golden outputs were never affected), which let the OAuth2-scopes fixture graduate
from a mapper-only assertion to a full golden comparison. Refresh instructions live in the libs
directory's README.
