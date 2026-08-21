# Connection credential store — frontend UX

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-19 | **Last updated:** 2026-05-19 | **Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Sibling spec:** [2026-05-19-connection-credential-store-design.md](2026-05-19-connection-credential-store-design.md) (backend; PRs 1-4 already shipped on `claude/amazing-brahmagupta-12e13d`)

## Why

The backend now supports external credential stores (AWS Secrets Manager, HashiCorp Vault) alongside the default Database store. Operators activate at most one external store via `bytechef.connection.credential-store.external.provider`. When configured, end users must be able to:

1. Choose which store backs a new connection (when the operator has activated an external).
2. Register a connection against a credential that already exists in the external store — required for read-only deployments where operators provision secrets out-of-band via Terraform / `aws cli` / `vault write`.

The frontend changes are narrowly scoped to `ConnectionDialog.tsx`. Other UI surfaces (Settings, connection list) are deliberately untouched — see Non-goals.

## What

Modify `client/src/shared/components/connection/ConnectionDialog.tsx` to conditionally render a credential-storage picker and a "Register existing credential" toggle. Behavior adapts to backend state at render time via `useConnectionCredentialStoresQuery`:

- **Single-store deployment** (DATABASE only): dialog renders unchanged from today.
- **Two-store deployment** (DATABASE + one external): picker visible; toggle visible when external selected.
- **Two-store, external read-only**: picker visible; selecting the external auto-enables and locks the "register existing" toggle.

Two backend surfaces are added in the same PR — both delegate to the same `ConnectionService.registerExisting` service method (added in PR 1):
- A REST endpoint `POST /api/v1/connections/register-existing` — part of the **public API** so external programmatic consumers can register connections backed by externally-provisioned secrets.
- A GraphQL mutation `registerExistingConnection` in `platform-connection-graphql` — used by the **internal UI** (ConnectionDialog).

This matches the ByteChef convention: REST for the public API, GraphQL for internal-only UI operations.

## Goals

1. Zero UI difference for deployments without an external store — the new code paths short-circuit on `stores.length === 1`.
2. A clear, single mental model for credential storage choice: one picker, one optional toggle, no separate "register" entry point.
3. Read-only deployments have a graphical path to onboard pre-existing secrets — they don't need to fall back to API calls.
4. Backend protection holds even if the UI is bypassed: the `ReadOnlyCredentialStoreException` from PR 1 still fires server-side, surfaced as a form-level error.
5. Existing connections are unaffected — picker is disabled on edit, toggle is hidden.

## Non-goals

- **Settings > Credential Stores page.** Deferred. Operator-level config (which store is active, read-only flag, vault auth, path templates) lives in `application.yml`; a future spec may expose status / health here.
- **Connection list page changes.** The existing credential-status badge (`VALID`/`INVALID` based on `Connection.credentialStatus`) already surfaces stale credentials. `TokenRefreshHandler` from PR 1 marks the status `INVALID` when token refresh fails on a read-only store — no new badge needed.
- **Editing the credential store backing of an existing connection.** Would require server-side migration tooling (separate follow-up per backend spec).
- **Bulk import / migration UI.** Operators using the documented manual workaround (decrypt + provision externally + update row) don't need GUI assistance in v1.
- **Provider-specific configuration UI.** Vault auth methods, path templates, AWS region — operator-only config in `application.yml`.

---

## Scope and architecture

**Single-component change.** `ConnectionDialog.tsx` (~700 lines today) gains:
- Two pieces of form state: `credentialStoreType` (default `DATABASE`) and `registeringExisting` (default `false`).
- One conditional `FormField` block (picker + toggle + register-existing fields).
- Branched submit: either `createConnection` mutation (with new `credentialStoreType` field) or new `registerExistingConnection` mutation.

**Backend companion changes** (in same PR series):
- REST: `POST /api/v1/connections/register-existing` (public API). OpenAPI spec change drives server skeleton + TypeScript client codegen.
- GraphQL: `registerExistingConnection` mutation in `platform-connection-graphql` (internal UI). The codegen-produced `useRegisterExistingConnectionMutation` hook is what `ConnectionDialog` consumes.
- Add `credentialStoreType` (optional, default `DATABASE`) to BOTH the existing REST connection create request body schema AND the existing GraphQL `createConnection` mutation input (if one exists — otherwise, the REST flow handles it).
- Both backend surfaces delegate to the existing `ConnectionService.registerExisting` service method (already in PR 1) — duplication is at the transport boundary only.

