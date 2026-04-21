<!--
Sample — vendor documentation used as the page-structure template for the docs deliverable:
  - https://docs.gumloop.com/api-reference/authentication
-->

# ByteChef Public API — Token Auth + Reference Docs

**Date:** 2026-04-21
**Status:** Draft (for review)
**Owner:** Ivica Cardic
**Ticket:** https://github.com/bytechefhq/bytechef/issues/4812
**Edition:** CE + EE. Workspace / org-scoped tokens are EE-gated; personal tokens
ship to both editions.

## 1. Summary

Ship a first-class public REST API for ByteChef with **token-based authentication**,
the **UI to manage tokens**, and the first page of the **API Reference docs**
(Authentication). The linked vendor page is used only as the *structural template* for
the docs deliverable — not as an integration target.

Three deliverables must land together:

1. **Backend** — API token data model, Spring Security filter, introspection endpoint.
2. **Frontend** — Settings → API Keys UI (generate / list / revoke).
3. **Docs** — Authentication reference page + locked page template for every
   follow-up reference page.

Follow-up tickets document each resource (Workflows, Connections, Executions,
Webhooks, Data Tables, …) using the template landed here.

## 2. Motivation

Docs that describe non-existent endpoints are worthless; unused endpoints without docs
are invisible. The three deliverables are inseparable for a useful first release.

ByteChef already publishes OpenAPI via SpringDoc, but that's a schema, not a reference.
A hand-authored reference on top of the generated spec gives integrators:

- A stable URL.
- Copy-paste runnable examples.
- A narrative home for cross-cutting concerns (auth, scoping, errors, retries).

## 3. Backend — Token Auth Infrastructure

### 3.1 Data model

New Liquibase table `api_token`:

```
api_token
├── id                UUID
├── user_id           UUID            (FK user.id; creator)
├── workspace_id      UUID NULL       (NULL for PERSONAL; set for WORKSPACE / ORG)
├── name              TEXT            (human-readable label)
├── token_hash        TEXT            (argon2id or bcrypt — §9.4)
├── token_prefix      TEXT            (first 8 chars of plaintext for UI display — safe to show)
├── scope             INT             (ordinal: PERSONAL=0, WORKSPACE=1, ORGANIZATION=2)
├── last_used_at      TIMESTAMPTZ NULL
├── created_at        TIMESTAMPTZ
└── revoked_at        TIMESTAMPTZ NULL
```

Scope enum persisted as INT ordinal per repo convention (see `feedback_enum_storage`
memory). Append-only — never reorder.

### 3.2 REST surface

Under `/api/platform/internal/api-tokens`:

- `POST` — body `{ name, scope, workspaceId? }`; returns `{ id, name, token }` **once**.
  `token` is never reissued — lost means revoked + new.
- `GET` — list caller's tokens (scope-filtered); metadata only (prefix + name + dates).
- `DELETE /{id}` — revoke (soft: set `revoked_at`; token stops authenticating
  immediately via hash check).

Admin-gated (`@PreAuthorize`) for `WORKSPACE` / `ORGANIZATION` scope; `PERSONAL` is
always allowed for the caller against their own records (orphan-recovery pattern from
`demoteConnectionToPrivate`).

Audit every mutation via the existing `platform-audit` aspect; no custom plumbing.

### 3.3 Spring Security filter

- New filter before the existing form-login filter.
- If `Authorization: Bearer <token>` is present: hash the presented value, match
  against `api_token` where `revoked_at IS NULL`, install the user's principal, and
  fire-and-forget `UPDATE api_token SET last_used_at = now()`.
- On mismatch: return `401` immediately — do **not** fall through to form-login (would
  confuse integrators with an HTML redirect).
- On absence of header: pass through to the next filter (form-login still works for
  the UI).

### 3.4 Introspection endpoint

