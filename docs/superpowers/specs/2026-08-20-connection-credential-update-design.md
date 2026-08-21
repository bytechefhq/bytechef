# Connection Credential Update — Design (write-only credential re-entry in `ConnectionDialog`)

- **Date:** 2026-08-20
- **Branch:** `0_732`
- **Status:** Accepted — all six design decisions were put to the user during the brainstorm and answered.
  §12 records each one, including the two where the user chose the option this document did not recommend.
- **Ticket:** none filed yet. The upstream issue is a separate, explicitly-approved action (§11).
- **Related:** `2026-08-10-resource-visibility-design.md` (the "use, not repoint" property this spec
  deliberately narrows), `2026-08-17-embedded-automation-hub-design.md` (the hub reconnect this spec
  leaves in place and schedules for migration in §10).

## 1. Summary

A connection whose credentials stop working is, today, a dead end: `ConnectionDialog`'s edit mode can
rename a connection and retag it, and nothing more. The only way to supply a new secret is to create a
second connection and repoint every workflow, deployment and test configuration at it by hand.

This spec makes credentials re-enterable **in place**, so an existing connection is reused rather than
replaced. The user never sees the stored secret — they cannot, and that is not a new guarantee (§3) —
they get blank, write-only inputs and submit new values.

Three things make this more than a form change, and are why it needs a spec:

1. **It narrows a documented security property.** `CLAUDE.md` states, of workspace-shared connections,
   that "no `ConnectionFacade` method mutates authorization parameters after creation" — a member can
   *run* a workflow against a colleague's credential but never *repoint* it. Credential re-entry is
   precisely the capability that sentence excludes, so the gate is a design decision (§4), not a detail.
2. **It fixes a live bug on the way past.** `TokenRefreshHandler` sets `credentialStatus = INVALID` when a
   refresh fails, and `ComponentDefinitionFacadeImpl` then blocks execution on `status != VALID`. **No
   production code path sets it back to `VALID`.** A reconnect that does not reset the flag reports
   success and leaves the connection blocked (§5).
3. **The embedded hub already does this, as an acknowledged hack**, and the hack constrains how the new
   mode may be built (§10).

## 2. Goals / non-goals

**Goals**

- Re-enter credentials on an existing connection, for every authorization type including OAuth2.
- Never render a stored secret, in any mode.
- Restore a connection blocked by `credentialStatus = INVALID` to working order.
- Introduce the explicit dialog mode that the embedded-hub migration (§10) will need.

**Non-goals**

- **Validating the submitted credential at write time.** There is no test-connection capability in scope;
  see §8.
- **Editing non-secret connection properties** (`baseUri` inputs, region, subdomain). They stay read-only,
  as today. A connection whose tenant moved still needs recreating.
- **Changing a connection's authorization type.** The type is locked to the stored one.

**Scope reversal, 2026-08-20.** The embedded hub migration was originally deferred (⚑4) and the
`credentialStatus` reset was originally scoped to the automation path only (⚑5). Both were reversed after
the plan was written: the hub migration is now **in scope** (§10), the published embedded endpoint moves to
replace-all semantics, and `ConnectionFacade.updateAuthorization` is **deleted** once its sole production
caller moves. §12 records the reversal against the original decisions rather than rewriting them.

## 3. What is already true

Two properties the feature does not have to build, established by reading the current code:

**Secrets already cannot reach the client.** `ConnectionApiController.toConnectionModel` runs
`ObfuscateUtils.toObfuscatedMap(authorizationParameters, 28, 8)` and then `.parameters(null)`. The wire
format carries masked values and nothing else, so a write-only input is not a new restriction — it is the
only thing the transport permits. `ConnectionParameters` renders those masked values read-only, and
continues to.

**Owner-or-admin is already enforced one layer down, for this exact operation.**
`ConnectionServiceImpl.updateConnectionParameters` and `updateConnectionCredentialStatus` both call a private
`validateOwnerOrAdmin(connection)` that throws unless the caller is `connection.createdBy` or holds
`ROLE_ADMIN`. The §4 gate is therefore not a new posture invented for this feature — it is the facade-level
statement of a rule the service already applies to every parameter write, made visible and testable at the
layer authorization is supposed to live at. The same method also rejects read-only credential stores
(`ReadOnlyCredentialStoreException`) and AI-provider connections (`rejectIfAiProviderConnection`), which is
the server-side backstop behind the client rule in §7.