## Dialog UX detail

### Picker visibility logic

```
useConnectionCredentialStoresQuery() → stores: [{type, readOnly}]

if (stores.length === 1):
    no picker, no toggle — dialog identical to today
else if (stores.length === 2):
    show picker (Select)
    if user picks external AND external.readOnly:
        force registeringExisting = true, disable toggle
    else if user picks external:
        show toggle (default OFF, user-controllable)
    else: // DATABASE selected
        toggle hidden
```

### Picker placement

Insert a new `FormField` after the existing **Connection name** field (around line 545 of `ConnectionDialog.tsx`), before the authorization-type selector. Uses the existing shadcn `Select` matching surrounding form fields.

```
┌─────────────────────────────────────────────────┐
│ Component         [acme-api ▾]                  │
│ Name              [my prod connection      ]    │
│ Credential storage [Database ▾]                 │  ← NEW (conditional)
│                     ├ Database (default)        │
│                     └ AWS Secrets Manager       │
│                                                 │
│ Authorization type [API Key ▾]                  │
│ API Key           [••••••••••••••]              │
│ ...                                             │
└─────────────────────────────────────────────────┘
```

Option labels are friendly forms of the enum — mapping lives in `connectionCredentialStoreLabels.ts`:

```typescript
export const connectionCredentialStoreLabels: Record<ConnectionCredentialStoreType, string> = {
    [ConnectionCredentialStoreType.Database]: 'Database',
    [ConnectionCredentialStoreType.AwsSecretsManager]: 'AWS Secrets Manager',
    [ConnectionCredentialStoreType.HashicorpVault]: 'HashiCorp Vault',
};
```

### "Register existing credential" toggle

Renders below the picker when an external store is selected:

```
Credential storage  [AWS Secrets Manager ▾]
☐ Register existing credential
  ↳ My credential already exists in AWS Secrets Manager;
    I'll provide the reference.
```

State machine:
- External store read-write, toggle OFF (default): normal create — ByteChef writes credentials to the vault.
- External store read-write, toggle ON: register-existing mode — user provides a path/UUID.
- External store read-only: toggle forced ON and disabled (locked). Info `Alert` explains: "This credential store is configured read-only by your administrator. Provision the secret externally, then reference it here."

### Register-existing form mode

When `registeringExisting = true`, the dialog **hides** the normal credential fields (API key, OAuth client id/secret, etc.) and **shows** one new field:

```
Credential reference  [bytechef/connections/abc-uuid-...]
                       ↑ The path or UUID where your secret lives in the
                         external store. Format depends on your
                         operator's path template configuration.
```

