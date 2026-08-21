# Offline Keygen.sh Licensing with Feature Entitlements — Design

**Date:** 2026-06-24
**Status:** Draft (pending review)
**Supersedes:** commit `77935be` (`LicenceChecker` + `NoOpLicenceChecker` + `SelfHostedLicenceChecker` + `CloudLicenceChecker`)

## Summary

Replace the three stub licence checkers with a single, edition-aware `LicenceManager`
built around **Keygen.sh signed license files (`.lic`)** verified **fully offline** via
Ed25519. The license carries an **entitlements set** (which EE sub-features are enabled,
for self-hosted pricing tiers below full Enterprise), an **expiry**, holder details, and a
**`allowedJobs`** limit (workflow-execution metering). EE features and the EE main guard
are gated at runtime by license validity + entitlement; job creation is metered per
calendar month against `allowedJobs`. Admins upload and view the license in Settings.

An **optional, additive online check-in** layer provides revocation/refresh when
connectivity exists, without compromising the air-gapped offline floor.

Licensing is **mandatory when present and cannot be disabled** — there is no enforcement
off-switch. The only "off" is Community Edition (CE).

## Goals

- One licence abstraction (`LicenceManager`), replacing the three checkers.
- Fully offline verification of Keygen signed `.lic` files (Ed25519, JDK-native).
- License-carried, typed **feature entitlements** gating EE sub-features.
- Runtime EE main guard layered on top of the existing static `bytechef.edition` wiring.
- Per-calendar-month **job-count enforcement** (`allowedJobs`), license-driven.
- Configurable **expiry grace period** with hard-block afterward.
- Admin **Settings UI** to upload, view, replace, and remove the license.
- Optional **online check-in** for revocation, additive and off by default.
- Survives restarts (DB-persisted) and supports **env/file bootstrap** for IaC/air-gapped.

## Non-Goals

- Seat/user-count enforcement (`maxUsers` is carried and displayed only).
- Account-wide (cross-instance) job pooling — limits are **per-instance**.
- Encrypted license files — we use signed-but-unencrypted (`base64+ed25519`); integrity is
  the requirement, not confidentiality of the dataset.
- Sweeping every EE code path with a guard — runtime feature gating is scoped to the
  enumerated EE feature facades (see Section C).

## Key Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Signed `.lic` file, Ed25519, offline | Air-gapped self-hosted; no per-check network. |
| 2 | Layered: `bytechef.edition=ee` wires beans; license gates function at runtime | Runtime license upload without restart; bean wiring stays stable. |
| 3 | Typed `LicenceFeature` enum; `.lic` carries string keys → mapped to enum | Compile-time safety in code; issuer/code can drift safely (unknown keys ignored). |
| 4 | Single-row DB table + env/file bootstrap | Cluster-consistent, UI-uploadable, IaC-friendly. |
| 5 | Hard-block EE features on missing/expired/tampered; expiry grace period; never refuse boot | Operationally safe; CE core always runs. |
| 6 | `getAllowedJobs()` metered per calendar month, per-instance; block at limit; `-1` = unlimited | License value enables/disables metering. |
| 7 | Online check-in optional & additive (off by default) | Revocation lever when online; zero impact air-gapped. |
| 8 | British `licence` for Java/modules; "License" in UI copy | Zero rename churn vs existing `com.bytechef.platform.licence`. |

## Naming

Existing code uses British **`licence`** (package `com.bytechef.platform.licence`, module
`server/libs/licence`). Java packages, classes, modules, and config keys use **`licence`**.
User-facing UI strings use **"License"**. (Open for review — could rename everything to
`license` at the cost of churn.)

---

## Section A — Module layout & the layered manager

Remove: `LicenceChecker`, `NoOpLicenceChecker`, `SelfHostedLicenceChecker`,
`CloudLicenceChecker`, and their `settings.gradle.kts` lines (170–171, 702) where replaced.

