# Approval/Trigger-Form Token Signing + Search-Provider Workspace Scoping — Design

> Closes the two remaining gecko IDOR items that are not a simple `@PreAuthorize` gate:
> T24 capability-token forgery (approval/trigger-form) and T25 search-provider cross-workspace leakage.
> Both are substantial/cross-cutting, so they get a design pass before implementation per the
> design-first workflow.

**Status:** proposed (awaiting review)
**Branch:** `0_732`

---

## Part A — HMAC-sign approval / job-resume capability tokens (T24)

### Problem

`JobResumeId` (live; `ApprovalFormFacadeImpl.getApprovalForm`, `TriggerFormApiController.getTriggerForm`,
`JobFacade.resumeApproval`) and the deprecated `ApprovalId` (`ApprovalController`) are **unsigned**
capability tokens:

```
JobResumeId.toString() = base64(tenantId + ":" + jobId + ":" + uuid)
ApprovalId.toString()  = base64(tenantId + ":" + jobId + ":" + uuid + ":" + approved)
```

They are minted into anonymous email/links (`ActionContextImpl` approval links) and consumed by
**anonymous** endpoints (no `SecurityContext`). Because they are plain Base64, an attacker can decode
one and **forge** another: change `jobId` to target a different job, or (for `ApprovalId`) flip
`approved` from `false` to `true`. The random `uuid` is the only secret, and nothing cryptographically
binds `jobId`/`approved` to it. The fix is to **sign** the token so its fields cannot be tampered with.

### Approach — mirror the shipped `FileEntryTokens` pattern

The codebase already has a reviewed HMAC-signing design for the analogous `/file-entries/{id}/content`
public endpoint (`docs/superpowers/specs/2026-05-18-hmac-signed-file-entry-tokens-design.md`,
`FileEntryTokens` / `FileEntryTokensImpl`). Reuse the exact shape:

- **Token format:** `v1.<exp>.<payload>.<sig>` where `<payload>` is the existing Base64 body
  (`tenantId:jobId:uuid[:approved]`) re-encoded Base64URL-without-padding, `<exp>` is decimal Unix epoch
  seconds, `<sig>` is Base64URL HMAC-SHA256 over `v1.<exp>.<payload>`.
- **Signing key:** derive from the existing `EncryptionKey` bean via
  `HMAC-SHA256(decode(encryptionKey), "bytechef-approval-token-signed-v1")` (domain-separation label,
  independent from the file-storage label and the AES master key). Falls back to an explicit
  `bytechef.approval.signed-token.secret` override for independent rotation, matching the file-storage
  key-resolution order. Works out of the box because `EncryptionKey` is always configured.
- **TTL:** approval tokens already imply a bounded validity (the job is suspended waiting for the
  approval); default e.g. 30 days, configurable. Expiry maps to a uniform 404/410.
- **Verify:** uniform failure → reject; constant-time `MessageDigest.isEqual`; multi-key list for
  rotation (active + previous).

### Backward-compat (in-flight email tokens)

Approval emails already sent contain **unsigned** tokens. Mirror the file-storage migration toggle:

- New property `bytechef.approval.signed-token.required` (default `false`).
- When `false`: the parser accepts **both** a signed `v1.…` token and a legacy unsigned Base64 token
  (existing `parse`). Emitters (`ActionContextImpl`) mint **signed** tokens going forward.
- When `true` (post-migration, after the longest approval TTL window): only signed tokens accepted.

### Where the code lives

- New interface `ApprovalTokens` in a low-level api module (alongside `JobResumeId`, i.e.
  `platform-workflow-execution-api`): `toSignedToken(JobResumeId)`, `parseSignedToken(String) ->
  Optional<JobResumeId>`, `looksLikeSignedToken`, `toSignedTokenIfConfigured`. Mirror `FileEntryTokens`.
