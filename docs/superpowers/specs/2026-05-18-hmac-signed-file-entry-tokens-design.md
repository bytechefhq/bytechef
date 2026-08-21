# HMAC-signed file-entry tokens

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-18 | **Last updated:** 2026-05-18

## Why

The `/file-entries/{id}/content` endpoint is intentionally unauthenticated — it serves files produced by `/webhooks/{id}` invocations, so external webhook callers (often unauthenticated) need to be able to fetch the file the webhook returned. Adding session auth here would break the webhook → file-fetch handoff.

After the path-traversal hardening (commit `a2310291222` + `FileEntry.parse()` validation), an attacker can no longer escape the per-tenant storage directory by crafting a malicious URL. But the ID itself is still an **unsigned, non-expiring capability**: anyone who guesses or obtains a valid `base64(<extension>_;_<mime>_;_<name>_;_<url>)` can fetch the file forever, from anywhere, with no audit trail tying retrieval back to the original recipient.

Practical risks:

- **URL leakage past intended use.** File URLs flow through browser history, server access logs, third-party analytics, error monitoring (Sentry payloads, screenshots), customer support tickets. A URL captured today still works in six months.
- **Long-tail enumeration.** UUIDs are large enough (~10^36) that random guessing is impractical *today*, but if a future backend uses shorter IDs, or a buggy custom component produces predictable filenames, enumeration becomes feasible. Capability-tokens close this regardless of the backend's ID scheme.
- **No recipient binding.** A legitimate recipient can share the URL with anyone; the URL has no notion of "who was supposed to see this."
- **No revocation.** If a file URL leaks (e.g. surfaced in an SDK consumer's misconfigured logger), there's no way to invalidate it short of deleting the underlying file. Even a JVM restart doesn't help — IDs are deterministic.

## What

Replace the unsigned `FileEntry.toId()` output used in **public-facing** URLs with HMAC-SHA256-signed tokens that include an expiry timestamp. Keep `toId()` and `parse()` for **internal** serialization (DB persistence, intra-process passing) where capability security is not the goal.

External URL form, before and after:

```
Before:  /file-entries/dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wLzEyMy50eHQ/content

After:   /file-entries/v1.1748883600.dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wLzEyMy50eHQ.0BZJh3xX9Q8u7K2nKZ3v8w-rA0CnQzL5VtHfNgZj8sk/content
```

## Goals

1. Public file URLs cannot be fetched after their expiry.
2. Public file URLs cannot be forged without the server's signing key.
3. Existing internal callers (notably `AiAgentEvalResult.transcriptFile` in the EE eval module — stored as opaque text in the database) keep working without DB migration.
4. Existing in-flight URLs continue to work for a deprecation window, with structured logging so operators can measure cutover progress before flipping the strict-mode flag.
5. Key rotation is supported without invalidating all in-flight URLs at once.

## Non-goals

- **Per-user authorization.** The endpoint stays public; a signature proves "this URL was minted by the server with the agreed TTL" but not "you are the intended recipient." Recipient binding (e.g. binding to webhook caller IP, or to a session) is a separate, larger problem and can be added later if a use case emerges.
- **Per-URL revocation list.** Tokens expire by timestamp; if the secret leaks, rotate the key (which invalidates all in-flight tokens signed with the old key).
- **A new file-storage backend.** This is a token-format change at the controller boundary, not a storage change. The Filesystem, Base64, and AWS backends are untouched.
- **Replacing `toId()` for internal use.** The internal serializer keeps its current behavior; this spec adds a parallel path for external URLs.

## Token format

```
v1.<exp>.<payload>.<sig>
```

- **`v1`** — version byte. Lets us evolve the algorithm or claim set (`v2`, etc.) without breaking the verify path; verifier always inspects the leading token before doing crypto work.
- **`<exp>`** — Unix epoch seconds at which the token expires. Decimal ASCII, no zero-padding, no trailing zero seconds.
- **`<payload>`** — the existing `FileEntry.toId()` output (Base64 of `extension_;_mime_;_name_;_url`), re-encoded as Base64URL **without padding** to keep the token URL-safe.
- **`<sig>`** — Base64URL-encoded HMAC-SHA256 of the literal ASCII string `v1.<exp>.<payload>` (including the dots), using the active server signing key. No padding.

Total token length: ~150 characters in practice. Well under common URL length limits (browsers ≥ 2 KB; Spring path-param default is unbounded for `@PathVariable`).

**Why not JWT?**

1. Avoid pulling in `auth0/java-jwt` or `spring-security-oauth2-resource-server` for what is fundamentally a single-claim MAC. The codebase already uses `Mac.getInstance("HmacSHA256")` directly (see `AiObservabilityWebhookDeliveryServiceImpl.java:640`) — consistency over framework sprawl.
2. JWTs are JSON. A malformed token throws deeply nested parser exceptions that are awkward to map to a single uniform 404 rejection without an exception-handler maze.
3. We don't need `nbf`, `aud`, `iss`, or `kid` — they would be dead weight here. (Key rotation works by signing-with-newest, verifying-against-all — the verifier doesn't need to know which key was used.)