```
server/libs/licence/
  licence-api/        (CE-visible)
                      LicenceManager, Licence, LicenceFeature,
                      LicenceStatus, LicenceException
  licence-service/    (CE)  NoOpLicenceManager  @ConditionalOnCEVersion
                            status = CE, allowedJobs = -1, no EE features
server/ee/libs/licence/
  licence-service/    (EE)  OfflineLicenceManager  @ConditionalOnEEVersion
                            Ed25519 verify, .lic decode, in-memory cache,
                            grace logic, optional online check-in
                            + Licence (JDBC entity) + LicenceRepository (single-row)
                            + LicenceFileParser, Ed25519Verifier
  licence-graphql/    (EE)  LicenceGraphQlController (admin-only)
```

(Module names `licence-impl` are renamed to `licence-service` to match the repo convention
that Spring Data JDBC repositories live in `*-service` modules, never `*-api`.)

### `LicenceManager` (CE api)

```java
public interface LicenceManager {
    LicenceStatus getStatus();                  // CE, VALID, GRACE, EXPIRED, MISSING, INVALID
    Optional<Licence> getLicence();             // decoded view for UI
    boolean isFeatureEnabled(LicenceFeature f); // status in {VALID,GRACE} AND entitled
    void checkFeature(LicenceFeature f);        // throws LicenceException otherwise
    long getAllowedJobs();                      // -1 = unlimited
    Licence upload(byte[] licFileBytes);        // verify + persist + swap cache
    void delete();                              // admin removes license
}
```

Why the interface lives in the **CE** api module: the job-creation guard is CE code and
must compile against `LicenceManager`. Spring injects `NoOpLicenceManager` (CE) or
`OfflineLicenceManager` (EE) by edition; call sites are edition-agnostic. This mirrors the
existing `@ConditionalOnCEVersion`/`@ConditionalOnEEVersion` bean-pairing convention.

### `LicenceStatus`

`CE` (no EE), `VALID`, `GRACE` (expired but within grace window), `EXPIRED` (past grace),
`MISSING` (edition=ee, none uploaded), `INVALID` (signature/tamper failure or
revoked/suspended via check-in). Treated as "EE-active" only when `VALID` or `GRACE`.

Persisted as INT ordinal if ever stored; append new values at end (enum-ordinal stability
convention).

---

## Section B — Domain model & offline verification

### `Licence` (immutable record)

```java
record Licence(
    String id,
    String holderName,
    String holderEmail,
    Instant issuedAt,
    Instant expiresAt,
    Set<LicenceFeature> features,   // entitlements
    long allowedJobs,               // -1 = unlimited
    Integer maxUsers)               // displayed only; enforcement is future
```

### `LicenceFeature` enum (starter set — extensible)

Drawn from existing EE settings features:
`SSO`, `AUDIT_LOG`, `CUSTOM_COMPONENTS`, `COMPONENT_POLICIES`, `API_CONNECTORS`,
`AI_PROVIDERS`, `AI_COPILOT`, `GIT_SYNC`, `ADMIN_API_KEYS`, `CONNECTION_VISIBILITY`,
`MCP_SERVER`.

