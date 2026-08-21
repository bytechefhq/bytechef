# Phase 3 Security Hardening (T26 + T27) — Design

- **Date:** 2026-06-20
- **Scope:** gecko remediation tasks T26 (auth & session hardening) and T27 (output
  encoding, file-path safety & shared-state isolation)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)

## Overview

This spec closes the final Phase 3 hardening findings from the gecko security review.
The two tasks are distinct domains but are grouped here as a single Phase 3 spec, with
one implementation plan to follow.

Two sub-items — the TipTap XSS (finding 7.6) and the `EMBED_INIT` postMessage origin
check — are **already mitigated** in the current tree and are documented below as
verified-done rather than re-implemented. Everything else is net-new work.

### Verified-done (no further work)

- **7.6 · TipTap XSS** — `buildPropertyMentionsContent`
  (`property-mentions-input/propertyMentionDom.ts:41`) now sanitizes the RICH_TEXT render
  path via `sanitizeHtml(content)` before `editor.commands.setContent`, and the save path
  (`PropertyMentionsInputEditor.tsx:302`) strips all tags for non-RICH_TEXT control types.
  The render path is the security-relevant one because server-stored values from other
  users/tenants are the untrusted source.
  - **Residual to confirm during implementation:** `propertyMentionDom.ts:52` wraps lines
    in `<p>${valueLine}</p>` without escaping for control types *outside*
    TEXT_AREA/TEXT/FORMULA_MODE on the newline branch. Confirm no RICH_TEXT-adjacent type
    reaches this branch; escape if it can.

- **`EMBED_INIT` postMessage origin** — `useWorkflowBuilder.ts:138-154` now reads a
  `VITE_EMBEDDED_PARENT_ORIGINS` allowlist, checks `event.source === window.parent`, and
  gates on `isAllowedOrigin(event.origin)` before writing `jwtToken` to `sessionStorage`.
  - **Residual to confirm:** when `VITE_EMBEDDED_PARENT_ORIGINS` is unset the allowlist is
    empty and `isAllowedOrigin` returns `true` (open by default). This is a deliberate
    dev/unconfigured fallback; confirm production deployments set the variable. No code
    change planned.

## T26 — Auth & session hardening

### 1. TOTP brute-force lockout (finding 8.7)

**Problem:** `POST /api/mfa/verify` (`TwoFactorVerificationFilter` →
`UserServiceImpl.verifyTotpCode`) has no rate limit or lockout, allowing unlimited TOTP
guesses.

**Approach:** DB-persisted attempt tracking on the user record. Chosen over an in-memory
cache because ByteChef runs both monolith and EE microservice topologies — in-memory
counters are not shared across EE nodes (an attacker rotates nodes) and reset on restart.
There is no reusable rate-limiting infrastructure today
(`ApplicationProperties.RateLimiting` is an empty `enabled`/`provider` stub under
observability), so a targeted counter on the user is the right scope — not a new
general-purpose limiter.

**Changes:**
- Liquibase migration: add `failed_totp_attempts INT NOT NULL DEFAULT 0` and
  `totp_lockout_until TIMESTAMP NULL` to the user table; mirror on the `User` entity.
- `UserServiceImpl.verifyTotpCode`:
  - If `totpLockoutUntil != null && now < totpLockoutUntil` → reject as locked (do not
    even check the code).
  - On invalid code → increment `failedTotpAttempts`; when it reaches the threshold, set
    `totpLockoutUntil = now + lockoutWindow`.
  - On valid code → reset `failedTotpAttempts = 0`, `totpLockoutUntil = null`.
- `TwoFactorVerificationFilter`: return **HTTP 429** when the account is locked, distinct
  from 401 for an invalid code (so the client can message "too many attempts").
- New properties: `bytechef.security.mfa.max-failed-attempts` (default **5**) and
  `bytechef.security.mfa.lockout-duration` (default **15m**).

### 2. `config()` SpEL function allowlist (findings 6.2 / 6.5)

**Problem:** `Config.execute` (`evaluator/Config.java:44`) returns *any* environment
property via `environment.getProperty(name)`, so a workflow expression like
`=config('spring.datasource.password')` or `=config('bytechef.encryption.key')` exfiltrates
secrets. The only documented legitimate use is `=config('app.setting')`.

**Approach:** Configurable prefix allowlist, **default-deny**.

**Changes:**
- New property `bytechef.workflow.config.allowed-prefixes` (list of strings).
- `Config.execute`: resolve the property only if `propertyName` starts with one of the
  allowed prefixes; otherwise throw the existing
  `SpelEvaluationException(PROPERTY_OR_FIELD_NOT_READABLE)` so no information leaks about
  whether the key exists.
- Empty/unset list ⇒ deny all (secure default). Document that operators opt-in the
  prefixes workflows are permitted to read.

### 3. Activation-email enumeration (finding 5.3)

**Problem:** `POST /api/send-activation-email` (`AccountController`, public) throws
`UserNotFoundException` for unknown emails, letting an anonymous caller enumerate
registered accounts.