`GET /api/platform/internal/whoami` — returns:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "workspaces": [{"id": "uuid", "name": "Acme"}],
  "scopes": ["PERSONAL"]
}
```

Permission-light (any authenticated principal). Stable. No external dependencies.
This is the doc's canonical first code sample — proving auth works without requiring
any resource to pre-exist.

### 3.5 Rate limiting (decision required — §9.3)

**Option A — ship:** Bucket4j per-token limiter. Defaults: 600 req/min/token.
Response headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`,
`Retry-After` on `429`.

**Option B — defer:** document "no limits currently enforced" in the doc. Do not
invent numbers.

### 3.6 Metrics

- `bytechef_api_token_create` / `_revoke` (tag: `scope`).
- `bytechef_api_token_auth_success` / `_failure` (tag: `reason` —
  `invalid | revoked | not_found`).

Wired via `ObjectProvider<MeterRegistry>` (same pattern as
`bytechef_connection_create`).

### 3.7 Module layout

- New: `server/libs/platform/platform-api-token/` (api + service + rest submodules).
  Follow `platform-connection` as the structural template.
- New Liquibase changelog under `server/libs/config/liquibase-config/`.
- EE: if workspace/org-scoped tokens need EE-only behavior, add a remote-client stub
  under `server/ee/libs/platform/platform-api-token-remote-client/` with
  `@ConditionalOnEEVersion`.

## 4. Frontend — API Keys UI

### 4.1 Route & placement

- New route under the settings area (mirror the existing "Settings → Account" shape).
- Accessible to any authenticated user; admin-only controls (scope selector for
  `WORKSPACE` / `ORG`) gated client-side **and** server-side.

### 4.2 Views

- **List** — table: `Name`, `Scope` (badge), `Prefix` (e.g. `byte_a1b2…`),
  `Created`, `Last Used`, actions (revoke).
- **Create dialog** — fields: `Name` (required), `Scope` (select). Scope options
  filtered by caller's permissions.
- **Reveal dialog** — shown **once** after creation; plaintext token + copy button +
  explicit warning ("this will not be shown again"). Closing returns to the list.

### 4.3 GraphQL operations

In `client/src/graphql/api-token/`:

- `query ApiTokens`
- `mutation CreateApiToken(input: CreateApiTokenInput!): CreateApiTokenPayload!`
  (payload carries the one-time plaintext `token`).
- `mutation RevokeApiToken(id: ID!): Boolean!`

Regenerate via `npx graphql-codegen`. Errors surface through the existing
`useFetchInterceptor`; no per-mutation `onError` unless resetting form state.

### 4.4 Conventions

- Interface names end in `I` or `Props`.
- Object keys sorted alphabetically.
- Lucide icons imported with `Icon` suffix (`KeyIcon`, `CopyIcon`, `TrashIcon`).
- `twMerge` for conditional classes.

## 5. Docs — Authentication Page + Template

### 5.1 Canonical page template (locked here, reused for every follow-up)

Every API Reference page, in this order:

1. One-sentence purpose.
2. Conceptual overview (1–2 paragraphs).
3. Request details — header names, URL shape, required/optional params, quoted
   verbatim from the schema.
4. Code samples — minimum `cURL`, `Python`, `JavaScript`; each copy-paste runnable
   with obvious placeholder tokens.
5. Example response (pretty-printed JSON).
6. Common errors table — status / meaning / cause / fix.

Lock this in `docs/reference/reference-contributing.md` before adding more pages so
authors don't drift.

### 5.2 Authentication page — content

Applies the template to auth:

