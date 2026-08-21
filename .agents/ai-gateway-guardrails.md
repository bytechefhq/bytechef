<!-- Extracted from CLAUDE.md so the agent-facing reference does not sit in every prompt.
     Load this when working on the AI Gateway's own guardrail adapter, its project overlay, or the
     model-based moderation/injection classifier SPIs. For the shared engine, the advisor, the
     workspace settings storage, and the canvas-agent / AI Hub surfaces, see
     .agents/ai-guardrails.md — this doc covers only what stays gateway-specific after the
     2026-07-31 extraction (docs/superpowers/specs/2026-07-31-ai-guardrails-standalone-design.md). -->

# AI Gateway content guardrails (EE)

`AiGatewayGuardrails` (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service`,
package `com.bytechef.ee.automation.ai.gateway.guardrail`) is a thin **adapter** over the standalone
`AiGuardrails` engine (`platform-ai-guardrails-service`) — it is not itself the guardrail engine
anymore. It runs in `AiGatewayFacadeImpl` on the chat sync + streaming paths (via `apply`) and the
embeddings path (via `applyToInputs`) after prompt resolution, preserving the exact
pre-extraction public surface (chat/embedding DTO methods, `projectId` overloads, static redaction
helpers) so every existing caller and test keeps working unchanged. It is gated by its own
`@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")` —
independent of the engine bean, which is registered unconditionally under EE.

- The adapter delegates request-direction PII/secret redaction, blocked terms, and injection
  detection to the engine (global properties ∪ per-workspace `AiGuardrailsWorkspaceSettings`, both
  described in `.agents/ai-guardrails.md`), then layers gateway-only concerns on top:
  - **Model-based moderation** — needs an `AiGatewayModerationClassifier` bean
    (`moderation-enabled` / `moderationEnabled`). Moderation never moved into the shared engine; it
    stays gateway-only because it is a chat-completion-only concept with no natural home on plain
    text.
  - **Per-project overlay** — `AiGatewayProjectSettings` (`PROJECT`-scoped `Property` row
    `ai_gateway_project_settings`, guardrail fields only) layers on top of global+workspace with the
    same additive-union semantics (a project can enable a guardrail / add blocked terms, never turn
    one off; `null` = inherit). `resolvePolicy(workspaceId, projectId)` unions all three levels; the
    guardrail methods have `projectId` overloads (originals delegate with `null`).
    `AiGatewayFacadeImpl.resolveProjectId` maps the `project_id` request tag (a per-workspace slug)
    to the numeric project id. Admin-only GraphQL: `aiGatewayProjectSettings` /
    `updateAiGatewayProjectSettings`. The project settings service is a Spring-optional `@Nullable`
    dep — absent bean → project layer skipped. Per-API-key scoping is NOT implemented (no api-key
    `Property` scope). This overlay was deliberately NOT generalized to the other agent surfaces —
    neither the canvas AI Agent nor AI Hub has a natural project scope at the call site.
- Dual-directional: response scanning (`response-scan-enabled` / `scanResponses`) redacts
  PII+secrets from the completion via the engine's `scanResponseText` before it is traced/returned.
  Redaction only, never blocks. Streaming responses are also covered (opt-in): when the operator
  flag `response-scan-streaming-enabled` is set AND response scanning is effective for the
  workspace, the engine's `newStreamingResponseRedactor` returns a `StreamingResponseRedactor` that
  masks SSE deltas across chunk boundaries and defers the terminal `finish_reason` onto its flush
  chunk. Null redactor → the streaming path is byte-for-byte unchanged.
- Order (request): redact PII → redact secrets → blocked terms → moderation → injection; every
  check sees the redacted text. Embeddings run the same minus moderation. Response path is
  redaction only.
- The gateway does **not** read `blockingMode` — a violation always throws
  `AiGatewayGuardrailException` (lives in `platform-ai-gateway-api` so the public-rest
  `AiGatewayExceptionHandler` can map it) → HTTP 422 `guardrail_violation`; the wire message never
  echoes the offending content or matched term. `blockingMode`'s `REDACT_AND_CONTINUE` option is an
  agent-surface concept (see `AiGuardrailsAdvisor` in `.agents/ai-guardrails.md`) — the
  gateway's API contract predates it and stays a hard block, by design.
- The gateway's own **trace-privacy check**, `AiGatewayFacadeImpl.isPiiRedactionEnabled`, reads
  `AiGuardrailsWorkspaceSettingsService` directly (not through this adapter, and with no project
  overlay) — deliberately workspace-scoped only, preserving the exact semantics it had before the
  guardrail fields moved off `AiGatewayWorkspaceSettings` onto the standalone settings entity.
- Both classifier SPIs (`AiGatewayModerationClassifier`, `AiGatewayInjectionClassifier`) are
  Spring-optional `@Nullable` constructor deps; their `PromptBased*` impls register only when
  `moderation-model` / `injection-model` name a catalog model identifier and fail open on any
  error. Regexes must stay free of nested optional quantifiers (SpotBugs ReDoS). Hardening spec:
  `docs/superpowers/specs/2026-07-22-ai-gateway-guardrail-hardening-design.md`.
- Metrics: the adapter's per-instance `AiGuardrailMetrics` (constructed with `surface = "gateway"`)
  emits the shared `bytechef_ai_guardrail` counter (tagged `event`, `surface`) — low-cardinality
  (no workspace/project tag), wired via `ObjectProvider<MeterRegistry>` so it no-ops without a
  registry, and gated with the same `bytechef.ai.gateway.enabled` property as the adapter itself.
  See `.agents/ai-guardrails.md` for the full event vocabulary and how this fits the
  `surface = gateway | ai_agent | ai_hub` model.
- Workspace-level guardrail configuration itself (the five toggles, blocked terms, blocking mode)
  lives in Workspace Settings → "AI" → **Guardrails**, not on the AI Gateway settings page anymore
  — the gateway settings page links there and keeps only the per-project overlay described above.


## Appendix: extracted from CLAUDE.md

### AI Gateway content guardrails (EE)

`AiGatewayGuardrails` (`automation-ai-gateway-service`) is now a thin adapter over the shared
`platform-ai-guardrails` engine described above, gated by its OWN `bytechef.ai.gateway.enabled` toggle
(unchanged pre-extraction behavior — the engine's unconditional registration does not change when the
gateway itself runs). It layers the gateway's project-level overlay (`AiGatewayProjectSettings`,
additive-only — a project can enable a guardrail or add blocked terms, never turn one off) and
its own moderation/injection classifier instances (same SPIs the shared engine also takes, wired separately
for the adapter's own DTO/project-overlay checks — moderation is no longer gateway-exclusive overall, see
"Model-based moderation" above) on top of the engine's global+workspace union, and preserves
the exact pre-extraction public surface (chat/embedding DTO methods, `projectId` overloads). The gateway
does not read `blockingMode` — a block always throws `AiGatewayGuardrailException` → HTTP 422, never
echoing the offending content. The gateway's own trace-privacy check
(`AiGatewayFacadeImpl.isPiiRedactionEnabled`) reads the workspace-level `AiGuardrailsWorkspaceSettingsService`
directly rather than through this adapter — deliberately workspace-scoped only, no project overlay, matching
pre-extraction semantics. See `.agents/ai-gateway-guardrails.md`.
