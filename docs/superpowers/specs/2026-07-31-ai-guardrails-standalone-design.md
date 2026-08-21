# Standalone AI Guardrails (workspace-level, all agent surfaces)

**Date:** 2026-07-31
**Status:** Implemented
**Scope:** EE — `platform-ai` (new module), `automation-ai-gateway` (delegation), `ai-hub-service`, the CE `ai/agent` component (SPI seam only), client Workspace Settings.

## Problem

Content guardrails (PII and secret redaction, blocked terms, prompt-based moderation and
injection detection, response/streaming redaction) exist only inside the AI Gateway:
`AiGatewayGuardrails` runs in `AiGatewayFacadeImpl`, so they bind exclusively on
gateway-routed traffic. The two agent surfaces bypass them entirely:

- The **canvas AI Agent component** resolves provider-native `ChatModel`s directly
  (`AbstractAiAgentChatAction.resolveChatModel`) — its only guardrails are the opt-in
  per-node guardrail cluster elements the workflow author attaches.
- **AI Hub** (personal agents, copilot chat, subagents) builds its ChatClients via
  `CatalogChatClientResolver` — no guardrail interception at all.

So "workspace policies are enforced" is true for tool RBAC and component policies, but
false for content policy on agent traffic. This spec extracts the guardrails into a
standalone, workspace-configured capability applied to the gateway AND both agent
surfaces.

## Design

### 1. Module and engine extraction

New EE module `server/ee/libs/platform/platform-ai/platform-ai-guardrails`
(`-api` / `-service`), beside `platform-ai-gateway` — the guardrail domain types,
classifier interfaces and `AiGatewayGuardrailException` already live under
`platform-ai-gateway-api`, so this is the natural home.

Moves verbatim into the new module (renamed only where gateway-branded):

- `AiGatewayGuardrails` → `AiGuardrails` — the engine: input guardrails
  (`applyToInputs`), response scanning/redaction, effective-policy resolution.
- `PromptBasedInjectionClassifier`, `PromptBasedModerationClassifier`,
  `StreamingResponseRedactor`, `AiGatewayGuardrailMetrics` → `AiGuardrailMetrics`.

**Effective policy stays additive:** global `bytechef.*` properties ∪ workspace
settings. The gateway's per-project overlay is deliberately NOT generalized — it stays
gateway-specific, applied by a thin adapter in `automation-ai-gateway-service` that
wraps the shared engine and layers the project overlay on top. The gateway's public
behavior (request/response DTO methods, HTTP 422 mapping, existing tests) is unchanged;
the extraction proof is the gateway regression suite passing untouched.

### 2. Config: `ai_guardrails_workspace_settings`

New entity/table with a nullable `workspace_id` (`Long`, never primitive) per the
workspace-scoping convention: `workspace_id IS NULL` is the tenant default, which is
also what embedded / no-workspace traffic resolves to. Fields:

- The guardrail fields split OUT of `AiGatewayWorkspaceSettings`: `piiRedactionEnabled`,
  `secretRedactionEnabled`, `blockedTerms`, `moderationEnabled`,
  `injectionDetectionEnabled`, `responseScanEnabled` (names per current entity).
- **New `blockingMode`** enum (INT ordinal, append-only): `BLOCK` (ordinal 0, default)
  | `REDACT_AND_CONTINUE` (ordinal 1). Governs only the blocking guardrails
  (blocked terms, moderation, injection). Redaction guardrails (PII, secrets) always
  redact-and-continue regardless of mode. In `REDACT_AND_CONTINUE`, a tripped blocking
  guardrail masks the offending content and annotates the turn instead of failing it.

`AiGatewayWorkspaceSettings` loses the guardrail fields; the gateway reads guardrail
config from the new entity through the shared engine. Both entities are 0_732-only
(unreleased), so their init changelogs are edited in place — no data migration, per the
released-vs-unreleased changelog rule.

Violation handling never echoes the offending content (kept from the gateway).

### 3. Integration shape: a Spring AI Advisor

`platform-ai-guardrails-service` provides `AiGuardrailsAdvisor` (call + stream
variants): input guardrails run before the model call, output scanning/redaction after,
streaming through `StreamingResponseRedactor` (one `response_redacted` event per
stream, at flush — kept). The advisor is constructed per-request with a resolved
`workspaceId` (nullable → tenant default).

**Ordering:** the advisor registers at highest precedence so the workspace floor
executes BEFORE any per-node guardrail cluster elements on the canvas agent. Node
elements can only add restrictions on top — additive, never loosening, matching the
documented policy model.

