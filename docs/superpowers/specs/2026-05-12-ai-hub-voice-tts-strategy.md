# AI Hub voice — TTS strategy decision

**Status:** v1 (Path A) shipped • v1.2 (real Path B with AI Hub LLM in the loop) shipped • Path B is single-mode (AI Hub LLM only) as of 2026-05-13 | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-13

> **History (2026-05-13):** v1.1 originally shipped a hybrid where Deepgram's voiceAgent ran the LLM; v1.2 added the real "AI Hub LLM in the loop" mode alongside it; later that day we removed the voiceAgent-passthrough mode entirely because it duplicated Path A's workflow-routed shape without giving customers any flexibility advantage. Path B is now unambiguously "AI Hub voice, AI Hub LLM, AI Hub tools." Customers wanting provider-runs-everything passthrough should use Path A with a voiceAgent component in their workflow.

## Context

AI Hub voice (shipped via [2026-05-12-ai-hub-voice-design.md](2026-05-12-ai-hub-voice-design.md)) routes audio through a customer-built workflow at `aiHubWorkspaceSettings.voiceWebhookUrl`. The user speaks; the workflow returns transcripts; AI Hub's agent generates the response. But how the **response** reaches the user as audio is an open question — three viable paths.

## The three paths

### Path A — Workflow is full STT → agent → TTS (defer to provider)

The configured `voiceWebhookUrl` points at a workflow with all three stages: speech-to-text, LLM reasoning, and text-to-speech all happening at the WS endpoint (typically via a single all-in-one component like `deepgram/v1/voiceAgent`).

**Flow:**
```
mic → WS → deepgram-voiceAgent (STT+LLM+TTS internal) → WS → speaker
                                ↓ optional transcript event
                              AI Hub message history (text only, for replay)
```

**Pros:**
- Zero AI Hub server-side voice infrastructure
- Provider handles barge-in, turn-taking, latency
- Works today with no code changes
- Customer owns the full voice pipeline configuration

**Cons:**
- **AI Hub's agent is bypassed for voice.** The provider's LLM is the brain; AI Hub's tool registry, chat memory, personal-agent instructions, MCP servers — none of it applies during voice
- Customer must configure model + provider keys at the workflow level (not via AI Hub's normal LLM provider config)
- The voice conversation is logically separate from the text conversation even when they share a task

### Path B — AI Hub native voice (server proxy)

AI Hub server runs its own voice WS endpoint, proxies audio to a configured voice provider, uses AI Hub's existing agent state to drive the LLM portion.

**Flow:**
```
mic → /api/automation/internal/ai-hub/voice/{taskId}/wss
       → AI Hub server bridges audio bidirectionally to OpenAI Realtime
         (or Deepgram, ElevenLabs)
         ↓ provider WS handles STT+TTS, but LLM stays with AI Hub
       ↑ AI Hub agent (tools, memory, MCP, personal-agent overlay) generates response
       → TTS audio back over WS to speaker
```

**Pros:**
- AI Hub's full agent intelligence applies to voice
- Zero customer configuration — voice "just works" once a workspace voice provider is configured
- Audit, metering, billing happen at AI Hub's server (not at the customer's workflow)
- One source of truth for the conversation (voice + text share the same task thread)

**Cons:**
- Substantial new server infrastructure: voice WS endpoint, provider abstraction, audio routing
- 1 week of work
- Provider API key management at workspace level (new GraphQL field, new admin UI)

### Path C — Browser TTS for assistant text

AI Hub's text agent generates text as today. When tokens stream in, the browser pipes them to the platform's built-in `SpeechSynthesis` API.

**Flow:**
```
mic → workflow webhook (STT only) → transcript → AI Hub agent (text response)
                                                   ↓ assistant_token stream
                                                 SpeechSynthesisUtterance → speaker
```

**Pros:**
- Cheap to implement (~half a day)
- AI Hub agent fully in play
- Works in every modern browser without additional providers

