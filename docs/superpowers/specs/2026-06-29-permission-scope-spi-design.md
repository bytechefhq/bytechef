# Permission scopes via per-module SPI (delete the central enum)

- **Date:** 2026-06-29
- **Branch:** 0_732
- **Status:** Implemented (Phases 1–2c + client landed; see §8)
- **Author:** Ivica Cardic

## 1. Motivation

`PermissionScope` is a single EE enum in `automation-configuration-api` that hardcodes **every** module's
capabilities (workflow, execution, connection, deployment, project, API key, workspace, data table, knowledge base,
AI gateway, MCP). Adding a feature module means editing this central enum *and* `BuiltInRoleScopes`, coupling unrelated
modules to one file. We want each module to **own and contribute its own scopes** via an SPI — the same pattern already
used for `ResourceOwnershipResolver`.

## 2. Current state

- `PermissionScope` (EE enum, 33 values) `implements PermissionScopeType` (CE marker).
- `BuiltInRoleScopes` (EE) hardcodes `WorkspaceRole → Set<PermissionScope>`: explicit VIEWER set, EDITOR = VIEWER ∪ delta,
  ADMIN = `EnumSet.allOf`.
- `PermissionService.hasWorkspaceScope(workspaceId, String scope)` checks a scope **name** against the caller's role's
  scope set; `hasResourceScope` resolves the owning workspace then calls `hasWorkspaceScope`.
- Custom roles persist scope **names** in `custom_role_scope.scope` (VARCHAR) — already name-based, not ordinal.
- `@PreAuthorize` references scopes as **string literals** (`'CONNECTION_DELETE'`) — already stringly-typed.
- `hasWorkspaceScopeTyped(<E extends Enum<E> & PermissionScopeType> E scope)` + the `PermissionScopeType` marker exist
  **only** because `PermissionScope` is an enum (compile-time scope safety for internal Java callers).
- `EnumOrdinalPinTest` pins ordinals; `client/.../useHasWorkspaceScope.ts` hand-mirrors the names as a TS union.

## 3. Target design

### 3.1 SPI (EE)

Lives in EE `automation-configuration-api` (`com.bytechef.ee.automation.configuration.security`):

```java
public interface PermissionScopeProvider {
    Set<ScopeDefinition> scopeDefinitions();
}

// minimumRole = the lowest built-in WorkspaceRole that is granted the scope.
record ScopeDefinition(String name, WorkspaceRole minimumRole) {}
```

Each module contributes one `@Component @ConditionalOnEEVersion PermissionScopeProvider` declaring its scopes and the
built-in tier each belongs to. Scope **names stay prefixed** (`CONNECTION_DELETE`) — the prefix is the granularity unit
(see the 2026-06-29 prefix discussion); the SPI changes *who declares* them, not *what they are*.

### 3.2 Registry + computed `BuiltInRoleScopes`

A `PermissionScopeRegistry` (EE) aggregates `List<PermissionScopeProvider>` into:
- `Set<String> allScopeNames()` — for custom-role validation.
- `Set<String> scopesForRole(WorkspaceRole role)` — every definition where `role.hasAtLeast(def.minimumRole())`.

`BuiltInRoleScopes.getScopesForRole(WorkspaceRole)` is **retained as the public contract** but re-backed by the
registry instead of the static `EnumMap`. The `VIEWER ⊂ EDITOR ⊂ ADMIN` invariant now holds *by rank monotonicity*:
a scope with `minimumRole = VIEWER` is returned for VIEWER/EDITOR/ADMIN, `EDITOR` for EDITOR/ADMIN, `ADMIN` for ADMIN
only — there is no explicit-delta construction to get wrong. `ADMIN` no longer means `allOf(enum)`; it means "every
registered scope" (same result, computed).

Because the registry is built once at startup from beans, `getScopesForRole` must read the aggregated map (not a static
initializer). Either inject the registry into `PermissionServiceImpl` directly (preferred — drop `BuiltInRoleScopes` as
a class) or have `BuiltInRoleScopes` delegate to an injected registry. **Preferred: delete `BuiltInRoleScopes`** and
fold `scopesForRole` into the registry, since its static-map identity is exactly what we're removing.

### 3.3 Scope ownership inventory (provider → scopes @ tier)

