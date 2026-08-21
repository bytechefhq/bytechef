# AI Hub voice — Path B v1.3 design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

v1.2 shipped real Path B (AI Hub LLM in the loop) with four conscious cuts. This spec scopes each as an **independent ship** — they don't depend on each other and can be prioritised individually. Suggested order: 0 → 1 → 2 → 3 → 4 (Item 0 is the prerequisite verification; the rest are cheapest/highest-value first).

## Item 0 — End-to-end verification of the v1.2 ship

**Why this is Item 0, not Item N.** The v1.2 ship (Path B simplification + personal-agent model override) is wired but never executed end-to-end with a real workspace, real provider, real LLM. Every other item in this spec assumes the v1.2 path actually works. Before building on it, verify it. Cheap fix; high risk if skipped.

**Concrete risks to verify.**

1. **State-key plumbing.** Do the new `PERSONAL_AGENT_LLM_PROVIDER_KEY` / `PERSONAL_AGENT_LLM_MODEL_KEY` set by [AiHubRoutingAgent.applyAiHubPersonalAgentOverlay](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java) actually reach [AiHubSpringAIAgent.resolveChatClient](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubSpringAIAgent.java) in production state shape (the AG-UI `RunAgentInput.state()` is populated as expected)? Unverified.
2. **Workspace-provider lookup.** Does [PersonalAgentChatClientResolver.resolveProvider](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/PersonalAgentChatClientResolver.java) actually find the provider via `WorkspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)` for a workspace with a real provider configured? Unverified.
3. **ChatOptions model override.** Does `ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().model(llmModel)).build()` at the ChatClient layer actually win over the underlying ChatModel's default model at the OpenAI / Anthropic API call? **This is the riskiest assumption.** Spring AI's option-merging behaviour varies by provider; if the underlying model name is baked into the per-provider client config (e.g. an `OpenAiChatModel` constructed with default options that include a model name), our override may be silently ignored.
4. **Cost attribution.** Do `ai_llm_usage` rows show the override model name vs the workspace default? If yes, per-agent cost tracking works automatically. If no, we have a metering bug that surfaces only after the feature is in use.

**Fastest test.**

Create two personal agents in a workspace whose AI Gateway has at least two providers (e.g. OpenAI + Anthropic, or two OpenAI variants):

1. Agent A: no LLM override (uses workspace default).
2. Agent B: explicit override to a deliberately different model (e.g. Claude Haiku on a workspace defaulting to Sonnet, or `gpt-4o-mini` on a workspace defaulting to `gpt-4o`).

Then in AI Hub for each agent:
1. Send "what model are you" — answer should differ.
2. Pull the OTLP trace for the request; verify the `gen_ai.request.model` attribute matches the chosen agent's model, not the workspace default.
3. Query `ai_llm_usage` for the run; verify the `model` column matches.

**Acceptance.** Both agents respond with the model they should be running, traces and usage rows match the picked model. If any of the three signals (response self-identification, OTLP trace, usage row) disagree, that's the bug to fix before v1.3 builds on top.

**Effort.** ~0.5 day end-to-end (assuming a workspace with two providers wired and OTLP enabled).

## Item 1 — Barge-in cancel on user speech start

**Today.** In `STT_TTS_NATIVE_LLM` mode, when the user starts speaking while the assistant is still talking:
- Deepgram STT emits `{"type":"SpeechStarted","timestamp":...}` on the listen WS
- [DeepgramSttTtsSession.SttListener](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/voice/DeepgramSttTtsSession.java) ignores it
- Assistant audio continues playing through the user's interruption
- The agent run also continues, generating tokens for audio the user will never finish hearing

This makes voice feel "deaf." Cheap fix.

**Change.**

1. **SPI extension.** Add `default void onSpeechStarted(String turnId)` to `AiHubVoiceProviderListener`.
2. **DeepgramSttTtsSession.SttListener.** Parse `SpeechStarted` events alongside `Results`. Emit `listener.onSpeechStarted(currentTurnId)`. Track `currentTurnId` on the session — bumped each time we open a new agent turn (i.e. when the WS handler kicks off `AiHubVoiceAgentBridge.runTurn`).
3. **NativeLlmListener.** On `onSpeechStarted`: call `providerSession.cancelTurn(currentTurnId)` (already wired to Deepgram TTS `Clear`) AND signal the in-flight `VoiceAgentSubscriber` to stop processing. Add a `cancel()` method on `VoiceAgentSubscriber` that flips its `closed` atomic so future `onTextMessageContentEvent` calls become no-ops. Optionally call `agent.cancelRun(...)` if AG-UI exposes one — otherwise the agent's run completes naturally but its output is discarded.
4. **Metrics.** New counter `bytechef_voice_barge_in_total{provider}` so we can see how often users interrupt — a strong UX signal.