**Cons:**
- Browser TTS quality is poor — robotic, no prosody, no per-language nuance
- iOS Safari quirks (utterance queue, volume controls)
- Customer perception: "ByteChef voice sounds bad" — anchors expectations low
- No barge-in (browser doesn't expose mid-utterance cancel reliably)

## Decision

**Ship Path A for v1.** Defer Path B to v1.1 as the proper long-term architecture.

Rationale:

- Path A is **shippable today** with the workspace settings GraphQL field that just landed. Customers create a Deepgram voiceAgent workflow, paste the webhook URL into the AI Hub voice settings, voice works end-to-end.
- The architectural cost (AI Hub agent bypassed for voice) is real but bounded — voice conversations remain visible in the task thread via the transcript event, so users can see what was said even though the LLM came from the provider.
- Path B is the correct end-state but a full week of work. Doing it now blocks shipping AI Hub voice; doing it later improves an already-shipped feature.
- Path C looks tempting for "AI Hub agent + voice output" but the audio quality penalty makes it a worse user experience than Path A's all-in-one provider.

## What v1 ships

| Component | Path A behavior |
|---|---|
| **mic button** | Enabled when `aiHubWorkspaceSettings.voiceWebhookUrl` is set |
| **audio in** | Browser → workflow's WS via `BrowserVoiceSession` |
| **STT** | Workflow's choice (typically Deepgram realtime listen or voiceAgent) |
| **LLM** | Workflow's choice (typically OpenAI/Claude via provider's WS, NOT AI Hub's agent) |
| **TTS** | Workflow's choice (typically Deepgram aura or ElevenLabs via voiceAgent) |
| **audio out** | Workflow's WS → browser playback via `BrowserVoiceSession` |
| **transcript persistence** | `transcript_final` events from workflow → AI Hub task thread via `pendingVoiceUserMessage` queue |
| **assistant text persistence** | `assistant_text` events from workflow → AI Hub task thread via `appendToLastAssistantMessage` |
| **AI Hub agent intelligence (tools, memory, personal agents)** | **NOT applied during voice** — workflow's agent is in charge |

## Customer-facing documentation requirements

Before shipping, the AI Hub voice setup docs must clearly state:

1. **Voice in AI Hub uses your workflow's agent, not AI Hub's chat agent.** The system prompt, tool selection, and memory configured in the linked workflow apply during voice mode; AI Hub's tools, MCP servers, and personal-agent overlays do not.
2. **For voice that uses AI Hub's full agent capabilities, wait for v1.1.** Includes a link to the v1.1 design doc when it lands.
3. **Recommended provider for v1:** Deepgram voiceAgent — it's a single component handling STT+LLM+TTS+turn-taking+barge-in, requires only Deepgram API credentials, and works with the platform's PCM16 audio bridge as-is.

## What v1.2 ships (Path B as it stands today)

Path B is **single-mode by design**: the provider does STT and TTS only; AI Hub's `AiHubRoutingAgent` drives the LLM in between. The personal-agent overlay (for PERSONAL_AGENT tasks), workspace LLM provider, tools, and chat-memory continuity with text mode all apply during voice turns. There is no "let the provider also run the LLM" sub-mode — customers wanting that should use Path A with a Deepgram voiceAgent component in their workflow.

What's wired:

1. **Workspace settings fields** on `ai_hub_workspace_settings` (mutually exclusive with `voice_webhook_url`):
   - `voice_provider: enum(DEEPGRAM, OPENAI_REALTIME, ELEVENLABS)` — only `DEEPGRAM` wired today
   - `voice_model: string?` — provider-specific STT model (defaults to `nova-3` for Deepgram)
   - `voice_connection_id: long?` (soft FK to a `connection` row carrying the API key)
   - `voice_id: string?` (defaults: `aura-asteria-en` for Deepgram)
2. **GraphQL mutation** `updateAiHubVoiceProvider` (admin-only via `@PreAuthorize`). Service-level mutex clears the Path A field on save; same for Path A clearing Path B fields.
3. **REST + WS endpoints** in `automation-ai-hub-rest`:
   - `POST /api/automation/internal/ai-hub/voice/{taskId}/voice-session-token` — mints a single-use 60s token keyed to (taskId, workspaceId).
   - `WSS /api/automation/internal/ai-hub/voice/{taskId}/wss?sessionToken=...` — bidirectional PCM16 audio bridge.
4. **Voice provider SPI** in `automation-ai-hub-api`: `AiHubVoiceProvider`, `AiHubVoiceProviderSession` (with `sendAudio` / `speak` / `endTurn` / `cancelTurn` / `close`), `AiHubVoiceProviderConfig`, `AiHubVoiceProviderListener`, `AiHubVoiceProviderException`.
5. **Deepgram impl** in `automation-ai-hub-service`: `DeepgramVoiceProvider` → `DeepgramSttTtsSession` connects to `wss://api.deepgram.com/v1/listen` (STT, linear16, nova-3, smart_format, endpointing=300) + `wss://api.deepgram.com/v1/speak` (TTS) and translates events to listener callbacks. The conversation id is bound to the AI Hub task threadId so voice + text turns share `SPRING_AI_CHAT_MEMORY`.
6. **Agent bridge** (`AiHubVoiceAgentBridge`) builds `RunAgentParameters` from the transcript + threadId, calls `AiHubRoutingAgent.runAgent`, subscribes via `AgentSubscriber` to receive `TextMessageContentEvent` token deltas, sentence-buffers, and flushes each sentence into `providerSession.speak`. Sentence boundaries: `. ! ?` followed by whitespace, or `\n`. `RunFinishedEvent` triggers a final `endTurn` → TTS Flush.
7. **WS handler** — `AiHubVoiceWebSocketHandler` opens the provider session, wires the browser-side listener that both forwards events to the browser and drives the agent bridge on `onTranscript`. Cost-protection caps: 30-min max session duration + 10 concurrent sessions per workspace.
8. **Client-side routing** in `useAiHubVoiceSession`: when `voiceProvider` is set the hook routes to the native endpoint (same-origin, session cookie applies); otherwise it falls back to Path A. The `AiHubVoiceSettingsPopover` exposes a Path A / Path B / Disabled radio.
9. **Metrics** in `VoiceMetricsRecorder`: counter `bytechef_voice_session_path_total{outcome,path=A|B,provider}` slices session outcomes by path and provider. The original `bytechef_voice_session_total{outcome}` continues unchanged for dashboard compatibility.
10. **Personal-agent overlay** works automatically because the routing agent applies it via `applyAiHubPersonalAgentOverlay` before delegating to `AiHubSpringAIAgent`.
11. **Chat memory continuity** works automatically because Spring AI's `PromptChatMemoryAdvisor` keys on the threadId — voice and text turns share the same `SPRING_AI_CHAT_MEMORY` rows.

## What v1.3 still needs

1. **WORKFLOW_CHAT voice polish**. Routing already works (the routing agent dispatches to `WebhookBridgeAgent`), but the perceived UX is rough — workflows are slower than LLMs, so users hear long silences. v1.3 adds filler audio and graceful failure speech.
2. **Token-by-token TTS** (lowest latency). v1.2 sentence-buffers (~600–1200 ms perceived gap between turns). v1.3 should kick TTS off on the first complete clause to narrow that further.
3. **Barge-in cancel on user speech start**. Deepgram STT emits `SpeechStarted` events; we ignore them in v1.2. v1.3 should map them to `providerSession.cancelTurn` → TTS Clear, so the assistant audibly stops mid-sentence when the user interrupts.
4. **OpenAI Realtime + ElevenLabs** STT/TTS modes. v1.2 ships Deepgram only.