Submit calls the new `registerExistingConnection` GraphQL mutation via a parallel `useRegisterExistingConnectionMutation` hook (added alongside `useCreateConnectionMutation` in `client/src/shared/mutations/automation/connections.mutations.ts` — the file becomes a mix of REST and GraphQL mutation hooks, which is fine: it reflects ByteChef's dual-API model). Backend probes the external store for the reference; on missing-secret, returns a typed error which the dialog surfaces as a Form-level error banner: "No secret found at that reference. Check the path with your administrator."

### Edit-mode behavior

- **Picker disabled.** Changing the store backing of an existing connection would require migration (out of scope per backend spec). Tooltip on hover: "Credential storage cannot be changed after creation."
- **Toggle hidden.** Re-registering an existing connection isn't a thing — the row already has its `credentialRef`.

### Loading + error states

| Condition | Behavior |
|---|---|
| `useConnectionCredentialStoresQuery` loading | Render picker section in a skeleton state (matches shadcn Skeleton); block submit until resolved. Typical resolve time < 50ms in practice. |
| `useConnectionCredentialStoresQuery` error | Fall back to today's behavior — no picker, default DATABASE. Log to console. Don't block users from creating connections because a metadata fetch failed. |
| Submit returns `ReadOnlyCredentialStoreException` | Form-level error banner naming the store. Should be unreachable through UI (defense-in-depth). |
| `registerExistingConnection` returns missing-secret | Form-level error: "No secret found at that reference. Check the path with your administrator." |

---

## Backend REST additions

The existing connection create flow uses REST (`ConnectionApi.createConnection`, generated from OpenAPI specs in the `automation-swagger` module). The new flow stays REST-consistent.

### New endpoint

```
POST /api/v1/connections/register-existing
Content-Type: application/json

{
    "componentName": "acme-api",
    "connectionVersion": 1,
    "name": "my prod connection",
    "environmentId": 1,
    "credentialStoreType": "AWS_SECRETS_MANAGER",
    "credentialRef": "bytechef/connections/abc-uuid-...",
    "tags": []
}
```

Response: same `ConnectionModel` shape the existing create endpoint returns.

The controller method delegates to the existing `ConnectionService.registerExisting(connection, storeType, credentialRef)` service method (added in PR 1). On `ReadOnlyCredentialStoreException` or external store probe failure, returns HTTP 4xx with a typed error body the client surfaces as the form-level error.

**Open item for plan phase**: locate the existing REST controller for connection create (likely under `server/libs/automation/automation-configuration-rest/.../web/rest/ConnectionApiController.java` — confirm). Mirror its layout for the new endpoint. The OpenAPI spec change drives both server skeleton (via swagger-codegen) and the TypeScript client (via the same generator).

### Existing endpoint update

The existing connection-create request body schema gets a new optional field:

```yaml
ConnectionModel:
  properties:
    credentialStoreType:
      type: string
      enum: [DATABASE, AWS_SECRETS_MANAGER, HASHICORP_VAULT]
      default: DATABASE
```

Default `DATABASE` for back-compat with existing clients. When clients don't send it, the connection gets DB-backed credentials — today's behavior.

---

## i18n entries

New keys added to `client/src/locales/en/messages.po` (Lingui format):

| Key | Translation |
|---|---|
| `Credential storage` | (picker label) |
| `Database` | (DATABASE option) |
| `AWS Secrets Manager` | (AWS_SECRETS_MANAGER option) |
| `HashiCorp Vault` | (HASHICORP_VAULT option) |
| `Register existing credential` | (toggle label) |
| `My credential already exists in {storeName}; I'll provide the reference.` | (toggle help) |
| `This credential store is configured read-only by your administrator. Provision the secret externally, then reference it here.` | (read-only alert) |
| `Credential reference` | (field label in register-existing mode) |
| `The path or UUID where your secret lives in the external store. Format depends on your operator's path template configuration.` | (field help) |
| `No secret found at that reference. Check the path with your administrator.` | (error message) |
| `Credential storage cannot be changed after creation.` | (edit-mode picker tooltip) |

Used via Lingui's `<Trans>` macro or `` t`` `` template. Engineer should match the pattern of existing strings in `ConnectionDialog.tsx`.

---

## Testing

### Component tests (Vitest + React Testing Library)

| # | Scenario | `useConnectionCredentialStoresQuery` mock | Assertions |
|---|---|---|---|
| 1 | Single-store deployment | `[{DATABASE, false}]` | Picker NOT rendered; existing fields render as today |
| 2 | Two-store, external read-write | `[{DATABASE, false}, {AWS_SECRETS_MANAGER, false}]` | Picker renders with both options; selecting AWS shows toggle (default OFF); toggling ON swaps to register-existing form |
| 3 | Two-store, external read-only | `[{DATABASE, false}, {HASHICORP_VAULT, true}]` | Selecting Vault auto-enables toggle, disables it, shows info alert; selecting Database leaves toggle hidden |
| 4 | Edit mode | (any) | Picker disabled with tooltip; toggle hidden |
| 5 | Submit branching | (two-store, external) | In register-existing mode, form calls `useRegisterExistingConnectionMutation`; in normal mode, calls `useCreateConnectionMutation` |
| 6 | Loading state | query in pending state | Picker section shows skeleton; submit button disabled |
| 7 | Query error | mock rejects | Falls back to no-picker DATABASE mode; error logged; user can still create |

### Backend controller test

New `RegisterExistingConnectionApiControllerIntTest` (or extend the existing connection controller test) using the project's `@WebMvcTest`/MockMvc pattern. Mocks `ConnectionService.registerExisting` and verifies:
- Happy path: request → service called with right args → 200 with Connection body.
- Service throws `ReadOnlyCredentialStoreException` → 4xx with typed error body.

### Manual smoke test (documented in PR description)

After backend `application.yml` configured with `bytechef.connection.credential-store.external.provider=aws-secrets-manager` and AWS credentials:
1. Open Create Connection dialog → picker visible.
2. Select AWS, toggle OFF, fill API key → connection created, AWS Secrets Manager has the secret.
3. Select AWS, toggle ON, enter known reference → connection created with that ref.
4. Verify connection list page shows credentials as `VALID`.

---

## Files modified or created

**New files:**
| File | Purpose |
|---|---|
| `client/src/shared/components/connection/connectionCredentialStoreLabels.ts` | Enum-to-label mapping |
| `client/src/shared/components/connection/ConnectionDialog.test.tsx` | Component tests (not currently present) |
| `client/src/graphql/platform/connection/registerExistingConnection.graphql` | Client GraphQL operation file |
| Backend REST controller test (extended) | New tests for `/register-existing` endpoint |
| Backend GraphQL resolver test (extended) | New test for the `registerExistingConnection` mutation |

**Modified files:**
| File | Change |
|---|---|
| `client/src/shared/components/connection/ConnectionDialog.tsx` | Conditional picker + toggle + register-existing mode + branched submit |
| `client/src/shared/mutations/automation/connections.mutations.ts` | Add `useRegisterExistingConnectionMutation` calling the new GraphQL mutation |
| `client/src/shared/middleware/automation/configuration/*` | Regenerated TypeScript REST client (for `credentialStoreType` field on existing create body) |
| `client/src/shared/middleware/graphql.ts` | Regenerated (adds `useRegisterExistingConnectionMutation` hook + input types) |
| `client/src/locales/en/messages.po` | New translation entries (11 keys) |
| `server/libs/automation/automation-configuration/automation-configuration-rest/...` (REST controller + OpenAPI) | New `POST /connections/register-existing` (public API) + `credentialStoreType` field on existing create body |
| `server/libs/platform/platform-connection/platform-connection-graphql/.../connection-credential-store.graphqls` | Add `registerExistingConnection` mutation + `RegisterExistingConnectionInput` input type (internal UI) |
| `server/libs/platform/platform-connection/platform-connection-graphql/.../ConnectionCredentialStoreGraphQlController.java` | Add `@MutationMapping registerExistingConnection` method delegating to `ConnectionService.registerExisting` |

---

## Open items for the plan phase

- **`createConnection` mutation discovery**: identify which existing mutation creates connections today. The frontend may already use a REST endpoint rather than GraphQL for create — if so, decide whether to add the field there or migrate to a new GraphQL mutation. The plan's first task inspects this.
- **Existing `ConnectionDialog.test.tsx`**: confirm whether it exists. If yes, extend; if no, create.
- **Resolver placement**: add `registerExistingConnection` to the existing `ConnectionCredentialStoreGraphQlController` or to a dedicated `ConnectionGraphQlController`? Convention check during plan.

---

## Implementation references

- [`ConnectionDialog.tsx`](../../client/src/shared/components/connection/ConnectionDialog.tsx) — the component to modify (~700 lines today)
- [`ConnectionListItem.tsx`](../../client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx) — existing credential-status badge pattern; no change needed
- [`ConnectionCredentialStoreGraphQlController.java`](../../server/libs/platform/platform-connection/platform-connection-graphql/src/main/java/com/bytechef/platform/connection/web/graphql/ConnectionCredentialStoreGraphQlController.java) — where `registerExistingConnection` resolver lands
- [`ConnectionService.java`](../../server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java) — `registerExisting` service method already in place (PR 1)
- [Sibling backend spec](2026-05-19-connection-credential-store-design.md) — for context on the SPI and storage model invariants