| Provider (EE, in/near module) | VIEWER | EDITOR | ADMIN |
|---|---|---|---|
| Workflow | `WORKFLOW_VIEW` | `WORKFLOW_CREATE/EDIT/DELETE` | — |
| Execution | `EXECUTION_VIEW` | `EXECUTION_DELETE` | — |
| Connection | `CONNECTION_VIEW` | `CONNECTION_EDIT/DELETE/USE` | — |
| Deployment | `DEPLOYMENT_VIEW` | `DEPLOYMENT_PUSH/PULL/EDIT` | — |
| Project | — | `PROJECT_CREATE` | `PROJECT_SETTINGS`, `PROJECT_DELETE` |
| API key | `API_KEY_VIEW` | `API_KEY_CREATE/DELETE` | — |
| Workspace | `WORKSPACE_VIEW` | — | `WORKSPACE_MANAGE`, `WORKSPACE_MEMBER_MANAGE` |
| Data table | `DATA_TABLE_VIEW` | `DATA_TABLE_CREATE/EDIT` | — |
| Knowledge base | `KNOWLEDGE_BASE_VIEW` | `KNOWLEDGE_BASE_CREATE/EDIT` | — |
| AI gateway | `AI_GATEWAY_VIEW` | — | — |
| MCP | `MCP_VIEW` | `MCP_CREATE/EDIT` | — |

This table is exactly the current `BuiltInRoleScopes` tiers, re-expressed as per-scope `minimumRole`.

## 4. The CE-resource-module wrinkle (key decision)

`WorkspaceRole` is EE, but several scope-owning modules are **CE** (`automation-data-table`,
`automation-knowledge-base`, `automation-ai-mcp`). A provider that references `WorkspaceRole.VIEWER` therefore needs an
EE classpath — so it **cannot** live directly in those CE modules. Options:

- **(A) EE companion provider per CE module.** Each CE resource module gains a tiny EE submodule (à la `*-remote-client`)
  hosting its `PermissionScopeProvider`. Cleanest ownership, most module churn.
- **(B) Group CE-module providers in the EE configuration module.** One EE class per domain under
  `automation-configuration` (EE) declares data-table/KB/MCP scopes. Less churn, but partially re-centralizes the very
  thing we're decoupling.
- **(C) CE-neutral tier in the SPI.** Put `PermissionScopeProvider`/`ScopeDefinition` in **CE** api with the tier as a
  String (`"VIEWER"`) or a small CE enum; the EE registry maps it to `WorkspaceRole`. Providers can then live in the CE
  modules that own the scopes — true per-module ownership for *every* module, no EE submodules. Trade-off: the SPI is
  CE-visible (but RBAC stays EE-enforced), which slightly contradicts "EE-side providers".

**Recommendation:** (A) for the genuinely EE modules (workspace, AI gateway, project/workflow/connection/deployment/
api-key live in the EE/core config already), and accept (B) for the three CE-only resource modules unless we want the
extra submodules. Revisit (C) if we'd rather every module own its provider without new submodules — it's the most
faithful to "each module brings them in" even though the SPI type sits in CE.

**Decision (implemented, Phase 2b):** none of the above as literally drawn — all eleven providers were **co-located in
`automation-configuration-service` (EE)**, one class per domain, under `…security.scope`. Rationale: `PermissionScopeRegistry`
aggregates only the providers on the *running app's* classpath, and it lives in `automation-configuration-service` (present
in every RBAC-enforcing app). Scattering providers into per-domain modules (option A) or `*-remote-client` modules risks
an app loading the registry without a domain's provider, silently dropping that domain's built-in role scopes and
rejecting valid custom-role scope names (custom roles are tenant-global). Co-location guarantees the registry is always
complete. This decentralizes the *god-class* into per-domain classes and removes the enum dependency — the stated goal —
without the module churn or the classpath-completeness hazard. The knowledge-base and AI-MCP modules have no EE submodule
today, so option A would also have required creating two new gradle modules.

## 5. What gets deleted / changed

- **Delete** `PermissionScope` enum and `EnumOrdinalPinTest` (no enum, no ordinals; persistence already by name).
- **Delete** `BuiltInRoleScopes` (folded into the registry) — or keep the `getScopesForRole` signature as a thin
  registry-backed shim if callers are widespread.
- **Remove** `PermissionScopeType` marker and the `hasWorkspaceScopeTyped` typed convenience variant — they require an
  enum. Internal Java callers use the String `hasWorkspaceScope(id, "SCOPE")`. (Confirmed acceptable.)
- **Unchanged:** `@PreAuthorize` annotations (string literals), `custom_role_scope` persistence (by name), the evaluator,
  `hasResourceScope`/`hasWorkspaceScope` signatures.

## 6. Custom-role validation

Validation of a custom role's scope set moves from "is it a `PermissionScope` enum value?" to "is it in
`registry.allScopeNames()`?". A name contributed by no provider is rejected at custom-role create/update. (A name that
*was* valid but whose provider is removed becomes inert on existing rows — same name-based behavior as today.)