### 4. Surface wiring

**AI Hub — all LLM turns.** The advisor is added at the ChatClient-construction seam so
it covers the ASK/BUILD agents, subagent one-shot clients, AND the per-request override
ChatClients used for personal-agent model overrides (`OverrideChatClientResolver` /
`CatalogChatClientResolver` outputs). `workspaceId` comes from the already-verified
`WORKSPACE_ID` state key. Personal agents therefore inherit guardrails via the same
path as plain copilot chat — no `kind` conditional. In `BLOCK` mode a tripped guardrail
produces a blocked-message turn response (the chat shows a guardrail notice; the raw
content is never echoed).

**Canvas AI Agent — CE component, EE capability.** The component must not depend on the
EE module, so a CE SPI is added (`ToolExecutionRecorder` pattern):

- CE (`platform-ai` — the CE AI-integration module): `AiGuardrailsAdvisorProvider` with
  one method returning an optional advisor for
  `(PlatformType platformType, Long jobPrincipalId /* deployment resolution input */)`.
  No bean present (CE deployments) → no-op, zero behavior change.
- EE implementation resolves the workspace from the run's project deployment for
  `PlatformType.AUTOMATION`; embedded and unresolvable cases resolve to the
  tenant-default (null-workspace) settings row.
- `AbstractAiAgentChatAction.getChatClientRequestSpec` consults the provider and
  registers the advisor. In `BLOCK` mode a tripped guardrail throws a guardrail
  exception that fails the step — the normal `on-error` dispatcher / error-workflow
  machinery applies, so a block is observable and handleable like any other task
  failure. The exception message names the guardrail category, never the content.