- **Methods** — Bearer API token (primary); session cookie (UI-only, mentioned so
  integrators don't get confused by DevTools).
- **Obtaining credentials** — UI path from §4 (*Settings → API Keys → New Token*).
- **Key scoping** — Personal / Workspace / Organization; CE sees Personal only.
- **Header format** — verbatim:
  ```
  Authorization: Bearer <token>
  Content-Type: application/json
  ```
- **Base URL** — `https://<your-instance>/api`; note self-hosted vs. cloud host
  variance.
- **Code samples** — all three languages, targeting `GET /whoami` (§3.4). Obvious
  `$BYTECHEF_TOKEN` placeholder.
- **Common errors** — 401 (bad/missing/whitespace), 403 (scope mismatch), 429
  (only if §3.5 Option A ships).

### 5.3 Placement & tooling

Three options; pick one before authoring:

1. **In-repo `docs/`** rendered by the existing site generator — zero new tooling if
   one is already in use.
2. **SpringDoc + Redoc overlay** at `/api-docs`.
3. **External docs vendor** (Mintlify / Readme.com) — fastest, external lock-in.

Recommendation: (1) if a generator is already in use; (2) otherwise. (3) only with
explicit leadership opt-in.

## 6. Testing

### 6.1 Backend

- Unit: token hashing round-trip, revocation semantics, scope filter predicate.
  Test class names end in `Test`, drop `Impl` from names.
- Integration: `ApiTokenServiceIntTest` — full CRUD against Testcontainers PostgreSQL.
  Spring Security filter test that asserts 401 on revoked token without falling
  through to form-login.
- Audit: verify `platform-audit` aspect fires on create/revoke.

### 6.2 Frontend

- Zustand store tests reset state in `beforeEach`.
- Vitest integration test for the reveal dialog (asserts plaintext shown once,
  clearable from DOM on close).
- PostHog mock per `.vitest/setup.ts`.

### 6.3 Docs

- Link-check — every relative link resolves; every external link returns 2xx.
- Sample execution — manual checklist in the PR (or CI step) that runs each code
  sample against a dev instance and asserts 2xx.

## 7. Sequencing

1. **Backend 3.1–3.4** — token model + Security filter + `/whoami`. Unblocks the
   other two deliverables.
2. **Frontend 4** — API Keys UI on top of the backend.
3. **Backend 3.5** — rate limiter (or explicit defer decision).
4. **Docs 5.1–5.2** — page template + Authentication page, targeting the
   now-existing endpoints.

Ship 1–2 behind a feature flag if the docs aren't ready, so the backend doesn't sit
exposed without a user-facing path to generate a token.

## 8. Out of Scope (phase 1)

- Endpoint-by-endpoint reference pages beyond Authentication — follow-up tickets.
- OAuth 2.0 / OIDC client-credentials flow for the public API.
- Published SDKs (Python/JS) — mentioned in docs but tracked separately.
- Token expiration / automatic rotation — tokens are long-lived until revoked.
- Per-token scope narrowing (read-only tokens, workflow-specific tokens) — v2.

## 9. Open Questions

1. **Existing PAT infra** — does `platform-user` already have a personal-access-token
   model we should extend, or is this net-new? Confirm before Liquibase authoring.
2. **Docs tool** — Hugo / Astro / Docusaurus / Mintlify already in use?
3. **Rate limiting** — ship limiter in phase 1 (§3.5 Option A) or defer (Option B)?
4. **Token hashing** — argon2id (modern, slower setup) vs. bcrypt (already in repo
   for passwords)? Consistency vs. modernity.
5. **Self-hosted vs cloud parity** — does the same token/header format work in both?
   Expected yes, but confirm.
6. **Workspace vs Organization token scoping** — do we need both distinct ordinals
   at phase 1, or collapse `ORGANIZATION` into a follow-up with EE connection
   visibility (Epic 1)?

## 10. Related ByteChef Context

- SpringDoc OpenAPI 3.0.0 is already wired (CLAUDE.md "Additional Tools").
- Connection visibility model (`PRIVATE | WORKSPACE | PROJECT | ORGANIZATION`) in
  `ConnectionFacadeImpl` — token scope mirrors this deliberately so users see one
  consistent mental model.
- Audit aspect pattern from `platform-audit-service` — reuse for token mutations.
- Enum persistence convention (INT ordinals, append-only) — `feedback_enum_storage`
  memory.
- EE permission groups (Epic 1 of
  `2026-04-21-enterprise-features-spec.md`) — will feed §4 scope filtering once
  landed.