**Why Base64URL without padding?** Avoids `=` characters which need percent-encoding in URLs, makes path-param parsing trivial, and matches the convention used in OAuth bearer tokens and JWTs.

## Verification flow

1. Split the path-variable string on `.`. Reject if not exactly 4 parts.
2. Reject if `parts[0]` ≠ `"v1"`.
3. Reconstruct the signing input: `"v1" + "." + parts[1] + "." + parts[2]` (the literal string, not a re-encoded form).
4. For each configured key (current first, then any rotation keys), compute expected HMAC-SHA256 and constant-time-compare against `parts[3]`. Use `MessageDigest.isEqual()` for the comparison.
5. Parse `parts[1]` as a `long`. Reject if `now > exp + clockSkew` (expired, with skew tolerance toward acceptance). We deliberately **do not** check for future-dated tokens: minting one requires the signing secret, in which case an attacker can do far worse than predate. A misconfigured signing clock would produce unusable tokens — annoying but not exploitable.
6. Base64URL-decode `parts[2]`, then pass to `FileEntry.parse(decodedAsStandardBase64)`. The existing `parse()` path-traversal validation still applies.
7. Read the file via the configured storage backend.

**All failure modes — malformed, bad signature, expired, bad payload — return HTTP 404 with no body details.** Never 401 or 403: those would confirm "a signed token was attempted" or "the token format is correct," which is information leakage. A structured log entry records the failure category for operator debugging (`token.malformed`, `token.bad_signature`, `token.expired`, `token.bad_payload`, `token.legacy_accepted`).

## Configuration

### Signing key derivation (default)

In the standard ByteChef deployment, no signing-key configuration is needed. `FileEntryTokensAutoConfiguration` detects the `EncryptionKey` bean (which is always present) and derives a dedicated signing key automatically:

```
signingKey = HMAC-SHA256(Base64.decode(encryptionKey.getKey()), "bytechef-file-storage-signed-url-v1")
```

The domain-separation label `"bytechef-file-storage-signed-url-v1"` ensures the 32-byte derived key is computationally indistinguishable from a random key — the HMAC-SHA256 PRF guarantees that a party who knows the label but not the master key cannot predict the signing key, and a party who knows the signing key but not the label cannot reverse-engineer the master key. The `-v1` suffix allows rolling forward to a different derivation scheme (e.g., HKDF) without rotating the underlying encryption key.

### Resolution order

1. **Explicit `secret` property** — takes precedence when set. Power-user override; allows rotating the signing key independently of credential encryption.
2. **`EncryptionKey` bean** — derived automatically (standard setup; see above).
3. **Neither** — unconfigured mode: mint throws `IllegalStateException`, verify path accepts legacy unsigned tokens only. Boot succeeds unless `required=true`.

### Full property reference

