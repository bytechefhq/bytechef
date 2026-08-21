# API connectors (EE)

Spec-as-source-of-truth: the definition pipeline, the two write paths, and the client wizard.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### API connectors (EE, spec-as-source-of-truth, 2026-08-11)

`server/ee/libs/platform/platform-api-connector`. One `api_connector` row plus two gzip'd blobs:
`specification.yaml` (the OpenAPI document — THE source of truth) and `definition.json` (a serialized
`ComponentDefinition` derived from it). Endpoints are never rows (`ApiConnectorEndpoint`'s `@Table` is
commented out) — they're re-derived from the spec on every read, ids = SHA-256 of `path:method`.

- **Definition pipeline is a direct mapper, not javac**: `OpenApiComponentDefinitionFactory`
  (configuration-service, generator package) builds the `ModifiableComponentDefinition` straight from the
  parsed `OpenAPI`. The old generate-compile-classload wrapper (`OpenApiGenerator`) lives in TEST scope
  only, as the golden reference `OpenApiComponentDefinitionFactoryTest` compares tree-equal JSON against —
  its bundled `src/test/resources/libs` jars gate compilation only (runtime links to the live SDK via the
  parent classloader); refresh procedure is in that directory's README. Function-valued definition fields
  (`baseUri`, OAuth URLs, `scopes`) serialize as `{}`/null — the runtime replays
  `action.metadata.{method,path,bodyContentType}` through `OpenApiClientUtils`, it never reads them.
- **Two write paths, different keys**: `importOpenApiSpecification` upserts BY NAME (all three creation
  wizards); `updateApiConnector(id, input)` with a `specification` is the rename-safe path — name conflicts
  throw `API_CONNECTOR_NAME_ALREADY_EXISTS`, a stale `input.version` throws
  `API_CONNECTOR_VERSION_CONFLICT`. `ApiConnectorServiceImpl.update` enforces name uniqueness for every
  caller (names are the runtime registry's lookup key). Blob lifecycle: replaced blobs are deleted on
  update, delete releases both, and `deleteOrphanedApiConnectorFiles` (admin mutation) sweeps historical
  orphans — deliberately manual, since an import stores blobs before its row commits.
- **Reads must go through the facade DTO**: the raw entity's `specification`/`definition` are `FileEntry`
  blob references — wiring a GraphQL resolver to the entity compiles fine and silently serves no content
  (that bug broke edit-wizard hydration once). List endpoint extraction is fail-soft: an unparseable
  stored spec degrades to zero endpoints instead of failing the whole `apiConnectors` query.
- **Client wizard**: one endpoint model with a bidirectional spec mapping — `generateOpenApiSpec` (review
  step) ⇄ `parseSpecificationForWizard` (`specification-utils.ts`). All four flows (manual, import, AI,
  edit) converge on manual-mode review; upload/AI hydrate the endpoint editor before Review. The store's
  `baseSpecification` makes regeneration a merge: top-level sections (`components`, `securitySchemes`) and
  per-operation `tags`/`security`/`x-*` survive; body/response schemas and parameter schemas ride as
  opaque JSON strings (`$ref`s round-trip; a retyped parameter falls back to plain `{type}`). Known-lossy:
  methods outside DELETE/GET/HEAD/PATCH/POST/PUT and cookie parameters drop from the visual model — the
  per-endpoint YAML editor is the escape hatch.

Spec with the full decisions log: `docs/superpowers/specs/2026-08-11-api-connector-wizard-edit-design.md`.
