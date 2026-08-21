# T15 — SSRF Defenses — Design

- **Date:** 2026-06-21
- **Scope:** gecko remediation task T15 (SSRF / open redirect)
- **Source findings:** `gecko-security-report.md`, tracked in `gecko-remediation-tasks.md`
- **Branch:** `0_732` (continuation of the `gecko`-prefixed remediation stream)

## Overview

This spec adds Server-Side Request Forgery (SSRF) defenses to ByteChef's
genuinely-SSRF-prone outbound surfaces: surfaces where a user-supplied URL is
fetched/contacted by the server. The work is small because a complete,
battle-tested validator already exists in the EE tree
(`AiObservabilityUrlValidator`); this spec extracts that logic into a shared CE
primitive and applies it at the selected call sites behind a global
enable/disable + allowlist escape-hatch.

### What already exists (reused, not rebuilt)

`server/ee/libs/platform/platform-ai/platform-ai-observability/platform-ai-observability-api/src/main/java/com/bytechef/ee/platform/ai/observability/security/AiObservabilityUrlValidator.java`
already implements:

- scheme allowlist (`http`/`https` only),
- resolution of **every** A/AAAA record for the host (so a mixed public/private
  DNS answer cannot slip a private IP through),
- blocking of loopback, site-local (RFC 1918), any-local, multicast,
  link-local, IPv4 CGNAT `100.64.0.0/10`, and IPv6 unique-local `fc00::/7`,
- a documented DNS-rebinding limitation (the JDK `HttpClient` does not expose a
  resolver hook, so validation is per-attempt; re-validating at delivery is the
  mitigation).

This spec ports that logic to CE and makes the EE class delegate to it.

### Webhook enforcement plumbing already exists

For reference (not changed here): the trigger `webhookValidate` SPI hook and its
controller enforcement path already exist — `TriggerDefinition` sets
`workflowSyncValidation = (webhookValidate present)` and the webhook controller
routes through validate-then-execute. That is the T16 surface and is out of
scope here.

## Scope

**In scope** (the selected outbound surfaces):

1. **Outbound job/task webhooks** — validate the webhook URL at job creation
   (`JobServiceImpl`) and re-validate at delivery
   (`WebhookTaskStartedApplicationEventListener`).
2. **EE documentation fetch** — validate the user-supplied `documentationUrl`
   before scraping (`ApiConnectorAiServiceImpl`).
3. **Open redirect** — extend `RedirectValidator` to also reject redirect
   targets that resolve to private/loopback addresses.

**Explicit non-goals (deferred):**

- The central `HttpClientExecutor` (covers ~180 HTTP components). Applying SSRF
  blocking there is high-coverage but would require the allowlist for any
  self-hosted deployment that legitimately points an HTTP component at an
  internal API. Deferred to keep this cycle low-breakage.
- Infrastructure connections (JDBC / RabbitMQ / Cassandra hosts). Private hosts
  are normal and expected for self-hosted deployments (DB at `10.x`/`192.168.x`),
  so SSRF private-IP blocking there would break legitimate use. Deferred.
- T16 webhook signature/origin verification (separate spec).

## Components

### 1. Shared primitive — `UrlValidator` (CE)

New class in `server/libs/core/commons/commons-util`
(`com.bytechef.commons.util.UrlValidator`), Apache-licensed, JDK-only:

```java
public final class UrlValidator {

    public static void validate(String url, Set<String> allowedHosts);  // throws UrlValidationException

    public static boolean isValid(String url, Set<String> allowedHosts);
}
```

- `validate` throws `UrlValidationException` (new unchecked exception in the same
  package) when the URL is malformed, uses a non-http(s) scheme, or resolves to
  a blocked address.
- `allowedHosts` is an escape-hatch: a host (exact hostname match) in the set is
  permitted even if it resolves to a private address. (CIDR entries in the set
  are matched against resolved IPs; hostname entries match the literal host.)
- The logic mirrors `AiObservabilityUrlValidator`: scheme check, resolve all
  A/AAAA records, reject if any is loopback/site-local/any-local/multicast/
  link-local/CGNAT/IPv6-ULA unless allowlisted.