```yaml
bytechef:
  file-storage:
    signed-url:
      # Optional override: base64-encoded HMAC-SHA256 secret (≥32 bytes of entropy).
      # May come from any Spring property source (env var, K8s Secret, Spring Cloud Config, Vault).
      # If omitted, the key is derived from the EncryptionKey bean (recommended; no extra config needed).
      # If set and required=true but this is blank, the application fails to start.
      secret: ${BYTECHEF_FILE_STORAGE_SIGNED_URL_SECRET:}

      # When true, only signed tokens are accepted; legacy unsigned IDs are rejected
      # with 404. Defaults to false in 1.x for backward compatibility. Will default to
      # true in the next major release.
      required: ${BYTECHEF_FILE_STORAGE_SIGNED_URL_REQUIRED:false}

      # Default TTL for newly minted tokens. ISO-8601 duration.
      default-ttl: PT24H

      # Acceptable clock skew when validating expiry (both directions).
      clock-skew: PT60S

      # Optional list of previous secrets for key rotation. Each entry base64-encoded.
      # Verification tries the current secret first, then each entry in order. Signing
      # always uses the current secret. Drop entries here once you're confident no
      # tokens signed with them remain in flight (rule of thumb: drop after 2 × max TTL).
      previous-secrets: []
```

## Backward compatibility plan

The current path-traversal fix is already shipped, so the *vulnerability* is closed. This work is purely a defense-in-depth upgrade — the rollout can take its time.

**Phase 1 (this PR):**
- Mint + verify paths implemented; `FileEntryController` accepts both formats.
- `bytechef.file-storage.signed-url.required` defaults to `false`.
- Legacy use emits a single WARN per minute (rate-limited; otherwise high-traffic deployments would flood logs) with calling user-agent so operators can spot SDK consumers vs browsers vs external integrations.
- All ByteChef-emitted URLs (webhook output, copilot/chat attachments, UI download links) switch to signed.

**Phase 2 (one minor release later):**
- Add `X-File-Token-Deprecation: legacy` response header on legacy-accepted requests.
- Add Micrometer counter `bytechef_file_entry_token_total{kind=signed|legacy,outcome=accepted|rejected}` so operators can graph cutover progress.

**Phase 3 (next major release):**
- Default `required: true`. Legacy tokens rejected with 404.
- The mint code stays in place indefinitely — zero cost if no callers use it.

The phasing leans long because external integrators may have URLs cached in queues, scheduled tasks, or audit trails that take days to drain. The path-traversal fix already removes the highest-severity exposure, so urgency is bounded.

## Internal callers

`AiAgentEvalResult.transcriptFile` (EE) stores `FileEntry.toId()` output in the database as opaque text. This value never flows through the public endpoint — it's read back via `FileEntry.parse()` inside the EE eval read path, then the file is fetched via the internal storage service directly. So it doesn't need signing.

Keep `toId()` and `parse(String)` exactly as they are today (with the path-traversal validation already shipped). The new methods `toSignedToken(Duration ttl)` and `parseSignedToken(String token)` are added alongside, not in place of, and live on a new `FileEntryTokens` collaborator (Spring bean) rather than as static methods on `FileEntry` — because they need the signing key, which is configuration, not a property of the entity.

## Where ByteChef emits public file URLs

Two known emission points to switch (verify scope during implementation):

- **Webhook response bodies.** When a workflow returns a `FileEntry` via a webhook trigger, the response includes the file as a URL constructed from `toId()`. Likely candidate: somewhere under `platform-webhook-rest-impl` that serializes `WorkflowExecutionResponse` — find the helper that turns `FileEntry → publicUrl`.
- **AI Hub / chat copilot responses.** When AI Hub responses or workflow chat surfaces include attached files. Likely candidate: `ContextImpl.java` or a sibling helper in `platform-component-context-service`.

Both emission paths funnel through one or two helpers; route those helpers through the new signer with the default TTL. (Implementation step: a `grep` audit is part of step 4 in Sequencing.)

## Threat model

**What signing defends against:**
- URL leakage past the TTL → expiry blocks reuse.
- URL guessing / enumeration → forging a valid signature requires the secret.
- URL tampering (e.g. swap one file's `<url>` for another's mid-token) → signature mismatch on verify.
- A buggy backend that ships predictable IDs → still gated by signature.