- Impl `ApprovalTokensImpl` + autoconfiguration in a `*-token-service` sibling (mirror
  `file-storage-token-service`), wired off `EncryptionKey`.
- `ActionContextImpl` approval-link minting → `ApprovalTokens.toSignedTokenIfConfigured(...)` with
  legacy fallback.
- `ApprovalFormFacadeImpl.getApprovalForm` / `TriggerFormApiController` / `ApprovalController` →
  `ApprovalTokens.parseSignedToken(...)` first, fall back to legacy `JobResumeId.parse`/`ApprovalId.parse`
  while `required=false`.
- `ApprovalId` (deprecated): apply the same signed/verify wrapping or retire with `ApprovalController`.

### Tests

Unit tests on `ApprovalTokensImpl` mirroring `FileEntryTokensImpl` tests: round-trip, tampered field
rejected, flipped `approved` rejected, expired rejected, legacy accepted while `required=false`, rotation
(previous key still verifies). Pin the domain-separation label wording.

---

## Part B — Workspace-scope the search providers (T25)

### Problem

`automationSearch(query, limit)` (GraphQL) → `AutomationSearchFacadeImpl.search` fans out to **10**
`SearchAssetProvider` implementations, each of which does a global `findAll`-style fetch
(`WorkflowSearchAssetProvider` → `getLatestProjectWorkflows()`, plus Project / ProjectDeployment /
Connection / DataTable / KnowledgeBase / KnowledgeBaseDocument / AssetFile / ApiCollection / ApiEndpoint).
Results leak across workspaces the caller cannot access (names/descriptions/ids of other workspaces'
artifacts). There is **no workspace parameter** in the SPI, so this is a contract change, not a gate.

### Approach — pass the caller's accessible workspaces through the SPI

1. Introduce a `SearchContext` carrying `query`, `limit`, and `accessibleWorkspaceIds` (a `Set<Long>`).
   Change the SPI from `search(String query, int limit)` to `search(SearchContext context)`.
2. `AutomationSearchFacadeImpl` computes `accessibleWorkspaceIds` **once** from the current user
   (`userService.getCurrentUser().getId()` → `workspaceFacade.getUserWorkspaces(userId)` → ids — the
   same membership source the AI Hub / asset-file guards already use, now SELF-gated) and passes it to
   every provider.
3. Each provider filters its results to `accessibleWorkspaceIds` via its own data→workspace mapping
   (workflow → project → `project.workspace_id`; connection → `workspace_connection`; data table →
   `workspace_data_table`; KB → `workspace_knowledge_base`; etc. — the same relations the per-resource
   gates already use). Empty set → empty results (fail closed).

### Why the SPI change (not internal per-provider current-user lookups)

Computing the accessible-workspace set once in the aggregator avoids 10 duplicate
`getCurrentUser`/`getUserWorkspaces` round-trips per search and keeps the membership policy in one place.
The GraphQL query and client are unchanged — the workspace set is derived server-side from the
authenticated user.

### Scope / sequencing

10 providers across 8 modules. Land as one SPI change + aggregator change, then one provider at a time
(each with a filter test: in-workspace result kept, out-of-workspace result dropped, empty-set → empty).
The 2 EE api-platform providers (ApiCollection/ApiEndpoint) and the platform KB/connection providers each
already have a workspace relation to filter on.

---

## Risks

- **A:** crypto correctness (constant-time compare, key derivation independence) — mitigated by mirroring
  the already-reviewed `FileEntryTokens` implementation verbatim. The migration toggle prevents breaking
  in-flight approval emails.
- **B:** a provider that can't cheaply map its rows to a workspace would force an N+1; acceptable for
  search (already bounded by `limit`), but note any provider that needs a batch workspace lookup.

## Out of scope

- Retiring the deprecated `ApprovalController`/`ApprovalId` entirely (separate cleanup).
- Changing the `automationSearch` GraphQL signature or client (the workspace set is server-derived).
