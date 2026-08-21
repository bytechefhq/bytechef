# AI Gateway guardrail hardening — design

Status: implemented (Phase 1 + Phase 2a streaming redaction + Phase 2b per-project scoping)
Date: 2026-07-22
Area: `server/ee/libs/automation/automation-ai/automation-ai-gateway`, `server/ee/libs/platform/platform-ai/platform-ai-gateway`

## Motivation

An evaluation of the AI Gateway against a competitor's enterprise DLP / prompt-injection
feature list surfaced concrete gaps. The gateway's own inline guardrails
(`AiGatewayGuardrails`) previously did only three things, all on the **request** direction of
the **chat-completion** path:

1. PII redaction — 5 regex types (email, US SSN, credit card, phone, IPv4).
2. Blocked-term deny-list — case-insensitive substring block.
3. Model-based safety moderation — SAFE/UNSAFE classifier, fail-open.

The workflow/agent guardrail layer (`server/libs/modules/components/ai/agent/guardrails`) is far
richer, but it runs **inside workflow execution** and only when an author wires it into an AI
Agent node. It does not protect raw traffic through the network gateway (`/api/ai-gateway/v1/*`),
which is what an enterprise control layer is expected to cover.

Gaps identified at the gateway boundary:

| # | Gap | Phase |
|---|-----|-------|
| 1 | No scanning of model **responses** (inbound/prompt-only) | **1 (this spec)** |
| 2 | No developer-**secret**/API-key detection (only PII) | **1 (this spec)** |
| 3 | No **prompt-injection / jailbreak** detection at the gateway | **1 (this spec)** |
| 5 | **Embeddings** path bypasses guardrails entirely | **1 (this spec)** |
| 6 | No per-**project** guardrail scoping (workspace only) | **2b (implemented)** |
| 4 | No Datadog/Splunk native observability sinks (only generic OTLP) | **not a code gap — OTLP already reaches both; documented** |
| 6b | No per-**API-key** guardrail scoping | 2 (out of scope — no per-key settings store) |

This spec covers Phase 1: closing gaps 1, 2, 3, and 5 — all within the gateway guardrail
subsystem, mirroring the existing moderation/redaction patterns so the change is additive and
low-risk.

## Design principles (unchanged from the existing guardrails)

- **Off by default.** Every new capability is gated by a global property that defaults to
  `false` (or, for the injection classifier, an unset model identifier), plus an optional
  per-workspace override. Effective policy = global OR workspace, matching the existing
  `redactPii` / `blockedTerms` / `moderationEnabled` union semantics.
- **Redaction rewrites and proceeds; classification blocks.** Redaction (PII, secrets) is
  deterministic, side-effect-free, and never fails the request. Classification (moderation,
  injection) blocks with `AiGatewayGuardrailException` → HTTP 422 `guardrail_violation`. The
  wire message never echoes offending content.
- **Classifiers fail open.** An injection-model outage must not hard-block all gateway traffic,
  exactly as moderation already does.
- **Regexes stay ReDoS-safe.** No nested optional quantifiers (SpotBugs constraint). All secret
  patterns are anchored, fixed-length, or single-separator.

## Phase 1 changes

### 1. Secret / API-key detection (gap 2)

`AiGatewayGuardrails` gains a second redaction pass, `redactSecrets(String)`, applied right after
PII redaction (so both run before blocked-terms/moderation/injection see the text). It recognises
high-precision developer-secret shapes and replaces each with `[REDACTED_SECRET]`:

- AWS access key id — `AKIA` + 16 upper-alnum
- GitHub tokens — `ghp_/gho_/ghu_/ghs_/ghr_` + 36 alnum, and `github_pat_` fine-grained tokens
- OpenAI keys — `sk-` / `sk-proj-` + ≥20 token chars
- Slack tokens — `xox[baprs]-` + token body
- Stripe secret/restricted keys — `sk_live_` / `rk_live_` + 24 alnum
- Google API keys — `AIza` + 35 token chars
- JWTs — three base64url segments separated by dots
- PEM private-key blocks — `-----BEGIN … PRIVATE KEY-----`

Rationale for a curated list rather than a generic entropy detector: the gateway redactor runs
on every message of every request and must be deterministic and allocation-cheap. The
entropy/random-string detection lives in the workflow-layer `SecretKeyDetectorUtils` for authors
who want it; the gateway ships the high-signal, low-false-positive subset.

Config: global `bytechef.ai.gateway.guardrails.secret-redaction-enabled` (default `false`) OR
workspace `redactSecrets`.

### 2. Prompt-injection / jailbreak detection (gap 3)

New SPI in `platform-ai-gateway-api`, mirroring `AiGatewayModerationClassifier`:

```java
public interface AiGatewayInjectionClassifier {
    boolean isInjection(String content); // fail open
}
```