**What signing does not defend against:**
- **URL sharing inside the TTL window.** Mitigation: keep TTL short for sensitive workflows (default 24h; configurable per emission point if a stronger story is needed — e.g. AI Hub copilot output could default to 1h).
- **Compromise of the signing secret.** Mitigation: key rotation via `previous-secrets`; treat the secret as a high-value credential alongside `bytechef.security.remember-me.key`. Document in the ops runbook.
- **A malicious workflow author exfiltrating file URLs after fetching them.** Out of scope — the workflow author is trusted by definition.
- **Replay during the TTL.** The endpoint is GET-idempotent (reading the same file); no harm from re-fetch.

**Boot failure modes:**
- `required=true` and missing secret → fail-fast in the autoconfiguration. Don't fall back to a random in-memory key (would silently break URLs across restarts and create silent multi-instance inconsistency in HA deployments).
- `required=false` and missing secret → mint path throws `IllegalStateException` if called (loud); verify path only accepts legacy. Boot succeeds with a single WARN at startup so operators see they're in legacy mode.

## Acceptance

End-to-end: a workflow that returns a `FileEntry` from a webhook trigger produces a response body containing a signed URL. Fetching that URL within the TTL returns the file. Fetching it after the TTL returns 404. Tampering with any character in the token (payload or signature) returns 404. The same URL re-fetched 10 times in a row succeeds 10 times (replay is fine within TTL).

Operator: deploying with `required=false` and `secret` set means signed tokens are minted and verified for new traffic, while pre-existing legacy URLs from a previous deployment still resolve and log a WARN. Flipping `required=true` after a deprecation window rejects legacy URLs with 404.

Key rotation: signing key changes from `K1` to `K2`. Operators add `K1` to `previous-secrets` and replace `secret` with `K2`. URLs minted with `K1` continue to verify until they expire by TTL. New URLs are minted with `K2`. After 2 × TTL, `K1` is removed from `previous-secrets`.

## Cross-cutting

### Metrics

- `bytechef_file_entry_token_total{kind,outcome}` counter. `kind ∈ {signed, legacy}`; `outcome ∈ {accepted, rejected_signature, rejected_expired, rejected_malformed, rejected_payload}`. Bounded cardinality.
- `bytechef_file_entry_token_mint_total` counter (mint-side; no labels needed beyond the counter itself — failure to mint already throws).

### Docs

- New section in the `platform-webhook` module README: "Public file URL signing."
- Update `CLAUDE.md`'s "Persistence Conventions" or add a new "Public URL Signing" section.
- Ops runbook entry: "How to rotate the file-storage signing key."

### Sequencing

1. **Signer + properties + autoconfig.** Pure functions, no integration yet. ~0.5 day.
2. **`FileEntryTokens` Spring bean** wrapping the signer; methods `toSignedToken(FileEntry, Duration)` and `parseSignedToken(String) → Optional<FileEntry>`. ~0.5 day.
3. **`FileEntryController`** accepts both signed and legacy formats; legacy path emits rate-limited WARN. Wire the counter. ~0.5 day.
4. **Audit URL emission points** (grep for `toId()` callers that produce URLs) and switch them to mint signed tokens with the default TTL. ~1 day — bulk of the time is the audit, not the code.
5. **Tests** (unit + integration). ~1 day.
6. **Docs.** CLAUDE.md update, module README, ops runbook entry. ~0.5 day.

**Total: ~4 days for a single engineer.**

## When to prioritize

The path-traversal vector is **closed today**. This is a defense-in-depth upgrade — capability tokens replace bearer-by-knowledge tokens. Prioritize if:

- A customer security review flags the unsigned URLs as a finding, OR
- An incident involves leaked file URLs (browser history dump, log shipping misconfig, leaked SDK payloads), OR
- A new feature materially raises the value of a leaked URL (e.g. workflow file outputs that routinely contain PII or credentials).

Otherwise this can sit as a ready-to-execute plan; the current path-traversal hardening already removes the highest-severity vector.
