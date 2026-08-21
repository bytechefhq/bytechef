# AI Gateway hardening: config parity, routing cache, streaming failover, remote-client analysis

Date: 2026-07-19
Status: Landed (config, cache, streaming, guardrails/moderation) + Decision (remote-client)

## Context

A review of the EE AI Gateway (`ai-gateway-app` + `platform-ai-gateway-*` +
`automation-ai-gateway-*`) surfaced several real gaps. This records what was fixed and the
one deliberate non-change (the distributed remote-client).

## Fixed

### 1. Config-server wiring (the gateway was disabled in a microservices topology)

- The config-server file was `llm-gateway-app.yml`, but the app's `spring.application.name` is
  `ai-gateway-app`, so Spring Cloud Config never served it. Renamed to `ai-gateway-app.yml`.
- Inside it the enable flag was `bytechef.ai.ai-gateway.enabled`, while every
  `@ConditionalOnProperty` in code checks `bytechef.ai.gateway.enabled`. Corrected the nesting.
- Every sibling EE app ships `-dev.yml` (server port + livereload) and `-prod.yml`; `ai-gateway-app`
  had only the base file. Added `ai-gateway-app-dev.yml` (port 7999, livereload 35736) and an empty
  `ai-gateway-app-prod.yml` for parity.

### 2. Response cache on the routing path

The response cache is keyed on request content (model-agnostic), but only `chatCompletionDirect`
consulted it; requests carrying a routing policy always bypassed the cache. `chatCompletionWithRouting`
now performs the same lookup at entry and caches after a successful routed call.

### 3. Streaming pre-first-token cross-deployment failover

`AiGatewayRetryHandler.executeStreamWithRetry` existed but had **zero callers** because it was unsafe:
it retried and failed over unconditionally, so a failure after some tokens had already been streamed
would re-subscribe and replay already-flushed SSE tokens.

- `tryDeploymentStream` now tracks whether any element has been emitted (`AtomicBoolean`) and gates
  BOTH same-deployment retry and cross-deployment failover on it. Once the first token is out, errors
  propagate terminally. (Unit-tested: fails over before first token; does NOT fail over or replay
  after; propagates when all deployments fail before emitting.)
- `chatCompletionStreamInternal` now, when a routing policy is set, selects the ordered deployments
  (`selectRoutedDeployments`) and streams through `executeStreamWithRetry`, resolving each deployment's
  model/provider/prompt per attempt. The request log finalizes for whichever deployment actually
  streamed a token (captured on first emit), falling back to the routed primary for a pre-token
  terminal error. (Verified with a StepVerifier facade test that drives the real per-deployment
  builder through a mocked `executeStreamWithRetry`.)

Streaming still does not cache (a live token stream isn't a cacheable single response) — unchanged and
intentional.

### 4. Inline content guardrails + model-based moderation (landed 2026-07-20)

`AiGatewayGuardrails` (`automation-ai-gateway-service`, `guardrail` package) runs in
`AiGatewayFacadeImpl` on both the sync and streaming paths, after prompt resolution and before
routing. Three guardrails, all off by default, each resolvable globally (properties) or
per-workspace (`AiGatewayWorkspaceSettings`, which gained `blockedTerms` + `moderationEnabled`):

- **PII redaction** — regex masking (email / US SSN / credit card / phone / IPv4 →
  `[REDACTED_*]`); active when `bytechef.ai.gateway.guardrails.pii-redaction-enabled` OR the
  workspace's existing `redactPii` setting is on. That setting previously only drove
  trace-payload digesting; it now also masks the upstream prompt. Patterns are written without
  nested optional quantifiers (SpotBugs ReDoS clean).
- **Blocked terms** — union of the global `…guardrails.blocked-terms` CSV and the workspace's
  `blockedTerms`; case-insensitive containment → reject.
- **Model-based moderation** — `AiGatewayModerationClassifier` SPI in
  `platform-ai-gateway-api`; `PromptBasedModerationClassifier` registers only when
  `…guardrails.moderation-model` names a catalog model identifier, resolves it →
  provider → `AiGatewayChatModelFactory.getChatModel`, asks for a one-word SAFE/UNSAFE
  verdict, and **fails open** on any resolution/call error. Active when
  `…guardrails.moderation-enabled` OR the workspace's `moderationEnabled` is on AND the
  classifier bean exists (guardrails takes it as a Spring-optional `@Nullable` constructor
  dependency).

A workspace-settings lookup failure degrades to global-only guardrails rather than failing the
request. Violations throw `AiGatewayGuardrailException` (`platform-ai-gateway-api`, so the
public-rest handler can see it) → HTTP **422** `guardrail_violation`; the wire message names
neither the offending content nor the matched term. Ordering: redact → blocked-terms →
moderation, so later checks see redacted text. Surfaced in the Gateway Settings GraphQL +
client form. Not verified here: a live moderation-model call (classifier covered by mocked
`ChatModel` tests only) — smoke-test against a real provider on first deploy.

## Deliberate non-change: distributed `ai-gateway-remote-client`

The review noted "no `ai-gateway-remote-client` for distributed deploy (monolith-only)." Investigation:

- `AiGatewayFacade` has exactly **one** in-process cross-module caller: `AiEvalExperimentExecutor`
  (`automation-ai-eval-experiment-service`), and it calls **only** `chatCompletion(request, headers, null)`.
- Both `automation-ai-gateway-service` (the impl) and `automation-ai-eval-experiment-service` are
  included **only in `ai-gateway-app`** — they are co-located. No cross-app boundary exists today, so
  there is no app that has the eval caller but lacks the `AiGatewayFacade` bean.

Therefore a remote-client is **not needed in the current topology**, and building one now would be dead,
untestable speculative infrastructure. It becomes necessary only if `automation-ai-eval-experiment-service`
is split into an app that does **not** also run `automation-ai-gateway-service`.

**Design for when that split happens** (follow the EE remote-client pattern):

- New module `automation-ai-gateway-remote-client` with a `@Component @ConditionalOnEEVersion`
  `AiGatewayFacade` implementation, gated `@ConditionalOnMissingBean(AiGatewayFacadeImpl.class)` (or a
  property) so it never conflicts with the real impl in `ai-gateway-app`.
- Implement `chatCompletion(...)` (the only method eval uses) as a `RestClient`/`WebClient` call to the
  gateway app's OpenAI-compatible endpoint `POST /api/ai-gateway/v1/chat/completions` (via
  `lb://ai-gateway-app`), mapping `AiGatewayChatCompletionRequest` ⇄ the OpenAI-format DTOs and passing
  the caller's API key. The streaming/embedding/score methods throw `UnsupportedOperationException`
  until a caller needs them (matching the established stub convention).
- Add the remote-client module to whatever app runs the split-out eval service.

This is left unbuilt on purpose: it cannot be verified without the split existing, and the co-located
topology does not exercise it.