Impl `PromptBasedInjectionClassifier` in `automation-ai-gateway-service`, mirroring
`PromptBasedModerationClassifier`: registered only when
`bytechef.ai.gateway.guardrails.injection-model` names a catalog model identifier, resolves the
model to its provider, calls it through the gateway's own chat-model factory with a fixed
INJECTION/CLEAN instruction covering the documented techniques (system-prompt override / "ignore
previous instructions", role/persona shift, data-exfiltration requests, indirect injection from
quoted content), and fails open on any error.

`AiGatewayGuardrails.apply` runs the injection check last (after moderation), on the redacted
text, and blocks flagged requests with `AiGatewayGuardrailException`.

Config: global `bytechef.ai.gateway.guardrails.injection-detection-enabled` (default `false`) OR
workspace `injectionDetectionEnabled`, AND a classifier bean present (gated by `injection-model`).

### 3. Dual-directional response scanning (gap 1)

`AiGatewayGuardrails` gains `redactResponseContent(String, workspaceId)`, which applies the PII
and secret redactors to model output when response scanning is active. `AiGatewayFacadeImpl`
calls it on the assembled **non-streaming** completion, before tracing and before returning, so
both the returned body and the stored trace see redacted output.

Response scanning is **redaction only** — it never blocks. Blocked-terms and moderation express
intent about the *prompt*; applying them to a completion the caller already paid for would waste
spend and surprise callers. The goal here matches the DLP framing: stop internal
secrets/PII from leaking back in the completion, without discarding the answer.

Config: global `bytechef.ai.gateway.guardrails.response-scan-enabled` (default `false`) OR
workspace `scanResponses`.

**Streaming (Phase 1 limitation, closed in Phase 2a).** The SSE streaming path
(`chatCompletionStream`) emits tokens incrementally; a secret or PII value can straddle two
chunks, so naive per-chunk redaction is unreliable. Phase 1 applied response redaction to the
**non-streaming** path only. Phase 2a adds opt-in streaming redaction — see below.

### 5. Streaming response redaction (Phase 2a)

`StreamingResponseRedactor` is a stateful, single-subscription redactor that masks a streamed
completion without buffering it whole. It keeps a bounded lookahead window and, each fragment,
emits only the leading portion of its buffer up to a **safe cut** — a position no matched span
crosses. A value straddling the tentative cut pulls the cut back to that value's start so the
whole match stays buffered and is redacted as one unit (`AiGatewayGuardrails.sensitiveMatchRanges`
locates the spans); a value still arriving sits inside the retained window until it completes or
is flushed at stream end. Because no complete match crosses a cut, the concatenation of every
`push` plus the final `flush` equals `redactAll(fullStream)`. The window bounds latency and the
worst case: a value still incomplete (not yet matchable) and longer than the window may have a
prefix emitted before its pattern matches — the documented trade-off of scanning a stream without
buffering it whole (default window 512 covers every fixed-shape key/token and typical JWTs).

Wiring: `AiGatewayFacadeImpl.chatCompletionStreamInternal` obtains a redactor from
`newStreamingResponseRedactor(workspaceId)` (null unless active — see below). When non-null, each
delta is masked through the redactor and the terminal `finish_reason` is **deferred** onto the
redactor's flush chunk (via an `AtomicReference`), so the client never sees `stop` before the
masked tail. When null, the streaming path is byte-for-byte unchanged. Raw output is still
accumulated for the request log (the trace path keeps its own redaction control).

Activation is intentionally an operator-level decision (holding a lookahead window trades away
some incremental latency): streaming redaction is active only when response scanning is effective
for the workspace (`response-scan-enabled` / `scanResponses`) **AND** the global
`response-scan-streaming-enabled` flag is set. No new per-workspace field.

### 6. Per-project guardrail scoping (Phase 2b)

Guardrail policy previously resolved from global properties + the request workspace only. Phase 2b
adds a **project** layer: `AiGatewayProjectSettings` (record in `platform-ai-gateway-api`),
persisted as a `PROJECT`-scoped `Property` row (`ai_gateway_project_settings`, keyed by numeric
project id) via `AiGatewayProjectSettingsService` — the same property-store pattern the workspace
settings use, so **no new table / migration**. It carries only the guardrail fields (`redactPii`,
`redactSecrets`, `blockedTerms`, `moderationEnabled`, `injectionDetectionEnabled`,
`scanResponses`).

`AiGatewayGuardrails.resolvePolicy(workspaceId, projectId)` unions the project overrides on top of
global + workspace with the **same additive semantics**: a project can enable a guardrail the
workspace did not, and its blocked terms union in, but it never turns a workspace/global guardrail
off (null = inherit). The four public methods (`apply`, `applyToInputs`, `redactResponse`,
`newStreamingResponseRedactor`) gained a `projectId` overload; the original overloads delegate with
`projectId = null`, so existing callers and behavior are unchanged. `AiGatewayFacadeImpl` resolves
the numeric project id from the request's `project_id` tag (a per-workspace slug →
`resolveProjectId`) and threads it into all four call sites. The project settings service is a
Spring-optional `@Nullable` constructor dep — absent bean → project layer is simply skipped.

