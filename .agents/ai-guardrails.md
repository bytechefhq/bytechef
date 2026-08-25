<!-- Extracted from CLAUDE.md so the agent-facing reference does not sit in every prompt.
     Load this when working on the standalone AI guardrails engine, the guardrails advisor, or
     either agent surface (canvas AI Agent, AI Hub). For the AI Gateway's own adapter (project
     overlay, moderation/injection classifiers, HTTP 422 mapping), see
     .agents/ai-gateway-guardrails.md. -->

# AI Guardrails (EE, standalone across surfaces)

Content guardrails (PII/secret redaction, blocked terms, moderation, injection detection,
response/streaming redaction) were extracted out of the AI Gateway into a standalone EE module so
they cover every LLM-calling surface, not just gateway-routed traffic. Spec:
`docs/superpowers/specs/2026-07-31-ai-guardrails-standalone-design.md`.

## Module and engine

- `server/ee/libs/platform/platform-ai/platform-ai-guardrails` (`-api` / `-service` / `-graphql`).
- The engine, `AiGuardrails` (`-service`, package `com.bytechef.ee.platform.ai.guardrails`), is
  `@Component @ConditionalOnEEVersion` and registered **unconditionally** — it does NOT depend on
  `bytechef.ai.gateway.enabled` (default `false`). It operates on plain strings/lists of strings,
  not on any caller's DTO shape:
  - `applyToInputs(inputs, workspaceId)` — the original always-throw contract (kept for the
    gateway adapter's HTTP 422 behavior): redacts PII/secrets inline, throws
    `AiGatewayGuardrailException` on a blocked-term match or a flagged injection.
  - `checkInputs(inputs, workspaceId, metrics)` — non-throwing counterpart used by the advisor:
    returns one `GuardrailCheckResult(text, category)` per input; `category` is `null` unless a
    blocking violation tripped, in which case the offending content is already masked out of
    `text`. This is the ONLY entry point that checks model-based moderation (an optional
    `AiGatewayModerationClassifier` bean, gated on `moderation-enabled` / workspace
    `moderationEnabled`) — `applyToInputs` deliberately never moderates, so the AI Gateway
    adapter (which moderates its own DTO pipeline with its own classifier wiring) is never
    double-moderated. A moderation verdict has no locatable span, so unlike a blocked-term match
    (which masks only the matched term) a moderation downgrade under `REDACT_AND_CONTINUE`
    replaces the WHOLE message with `[REDACTED_MODERATED]`. Injection detection downgrades
    without masking anything beyond the pii/secret redaction already applied (an existing,
    unchanged behavior) — moderation intentionally does not mirror that, since "masking the
    offending content" for a whole-message judgment means masking the whole message. Fails open
    on a classifier error (the classifier's own responsibility, same as injection).
  - `scanResponseText(text, workspaceId)` — response-direction redaction only, never blocks.
  - `newStreamingResponseRedactor(workspaceId)` — returns a `StreamingResponseRedactor` (or
    `null`) when streaming response scanning is active (requires BOTH the workspace/global
    `scanResponses` policy AND the operator flag `response-scan-streaming-enabled`).
  - `resolveBlockingMode(workspaceId)` — the workspace's `BlockingMode`, defaulting to `BLOCK`.
  - `isActive(workspaceId)` — whether any guardrail is active for a workspace once global +
    workspace policy are unioned, including moderation (only counted active when a moderation
    classifier bean is present); callers use this to skip attaching an advisor entirely.
  - `redactPii` / `redactSecrets` / `redactAll` are instance methods (no longer static — see
    "Sensitive-data detectors" below for why) reused by the gateway adapter's project overlay and by
    `scanResponseText`/`StreamingResponseRedactor` for response-direction and streaming redaction.
    The old `sensitiveMatchRanges` helper is gone, superseded by `SensitiveDataRedactor`'s span-based
    detect/resolve/apply pipeline.
- Redaction always runs before the blocked-term check and injection detection, so those checks see
  already-redacted text. Regexes avoid nested optional quantifiers (ReDoS-safe).
- Effective policy per call = union of global `bytechef.ai.gateway.guardrails.*` properties
  (property names kept for compatibility — the AI Gateway is still the sole reader/writer of these
  keys) and the call's workspace `AiGuardrailsWorkspaceSettings` — additive: a level can enable a
  guardrail or add blocked terms, never turn one off.

## Sensitive-data detectors

- PII/secret redaction, wherever it runs (request-direction, response-direction, or the streaming
  path), is a **detect → resolve → apply** pipeline over the original text — not a chain of
  `replaceAll` calls in which each pattern rewrote the text the next pattern was about to scan. The
  old chain (`redactAll` = `redactSecrets(redactPii(x))`) leaked part of any secret whose body
  contained a credit-card-shaped digit run: the PII pass claimed the digits first and destroyed the
  text the secret pattern needed, so `xoxb-1234567890123456-abcdef` came out as
  `xoxb-[REDACTED_CC]-abcdef` — the token's prefix and suffix intact, disclosing that a Slack
  credential was present. Detecting against the untouched original and resolving overlaps centrally
  fixes it: the same input now redacts to `[REDACTED_SECRET]`.
- The SPI is `SensitiveDataDetector` (`platform-ai-guardrails-api`, package
  `com.bytechef.ee.platform.ai.guardrails.detector`, still a zero-dependency module — not even Spring
  is on its compile path): `name()`, `detect(String text) -> List<SensitiveSpan>`, and
  `default streamSafe() -> true`. Contributing a detector is a bean (`@Component
  @ConditionalOnEEVersion`) — no engine change. `SensitiveDataRedactor` (`-service`) collects every
  registered detector, runs each over the WHOLE original text and never another detector's output (so
  no detector can corrupt another's result), and resolves the combined candidate spans into a
  non-overlapping accepted set before applying placeholders right-to-left. The two built-ins,
  `RegexPiiDetector` (categories `EMAIL`/`SSN`/`CC`/`PHONE`/`IP`) and `RegexSecretDetector` (single
  category `SECRET`), reproduce the exact patterns and placeholder strings the old chain emitted, so
  the common case is byte-identical.
- **Resolution order is total, and independent of registration order**: SECRET spans before PII, then
  longer before shorter, then earlier before later, then category ascending — candidates are taken
  greedily in that order and a candidate overlapping an already-accepted span is dropped. Because the
  order is a property of the spans themselves (kind, length, offset, category) and never of which
  detector reported them or in what sequence detector beans happen to be registered, contributing a
  new detector cannot change how an existing detector's spans resolve. This is the property that
  makes the SPI safe to extend.
- `SensitiveSpan.kind()` (`PII`/`SECRET`) is closed — it is exactly the axis the `redactPii` /
  `redactSecrets` policy toggles govern, and a redaction call always names which kinds it wants
  filtered. `SensitiveSpan.category()` is open (a validated uppercase identifier) and drives
  presentation only: `SensitiveSpan.placeholder()` derives `[REDACTED_<category>]` with no lookup
  table, so a detector can introduce a new entity type (e.g. `PERSON`) without touching this module.
  The kind filter is applied to the CANDIDATES, before resolution — filtering the accepted set instead
  would let a span the caller did not ask for still win an overlap and then be discarded, so a
  PII-only redaction over a secret containing a digit run would return the text unredacted.
- `streamSafe()` exists because the streaming response path can only ever offer a detector a bounded
  lookahead window, not the whole document. A detector needing wider context than that (sentence-level
  NER, say) must declare `streamSafe() = false`; `SensitiveDataRedactor.streamSafeView()` (resolved
  once, at `AiGuardrails` construction) then excludes it from the streaming redactor entirely and logs
  the exclusion once, rather than feeding it a mid-sentence fragment and letting it silently produce a
  worse answer than it would give over the complete text — a detector that cannot honestly cover a
  stream is visibly absent from it, not silently contributing nothing usable. Both built-in regex
  detectors are local, so today's stream-safe set is unchanged.
- **Failure is fail-open, per detector.** A detector that throws (or reports a span past the end of
  the text) is caught, WARN-logged, counted as the `detector_failed` metric event, and skipped for
  that call — the remaining detectors still run, so one flaky detector cannot take down every AI
  surface. See Metrics below for which instance each call path counts that event through.
- **Optional detector: `platform-ai-guardrails-opennlp`** (EE, package
  `com.bytechef.ee.platform.ai.guardrails.opennlp`) plugs Apache OpenNLP named-entity recognition
  into the SPI to cover what the regex detectors above cannot — unstructured PII: person names,
  organizations, locations. It is a bean like any other detector (no engine change) and is **off by
  default**. **It ships no models, and Apache distributes none**: OpenNLP's Maven Central and
  models page carry sentence/tokenizer/POS models but zero English NER artifacts, and the only
  English NER models that exist are the legacy SourceForge 1.5 binaries — English-only,
  newswire-trained, roughly fifteen years old. This module is not a feature an operator merely
  switches on; it is inert until they supply their own compatible models (realistically ones
  trained on their own corpus, which is also the case where accuracy is adequate for destructive
  redaction). Anyone reaching for the legacy 1.5 models should understand they are pointing a
  fifteen-year-old newswire model at chat and code text, in a redaction path that rewrites prompts
  irreversibly before the LLM ever sees them. Configuration:
  ```yaml
  bytechef:
    ai:
      guardrails:
        opennlp:
          enabled: false
          tokenizer-model:                 # optional Resource; SimpleTokenizer when unset
          min-confidence: 0.85
          entity-models:                   # empty by default
            PERSON: file:/opt/bytechef/models/en-ner-person.bin
            ORGANIZATION: classpath:models/en-ner-organization.bin
  ```
  The bean registers only when BOTH `enabled=true` and `entity-models` is non-empty
  (`OpenNlpGuardrailsConfiguration`) — an enabled-but-empty configuration contributes nothing
  rather than a detector the engine would call on every request for no gain. `entity-models` keys
  ARE `SensitiveSpan` categories, not a separate vocabulary: a `PERSON` key produces
  `[REDACTED_PERSON]` through the same `placeholder()` mechanism described above, with no mapping
  table anywhere. Models are loaded **eagerly**, in the detector's constructor: a missing,
  unreadable, or corrupt model fails application startup rather than being caught later by the
  fail-open policy above — deliberately, because a lazily-loaded broken model would be caught,
  counted, and skipped, handing the operator a guardrail that silently protects nothing (a typo in
  a model path should fail loudly, not turn into silent non-coverage). `streamSafe()` returns
  **`false`**: NER over a bounded lookahead window that starts mid-sentence gives different, worse
  answers than over the whole text, so **streamed completions get regex redaction only** — batch
  response scanning and all request-direction scanning still cover NER. `min-confidence` (default
  `0.85`, `NameFinderME`'s per-span probability) is the only false-positive control on this path,
  and it carries more weight than a tuning knob normally would: redaction here is destructive and
  pre-model, so a spurious `PERSON` hit on "Claude", "Stripe", or "Redis" in a developer's prompt
  silently corrupts the text the model receives, with no signal to the user — exactly the input
  distribution a newswire-trained model produces on chat and code text. Every span this detector
  reports is `SensitiveKind.PII`, so it is gated by the same workspace `redactPii` toggle (or the
  gateway's `pii-redaction-enabled` property) as the regex PII detector — an operator who enables
  this module and configures valid models but leaves `redactPii` off gets zero redaction from it,
  silently; the detector's own startup log only confirms what it loaded, not that it is doing
  anything. And unlike the regex detectors, NER here is not a linear scan: `NameFinderME` runs
  beam-search sequence decoding over the entire input, synchronously, on the request path, before
  the LLM call, with no input-size bound anywhere in this module — materially more expensive than
  the regex detectors, and the engine's fail-open policy does not help, because a slow detector
  never throws. Finally, the same keys are
  mirrored on `ApplicationProperties.Ai.Guardrails.OpenNlp` (`server/libs/config/app-config`) — not
  redundancy: `ApplicationProperties` binds all of `bytechef.*` with `ignoreUnknownFields = false`,
  so an operator-set key with no field there fails EVERY app's context, including apps that do not
  carry this optional module. This is the trap for whoever adds the next optional module with
  operator-settable properties: a standalone `@ConfigurationProperties` class is fine on its own
  only as long as nothing ever sets its keys; the moment an operator can set `enabled: true`, the
  keys become present-in-a-property-source and strict binding needs a field for them somewhere every
  app still loads, module or no module.

## Settings storage

`AiGuardrailsWorkspaceSettings` (`-api`, package `...guardrails.domain`) is **property-backed, not
a table** — `AiGuardrailsWorkspaceSettingsServiceImpl` persists one `PropertyService` row per
workspace keyed by `AiGuardrailsWorkspaceSettings.PROPERTY_KEY`:

- A real workspace → `Property.Scope.WORKSPACE`, `scopeId = workspaceId`.
- The tenant default (`workspaceId == null`) → `Property.Scope.PLATFORM`, `scopeId = null` — NOT
  `Scope.WORKSPACE` with a sentinel id. This matches the existing convention for scope-less rows
  (`AiProviderConnectionSourceImpl`, the `"mcp.server"` property).
- Only non-null fields are persisted (`null` means "inherit from tenant default"), so a partial
  override never clobbers other fields.
- Fields: five booleans (`redactPii`, `redactSecrets`, `moderationEnabled`,
  `injectionDetectionEnabled`, `scanResponses`), `blockedTerms` (comma-separated string), and
  `blockingMode` (`BlockingMode` enum, `BLOCK` default | `REDACT_AND_CONTINUE`, INT-ordinal,
  append-only — governs only the blocking guardrails; redaction guardrails always
  redact-and-continue regardless of mode).

## The advisor

`AiGuardrailsAdvisor` (`-service`, package `...guardrails.advisor`) implements Spring AI's
`CallAdvisor` + `StreamAdvisor`, constructed per-request with a resolved (nullable) `workspaceId`
and its own `AiGuardrailMetrics` instance tagged with the caller's `surface`:

- **Order**: `HIGHEST_PRECEDENCE` — the guardrail floor must see the final outbound request before
  any other advisor's rewrite, and the model's raw completion before any other advisor
  post-processes it. This also means it runs BEFORE per-node canvas guardrail cluster elements —
  workspace policy is a floor, node elements only add restrictions on top.
- **Request direction**: every USER/SYSTEM message runs through `AiGuardrails#checkInputs`. In
  `BLOCK` mode a blocking violation throws `AiGuardrailViolationException` (category-only message,
  never the offending content) which aborts the call (or, for streaming, becomes `Flux.error`). In
  `REDACT_AND_CONTINUE`, the masked text is patched into the request, the call proceeds, and a
  `blocking_downgraded` event is recorded.
- **Response direction**: non-streaming responses are scanned via `scanResponseText` and rewritten
  in place (`response_redacted` recorded on change). Streaming responses are piped through one
  `StreamingResponseRedactor` for the whole stream so a value split across an SSE chunk boundary is
  never emitted in the clear; the redactor's held-back remainder flushes as a trailing chunk once
  the upstream stream completes, and `response_redacted` is recorded at most once per stream.

## Surface wiring

- **Canvas AI Agent (CE component, EE capability)**: the CE `ai/agent` component
  (`server/libs/modules/components/ai/agent`) must not depend on the EE module, so it reaches the
  advisor through a CE SPI, `AiGuardrailsAdvisorProvider`
  (`server/libs/platform/platform-ai/platform-ai-api`, package `com.bytechef.platform.ai.guardrails`
  — same `ToolExecutionRecorder`-style optional-bean idiom). `getAdvisor(platformType,
  jobPrincipalId, surface)` returns `Optional.empty()` when no EE implementation is registered or
  every guardrail category is disabled for the resolved workspace. `AbstractAiAgentChatAction`
  consults it via `@Nullable ObjectProvider` and registers the advisor first in
  `getChatClientRequestSpec`. The EE implementation, `AiGuardrailsAdvisorProviderImpl`
  (`platform-ai-guardrails-service`), resolves the workspace only for `PlatformType.AUTOMATION`
  with a non-null `jobPrincipalId` (interpreted as a `ProjectDeployment` id): `jobPrincipalId` →
  `ProjectDeploymentService.getProjectDeployment` → `Project.getWorkspaceId()`, memoized 5 minutes
  in a Caffeine cache. Embedded runs, a null `jobPrincipalId`, or any resolution failure fall back
  to the tenant-default (`workspaceId = null`) settings — only the workspace SCOPE is fail-open;
  guardrails are never silently skipped because attribution failed. In `BLOCK` mode a tripped
  guardrail fails the step normally, so `on-error` / error-workflow handling applies like any other
  task failure. Surface string: `"ai_agent"`.
- **AI Hub — all LLM turns**: `AiHubSpringAIAgent` wires the advisor at the ChatClient-construction
  seam, covering the ASK/BUILD agents and the per-request override clients used for task
  model overrides. `workspaceId` comes from the already-verified `WORKSPACE_ID` state key. Surface
  string: `"ai_hub"`. Subagent one-shot delegate ChatClients (skills, research, data_analyst, the
  manager subagents, etc.) are covered too: `SubAgentGuardrailedChatClient` wraps every delegate
  registration in `AiHubConfiguration` — a stateless decorator that captures the forwarded
  tool-context workspace id per call (fresh request spec per `prompt()`, so concurrent calls cannot
  cross workspaces) and attaches a fresh advisor when guardrails are active; a BLOCK inside a
  delegate surfaces as a leak-free tool error (`ToolErrors.runtimeFailure`, class name only).
  Residuals, recorded in the spec's decisions log: the parent agent never re-scans tool outputs
  (a delegate's completion reaches the client via tool-result events without passing the parent's
  response scan — the delegate's own advisor is what guards that content), and the MCP-surface
  manager subagent invocations are a different surface (management MCP, no AG-UI stream), still
  unguarded there.
- **Gateway**: see `.agents/ai-gateway-guardrails.md` — the gateway wraps the engine in its own
  adapter rather than using the advisor (its integration point is `AiGatewayFacadeImpl`, a plain
  request/response DTO pipeline, not a Spring AI `ChatClient`).

## Settings UI

Workspace Settings → **AI Agents** → **Guardrails** tab
(`client/src/ee/pages/settings/automation/ai/guardrails/AiGuardrails.tsx`, rendered by
`AiAgents.tsx` under the tabbed route `/automation/settings/ai/agents/:tab`, so the page's URL is
`/automation/settings/ai/agents/guardrails`; `/automation/settings/ai/agents` redirects there.
`PrivateRoute(ROLE_ADMIN)` + `EEVersion` gated). GraphQL:
`aiGuardrailsWorkspaceSettings(workspaceId: ID)` returns `null` on a missing row (the client
synthesizes all-off/`BLOCK` defaults rather than the server manufacturing a default record);
`isAuthenticated()`-gated. `updateAiGuardrailsWorkspaceSettings` is `ROLE_ADMIN`-gated on the
GraphQL controller itself (the A2A precedent — no facade layer exists to own the check for this
feature). The blocked-terms editor splits on comma+newline and re-joins comma-only, matching what
`AiGuardrails`' `parseBlockedTerms` expects. The old AI Gateway settings page's guardrail controls
relocated here; the gateway page links to this page for workspace-level configuration and keeps
only its own per-project guardrail overlay.

## Metrics

`bytechef_ai_guardrail{event, surface}` — `surface = gateway | ai_agent | ai_hub`. Events:
`pii_redacted`, `secret_redacted`, `blocked_term`, `moderation_flagged`, `injection_flagged`,
`response_redacted`, `detector_failed` (a `SensitiveDataDetector` threw and was skipped — see below
for which call paths actually count it), plus `blocking_downgraded` when `REDACT_AND_CONTINUE`
converts a would-be block. `moderation_flagged` is no longer gateway-only (superseded, F2 of the standalone-guardrails
follow-up): `AiGuardrails#checkInputs` now also checks moderation, so `ai_agent`/`ai_hub`-tagged
`moderation_flagged` events are emitted whenever a workspace enables moderation and a moderation
classifier bean is configured — the engine's `applyToInputs` (the gateway's own throwing path)
still never moderates, so gateway-tagged `moderation_flagged` events keep coming exclusively from
the gateway adapter's own resolution, unchanged.

Two independent `AiGuardrailMetrics` instances exist per call path, split by ENTRY POINT rather
than by event: `AiGuardrails#applyToInputs` (the gateway adapter's unconditional-throw entry
point) always records through the engine's own internal bean —
`@ConditionalOnProperty(bytechef.ai.gateway.enabled=true)`, so it is a no-op (engine still
functions, just doesn't emit) when the gateway is disabled, and always tagged `surface=gateway`
regardless of which caller triggered it, since that bean's `surface` is a single deployment-wide
property. `AiGuardrails#checkInputs` (the advisor's non-throwing entry point) takes an
`AiGuardrailMetrics` parameter instead and records EVERY request-direction event
(`pii_redacted`, `secret_redacted`, `blocked_term`, `injection_flagged`, `moderation_flagged`)
through whatever instance the caller supplies — `AiGuardrailsAdvisor` always passes its own per-request instance, tagged
with the caller's own `surface` (`ai_agent` / `ai_hub`), constructed by
`AiGuardrailsAdvisorProviderImpl` / `AiHubSpringAIAgent` respectively and independent of the
gateway toggle. The advisor also uses that same instance for the events it decides on its own
(`blocking_downgraded`, `response_redacted`), so every metric on the advisor path — request- and
response-direction alike — carries an accurate per-surface tag and emits regardless of whether the
gateway is enabled; only the gateway's own `applyToInputs` path is gated. Wired via
`ObjectProvider<MeterRegistry>` so registry-less apps start clean.

**`detector_failed` follows the same per-surface split as every other event, but it took a second
pass to get there.** `SensitiveDataRedactor`'s detect/redact methods take an
`@Nullable AiGuardrailMetrics` and record `detector_failed` through whatever is handed to them, and
every path that a surface-tagged instance can reach now hands one in:

- **Request direction** — `AiGuardrails#checkInputs` → `#checkInput` → `#redactPiiAndSecrets` threads
  the caller-supplied instance, so the event lands under the calling surface exactly like
  `pii_redacted`/`secret_redacted`.
- **Response direction** — `scanResponseText` and `redactAll` each have an overload taking an
  `@Nullable AiGuardrailMetrics`, and `AiGuardrailsAdvisor` passes the same per-surface instance it
  already uses for `response_redacted`. A detector failing while scanning an `ai_agent`/`ai_hub`
  completion is therefore counted under THAT surface, and counted at all regardless of the gateway
  toggle.
- **Streaming** — `StreamingResponseRedactor` takes an `@Nullable AiGuardrailMetrics` through its
  constructor and threads it into all three redactor calls;
  `AiGuardrails#newStreamingResponseRedactor(workspaceId, metrics)` is how the advisor supplies it.

Two paths deliberately still record through the engine's own constructor-injected bean, and that is
correct rather than a gap: the gateway adapter's `applyToInputs`, and the no-argument
`newStreamingResponseRedactor()` / bare `redactPii`/`redactSecrets`/`redactAll` calls the gateway's
project overlay makes. Those callers ARE the gateway, so the bean's fixed `surface=gateway` tag is
accurate for them — and being gated on `bytechef.ai.gateway.enabled` costs nothing, since the gateway
is by definition enabled when they run.

Note there is deliberately no `newStreamingResponseRedactor(AiGuardrailMetrics)` single-argument
overload: it would be ambiguous with `newStreamingResponseRedactor(Long workspaceId)` for a bare
`null` argument, forcing callers to cast. A caller supplying its own metrics instance passes it
alongside the workspace id.

## Appendix: extracted from CLAUDE.md

### AI Guardrails (EE, standalone across surfaces)

Content guardrails (PII/secret redaction, blocked terms, moderation, injection detection, response/streaming
redaction) live in the standalone EE module `platform-ai-guardrails` (`-api`/`-service`/`-graphql`, under
`server/ee/libs/platform/platform-ai/`) — not inside the gateway. This is an extraction from the earlier
gateway-only implementation; see "AI Gateway content guardrails" below for the gateway's own adapter.

- **Engine**: `AiGuardrails` (`@Component @ConditionalOnEEVersion`) is registered UNCONDITIONALLY —
  decoupled from `bytechef.ai.gateway.enabled` (default false); it is inert when no workspace has anything
  enabled, so registering it unconditionally costs nothing and lets the agent surfaces work even with the
  gateway toggled off. PII/secret redaction itself is a detect → resolve → apply span pipeline
  (`SensitiveDataRedactor`) behind a bean-contributed `SensitiveDataDetector` SPI published in
  `platform-ai-guardrails-api` — not the old sequential `String.replaceAll` chain. The resolution order over
  overlapping spans is total, so detector registration order can never affect the redacted output; see
  the "Sensitive-data detectors" section above for the full breakdown.
- **Settings**: `AiGuardrailsWorkspaceSettings` is PROPERTY-BACKED, not a dedicated table — one
  `PropertyService` row per workspace (`Property.Scope.WORKSPACE`); the tenant default (null `workspaceId`)
  uses `Property.Scope.PLATFORM` with a null `scopeId`, the same convention as
  `AiProviderConnectionSourceImpl` / the `"mcp.server"` property. Five boolean toggles (`redactPii`,
  `redactSecrets`, `moderationEnabled`, `injectionDetectionEnabled`, `scanResponses`) plus a `blockedTerms`
  editor and `blockingMode` (`BLOCK` default | `REDACT_AND_CONTINUE`, INT-ordinal enum — governs only the
  blocking guardrails; redaction guardrails always redact-and-continue). GraphQL:
  `aiGuardrailsWorkspaceSettings(workspaceId)` returns `null` on a missing row (client synthesizes defaults),
  `isAuthenticated()`-gated; `updateAiGuardrailsWorkspaceSettings` is `ROLE_ADMIN`-gated on the controller
  itself (A2A precedent — no facade layer owns the check here). Settings UI: Workspace Settings → AI Agents →
  Guardrails tab (`/automation/settings/ai/agents/guardrails`, admin + EE gated). The former "AI" sidebar group
  is gone: Guardrails and System Prompt are tabs of one AI Agents page. The earlier `ai/guardrails` and
  `ai/system-prompt` routes were dropped rather than redirected — neither reached a release tag or `master`, so
  no bookmark could exist.
- **Agent surfaces**: `AiGuardrailsAdvisor` (Spring AI `CallAdvisor`/`StreamAdvisor`,
  `platform-ai-guardrails-service`) registers at `HIGHEST_PRECEDENCE`, ahead of per-node canvas guardrail
  cluster elements — the workspace policy is a floor, node elements only add restrictions. The canvas AI
  Agent component (CE, `server/libs/modules/components/ai/agent`) reaches it through a CE SPI seam,
  `AiGuardrailsAdvisorProvider` (`platform-ai-api`, same idiom as `ToolExecutionRecorder` — optional bean,
  no-op on CE). AI Hub wires the advisor at the ChatClient-construction seam in `AiHubSpringAIAgent`,
  covering ASK/BUILD agents and task override clients.
- **Subagent delegate LLM calls (F3, ticket 732)**: closed. `SubAgentGuardrailedChatClient`
  (`ee.ai.hub.guardrails`, ai-hub-service) is a hand-written `ChatClient` decorator that wraps a delegate's
  own inner `ChatClient` so its one-shot `.call()`/`.stream()` attaches fresh, per-request advisors before
  delegating (`chatClient.mutate().defaultAdvisors(...)`'s per-request shape, deferred to request time since
  the delegate `ChatClient` bean is a singleton shared by every workspace). It no longer hardcodes which
  advisors: it dispatches a `List<SubAgentAdvisorContributor>`, of which `WorkspaceAdvisorContributor`
  (guardrails + workspace system prompt) is one — see "Subagent conversation memory and interactive
  questions" for the seam and the other contributor.
  It resolves the workspace id from the SAME forwarded `ToolContext` map every hand-rolled delegate
  `ToolCallback` (`SkillsAgentToolCallback`, `ResearchToolCallback`, etc.)
  already builds and passes to `.toolContext(Map)` — via `AgentToolInvocationContext
  .TOOL_CONTEXT_WORKSPACE_ID_KEY` — so no delegate class needed to change. One seam in
  `AiHubConfiguration` (`#wrapDelegate`) covers both delegate families the hub has: the
  catalog-backed intelligent delegates (via `registerIntelligentToolCallbacks`) and the AI-hub-owned
  generative one-shots research/data_analyst/image_generator/slide_builder (via
  `registerSubAgentToolCallbacks`). It does NOT reach the Copilot panels or the management MCP
  surface, which build the same delegates with an identity `chatClientDecorator`. A
  BLOCK-mode violation inside a delegate call throws `AiGuardrailViolationException` synchronously out of
  `.call()`; every delegate's pre-existing `catch (RuntimeException)` arm converts it to a tool-error string
  via `ToolErrors.runtimeFailure(...)` (class-name only, not `getMessage()`) rather than crashing the turn.
  **Still uncovered, inherent to the advisor approach**: a delegate's completion still returns to the
  *parent* as a tool message (skips the parent's own input scan) and streams to the client as tool-result
  events (skips the parent's response scan) — the delegate's OWN advisor now redacts/blocks its own
  request+response, but the parent agent never re-scans tool outputs. **Also still uncovered: the
  Copilot panels and the management MCP surface.** They build the same intelligent delegates from the
  shared `IntelligentToolCatalog` with an identity `chatClientDecorator`, so no
  `SubAgentGuardrailedChatClient` is applied and those delegate calls run without the workspace's
  guardrails or system prompt — the same seam asymmetry that leaves them without session memory. (The
  older `SubAgentToolCallback` construction path this bullet used to flag is gone, but the surfaces it
  named still are not covered.)
- **Model-based moderation (F2, ticket 732)**: covers every advisor-fronted surface, not just the gateway.
  `AiGuardrails#checkInputs` (the advisor's non-throwing entry point) takes an optional
  `AiGatewayModerationClassifier` (same SPI the gateway adapter's own classifier already implements — no
  cycle, the module already depended on `platform-ai-gateway-api`) and checks it when `moderation-enabled` /
  workspace `moderationEnabled` is active, fail-open on a classifier error like injection. The throwing
  `AiGuardrails#applyToInputs` (the gateway adapter's own path) deliberately never moderates — the gateway
  already moderates its own DTO pipeline with its own classifier + project overlay, so running it a second
  time here would double-moderate. `isActive` now also counts moderation as an activation reason. Because a
  moderation verdict has no locatable span (unlike a blocked term), a `REDACT_AND_CONTINUE` downgrade
  replaces the WHOLE message with `[REDACTED_MODERATED]` (category/metric `moderation_flagged`) rather than
  masking a substring — a deliberate departure from injection's existing downgrade (which still forwards
  only the pii/secret-redacted original text, unchanged by this work). The settings UI's old "currently
  applies to AI Gateway traffic only" caveat on the moderation toggle is gone; it now names the
  `bytechef.ai.gateway.guardrails.moderation-model` property required for the toggle to take effect. See
  the design spec's decisions log for why REDACT_AND_CONTINUE was implemented as whole-message masking
  rather than "moderation never downgrades."
- **Metric**: `bytechef_ai_guardrail{event, surface}` (`surface = gateway | ai_agent | ai_hub`), generalized
  from the old gateway-only counter; wired via `ObjectProvider<MeterRegistry>`. The per-surface split is
  clean for EVERY event on the advisor path, including request-direction ones: `AiGuardrails#checkInputs`
  (the advisor's non-throwing entry point) takes an `AiGuardrailMetrics` parameter and records
  `pii_redacted`/`secret_redacted`/`blocked_term`/`injection_flagged`/`moderation_flagged` through whatever instance
  `AiGuardrailsAdvisor` passes in — its own per-request, surface-tagged instance — not through the engine's
  own bean. Only `AiGuardrails#applyToInputs` (the gateway adapter's throwing entry point) still records
  through the engine's own internal `AiGuardrailMetrics` bean, gated on `bytechef.ai.gateway.enabled` and
  tagged `surface=gateway`. See `.agents/ai-guardrails.md` for the full breakdown.
- **Optional NER detector.** `platform-ai-guardrails-opennlp` (EE, off by default) plugs Apache OpenNLP
  into the `SensitiveDataDetector` SPI for unstructured PII (names, organizations) with no engine
  change. It ships no models — Apache distributes none — and is not stream-safe, so streamed
  completions still get regex redaction only. See the "OpenNLP detector" section above.
- Spec: `docs/superpowers/specs/2026-07-31-ai-guardrails-standalone-design.md`. Agent docs:
  `.agents/ai-guardrails.md` (engine, advisor, surfaces) and `.agents/ai-gateway-guardrails.md`
  (gateway adapter specifics, project overlay).