The `.lic` dataset carries feature **string keys**; the parser maps known keys to enum
values and **ignores unknown keys with a warning** (issuer can list features the running
version doesn't know yet). Each enum value has a stable wire key (e.g. `SSO` ↔ `"sso"`).

### Offline verification (JDK-native Ed25519 — no BouncyCastle)

1. Strip `-----BEGIN/END LICENSE FILE-----` armor; base64-decode the envelope
   `{ "enc": "...", "sig": "...", "alg": "base64+ed25519" }`.
2. Verify Ed25519 `sig` over the signing string `license/<enc>` with the baked-in account
   public key via `Signature.getInstance("Ed25519")`. Failure → `INVALID`.
3. Base64-decode `enc` → license dataset JSON.
4. Map dataset → `Licence`: `expiry` → `expiresAt`; license `metadata` →
   `allowedJobs` / `features` / holder fields; `created` → `issuedAt`; `id` → `id`.
5. Cache the decoded `Licence` in a `volatile` field. Expiry/grace is recomputed cheaply
   per call against `Instant.now()` (no re-verify needed).

**Public key:** baked-in constant in EE `licence-service`, overridable via
`bytechef.licence.public-key`. *Placeholder must be filled with the real Keygen account
verify key* — clearly marked `TODO`-free comment ("replace with production key").

### Online check-in (optional, additive)

- Gated by `bytechef.licence.check-in.enabled` (default `false`) and
  `bytechef.licence.check-in.interval` (default `PT24H`).
- EE-only `@Scheduled` task in `OfflineLicenceManager`. When enabled and a license is
  cached, call Keygen's validate endpoint using the license id/key embedded in the verified
  dataset + `bytechef.licence.account-id`.
- Result `SUSPENDED` / `REVOKED` / `EXPIRED` → set cached status to `INVALID` (→ grace →
  hard-block). Network failure / disabled → no-op (offline behavior unchanged).
- The signed `.lic` remains the source of truth for **entitlements**; check-in contributes
  only a **revocation/expiry** signal. Exact endpoint/auth is an implementation detail for
  the plan.

---

## Section C — Enforcement

### EE feature guard (EE main guard + sub-feature gating)

`checkFeature(LicenceFeature)` throws `LicenceException` when status ∉ {VALID, GRACE} or the
feature is not entitled. `NoOpLicenceManager` (CE) throws for every feature → CE never
reaches EE behavior even where a bean exists.

Applied at the **facade entry points** of the enumerated EE features (Section B list).
A shared server-side helper mirrors the client `EEVersion` gate. Broader code-path sweeps
are out of scope for this spec.

### Job-count metering (CE + EE, license-driven)

**Metering point (decision):** the guard lives at the **platform layer** in
`PrincipalJobFacadeImpl` — `createJob(JobParametersDTO, long jobPrincipalId, PlatformType)`
and `createJobWithoutDispatch(...)`. This counts **top-level production workflow
executions only**:
- **Excludes** sub-workflow/child jobs (`createChildJob`, `createPrincipalLinkedJob`) — a
  workflow that calls sub-flows is not billed multiple times.
- **Excludes** editor "Test run" executions (the `JobSyncExecutor` path) — testing does
  not burn quota.
- Keeps the **atlas core engine decoupled** from the licence (a platform/commercial
  concern); only `platform-workflow-execution-service` depends on `licence-api`.

**Mechanism — per-tenant monthly usage counter (not a re-derived `COUNT`):** because the
guard meters a specific set (top-level, principal-linked) rather than every `job` row, a
`COUNT(*)` over `job` cannot cleanly reproduce that set. Instead, a dedicated
`licence_job_usage` table (one row per `year_month`, per tenant schema) is incremented
atomically at the guard point. The increment is by-construction exactly the metered set,
and the row also feeds the UI's current-month usage display.

Guard logic (in `LicenceJobUsageService`, called by `PrincipalJobFacadeImpl`):
```
long allowed = licenceManager.getAllowedJobs();
if (allowed < 0) return;                     // CE NoOp = -1; EE unlimited = -1 → no metering
String ym = YearMonth.now(clock).toString(); // e.g. "2026-06"
// upsert row for ym, then atomic guarded increment:
//   INSERT INTO licence_job_usage(year_month, count, ...) VALUES(:ym, 0, ...)
//     ON CONFLICT (year_month) DO NOTHING;
//   UPDATE licence_job_usage SET count = count + 1
//     WHERE year_month = :ym AND count < :allowed;
// if 0 rows updated → throw JobLimitExceededException
```
The atomic guarded `UPDATE` makes the check-and-increment race-free without locking.

- **Per-instance / multi-tenant:** the counter lives in the current tenant schema, so the
  limit applies per-tenant; for the standard single-tenant self-hosted case (`public`) this
  equals "instance jobs this month." No central counter; fully offline.
- In CE (and EE with an unlimited license) `getAllowedJobs()` returns `-1`, so the guard
  short-circuits before touching `licence_job_usage` — the table stays dormant.
- The month boundary uses an injectable `Clock` (`YearMonth.now(clock)`) so tests are
  deterministic.

---

## Section D — Settings UI (EE, admin-only)

New page: `client/src/ee/pages/settings/platform/license/License.tsx`, wrapped
`PrivateRoute[AUTHORITIES.ADMIN] → EEVersion → LazyLoadWrapper`. Nav item **"License"**
added to `platformSettingsRoutes.children` and `.navItems` in `client/src/routes.tsx`.

States:
- **No license:** upload card — drag-drop `.lic` (mirrors `UploadCustomComponentDialog`,
  `accept=".lic"`), `X-XSRF-TOKEN` header.
- **License present:** detail view — holder name/email, **status badge** (Valid /
  In grace until `<date>` / Expired / Invalid), issued + expiry dates, **enabled features**
  as chips, **allowedJobs** + current-month usage, max users; **Replace** + **Remove**
  actions (Remove behind a confirm dialog).

GraphQL domain `client/src/graphql/platform/license/`:
- `licence` query → license detail + status + current-month job usage.
- `uploadLicence(file)` mutation (or REST multipart, mirroring custom-components upload).
- `deleteLicence` mutation.

Add the EE `licence-graphql` schema path to `client/codegen.ts`; regenerate
`graphql.ts` / `graphql-types.ts`.

Backend: `LicenceGraphQlController` (`@Controller @ConditionalOnEEVersion`,
admin-authorized) delegating to `LicenceManager`. Upload accepts the `.lic` bytes; query
returns the decoded `Licence` + `LicenceStatus` + current-month job count.

---

## Section E — Config, migration, testing

### Config (`bytechef.licence.*`)

| Key | Default | Purpose |
|-----|---------|---------|
| `bytechef.licence.path` | _(unset)_ | Bootstrap `.lic` file path; seeds DB on boot if empty. |
| `BYTECHEF_LICENSE` (env) | _(unset)_ | Alternative bootstrap: inline `.lic` contents. |
| `bytechef.licence.public-key` | baked-in | Override the Ed25519 verify key. |
| `bytechef.licence.account-id` | _(unset)_ | Keygen account id (online check-in). |
| `bytechef.licence.grace-period-days` | `14` | Days EE features keep working after expiry. |
| `bytechef.licence.check-in.enabled` | `false` | Enable optional online revocation check. |
| `bytechef.licence.check-in.interval` | `PT24H` | Check-in cadence. |

No `enabled` flag — licensing is mandatory. Bootstrap order: env/file → seeds the single DB
row if empty on boot; UI upload overwrites.

### Migration

- New **`licence`** single-row table (platform Liquibase changelog): id, raw `.lic` blob,
  decoded summary columns (optional), audit columns.
- New **`licence_job_usage`** table: `year_month VARCHAR PRIMARY KEY`, `count BIGINT NOT
  NULL`, audit columns. One row per month, per tenant schema.
- Delete the 3 checker classes + `LicenceChecker` interface; remove/rename
  `settings.gradle.kts` entries; rewire any references (none enforce today).
- EE files carry the ByteChef Enterprise license header + `@version ee` Javadoc (Spotless
  selects header by content, so this is mandatory for `server/ee/` files).

### Testing

- **Unit (CE api / EE service):**
  - Envelope parse + Ed25519 verify: valid, tampered-signature, tampered-payload, expired,
    within-grace fixtures (generated with a throwaway test keypair; test public key
    injected via `bytechef.licence.public-key`).
  - Feature mapping incl. unknown-key skip; `allowedJobs` parsing (number/string/absent).
  - `NoOpLicenceManager`: CE status, unlimited jobs, all features denied.
  - Grace-window math (expiry boundary, grace boundary, past grace).
- **Job-limit guard** (`LicenceJobUsageService`): under / at / over limit (atomic guarded
  increment returns 0 rows → throw); `-1` bypass (table untouched); month rollover via
  injected `Clock`; concurrent increments don't exceed the limit.
- **Online check-in:** REVOKED/SUSPENDED → INVALID; network failure → no status change
  (mock the HTTP client; do not hit Keygen in tests).
- **Integration (`*IntTest`):** upload → persist → reload from DB → status; bootstrap from
  file seeds DB.
- **Client:** License page render states (none / valid / grace / expired / invalid) +
  upload hook (success + error) + admin/EE gating.

## Open Questions (for reviewer)

1. British `licence` in code vs renaming to `license` (churn).
2. `LicenceFeature` starter set — correct values? any missing EE features?
3. Per-EE-feature gating scoped to the 11 enumerated facades for this spec — acceptable, or
   gate more/less?
4. Metering point = `PrincipalJobFacadeImpl` (platform), top-level production jobs only,
   per-tenant monthly usage counter (confirmed).
5. Online check-in endpoint/auth specifics (deferred to the implementation plan).