Config surface: `aiGatewayProjectSettings(projectId)` GraphQL query +
`updateAiGatewayProjectSettings(input)` mutation (`AiGatewayProjectSettingsFacade`, admin-only for
both read and write — project guardrail config is administrative). Client:
`AiGatewayProjectGuardrailsSection` renders the six guardrail toggles inside the project **edit**
dialog (only for a saved project with a numeric id), loading/saving through the operation above
with its own query invalidation.

### 4. Embeddings coverage (gap 5)

`AiGatewayFacadeImpl.embedding` now runs the request-side guardrails over each input string
before building the `EmbeddingRequest`: PII + secret redaction (rewrite), blocked-terms and
injection checks (block). Moderation is intentionally **not** run on embeddings — embedding
inputs are documents/records, not conversational prompts, and safety moderation there produces
noise; PII/secret redaction and injection/blocked-term blocking are the relevant controls. A
new `applyToInputs(List<String>, workspaceId)` on `AiGatewayGuardrails` returns the redacted
input list and throws on a blocked/injected input.

## Configuration surface (summary)

Global properties (all default off):

```
bytechef.ai.gateway.guardrails.pii-redaction-enabled        (existing)
bytechef.ai.gateway.guardrails.blocked-terms                (existing)
bytechef.ai.gateway.guardrails.moderation-enabled           (existing)
bytechef.ai.gateway.guardrails.moderation-model             (existing)
bytechef.ai.gateway.guardrails.secret-redaction-enabled     (new)
bytechef.ai.gateway.guardrails.injection-detection-enabled  (new)
bytechef.ai.gateway.guardrails.injection-model              (new)
bytechef.ai.gateway.guardrails.response-scan-enabled        (new)
bytechef.ai.gateway.guardrails.response-scan-streaming-enabled (new, Phase 2a; operator-level)
```

Per-workspace overrides on `AiGatewayWorkspaceSettings` (union with global):

```
redactPii, blockedTerms, moderationEnabled                  (existing)
redactSecrets, injectionDetectionEnabled, scanResponses     (new)
```

Surfaced through the existing `aiGatewayWorkspaceSettings` GraphQL query / mutation and the AI
Gateway settings page.

Per-project overrides on `AiGatewayProjectSettings` (Phase 2b; union on top of workspace):
`redactPii`, `redactSecrets`, `blockedTerms`, `moderationEnabled`, `injectionDetectionEnabled`,
`scanResponses` — via the `aiGatewayProjectSettings` GraphQL query / mutation (admin-only).

## Evaluation order

Request path (`apply`): redact PII → redact secrets → blocked terms → moderation → injection.
Every check sees the fully redacted text. Embeddings path (`applyToInputs`): redact PII → redact
secrets → blocked terms → injection (no moderation). Response path
(`redactResponseContent`): redact PII → redact secrets (no blocking).

## Testing

- `AiGatewayGuardrailsTest` — extended for secret redaction (each pattern), response redaction,
  the injection classifier hook, embeddings redaction/blocking, and the union of new
  global/workspace toggles.
- `PromptBasedInjectionClassifierTest` — new, mirroring `PromptBasedModerationClassifierTest`:
  flagged/clean verdicts, blank/empty input, unknown model, and fail-open on classifier error.

## Gap 4 — Datadog/Splunk (resolved as documentation, not code)

Investigation showed gap 4 is **not a code gap**. ByteChef observability is standard Spring Boot
Actuator + Micrometer exporting over OTLP/HTTP (`observability-config`), and the gateway's
`ai_gateway.*` meters and spans ride that pipeline. Datadog (Agent OTLP receiver / intake) and
Splunk (OTel Collector / Observability Cloud ingest) both accept OTLP, so both are reachable today
by pointing the existing per-signal endpoints at them — no ByteChef-specific integration is needed.
Adding a dedicated exporter (e.g. `micrometer-registry-datadog`) would be a redundant convenience
that adds a build dependency for no capability gain. The resolution is the concrete wiring added to
`docs/content/docs/self-hosting/observability/index.mdx` ("Datadog and Splunk (direct OTLP)").

## Out of scope (Phase 2)

- **Gap 6b** — per-**API-key** guardrail scoping. Per-project is done (Phase 2b); per-API-key has
  no settings-store primitive yet (unlike PROJECT, there is no api-key `Property` scope), so it
  needs a dedicated store before the same union-overlay approach can extend to it.