**Gateway.** `AiGatewayFacadeImpl` keeps calling its adapter exactly as today; the
adapter delegates to the shared engine plus the project overlay. HTTP 422 semantics,
streaming failover interaction, and metrics behavior are unchanged. The gateway does
not get `blockingMode` — API callers keep hard 422 blocks (mode is an agent-surface
concept; the gateway's contract predates it and stays stable).

### 5. Settings UI

New **"AI" group in Workspace Settings** (automation settings navigation) with a
**Guardrails** page: the six toggles, the blocked-terms editor, and the blocking-mode
radio. The guardrail controls RELOCATE from the AI Gateway settings page; the gateway
page keeps its per-project guardrail overlays and links to the new page for the
workspace level. GraphQL: `aiGuardrailsWorkspaceSettings` query +
`updateAiGuardrailsWorkspaceSettings` mutation, admin-gated per the existing settings
pattern (SCREAMING_SNAKE_CASE enum values in the schema). The AI group is the intended
future home for the workspace system prompt and per-workspace policy overrides
(separate specs).

### 6. Metrics

`bytechef_ai_guardrail{event, surface}` with `surface = gateway | ai_agent | ai_hub`
(generalizing `AiGatewayGuardrailMetrics`; the gateway emits through the shared engine
so its counts continue under the new name). Events keep the existing vocabulary
(`pii_redacted`, `secret_redacted`, `blocked_term`, `moderation_blocked`,
`injection_blocked`, `response_redacted`, plus `blocking_downgraded` when
`REDACT_AND_CONTINUE` converts a would-be block). Wired via
`ObjectProvider<MeterRegistry>` so registry-less apps start clean.

### 7. Testing

- Engine tests move with the engine unchanged.
- New advisor tests: `BLOCK` fails the call with the category-only message;
  `REDACT_AND_CONTINUE` masks and continues and emits `blocking_downgraded`; streaming
  redaction catches a secret split across chunk boundaries.
- AI Hub: a test pinning that the advisor is present on the default agent client AND on
  a per-request override client (the personal-agent model path).
- Canvas agent: tests with a fake provider pinning both modes, plus the no-bean no-op.
- Gateway: existing guardrail + facade tests pass without modification (extraction
  proof). `RedisPlanEnforcement`-style int tests are not needed; the engine is
  storage-free apart from the settings service.

## Rejected alternatives

- **Routing agent traffic through the AI Gateway facade** — couples both agent runtimes
  to the gateway's request/response DTOs and deployment model for what is only a
  content-policy concern; the advisor applies the same engine without the detour.
- **Generalizing the per-project overlay** — projects are an automation-gateway concept;
  neither agent surface has a natural project scope at the call site. Workspace-level
  only, gateway keeps its overlay.
- **Per-agent guardrail configuration** — explicitly out of scope; guardrails are a
  workspace governance floor, not an agent preference. Per-node canvas guardrail
  elements already cover author-level additions.

## Decisions log

- Blocking guardrails: **configurable per workspace** (`BLOCK` default,
  `REDACT_AND_CONTINUE` opt-in); redaction guardrails always continue.
- Settings: **split into `ai_guardrails_workspace_settings`**; unreleased init
  changelogs edited in place; gateway entity keeps non-guardrail fields.
- AI Hub scope: **all LLM turns**, not just personal agents.
- Ordering: workspace advisor at highest precedence, before per-node guardrail
  elements.
- Gateway keeps hard 422 (no blockingMode on the API surface).
- Everything EE; CE ships only the no-op SPI.

**Post-implementation amendments (Task 10, folding in resolutions surfaced during
Tasks 1–9):**

- **Storage is NOT the `ai_guardrails_workspace_settings` table this spec assumed.**
  Settings are **property-backed records**: `AiGuardrailsWorkspaceSettingsServiceImpl`
  persists one `PropertyService` row per workspace under
  `AiGuardrailsWorkspaceSettings.PROPERTY_KEY`, `Property.Scope.WORKSPACE`. The
  tenant-default (null-workspace) row uses `Property.Scope.PLATFORM` with a `null`
  `scopeId` — not `Scope.WORKSPACE` with a sentinel id — matching the existing
  `AiProviderConnectionSourceImpl` / `"mcp.server"` convention for scope-less rows.
  There is no liquibase changelog and no data migration for this feature; §2's table
  design and its "unreleased init changelogs edited in place" clause never happened
  because there was never a table to migrate.
- **The `AiGuardrails` engine bean is registered unconditionally under EE** —
  `@Component @ConditionalOnEEVersion`, with no dependency on
  `bytechef.ai.gateway.enabled` (which defaults `false`). The gateway adapter
  (`AiGatewayGuardrails`, package `com.bytechef.ee.automation.ai.gateway.guardrail`)
  keeps its own `@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name =
  "enabled", havingValue = "true")` gate. Coupling the engine bean to the gateway
  toggle would have made the canvas-agent and AI Hub surfaces silently lose
  guardrails whenever the (separately-toggled, default-off) gateway was disabled —
  the engine is inert on its own when no workspace has any guardrail enabled, so
  there is no cost to registering it unconditionally.
- **AI Hub subagent one-shot delegate ChatClients are a known, documented coverage
  gap** against the "all LLM turns" scope decision above. Their prompts derive from
  the parent's already-guarded turn, but a subagent's own output returns to the
  parent as a **tool message**, which the advisor's input scan does not re-inspect,
  and streams to the client as tool-result events that bypass the response scan
  entirely. This is accepted as an explicit scope decision, not an oversight: closing
  it means threading the advisor into each of the per-domain subagent
  `ChatClient` configurations individually (the `workspaceId` is available there via
  the forwarded `ToolContext`), which is follow-up work, not part of this feature.
  The MCP-surface manager subagents (`mcp_manager`, `personal_agent_manager`,
  `deployment_manager`, `api_collection_manager`) are a different surface
  (management MCP, no AG-UI stream) and are out of scope for this gap entirely.
- **(Supersedes the entry above) AI Hub subagent delegate LLM calls are now guarded, closing the
  gap for the AI Hub chat surface.** Follow-up ticket 732 (F3) added
  `SubAgentGuardrailedChatClient` (`ee.ai.hub.guardrails`, ai-hub-service): a hand-written
  `ChatClient` decorator wrapping a delegate's inner `ChatClient` so its own one-shot `.call()` /
  `.stream()` attaches a fresh, workspace-scoped `AiGuardrailsAdvisor` before delegating — the same
  `chatClient.mutate().defaultAdvisors(advisor).build()` shape `AiHubSpringAIAgent#
  attachGuardrailsAdvisor` uses for the top-level agent, just deferred to request time. Because the
  workspace id is only known once the delegate's own hand-rolled `ToolCallback.call(toolInput,
  toolContext)` forwards its `ToolContext` into `.toolContext(Map)`, the decorator intercepts that
  one method to capture the map and resolves the workspace id from it via the SAME
  `AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY` key the specialist's own
  workspace-scoped tools already read — no new context channel, no changes to any delegate class.
  `AiGuardrails#isActive(workspaceId)` is re-checked per call (not cached at wrap time), matching
  `attachGuardrailsAdvisor`'s no-op fast path for an inactive workspace.
  - **One seam covers every family.** All three delegate families —
    Copilot specialists (`registerCopilotSubAgentToolCallbacks`), the AI-hub-owned subagents
    (`registerSubAgentToolCallbacks`: research/data_analyst/image_generator/slide_builder), and the
    manager specialists (`registerManagerSubAgentToolCallbacks`: mcp_manager/personal_agent_manager/
    deployment_manager/api_collection_manager) — construct their delegate `ChatClient` from an
    `ObjectProvider<ChatClient>` bean and hand it straight to a `createXToolCallback(ChatClient)`
    factory or a `new XAgentToolCallback(ChatClient)` constructor. Wrapping the `ChatClient` argument
    with `SubAgentGuardrailedChatClient.wrap(...)` at that single call site in `AiHubConfiguration`
    (one `ai_hub`-tagged `AiGuardrailMetrics` instance resolved once per bean method and reused for
    both the top-level agent's own advisor and every delegate) was sufficient — none of the ~16
    individual `*ToolCallback`/`*Configuration` classes needed touching.
  - **BLOCK-mode UX inside a subagent, observed rather than assumed.** A BLOCK-mode violation makes
    `AiGuardrailsAdvisor#adviseCall` throw `AiGuardrailViolationException` synchronously out of
    `.call()`. Every hand-rolled delegate already wraps its `chatClient.prompt(...).call()` in a
    `catch (RuntimeException exception)` arm converting the failure via
    `ToolErrors.runtimeFailure(...)` — so the turn is never aborted. That helper reports only the
    exception's simple class name, not `getMessage()`, so what actually reaches the parent LLM is
    e.g. `{"error":"mcp_manager failed (AiGuardrailViolationException)"}` — stricter than the
    top-level agent's own category-only surfacing (`"Blocked by AI guardrail: <category>"`), not a
    regression. No delegate class was changed to preserve the category; the spec's original "the
    parent LLM seeing a guardrail message is acceptable" bar is met by the class-name form too.
  - **Still uncovered, inherent to the advisor approach, not fixed by F3:** a delegate's completion
    still returns to the parent as a **tool message**, which the top-level agent's own advisor input
    scan skips (advisor only re-inspects USER/SYSTEM messages), and still streams to the client as
    tool-result events that bypass the response-direction scan entirely — response/streaming
    redaction on a delegate's own output would require either the delegate's own advisor to redact
    before returning (it does: `AiGuardrailsAdvisor#adviseCall` runs `applyResponseGuardrails` on
    the delegate's completion before `.content()` returns it) or the parent's advisor to re-scan tool
    results (out of scope — the advisor's response-direction scan is deliberately scoped to the
    model's own assistant message, not arbitrary tool outputs). In practice this means: redaction
    scanning now works even for delegate output, but BLOCK-mode on a delegate's own *response*
    direction (as opposed to its request direction, which F3 covers) was already a `REDACT`-only
    check (`scanResponseText` never blocks) even for the top-level agent, so there is no new gap
    introduced here.
  - **MCP-surface manager invocations remain out of scope**, as the entry above already stated. The
    separate `AiHubManagerMcpContributorConfiguration` / `ManagerMcpContributorConfiguration` /
    `ApiCollectionManagerMcpContributorConfiguration` beans construct their own
    `ManagerSubAgentToolCallback` instances directly from the same underlying `ChatClient` beans, on
    the management MCP server surface — a different Spring `@Bean` method than the ones F3 wraps, so
    they are not guarded. Left for a future follow-up if MCP-surface coverage is desired.
- The gateway's trace-privacy check, `AiGatewayFacadeImpl.isPiiRedactionEnabled`,
  reads `AiGuardrailsWorkspaceSettingsService` directly rather than going through the
  `AiGatewayGuardrails` adapter or its project-overlay union. This is deliberate:
  trace-payload digesting is workspace-scoped only, with no project overlay,
  preserving the exact pre-extraction semantics (the field lived on
  `AiGatewayWorkspaceSettings` with no project-level counterpart before the
  extraction either).
- §5's "the six toggles" is a wording slip carried from the pre-extraction feature
  set — the settings UI has **five boolean toggles** (`redactPii`, `redactSecrets`,
  `moderationEnabled`, `injectionDetectionEnabled`, `scanResponses`) plus the
  blocked-terms editor and the blocking-mode radio; "six toggles" should read "five
  toggles and a blocked-terms editor."
- GraphQL surface as shipped: `aiGuardrailsWorkspaceSettings(workspaceId: ID)` returns
  `null` on a missing row rather than a zero-valued record — the client
  (`AiGuardrails.tsx`) synthesizes all-off/`BLOCK` defaults from `null` instead of the
  server manufacturing a default row. `updateAiGuardrailsWorkspaceSettings` is
  `ROLE_ADMIN`-gated on the GraphQL controller itself (the A2A precedent — there is no
  facade layer for this feature to own the check instead). The query is
  `isAuthenticated()` only, same posture as the pre-existing `aiGatewayProjectSettings`
  query family; note this means any authenticated user can currently read another
  workspace's guardrail settings including `blockedTerms` (carried from Task 8's
  review as a possible follow-up outside this feature, not fixed here).
- **(Supersedes the entry above) The read query is now scoped to workspace members.**
  Follow-up ticket 732 (F4, the final follow-up) closed the gap: the query's
  `@PreAuthorize` on `AiGuardrailsWorkspaceSettingsGraphQlController` changed from
  `isAuthenticated()` to `hasAuthority('ROLE_ADMIN') or (#workspaceId != null &&
  hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW'))`. No new permission
  token was introduced — `AI_GATEWAY_VIEW` is already the de facto general "AI
  settings viewer" workspace scope (registered once, at `WorkspaceRole.VIEWER`, by
  `AiGatewayPermissionScopeProvider`) and is reused verbatim by every other AI-settings
  read facade that predates this one (`AiGatewayWorkspaceSettingsFacadeImpl`,
  `AiGatewayRequestLogFacadeImpl`, `AiEvalRuleFacadeImpl`, `AiEvalScoreFacadeImpl`,
  `AiEvalScoreConfigFacadeImpl`, `AiPromptFacadeImpl`, and the four
  `automation-ai-observability` facades), despite living in a different EE module
  (`automation-configuration-service`) than guardrails (`platform-ai-guardrails-*`) —
  the SpEL `hasPermission(...)` built-in resolves against the globally-registered
  `AutomationPermissionEvaluator` bean regardless of which module's `@PreAuthorize`
  string references it, so no new compile-time dependency was needed. The
  null-`workspaceId` tenant-default read stays `ROLE_ADMIN`-only (it has no workspace
  to scope a membership check against): the SpEL `&&` short-circuits, so
  `hasPermission(#workspaceId, ...)` is never invoked with a null id, and that branch
  falls through to `hasAuthority('ROLE_ADMIN')` alone — mirroring the mutation's
  existing gate. Tenant admins still pass for any
  workspace: `hasAuthority('ROLE_ADMIN')` short-circuits before `hasPermission` is even
  evaluated, and `PermissionServiceImpl.hasResourceScope` independently short-circuits
  on `isTenantAdmin()` regardless. The client (`AiGuardrails.tsx`) is unaffected — the
  query is only ever issued with a concrete `workspaceId`, and the page already sits
  behind `PrivateRoute(ADMIN)` — so this tightens a server-side gap the UI never
  exercised, not a client-visible behavior change.
- **Moderation is gateway-only in this iteration.** The shared engine implements no moderation
  classifier at all — `moderationEnabled` is persisted on `AiGuardrailsWorkspaceSettings` and
  surfaced in the settings UI, but only the AI Gateway adapter (`AiGatewayGuardrails`) reads it
  and performs a moderation check. The canvas AI Agent and AI Hub surfaces are unaffected by the
  toggle regardless of its value. The settings UI carries an explicit caveat on the "Model-based
  moderation" control noting it currently applies to AI Gateway traffic only, so admins don't
  assume workspace-wide coverage.
- **(Supersedes the entry above) Moderation now covers every advisor-fronted surface, not just
  the gateway.** Follow-up ticket 732 (F2) closed the gap: `AiGuardrails` gained a `@Nullable
  AiGatewayModerationClassifier` constructor dependency (the same SPI the gateway adapter's own
  classifier wiring already implements — no new interface, no dependency cycle, since
  `platform-ai-guardrails-service` already depended on `platform-ai-gateway-api` for the
  injection-classifier SPI) plus a `moderation-enabled` global property, unioned with workspace
  `moderationEnabled` via the same additive `resolvePolicy` pattern as every other guardrail.
  `AiGuardrails#checkInputs` — the advisor's non-throwing entry point — now checks moderation
  (fail-open on a classifier error, mirroring injection); the throwing `applyToInputs` entry point
  (the AI Gateway adapter's own path) deliberately still never moderates, so the gateway — which
  already moderates its own DTO pipeline with its own classifier wiring and project overlay — is
  never double-moderated. `isActive` now also counts moderation (classifier present AND enabled)
  as an activation reason, so a workspace that enables ONLY moderation gets the advisor attached.
  The settings UI's "currently applies to AI Gateway traffic only" caveat is removed; it now notes
  that moderation requires a configured moderation model
  (`bytechef.ai.gateway.guardrails.moderation-model`) to take effect, matching how the
  prompt-injection toggle's copy already reads.
  - **BlockingMode interaction, decided against this spec's own text**: §2 explicitly documents
    `blockingMode` as governing "the blocking guardrails (blocked terms, moderation, injection)"
    and says `REDACT_AND_CONTINUE` "masks the offending content and annotates the turn instead of
    failing it." Since a moderation verdict has no locatable span — the classifier judges the
    whole message, not a substring — "masks the offending content" for moderation was implemented
    literally: the downgraded message is replaced WHOLLY with a fixed placeholder,
    `[REDACTED_MODERATED]` (new category `moderation_flagged`, reusing the gateway's existing
    metric-event name — the design draft's `moderation_blocked` name at §6 was never actually
    shipped; `AiGatewayGuardrails` and `AiGuardrailMetrics` both already use `moderation_flagged`,
    so F2 reuses that instead of introducing a second name for the same event). This was a
    deliberate departure from injection's existing downgrade behavior (injection continues with
    only the pii/secret-redacted original text on downgrade, not a placeholder, because closing
    that gap was out of scope for F2) — moderation and injection both lack a span, but only
    moderation's downgrade was specified here to fully mask the message, since the alternative
    ("moderation never downgrades, always hard-blocks") cannot be justified against this spec's
    own "governs blocked terms, moderation, injection" wording.
  - New engine/advisor tests: moderation flagged → `BLOCK` throws `AiGuardrailViolationException`
    with category `moderation_flagged` and records the metric under the caller's surface;
    `REDACT_AND_CONTINUE` replaces the whole message with `[REDACTED_MODERATED]` and records
    `blocking_downgraded`; moderation enabled with no classifier bean → no-op, no metric;
    classifier already fail-open → no-op; `isActive` true when only moderation is enabled with a
    classifier present, false when the classifier is absent. Gateway and AI Hub test suites pass
    unmodified (constructor-signature-only plumbing changes, zero assertion edits) — proof the
    gateway's own moderation resolution and HTTP 422 contract are untouched.
- **The per-surface metric split applies to advisor-emitted events only.** `surface = gateway |
  ai_agent | ai_hub` is accurate for `blocking_downgraded` and `response_redacted`, which the
  `AiGuardrailsAdvisor` records through a fresh per-request `AiGuardrailMetrics` instance tagged
  with the caller's surface. The request-direction events (`pii_redacted`, `secret_redacted`,
  `blocked_term`, `injection_flagged`) are recorded by the engine's own internal
  `AiGuardrailMetrics` bean (`@ConditionalOnProperty(bytechef.ai.gateway.enabled=true)`), which is
  constructed once with a single configured `surface` (default `gateway`) regardless of which
  surface actually triggered the event. Wiring those events per-surface — e.g. by having each
  surface pass its own metrics instance into the engine call — is a follow-up, not part of this
  feature.
- **(Supersedes the entry above) The per-surface metric split now also covers the
  advisor's request-direction events.** Follow-up ticket 732 closed the gap the previous entry
  flagged: `AiGuardrails#checkInputs` — the non-throwing entry point `AiGuardrailsAdvisor` uses —
  now takes an `AiGuardrailMetrics` parameter and records `pii_redacted`, `secret_redacted`,
  `blocked_term`, and `injection_flagged` through whatever instance the caller supplies, instead of
  through the engine's own constructor-injected bean. `AiGuardrailsAdvisor` passes its own
  per-request instance (the same one it already used for `blocking_downgraded` /
  `response_redacted`), so every event on the advisor path now carries an accurate `ai_agent` /
  `ai_hub` surface tag and emits regardless of the `bytechef.ai.gateway.enabled` toggle. The
  engine's own internal bean is untouched and still gates on that property — it is now used
  EXCLUSIVELY by `AiGuardrails#applyToInputs`, the gateway adapter's throwing entry point, whose
  `surface=gateway`-tagged (or gated-off) behavior is unchanged. No double counting: a call through
  `checkInputs` records only via the caller-supplied instance, never via the engine's own bean.

**Status: Implemented.**