**Acceptance.** With v1.3, you can interrupt the assistant mid-sentence and it stops within ~200 ms (the Deepgram SpeechStarted detection window). The next turn starts immediately on your speech ending. Manual test: ask a long question, interrupt 2 s into the answer, confirm assistant stops + listens for your follow-up.

**Effort.** ~1 day.

## Item 2 — Token / clause-streaming TTS

**Today.** [AiHubVoiceAgentBridge.VoiceAgentSubscriber](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/voice/AiHubVoiceAgentBridge.java) sentence-buffers (`. ! ?` + whitespace, or `\n`). Each full sentence is one `speak()` call. Typical perceived latency from user-finishes-talking to assistant-starts-talking: 600–1200 ms — dominated by waiting for the first complete sentence.

**Change.** Pick one of two granularities (both feasible, the tradeoff is naturalness vs latency):

- **Clause-bounded (conservative).** Extend the boundary detector to also break on `,`, `;`, `:`, `—`, `–` when followed by whitespace. Cuts first-audio latency roughly in half (most LLM responses have a comma within the first 5–8 tokens). Risk: occasional unnatural pauses at clause boundaries; TTS prosody resets between speaks.
- **Word-bounded with chunk pacing (aggressive).** Flush every 4–8 words OR at clause boundary OR at sentence boundary, whichever comes first. Lowest latency but highest risk of awkward prosody seams. Need to A/B test before defaulting.

**Recommended.** Ship clause-bounded as v1.3 default. Add a workspace setting `voice_streaming_mode` (enum `SENTENCE | CLAUSE | WORD`) so we can tune per-workspace based on user feedback. Default = `CLAUSE`.

**SPI.** No changes — purely a server-side buffering tweak in `AiHubVoiceAgentBridge`. The Deepgram TTS WS already accepts arbitrary text fragments and synthesizes in order; we just send shorter `Speak` payloads.

**Acceptance.** First-audio latency drops to <500 ms for typical responses. Manual test: ask "what's the capital of France" — assistant starts speaking before the period.

**Effort.** ~0.5 day for clause-bounded; +0.5 day for word-bounded + the setting.

## Item 3 — ElevenLabs provider

**Today.** Only Deepgram is wired in Path B. The `VoiceProvider` GraphQL enum has `DEEPGRAM` and `ELEVENLABS` slots; `OPENAI_REALTIME` was removed (commit 2e2d0811389) because its bidirectional-multimodal API doesn't fit Path B's single-mode contract — customers wanting OpenAI Realtime should use Path A with a workflow voiceAgent component.

**Change.** Add `ElevenLabsVoiceProvider` alongside `DeepgramVoiceProvider`:

