# T4 — Tenant-ID Validation & SQLi Prevention — Design

- **Date:** 2026-06-21
- **Scope:** gecko remediation task T4 (Phase 0 Critical — 2× CVSS 9.4 SQLi via tenant id → `SET search_path`)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)

## Overview

A tenant identifier reaches a raw `SET search_path TO <schema>` SQL statement by
string concatenation, and the identifier is never validated on the request
paths that set it. An attacker who controls the `CURRENT_TENANT_ID` header (or a
base64 tenant token) can inject arbitrary SQL into the schema name. This spec
closes every vector with a single central validation chokepoint plus
defense-in-depth at the SQL sinks and token boundaries.

### Why a chokepoint works

Every runtime path that sets the active tenant funnels through
`TenantContext.setCurrentTenantId(String)`:

- `runWithTenantId` and `callWithTenantId` both call `setCurrentTenantId`
  (verified: `TenantContext.java:44`, `:86`), and `resetCurrentTenantId` calls
  it with the default (`:79`).
- The SQL sinks read the value back via `TenantContext.getCurrentTenantId()` /
  `getCurrentDatabaseSchema()`.

So validating inside `setCurrentTenantId` rejects a malformed tenant id before
it can ever reach a sink — the header filter, both token parsers, and the
scheduler are all covered at once. The sink and parser changes below are
belt-and-suspenders / fail-fast, not the primary control.

### Real tenant-id format (informs the charset)

Live schemas are `bytechef_000001` … `bytechef_000005` plus the default
`public` — i.e. tenant ids are zero-padded numerics, and `public`. There are no
hyphens or letters in real ids. The derived schema name (`bytechef_<id>`,
`bytechef_vectorstore_<id>`, or `public`) must be a valid Postgres identifier.

**Canonical charset: `^[a-zA-Z0-9_]+$`.** It admits `000001` and `public`,
always yields a valid identifier, and excludes every injection metacharacter
(`"`, `;`, whitespace, `--`, `\`, etc.). We deliberately do **not** over-fit to
`^\d{6}$` (the zero-pad width is an implementation detail) and deliberately do
**not** allow the hyphen that the legacy `TenantRepository` pattern permitted (a
hyphen cannot appear in an unquoted identifier; no real id uses one).

## Components

### 1. Shared validator — `TenantIdValidator` (CE)

New class `com.bytechef.tenant.TenantIdValidator` in `core/tenant/tenant-api`
(Apache, JDK-only):

```java
public final class TenantIdValidator {
    public static boolean isValid(String tenantId);
    public static void validate(String tenantId);            // throws IllegalArgumentException
    public static void validateDatabaseSchema(String schema); // throws IllegalArgumentException
}
```

- Single compiled `Pattern` `^[a-zA-Z0-9_]+$`.
- `validate` is for tenant ids; `validateDatabaseSchema` applies the same pattern
  to an already-assembled schema name (clearer error message at the SQL sink).
  Both reject null/blank.
- `IllegalArgumentException` is chosen so it slots into the existing
  `Assert.notNull` contract already in `setCurrentTenantId` and the existing
  `TenantRepository` behavior.

### 2. Central chokepoint — `TenantContext.setCurrentTenantId`

Add `TenantIdValidator.validate(tenantId)` immediately after the existing
`Assert.notNull`. Every set/run/call path inherits it. `public` and numeric ids
pass; a malformed id throws before the value is stored.

### 3. SQL sink hardening (defense-in-depth)

Both raw-concat sinks validate the assembled schema name immediately before
building the statement:

- `BaseDataSource.setSearchPath` (CE, `tenant-api/sql`):
  `TenantIdValidator.validateDatabaseSchema(currentDatabaseSchema)` before
  `prepareStatement(SET_SEARCH_PATH_STATEMENT + currentDatabaseSchema)`.
- `MultiTenantDriverDelegate.setSearchPath` (EE scheduler):
  `TenantIdValidator.validateDatabaseSchema(databaseSchemaName)` before
  `statement.execute("SET search_path TO " + databaseSchemaName)`.

These are unreachable via the normal setter once (2) is in place (you cannot set
an invalid tenant), so they protect any future caller that assembles a schema
without going through `setCurrentTenantId`. The existing
`@SuppressFBWarnings("SQL_INJECTION_JDBC")` annotations stay, now backed by real
validation.

### 4. Token-parser fail-fast

- `WorkflowExecutionId.parse` (platform-api): validate the tenant segment
  (`items[0]`) with `TenantIdValidator.validate` before constructing the object.
- `JobResumeId.parse` (platform-workflow-execution-api): validate the tenant
  segment (`items[0]`) the same way (it already validates the UUID segment).

These give a clear error at the trust boundary rather than deferring to the
later `setCurrentTenantId` call.

### 5. Request-filter guard — `RemoteMultiTenantFilter` (EE)

Currently passes the raw header straight to `runWithTenantId` with no null/format
check (a null header NPEs the `Assert.notNull`; a malformed one would now throw
inside `setCurrentTenantId` and surface as a 500). Change: if the
`CURRENT_TENANT_ID` header is missing/blank or fails `TenantIdValidator.isValid`,
respond **400 Bad Request** and do not proceed. Otherwise run as today.

### 6. Consolidate existing EE validation

`TenantRepository.validateTenantId` (EE) currently holds its own
`^[a-zA-Z0-9_-]+$` pattern used by create/delete. Refactor it to delegate to
`TenantIdValidator.validate`. This is a deliberate tightening (drops the
never-used hyphen) and removes the duplicated regex.

## Error handling

- `TenantIdValidator.validate` / `validateDatabaseSchema` throw
  `IllegalArgumentException`.
- `setCurrentTenantId`, the sinks, and the parsers let it propagate (reject the
  operation).
- `RemoteMultiTenantFilter` converts a missing/invalid header into a 400
  response instead of letting it become a 500.

## Testing

- **`TenantIdValidatorTest`**: `public`, `000001`, `bytechef_000001` (schema)
  pass; `public"; DROP TABLE users; --`, `a b`, `a;b`, `a-b`, empty, null are
  rejected by `validate`/`validateDatabaseSchema`; `isValid` mirrors.
- **`TenantContext` test**: `setCurrentTenantId` accepts `public`/`000001`,
  rejects a malformed id; `callWithTenantId`/`runWithTenantId` reject a malformed
  id and still restore the prior tenant in `finally`.
- **`WorkflowExecutionId` / `JobResumeId`**: `parse` of a token whose tenant
  segment is malformed throws; a well-formed token still round-trips.
- **`RemoteMultiTenantFilter`** (EE): missing header → 400; malformed header →
  400; valid header → chain proceeds under the tenant.
- Sinks: covered transitively by `TenantIdValidatorTest` (the validation they
  call); no Connection-mock test is added because the normal setter path can no
  longer produce an invalid schema to drive them with.

## Out of scope

- Quoting/parameterizing `SET search_path` itself — Postgres does not support a
  bind parameter for `SET`, so whitelisting the identifier is the correct and
  only mitigation.
- Changing the tenant-id *generation* scheme (zero-padded numerics) — unchanged.

## Defaults chosen (flag if these should change)

- Canonical charset `^[a-zA-Z0-9_]+$` (no hyphen; not width-locked).
- Validation throws `IllegalArgumentException`; the remote filter maps
  missing/invalid to HTTP 400.
- EE `TenantRepository` consolidated onto the shared validator (tightens its
  charset by dropping the hyphen).