`AiObservabilityUrlValidator.validateExternalUrl(String)` is refactored to
delegate to `UrlValidator.validate(url, Set.of())`, removing the duplicate
implementation while preserving its existing callers.

### 2. Configuration

Two properties, default-secure:

- `bytechef.security.ssrf.enabled` (boolean, default `true`)
- `bytechef.security.ssrf.allowed-hosts` (list of hostnames/CIDRs permitted even
  if private)

Read via `@Value` at each Spring call site, matching the cycle-avoiding pattern
already used for the TOTP MFA settings (the low-level modules involved —
atlas-execution, platform-coordinator — do not depend on app-config, so
injecting `ApplicationProperties` there would invert the dependency). When
`enabled=false`, validation is skipped at every site.

### 3. Call-site changes

- **`JobServiceImpl`** (`atlas-execution-service`): in the existing webhook
  parameter validation, reject a webhook whose URL fails `UrlValidator.validate`
  at job creation (surfaces as a bad-request to the caller).
- **`WebhookTaskStartedApplicationEventListener`** (`platform-coordinator`):
  re-validate the webhook URL before the `RestTemplate` POST. On failure, skip
  the delivery and log a warning rather than throwing out of the listener. This
  is the TOCTOU/DNS-rebind mitigation — the save-time check alone is
  insufficient.
- **`RedirectValidator`** (`platform-webhook-rest-api`): inside
  `isValidRedirect`, after the existing scheme/host checks, reject a target that
  resolves to a private/loopback address. No new config here — a user-facing
  redirect to a private IP is never legitimate, so this is unconditional (still
  honors the existing relative-path / same-host fast-paths, which never resolve
  externally).
- **`ApiConnectorAiServiceImpl`** (EE): validate `documentationUrl` with
  `UrlValidator.validate` before `WebScrapeService.scrape`.

## Error handling

- `UrlValidator.validate` throws `UrlValidationException` (unchecked).
- Save-time sites (`JobServiceImpl`, `ApiConnectorAiServiceImpl`) let it
  propagate to the normal request-error path (rejected request).
- The delivery-time site (`WebhookTaskStartedApplicationEventListener`) catches
  it, logs a warning, and skips delivery — a blocked target must not crash the
  event listener or wedge the job.
- `RedirectValidator` returns `false` (its existing contract) for a blocked
  target rather than throwing.

## Testing

- **`UrlValidatorTest`**: public host passes; `127.0.0.1`, `10.0.0.1`,
  `192.168.1.1`, `169.254.169.254`, `::1`, `100.64.0.1` each blocked; non-http
  scheme blocked; malformed URL blocked; an allowlisted private host passes.
- **Per-site unit tests** (mock service deps):
  - `JobServiceImpl` rejects a private webhook URL, accepts a public one, and
    skips validation when `enabled=false`.
  - `WebhookTaskStartedApplicationEventListener` does not POST to a private URL
    and does POST to a public one.
  - `RedirectValidator` rejects a private redirect target, allows a public one,
    and still allows relative/same-host targets.
  - `ApiConnectorAiServiceImpl` rejects a private `documentationUrl`.

## Out of scope (recorded for the tracker)

- Central `HttpClientExecutor` SSRF enforcement (deferred — breakage/coverage
  tradeoff).
- Infra connection host validation: JDBC / RabbitMQ / Cassandra (deferred —
  private hosts are legitimate for self-hosted).
- DNS-rebinding cannot be fully closed with the JDK `HttpClient` (no resolver
  hook); per-attempt validation (including the delivery-time re-check) is the
  accepted mitigation, consistent with the existing EE validator.

## Defaults chosen (flag if these should change)

- SSRF protection **enabled by default**; `allowed-hosts` **empty by default**.
- EE `AiObservabilityUrlValidator` refactored to delegate to the new CE
  primitive (single implementation).
- `RedirectValidator` blocks private targets **unconditionally** (no allowlist).