**Approach:** Uniform response.

**Changes:**
- `AccountController.sendActivationEmail`: look up the user; send the activation mail only
  when present; always return HTTP 200 regardless. Do not surface
  `UserNotFoundException` to the caller.

### 4. `EMBED_INIT` postMessage origin (client) — VERIFIED-DONE

See the Verified-done section above. The trusted-origin allowlist, source check, and
gate-before-store are already present in `useWorkflowBuilder.ts`. No work in this plan
beyond confirming the unconfigured-allowlist fallback is acceptable for production.

## T27 — Output encoding, file-path safety & shared-state isolation

### 5. `Content-Disposition` header injection (CRLF)

**Problem:** Export endpoints concatenate user-controlled names directly into the
`Content-Disposition` header:
- `ProjectApiController` (`project.getName()`)
- `AbstractWorkflowApiController` (`workflow.getLabel()`)
- `ApiCollectionApiController` (collection `name`)

CR/LF in a name enables header/response splitting.

**Approach:** One shared sanitizer helper.

**Changes:**
- Helper that strips CR/LF and control characters and emits both an ASCII-fallback
  `filename="..."` and an RFC 5987 `filename*=UTF-8''...` form for unicode names.
- Apply at all three export sites.

### 6. File-path safety

**Problem:** `FilesystemWriteFileAction.perform` passes a workflow-supplied `filename`
straight to `Files.copy(in, Path.of(fileName), REPLACE_EXISTING)` — arbitrary overwrite /
traversal. Related storage writers have similar exposure
(`FileDataStorageServiceImpl`, `JGitWorkflowOperations`, `AwsFileStorageServiceImpl`).

**Approach:** Canonicalize only. `FilesystemWriteFileAction` is a user-facing component
whose purpose is writing files where the user points it, so a mandatory base-dir jail
would break legitimate workflows. We harden against traversal-via-tricks without
constraining the destination.

**Changes:**
- `FilesystemWriteFileAction`: reject null bytes in the filename; `Path.normalize()`
  before `Files.copy`.
- Apply the same minimal normalization (null-byte rejection + normalize) to the storage
  key/path inputs of `FileDataStorageServiceImpl`, `JGitWorkflowOperations`, and
  `AwsFileStorageServiceImpl` to prevent `../` escape *within* their storage namespaces.

### 7. Chat-memory tenant isolation (finding 7.8)

**Problem:** `InMemoryChatMemory` builds a `static final MessageWindowChatMemory` at
class-load time from `InMemoryChatMemoryRepositoryHolder.getInstance()`. The holder is
*already* tenant-scoped (keyed on `TenantContext.getCurrentTenantId()` via a Caffeine
cache), but resolving it once into a `static final` field defeats that scoping: whichever
tenant first triggers class-loading gets their repository permanently baked in, and every
subsequent `apply()` for **all** tenants returns that one instance — a cross-tenant
conversation leak.

**Approach:** Resolve per-invocation.

**Changes:**
- `InMemoryChatMemory`: move the `MessageWindowChatMemory.builder()...build()` out of the
  `static final` field and into `apply()`, so `getInstance()` runs in the caller's tenant
  context. Building the wrapper per call is cheap (the underlying repository is the cached,
  tenant-scoped one).
- **Audit** the Jdbc / Mongo / Cosmos / Cassandra chat-memory get/delete actions and
  `LangchainAgent`, `SpringAIAgent` / `CopilotConfiguration` for the same
  static-capture-of-scoped-state pattern; fix any that statically capture a tenant- or
  user-scoped instance. Backends that persist by `conversationId` in a shared store are in
  scope only if `conversationId` is not already tenant-unique.

## Testing

- **TOTP lockout:** unit tests for increment, threshold → lockout, reset-on-success, and
  the 429 (locked) vs 401 (invalid) filter responses.
- **`config()`:** allowed-prefix read succeeds; denied-prefix throws the not-readable
  error; empty allowlist denies all.
- **Activation email:** returns 200 for an unknown email and does not dispatch mail.
- **Content-Disposition sanitizer:** CRLF stripped; unicode name produces a valid RFC 5987
  `filename*`.
- **File path:** `../` and null-byte inputs rejected/normalized.
- **Chat-memory isolation:** two-tenant regression test proving tenant A cannot read tenant
  B's conversation — the test that proves the static-capture fix.

## Out of scope

- TipTap XSS (7.6) — already fixed; only the line-52 escaping residual is to be confirmed.
- A general-purpose rate-limiting framework — TOTP gets a targeted DB counter only.
- Base-directory jailing for filesystem writes — explicitly declined in favor of
  canonicalize-only to preserve the component's purpose.

## Defaults chosen (flag if these should change)

- TOTP: **5** failed attempts, **15m** lockout window.
- `config()` allowlist: **empty by default** (deny-all).
- Chat-memory backends other than in-memory: **audit-and-fix-if-needed**, not assumed
  broken.