## 7. Client (fast-follow, separate phase)

`useHasWorkspaceScope.ts` hardcodes the `WorkspaceScopeType` union. The original plan was to **fetch** the scope names
(a GraphQL query over `registry.allScopeNames()`) so the client cannot drift.

**Resolved differently (implemented):** `WorkspaceScopeType` is a *compile-time* TS union typing the hook's `scope`
argument — a type cannot be sourced from a runtime fetch. It is also not a *mirror* of the server set but a curated
**allowlist** of the scopes the client actually gates on, so it does not drift when a server module adds an
unused-by-client scope (you add a union member only when you write a gate). The *runtime granted* scopes — the thing
that genuinely must track the server — already flow through the existing `myWorkspaceScopes` query, which the server
now backs with `registry.getAllScopeNames()` for admins. A GraphQL "all scope names" query would only be needed by a
UI that renders the full available-scope set (e.g. a custom-role editor); no such UI exists yet, so building the query
now would be dead code. The client change was therefore limited to correcting the union's doc comment (it referenced
the deleted `PermissionScope.java`) and reframing it as an allowlist, with a forward-pointer to add the registry-backed
query if/when a scope-listing UI lands.

## 8. Phasing

1. **SPI + registry** (EE): introduce `PermissionScopeProvider`, `ScopeDefinition`, `PermissionScopeRegistry`; wire
   `PermissionServiceImpl` to the registry; keep the enum temporarily as a single provider to prove the registry.
2. **Decentralize**: move scope declarations into per-module/per-domain providers (table in §3.3); delete the enum,
   `BuiltInRoleScopes`, `EnumOrdinalPinTest`, `PermissionScopeType`, the typed variant.
3. **Custom-role validation** against the registry.
4. **Client** GraphQL fetch (fast-follow).

### Implementation log (landed on `0_732`)

Phases 2 and 4 were split for review and committed separately:

| Phase | Commit | Summary |
|---|---|---|
| 1 — SPI + registry + bridge provider | `88b69c5` | `PermissionScopeProvider`, `ScopeDefinition`, `PermissionScopeRegistry`, and a single `CorePermissionScopeProvider` bridging the enum + `BuiltInRoleScopes`. |
| 2a — scope runtime → `Set<String>` | `c13b197` | Custom-role domain/SPI/pipeline/service/GraphQL/remote-stub moved off `Set<PermissionScope>`; `WorkspaceScopeCacheService` resolves built-in scopes via the registry; custom-role scope validation moved to `CustomRoleServiceImpl` against `getAllScopeNames()`. Enum kept only as the bridge provider's source. |
| 2b — per-domain providers | `6df6c4e` | 11 per-domain providers (Workflow, Execution, Connection, Deployment, Project, ApiKey, Workspace, DataTable, KnowledgeBase, Mcp, AiGateway) **co-located in `automation-configuration-service`** (see §4 Decision); `CorePermissionScopeProvider` deleted. `PermissionScopeRegistryTest` pinned the aggregate against the still-present enum/`BuiltInRoleScopes` as a regression oracle. |
| 2c — delete the enum | `3d05d0f` | Deleted `PermissionScope`, `BuiltInRoleScopes`, `BuiltInRoleScopesTest`, `PermissionScopeType`, and `hasWorkspaceScopeTyped`. Kept `WorkspaceRole`/`WorkspaceRoleType`/`hasWorkspaceRoleTyped` (role enum persists as an INT ordinal). `EnumOrdinalPinTest` trimmed to `WorkspaceRole`; registry test oracle externalized to explicit expected sets. |
| 4 — client | `449c999` | `WorkspaceScopeType` doc comment corrected and reframed as a curated allowlist (see §7 resolution). No GraphQL query added (no consumer). |

Follow-up: `custom-role.graphqls` input descriptions updated to reference the SPI instead of the deleted enum
(separate commit).

## 9. Risks & non-goals

- **Startup-order**: the registry must aggregate all providers before first authorization; standard Spring bean
  collection injection handles this (providers are `@Component`s).
- **Subset invariant**: holds by rank — but a provider could declare an inconsistent tier (e.g. `*_VIEW`@ADMIN). Add a
  registry self-check/test that VIEWER ⊆ EDITOR ⊆ ADMIN and that no name is declared twice with different tiers.
- **Lost compile-time scope checks** for internal Java callers (typed variant gone). Annotations were already string-based.
- **Non-goal:** changing scope names, the `@PreAuthorize` token shape, or the evaluator. This is purely *where scopes are
  declared*.
- **Non-goal:** CE behavior — CE contributes no providers (RBAC is EE; CE `hasResourceScope` stays permissive/owner-
  isolated as designed).