- **STT WS:** `wss://api.elevenlabs.io/v1/speech-to-text/stream` (real-time streaming Scribe).
- **TTS WS:** `wss://api.elevenlabs.io/v1/text-to-speech/{voice_id}/stream-input` (WS streaming TTS — accepts text chunks via `{"text":"..."}` and a `try_trigger_generation` flag).
- Session class `ElevenLabsSttTtsSession` mirrors `DeepgramSttTtsSession`.
- ElevenLabs TTS sample rate: 22050 Hz (different from Deepgram's 24000). The Deepgram TTS WS hardcodes `TTS_SAMPLE_RATE = 24000`; provider-specific output rates need to be plumbed through the `connected` JSON envelope so the browser's `AudioWorklet` knows what to play.
- **Connection schema.** ElevenLabs uses `connection.parameters.token` (same shape as Deepgram). No new connection types — the existing ByteChef ElevenLabs component already defines a compatible connection.
- **Settings validation.** The WS handler currently looks up `providersByKey.get(settings.getVoiceProvider())`; a missing provider closes with `SERVER_ERROR`. Pre-flight (token mint time) should also validate.

**Acceptance.** Workspaces can pick Deepgram or ElevenLabs; each works end-to-end.

**Effort.** ~2 days.

## Item 4 — WORKFLOW_CHAT voice

**Today.** `STT_TTS_NATIVE_LLM` mode invokes `AiHubRoutingAgent.runAgent`, which dispatches by `ConversationKind`. For `WORKFLOW_CHAT` tasks it delegates to [WebhookBridgeAgent](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/WebhookBridgeAgent.java) which executes a workflow rather than calling an LLM. The current voice WS handler doesn't block this dispatch, but `WebhookBridgeAgent`'s response timing is unpredictable — a sync workflow could take 5–30 s, during which the voice line is silent.

**Change.** Three pieces:

### 4a. Path through `AgUiStreamBridge`

`WebhookBridgeAgent` already emits `TextMessageContentEvent` / `TextMessageEndEvent` via `AgUiStreamBridge` when the workflow streams. Voice mode should consume those the same way `VoiceAgentSubscriber` consumes LLM-driven content events. Verify in a smoke test that this works end-to-end without code changes.

For **sync** (non-streaming) workflows: `WebhookBridgeAgent` emits a single `TextMessageContentEvent` with the full response, then `TextMessageEndEvent`. Voice mode treats that as one big speak — fine, but the user heard nothing for the entire workflow execution time.

### 4b. "Thinking..." filler audio

While the workflow is running, emit a periodic neutral filler ("just a moment", "checking", "one second", varied so it doesn't feel mechanical) every ~3 s of silence. The filler is generated by AI Hub at the WS handler layer — it doesn't roundtrip through the agent.

Implementation: a new `VoiceFillerScheduler` that the WS handler arms when `kind == WORKFLOW_CHAT` and disarms on `TextMessageContentEvent`. Fires `providerSession.speak(filler)` with a random filler from a small list.

Risk: chatty users may find this annoying. Make it a workspace setting (`voice_workflow_filler: enum DISABLED | MINIMAL | NORMAL`, default = `MINIMAL` = one filler after 3 s, no further fillers).

### 4c. Workflow failure → graceful voice degradation

If `webhookFacade.executeStreaming` fails (workflow exception, network error), the WS handler must:

1. Speak an apology: "Sorry, I couldn't reach that workflow. Try again or check the workflow's status in the UI."
2. Emit `error` JSON to the browser so the chat UI shows the failure.
3. Persist a placeholder assistant message in `SPRING_AI_CHAT_MEMORY` so the user can see the failure in text mode.

**Acceptance.** WORKFLOW_CHAT tasks work in voice mode. A 5-second workflow shows one filler utterance midway. A workflow exception speaks the apology + shows the error toast. Round-trip transcripts persist to the task thread.

**Effort.** ~2 days, mostly in test coverage (workflow voice paths have many failure modes).

## Item 5 — Personal-agent override post-ship cleanup

**Why this is in the voice spec.** The per-agent LLM override shipped alongside Path B's simplification — same v1.2 ship — and the voice path is the surface where mis-configured overrides surface most visibly (a wrong model => wrong voice persona). Closing the trust loop on the override means closing it for voice too. Three sub-items, all small.

### 5a. Test coverage for the just-shipped code

Zero new tests today. Specific gaps:

- [`PersonalAgentChatClientResolver.validate(workspaceId, provider, model)`](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/PersonalAgentChatClientResolver.java) — Mockito-driven. Cases: provider not enabled in workspace → throws with "provider not enabled" message; model not enabled under provider → throws with "model not enabled" message; happy path → returns silently. ~1 hour.
- [`AiHubPersonalAgentServiceImpl.validateLlmOverrideAvailability`](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java) — verify the ObjectProvider-absent path is a no-op (gateway disabled), and the provided path delegates correctly. ~0.5 hour.
- `AiHubRoutingAgent.applyAiHubPersonalAgentOverlay` LLM key injection — verify `agent.hasLlmOverride()` true path injects both state keys, false path injects neither. Likely extends an existing routing-agent test. ~0.5 hour.
- `AiHubPersonalAgentForm` Vitest tests — provider→model reset on provider change, "default" sentinel maps to null on save, server validation errors surface to user. ~1–2 hours.

### 5b. Form UX edge cases

Three small UX gaps in the dropdowns that just shipped:

- **Empty-workspace case.** When the workspace has zero enabled providers, the provider dropdown shows only "Use workspace default" with no other options. Should show inline help: "No AI Gateway providers configured. Configure one in admin to enable per-agent model selection." ~15 min.
- **Orphaned override.** When editing an agent whose persisted `llmProvider` is no longer enabled in the workspace (provider got disabled/deleted after save), the provider dropdown shows nothing selected — admins see the override silently "disappear" in the UI. Should display the orphaned value as a special "[name] (no longer available)" option with a warning icon, prompting re-select. ~30 min.
- **Server validation error routing.** The new `IllegalArgumentException` messages from `PersonalAgentChatClientResolver.validate()` currently surface as generic toasts. Better UX: route them to the specific offending field (provider or model dropdown) and red-outline it. ~30 min.

### 5c. Design note — `llmProvider` as free-form string

The entity stores `llmProvider` as a `VARCHAR(64)` free-form string (e.g. `"openai"`, `"anthropic"`) that's matched against `AiGatewayProviderType.name().toLowerCase()` at runtime. If `AiGatewayProviderType` enum values are ever renamed or removed, existing rows would have stale string references that fail to resolve at runtime (the resolver's warn-and-fallback path catches this, but admins see silent override degradation).

Two options for v1.4 if drift becomes a concern:

- **Option A:** migrate `llm_provider` to an FK on `ai_gateway_provider.id`. More robust; on-delete cascade or restrict policy needed.
- **Option B:** document the constraint ("do not rename `AiGatewayProviderType` enum values after personal-agent rows exist") and rely on the runtime fallback.

**Recommended:** Option B for v1.2; revisit if a customer reports drift. Adding the FK is bigger than v1.3 scope and the runtime fallback already handles the failure mode gracefully (just suboptimally).

**Effort (5a + 5b + 5c):** ~1 day total. Test coverage is the largest chunk.

## Cross-cutting

### Metrics

Add the following to [VoiceMetricsRecorder](../../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/VoiceMetricsRecorder.java):

- `bytechef_voice_barge_in_total{provider}` — Item 1
- `bytechef_voice_first_audio_latency_seconds` (Timer, histogram-enabled) — Item 2: measure time from `transcript_final` to first `onAudio` callback. The single most user-visible metric for voice UX.
- `bytechef_voice_workflow_chat_filler_total{outcome=spoken|skipped}` — Item 4b

### Migration

Item 2 + 4 both need a new column. Group them in one Liquibase migration:

```sql
ALTER TABLE ai_hub_workspace_settings
    ADD COLUMN voice_streaming_mode INT NULL,    -- Item 2 enum
    ADD COLUMN voice_workflow_filler INT NULL;   -- Item 4b enum
```

Both INT ordinals per the project's enum-storage convention. NULL → defaults to `CLAUSE` and `MINIMAL` respectively.

### Docs

When each item ships:
- Update [TTS strategy doc](2026-05-12-ai-hub-voice-tts-strategy.md) "What v1.3 still needs" — strike through the shipped item, link to its v1.3 sub-section.
- Update [voice quickstart](../../voice/quickstart.md) Path B section if customer-visible (popover toggles, new providers, new latency profile).

## Decision points

A few choices to confirm before implementation:

1. **Item 2 default — `CLAUSE` or `WORD`?** Recommend `CLAUSE` (safer prosody). Word-bounded as an opt-in setting.
2. **Item 4b — filler audio in the default install?** Could feel cheesy. Default to `MINIMAL` (one filler after 3 s, no further) and let workspaces upgrade to `NORMAL` if their workflows are routinely 10+ s.
3. **Item 1 — also abort the agent run or just discard its output?** AG-UI may not expose a clean `cancelRun` hook. If not, discarding output is fine — the LLM cost is sunk either way and the next user turn drives a new run.
4. **Item 5c — `llmProvider` FK migration or document-the-constraint?** Recommend the latter for v1.3; Option A (FK) is bigger than v1.3 scope.

## Total effort

| Item                                                  | Effort  |
| ----------------------------------------------------- | ------- |
| 0. End-to-end verification (smoke test, no code)      | 0.5 day |
| 1. Barge-in cancel                                    | 1 day   |
| 2. Clause-streaming TTS                               | 0.5 day |
| 3. ElevenLabs provider                                | 2 days  |
| 4. WORKFLOW_CHAT voice                                | 2 days  |
| 5a. Personal-agent test coverage                      | 0.5 day |
| 5b. Personal-agent form UX edge cases                 | 0.25 day |
| 5c. Free-form-string design note                      | 0 (decision only) |
| Cross-cutting (metrics, docs)                         | 0.5 day |
| **Total**                                             | **~7.25 days** |

**Recommended bite (~1.25 days, closes the v1.2 trust loop):** Item 0 (smoke test) + Item 5a–b (tests + form UX edge cases). Verifies what shipped works AND raises its quality from "wired but fragile" to "tested + polished" before any new voice features build on top.

Items 1 and 2 alone are ~1.5 days and capture the most user-visible voice UX improvement; ship them after the trust loop closes if v1.3 ships in stages.