**The credential/non-credential key partition already exists.** `ConnectionFacadeImpl.toConnectionDTO`
derives `authorizationPropertyNames` (flat-mapped from the connection definition's authorizations) and
`connectionPropertyNames` (from the definition's own properties) in order to split stored parameters into
the two DTO maps. The replace operation in §5 reuses exactly that derivation, which is why the client
never has to tell the server which submitted keys are credentials.

## 4. Authorization

A new method on the CE `WorkspaceConnectionFacade`. The EE interface extends the CE one, so a single
declaration covers both editions:

```java
@PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
    "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version);
```

The admin half is `hasResourceRole`, not `hasAuthority("ROLE_ADMIN")` — that is the exact expression
`setConnectionVisibility`, `grantConnectionAccess` and `revokeConnectionAccess` already carry, and per
`CLAUDE.md` it is deliberately the owner-or-admin *sharing-management* posture: an admin must be able to
repair a credential on a connection they cannot themselves see.

**Owner-or-admin, not `CONNECTION_EDIT`.** `CONNECTION_EDIT` is the gate on rename and retag, and reusing
it would let any member holding edit scope on a workspace-shared connection swap it to an account they
control, with every workflow using that connection silently following. Owner-or-admin preserves the
existing property: a member may see and use a shared connection, and must ask its owner (or an admin) to
repoint it. This is the same idiom `ProjectSharingFacadeImpl` and `ConnectionSharingFacadeImpl` already
use for sharing management, and it is deliberately the same posture — an admin must be able to repair a
credential on a connection they cannot themselves see.

**CE is unaffected.** CE's `PermissionServiceImpl.isResourceOwner` returns `SecurityUtils.isAuthenticated()`,
consistent with CE having no authorization boundary between workspace members.

**Rejected states.** A connection whose `ConnectionStatus` is `PENDING_REASSIGNMENT` or `REVOKED` is
rejected — reassignment has its own flow and must not race this one.

## 5. Server operation

A **new** platform-level method, not a reuse of the existing one:

```java
void replaceAuthorizationParameters(long connectionId, Map<String, ?> parameters);
```

`ConnectionFacade.updateAuthorization` merges (`connection.putAllParameters`), which is the opposite of the
replace-all semantics chosen in §12/⚑3. It is added as a **sibling** rather than as a flag on the existing
method so that every task up to the hub migration leaves the embedded path compiling and working unchanged
— then §10 moves the sole caller across and **deletes `updateAuthorization` outright**. Two near-identical
methods differing only in merge-vs-replace is a trap: the wrong one produces a silently surviving stale
credential, with no failing test to show for it.

**Semantics.**

```
newParameters = storedParameters − authorizationPropertyNames + submittedParameters
credentialStatus = VALID
```

Subtracting by `authorizationPropertyNames` (§3) is what makes "replace all" safe: connection-level
properties survive by construction, without the client enumerating them. A required field left blank is a
client-side validation failure; an optional field left blank is genuinely cleared, which is what
replace-all means.

**A new service method is required too — the merge is not only in the facade.**
`ConnectionServiceImpl.updateConnectionParameters` reads the stored secret and does
`curParameters.putAll(parameters)` before `store.storeSecret`. Handing it the already-computed final map
would therefore still merge the *old* authorization keys back in, so a cleared optional field would silently
survive. `replaceAuthorizationParameters` needs a sibling
`ConnectionService.replaceConnectionParameters(long, Map)` that calls `store.storeSecret` with the supplied
map verbatim, keeping every other guard (`rejectIfAiProviderConnection`, the read-only store check,
`validateOwnerOrAdmin`, and the non-DATABASE inline-column clearing) identical.

Both methods share the existing private `resolveOAuth2AuthorizationCode` helper, so an OAuth2 re-consent
runs the identical authorization-code exchange as the original connect — including the predefined
client-id/secret restoration and the missing-refresh-token warning.

**The `VALID` reset is the substance, not bookkeeping.** Beyond unblocking `ComponentDefinitionFacadeImpl`,
`ConnectionAfterSaveEventListener` re-arms the scheduled token-refresh routine on update *only when*
`credentialStatus == VALID`. Without the reset, a successful re-auth leaves the refresh scheduler dormant
and the connection heads straight back to `INVALID`.

**Optimistic locking.** The `version` argument is checked in the **workspace facade** (§4), against the
connection's stored version, before it delegates — so a concurrent edit fails cleanly rather than silently
overwriting a credential someone else just rotated. `replaceAuthorizationParameters` itself takes no
version: it is the platform-level primitive, and the embedded reconnect path (which has no version to
supply) is its other prospective caller once §10 lands.

**Audit.** A new `ConnectionAuditEvent.CONNECTION_CREDENTIALS_UPDATED(true)` — `strictAudit`, on the same
reasoning as `CONNECTION_REASSIGNED`: the account a credential points at changes, and that must not be able
to happen untraceably. Payload carries no credential material; `connectionId` identifies the row. Emitted
through the existing `AuditConnection` aspect, which defers to `afterCommit`.

## 6. API surface

GraphQL, extending `automation-configuration-graphql`'s `connection.graphqls`:

```graphql
extend type Mutation {
    """Replace the authorization parameters of an existing connection. Owner-or-admin only.
    Submitted parameters replace the connection's authorization parameters wholesale; its
    connection-level properties are untouched."""
    updateConnectionCredentials(input: UpdateConnectionCredentialsInput!): Boolean!
}

input UpdateConnectionCredentialsInput {
    connectionId: ID!
    parameters: Map!
    version: Int!
}
```

`Map` is the codebase's registered scalar for free-form key/value payloads; there is no `JSON` scalar.
`CreateOrganizationConnectionInput` in `organization-connection.graphqls` already carries a connection's
`parameters: Map!`, so this is the established shape for exactly this payload.

GraphQL rather than REST because the two most recent connection mutations — `disconnectConnection` and
`registerExistingConnection` — already live there, and because it avoids regenerating the OpenAPI client
for a mutation the public API has no reason to expose.

## 7. Client

`ConnectionDialog` already models a two-step wizard (`wizardStep: 'configuration_step' | 'oauth_step'`).
Add a third value, `'credentials_step'`, rather than growing a second form inside the edit body.

**An explicit mode prop.** The dialog gains `mode?: 'create' | 'edit' | 'updateCredentials'`, **defaulting
to the value derived from `connection?.id` exactly as today**. Every existing caller is therefore
unchanged, including the embedded hub's id-less prefill. The prop exists now, ahead of its second
consumer, because §10 shows the hub migration cannot work without it.

**Flow.**

- Edit mode gains an **Update credentials** button. Activating it switches the dialog body to
  `credentials_step`, with its own Back / Update footer. The credential write is never conflated with the
  name/tags Save — the two carry different guards (§4), and a single button would make a partial failure
  unexplainable.
- The step renders the same `Properties` form the create path renders, for the connection's **stored**
  `authorizationType`. Fields are blank; sensitive ones render as `type="password"`. No authorization-type
  selector.
- OAuth2 types render the existing scopes list and `OAuth2Button` instead, with success routed to the new
  mutation rather than to create.
- The read-only, obfuscated `ConnectionParameters` block stays where it is. It is the "what is set right
  now" reference, and removing it would lose information without improving secrecy.
- When `connection.credentialStatus === 'INVALID'`, the dialog opens **directly** into `credentials_step`
  behind a warning alert naming the problem. That is the case the feature exists for; making the user find
  a button first would be perverse.

**Mutation wiring.** A dedicated `useUpdateConnectionCredentialsMutation` prop, not an overload of the
existing pair. `connectionMutation = (useUpdateConnectionMutation || useCreateConnectionMutation)` picks
update over create when both are supplied, so folding a third behaviour into that expression would be
ambiguous — and the hub already depends on that precedence (§10).

**Hidden for externally-stored credentials.** The step is not offered when
`credentialStoreType !== DATABASE` or when `managed` is true. In those cases the secret lives in AWS
Secrets Manager, HashiCorp Vault, or is otherwise not the platform's to write; a line naming the store
replaces the button. Writing here would be either rejected or silently ineffective, and the user would
have typed a secret first to find out.

## 8. Error handling and known gaps

**No write-time validation.** A wrong credential is accepted, stamped `VALID`, and re-stamped `INVALID` by
`TokenRefreshHandler` on the next execution. This is deliberate — there is no test-connection capability
to call — but it means the success toast attests that the value was *stored*, not that it *works*, and its
wording should say so. A validation step is a natural follow-on and is explicitly out of scope here.

**Optimistic-lock failure** surfaces as a normal GraphQL error through `useFetchInterceptor`'s centralised
toast; no per-mutation `onError` is needed.

**Guard failure** (a member attempting to repoint a shared connection) returns the standard authorization
error. It does not need to be disguised: unlike the sharing mutations, no identifier is enumerable here —
the caller already knows the connection exists, because they can see it.

## 9. Testing

**Server**

- `WorkspaceConnectionFacadeAuthorizationTest` — a case pinning the owner-or-admin SpEL expression. That
  test asserts annotation text, so it is the regression guard for the gate itself.
- `ConnectionFacadeTest` — replace-not-merge; connection-level properties survive; `credentialStatus` is
  reset to `VALID`; OAuth2 submissions still run the code exchange.
- `WorkspaceConnectionFacadeTest` — `PENDING_REASSIGNMENT` / `REVOKED` rejection; version conflict.
- `ConnectionGraphQlControllerIntTest` — the new mutation, authorized and unauthorized.

**Client** (`ConnectionDialog.test.tsx`)

- `credentials_step` renders blank and never prefills from `authorizationParameters`, including when the
  connection carries obfuscated values.
- An `INVALID` connection opens straight into the step.
- The step is absent for `credentialStoreType !== DATABASE` and for `managed`.
- Edit mode is otherwise unchanged for existing callers, and the hub's id-less path still renders
  credentials — the regression guard for §10 not having happened yet.

## 10. The embedded hub migration (in scope as of 2026-08-20)

**What the hub does today.** `HubConnectionDialog` passes a `connection` object *deliberately without an
`id`*, because `ConnectionDialog` only renders credential fields when `connection?.id` is falsy; with an
id it flips to rename-only mode. It then supplies `useUpdateConnectionMutation`, and since
`connectionMutation = (useUpdateConnectionMutation || useCreateConnectionMutation)` prefers update, the
reconnect runs the **create-path payload builder** (`getNewConnection()`) into the **reauthorize
endpoint**, with `isEdit` false throughout.

**Three consequences, all live today:**

1. Reconnecting toasts **"Connection created"**, because that branch is gated on `!isEdit`.
2. `onConnectionCreate` is wired but can never fire — reauthorize returns `void`, and the guard is
   `if (connectionId && onConnectionCreate)`.
3. `showOAuth2Step` is gated on `!connection?.id`. **The moment the hub passes a real id, OAuth2 reconnect
   breaks.** The gate must key off the mode instead. This is the concrete reason §7 introduces an explicit
   mode prop rather than continuing to infer the mode from `id`.

**The published endpoint moves to replace-all.** The hub's reconnect is not only client code: it is
`POST /connections/{id}/reauthorize`, a **published embedded REST endpoint**
(`ReauthorizeConnectionRequestModel{parameters}` → `ConnectedUserConnectionFacadeImpl
.reauthorizeConnectedUserConnection` → `updateAuthorization`), and it **merges** today. Per ⚑7 it switches
to `replaceAuthorizationParameters`, so both surfaces carry identical semantics.

This is a behaviour change on an API embedded customers call directly, and it needs a release note: a
caller that today posts only a rotated secret and relies on its other authorization parameters surviving
will find them cleared. The endpoint's OpenAPI description must say so.

**`updateAuthorization` is deleted, not left behind.** `ConnectedUserConnectionFacadeImpl` is its **only**
production caller — verified by grep across `server/`; the remaining references are the EE remote-client
stub and its own tests. Once the caller moves, the method is dead code, and leaving a merge-shaped sibling
next to a replace-shaped one is exactly the trap described in §5.

**What the migration does client-side:** passes a real `connection` (with its `id`) plus
`startInCredentialsMode` from `HubConnectionDialog`; deletes the id-less prefill, the
`getReauthorizeHubConnectionMutation` closure (the id now rides on the mutation variables, as it does for
automation), and the `title`/`description` overrides that exist only to make a fake-create read as a
reconnect; and fixes consequences 1 and 2 above, which the mode makes trivially correct because `isEdit` is
finally true.

**The hub's broken reconnect is fixed as a side effect.** Moving onto `replaceAuthorizationParameters`
means the hub inherits the `credentialStatus = VALID` reset, so a successful reconnect actually unblocks
the connection and re-arms the refresh scheduler. No separate two-line change is needed — this is why ⚑5
became moot rather than being separately reversed.

## 11. Rollout and documentation

- No schema change. No database migration. `credentialStatus` and `ConnectionStatus` are existing columns.
- **Breaking change to a published API, needs a release note.** `POST /connections/{id}/reauthorize` moves
  from merge to replace-all (§10, ⚑7). A caller posting a partial parameter set will have the omitted
  authorization parameters cleared rather than preserved. The endpoint's OpenAPI description states this.
- `CLAUDE.md`'s "What sharing exposes" paragraph must be amended: "no `ConnectionFacade` method mutates
  authorization parameters after creation" stops being true. The replacement sentence should say that
  credentials may be replaced by the connection's **owner or an admin**, and that a member with
  `CONNECTION_EDIT` still cannot — the guarantee narrows, it does not disappear.
- The upstream GitHub issue (`bytechefhq/bytechef`) is a public action and is filed only on explicit
  approval, with this spec as the body per the established flow.

## 12. Decisions (⚑ = put to the user during the brainstorm)

1. **⚑ Scope — all authorization types including OAuth2.** *Chosen over static-credentials-only.* OAuth2
   revocation is the most common way credentials become invalid, and `updateAuthorization`'s existing code
   re-exchange means the machinery is already there. Non-secret connection properties were offered as an
   extension and **declined** — they stay read-only.
2. **⚑ Gate — owner-or-admin.** *Chosen over `CONNECTION_EDIT` and over a new dedicated
   `CONNECTION_CREDENTIAL_EDIT` scope.* Preserves the "use, not repoint" property for shared connections
   without adding a scope to the RBAC surface. Rationale in §4.
3. **⚑ Blank fields — replace all, blank clears.** *Chosen over merge-keeps-existing and over a per-field
   "keep" toggle.* Since stored values are obfuscated, the form cannot show which fields are already set,
   so under merge semantics "left blank" and "empty" would be visually identical — and an optional field
   could never be cleared.
4. **⚑ Reach — ~~automation now, hub as a tracked follow-up~~ → REVERSED 2026-08-20: both surfaces in one
   change.** Originally chosen over unifying, to sequence the risk. Reversed before implementation began,
   on the user's instruction to add the migration as plan tasks 13+. §10 is now in scope.
5. **⚑ `credentialStatus` reset — ~~new path only~~ → MOOT as of the ⚑4 reversal.** Originally scoped to
   the automation path, knowingly leaving the hub's reconnect broken; this spec recommended against it and
   the user chose the tighter diff boundary. The ⚑4 reversal dissolves the question: the hub now calls
   `replaceAuthorizationParameters`, which resets the status, so no separate change to
   `updateAuthorization` exists to make — the method is deleted instead.
7. **⚑ Embedded public endpoint semantics — replace-all.** *Chosen over keeping merge on the endpoint, and
   over a request flag defaulting to merge.* Both surfaces get one semantics, the client unification is a
   straight deletion, and the hub inherits the status reset. Accepted cost: a behaviour change on a
   published API, requiring a release note and an OpenAPI description update (§10). The rejected flag
   option was a public API field whose only purpose would have been to date a migration.
6. **⚑ External stores — hidden for `credentialStoreType != DATABASE` and for `managed`.** *Chosen over
   hiding only for non-DATABASE stores, and over rendering everywhere and letting the store reject.*
   Anything the platform does not exclusively own is read-only here.

**Non-⚑ decisions** (made without review, flagged for the spec review): GraphQL over REST (§6); a dedicated
mutation prop rather than overloading the existing pair (§7); a third `wizardStep` value rather than an
inline expander (§7); `strictAudit` on the new audit event (§5); auto-opening the step for `INVALID`
connections (§7); rejecting `PENDING_REASSIGNMENT` / `REVOKED` (§4).
