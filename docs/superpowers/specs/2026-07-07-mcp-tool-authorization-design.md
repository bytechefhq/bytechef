# MCP Tool-Level Authorization — Design

**Date:** 2026-07-07
**Status:** Implemented — Phase 1 (automation server enforcement), Phase 2 (admin UI + GraphQL), and Phase 3 (embedded enforcement) all shipped 2026-07-08.
**Scope:** Gate *which MCP tools a principal may see and call* by the principal's authorities, so a validated token no longer grants blanket access to every tool an MCP server exposes. Picks up the "IdP-claims → tool authorization" item ("both, configurable per tenant").

## Decisions (locked)

- **Authorities-based** — a component declares the authorities that grant access; a principal may use it only if they hold at least one.
- **Per component** — the unit of authorization is the component (all of a component's tools share its grant), matching how MCP servers are configured.
- **Deny by default** — when authorization is enforced, a component with no granting authority the principal holds is hidden.

## Why authorities (not a new claim or policy store)

A principal's authorities already come from **both** sources the "both, configurable per tenant" decision requires:
- **ByteChef roles** — API-key path (the ByteChef user's authorities) and embedded-AS JWT path (minted `authorities` claim).
- **IdP claims** — external-issuer path, via each issuer's configured `authoritiesClaim` (`McpJwtIdentityMapper`).

So tool authorization consumes an already-unified authority set; a tenant governs tool access with ByteChef roles, IdP groups, or both, purely by how its issuer is configured. No new claim contract, no parallel policy store.

## Safe rollout: enforcement is opt-in per server

Global deny-by-default would dark every existing MCP server the moment it ships. So deny-by-default is the semantics **of the feature**, and the feature is enabled per server:

- `mcp_server.enforce_tool_authorization` (boolean, default `false`).
  - `false` (legacy) → every configured tool is exposed, as today.
  - `true` → deny-by-default: a component's tools are exposed only to a principal holding one of the component's granting authorities.

This is the standard "turn on RBAC → deny by default" pattern and keeps the change non-breaking until an admin opts in.

## Config storage

- `mcp_server.enforce_tool_authorization BOOLEAN NOT NULL DEFAULT false`.
- `mcp_component.required_authorities` — the authorities that grant the component, OR semantics (principal needs at least one). Stored as a child relation `mcp_component_authority(mcp_component_id, authority)`, mirroring the existing `mcp_server_tag` child pattern (Spring Data JDBC `Set<...>`). A component with an empty set, under an enforcing server, is denied to everyone.

## Mechanism

1. **Capture authorities (request thread).** Each server's `contextExtractor` already runs inside the established security context. Add the authenticated principal's authority names to the `McpTransportContext` (key `AUTHORITIES`). (`SecurityContextHolder.getContext().getAuthentication().getAuthorities()`.)
2. **Enforce (tool-filter thread).** A shared `McpToolAuthorizationEvaluator` in `platform-mcp-server-support` decides, per component, whether the principal's captured authorities grant it. Each server's `toolFilter`, after building candidate tools, drops the tools of components the evaluator denies — but only when the server has `enforceToolAuthorization = true`.

`McpToolAuthorizationEvaluator`:
```
boolean isComponentAuthorized(Set<String> principalAuthorities, Set<String> componentRequiredAuthorities)
// enforcing caller only; returns true iff intersection is non-empty
```
The per-server enforce flag and the "which tools belong to which component" grouping stay in each server's filter (they already look up the server + components by secret).

## Authorities source per endpoint

- **automation / management** — first-party ByteChef users. Authorities are the user's roles (API-key path) or the JWT's mapped authorities (`McpJwtIdentityMapper`). Fully available today.
- **embedded** — third-party connected users. Authorities come from the IdP token's `authoritiesClaim` (external-direct). A connected user authenticated by the signing key carries no authorities, so under enforcement they see nothing — the correct deny-by-default outcome for a tenant that opts in without wiring IdP groups. (Configurable per tenant, as decided.)

## Phasing

- **Phase 1 (this pass, server):** `McpToolAuthorizationEvaluator` + domain/config (`enforce_tool_authorization`, `mcp_component_authority` + migration) + capture-and-enforce in the **automation** server + tests. Config is exercised directly in tests (no UI yet).
- **Phase 2 (UI) — IMPLEMENTED (2026-07-08):** the MCP GraphQL API exposes `McpServer.enforceToolAuthorization` (settable via `McpServerUpdateInput`) and `McpComponent.requiredAuthorities` (settable via `McpComponentWithToolsInput`) (commit `6439673`); the automation and embedded MCP-server dialogs gained an "Enforce tool authorization" toggle and the component dialogs a per-component "Required Authorities" editor (commit `be36862`). The management server has no per-component model, so no UI applies there (see below).
- **Phase 3 (embedded) — IMPLEMENTED (2026-07-08):** the embedded server now mirrors the automation enforcement. The OAuth2 converter maps the IdP token's group claim through the tenant's identity provider group-to-authority map (reusing `McpJwtIdentityMapper`) onto the connected-user principal; the embedded `contextExtractor` publishes those authorities and the `toolFilter` gates each `McpComponent` via `McpToolAuthorizationEvaluator`. Commit `33a22e84a35`. Integration-instance-configuration tools stay ungated (see below).

### What stays ungated, and why (Phase-1/3 decision)

Per-component authorization applies only to a server's **`McpComponent`** catalog — the component tools built through `FilterableMcpServerBuilder`'s per-request filter. Other tool sources are intentionally left ungated because each is already governed by a coarser boundary:

- **automation project / workspace tools** (`getMcpServerMcpProjects`, the workspace tool-callback contributors) — governed by project/workspace RBAC and the deployments the caller can reach; they are not part of the `McpComponent` authority model.
- **embedded integration-instance-configuration tools** — governed by the connected user's own integration instances (what they have connected), the embedded server's primary authorization boundary.
- **management built-in tools** (`ai-mcp-server`) — the management server exposes a *fixed* set of built-in management/copilot tools built once from its `McpServerToolCallbackContributor` beans (`ComponentTools`, `ProjectTools`, `TaskTools`, …), i.e. `.tools(...)` at build time rather than a per-request `toolFilter` over `McpComponent`s. There is no `McpComponent` catalog to attach required authorities to, so per-component authorization does not apply; the tools are already gated by the `mcp:management` scope (Phase B). Finer per-management-tool authorization would be a separate model (each built-in tool declaring a required authority) and remains out of scope.

## Testing (Phase 1)

- **Evaluator (unit):** intersection true/false; empty required set → denied.
- **Filter (unit/int):** with enforcement on, a principal holding a component's authority sees its tools; a principal without it does not; with enforcement off, all tools are exposed regardless of authorities.
- **End-to-end (int):** an automation MCP server with `enforce_tool_authorization = true` and a component requiring `ROLE_X` — a JWT carrying `ROLE_X` lists the component's tools; one without it lists none.

## Explicitly deferred (honest scope)

- Per-tool (sub-component) granularity — components are the unit; a finer split is a later enhancement.
- Deny/allow-list precedence beyond simple OR-of-authorities.
- A configurable per-IdP group-claim name — the embedded and automation/management paths both read the conventional OIDC `groups` claim.
