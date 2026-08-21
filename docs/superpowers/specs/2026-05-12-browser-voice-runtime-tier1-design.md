# Browser voice runtime — Tier 1 design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Let users talk to a ByteChef-built voice agent **directly from a browser** — no phone number, no Twilio. The user opens a workflow's "voice mode" in the ByteChef client, grants mic permission, and gets the same streaming voice experience the Twilio path provides.

This is a **transport** spec, not an agent spec. The agent layer is provided by either:

- the existing `deepgram/v1/voiceAgent` action (end-to-end STT+LLM+TTS in one WS — easiest), or
- the composable streaming pipeline from `2026-05-12-voice-agent-runtime-tier1-design.md` (STT → LLM → TTS as separate components — flexible).

Both work over the same browser-voice transport. This spec defines that transport plus the frontend client needed to use it.

## Is browser voice easier than Twilio voice? Yes — materially.

The honest comparison:

**Server-side: ~50% of Twilio-path effort.** Most of the WS infrastructure (`platform-websocket-webhook-rest`, `WebhookWebSocketHandler`, sub-workflow execution, the `WebSocketHandler`/`WebSocketEmitter` interfaces) is already in place and provider-agnostic. The browser path needs a new trigger, a session-token API, and an audio envelope — but no TwiML, no Twilio REST API integration, no `streamSid`/`callSid` semantics, no signature validation, no μ-law transcoding, and no telephony provider abstraction concerns.

**Client-side: 100% new work.** ByteChef's `client/src` has zero `getUserMedia`, zero `AudioContext`, zero `new WebSocket(...)` today. Mic capture, audio playback, WS protocol, permission UX, mic level indicators, mute/end-call controls — all greenfield. The good news: these are well-trodden MDN APIs with mature patterns.

**Killer simplifications vs Twilio**:
- Hangup = close the WS (no REST call to Twilio, no `delayMs` drain coordination).
- Barge-in = stop sending audio frames and tell client to flush buffer (no Twilio `clear` envelope, no `streamSid` capture).
- Auth = your own session token bound to the user's existing session (no Twilio signature header).
- No TwiML — just a WS upgrade.
- Better audio fidelity available (PCM 16-bit at 16–24 kHz vs PSTN-constrained μ-law 8 kHz), which materially improves STT accuracy and reduces TTS artifacting.
- Lower latency (no Twilio hop, no PSTN bridge — typically ~80–150 ms shaved off mic→speaker).

**Combined estimate**: roughly 60–70% of the Twilio-path total effort, with most of the remaining work on the frontend rather than the platform. If the Deepgram voiceAgent action is used as the agent layer, you don't need any of the Tier 1 voice runtime features (streaming agent action, inter-task wiring, turn detector, turn-scoped cancellation) — making the browser path shippable independently and considerably faster.

## Background — what exists

- `platform-websocket-webhook-rest` accepts WS upgrades and runs an embedded sub-workflow. Reusable as-is; `WebhookWebSocketHandler` does not know about Twilio specifically (it does inspect a `callSid` query param, but the registry pattern is generic).
- `WebSocketHandler` / `WebSocketEmitter` in `ActionDefinition` — provider-agnostic. ElevenLabs realtime and Deepgram voice agent already use them.
- `deepgram/v1/voiceAgent` ([DeepgramVoiceAgentAction.java](../../server/libs/modules/components/deepgram/src/main/java/com/bytechef/component/deepgram/action/DeepgramVoiceAgentAction.java)) — one action, end-to-end voice agent over a single WS to Deepgram. Configurable LLM provider, TTS provider, language, greeting, prompt. Takes input audio (`linear16` PCM at configurable sample rate), emits output audio.
- `elevenLabs/v1/createRealtimeTranscript` accepts `pcm_16000` / `pcm_22050` / `pcm_44100` — no Twilio coupling. Drop-in for browser.
- `elevenLabs/v1/createRealtimeSpeech` likewise accepts PCM output formats — drop-in for browser playback.
- Client uses GraphQL + REST. No WebSocket client today. Bundled stack: React 19, TypeScript 5.9, Vite 8.

## Non-goals (Tier 2 / later)

- **WebRTC transport.** WebSocket is good enough for Tier 1; WebRTC's latency win (~50 ms) doesn't justify the SDP/ICE/STUN/TURN complexity. Tier 2 question.
- **Echo cancellation tuning.** Browsers expose AEC via `getUserMedia({audio: {echoCancellation: true}})`. Tier 1 accepts the browser default. Quality tuning is later.
- **Multi-tab session collision.** If a user opens two voice sessions on the same workflow, we accept that they get independent sessions; collision handling is Tier 2.
- **Offline mode / poor-network resilience.** Reconnect-with-resume is Tier 2; on disconnect we close the session.
- **Custom audio worklets.** Tier 1 uses standard `MediaRecorder` or `AudioWorklet` with PCM; advanced DSP (noise suppression beyond browser defaults, custom VAD on the client) is Tier 2.
- **Mobile browser support beyond "works on Safari iOS 17+ and Chrome Android."** No native app, no PWA-specific tuning.
- **Recording / transcript persistence.** Same as Twilio-path non-goal.

## Design

### Feature 1 — Browser voice trigger `browser/v1/voiceSession`

A new trigger type whose webhook accepts an authenticated WS upgrade rather than a Twilio-shaped HTTP request.

#### Trigger surface

```java
public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("voiceSession")
    .title("Browser Voice Session")
    .description("Triggers when a browser opens a voice session against this workflow.")
    .type(TriggerType.WEBSOCKET)                              // new TriggerType — see below
    .properties(
        string(SUB_WORKFLOW)
            .label("Real-Time Workflow")
            .required(true)
            .description("The sub-workflow to run for the duration of the voice session."),
        string(SAMPLE_RATE)
            .label("Audio Sample Rate")
            .options(option("16 kHz", "16000"), option("24 kHz", "24000"))
            .defaultValue("16000")
            .required(false),
        bool(ECHO_CANCELLATION).defaultValue(true).required(false),
        bool(NOISE_SUPPRESSION).defaultValue(true).required(false))
    .output(
        outputSchema(object()
            .properties(
                string("sessionId").description("ByteChef-issued session id (UUID)"),
                string("userId").description("Authenticated user id"),
                string("startedAt").description("ISO-8601 session start timestamp"))));
```

#### New `TriggerType.WEBSOCKET`

The Twilio inbound-call trigger today self-types as `STATIC_WEBHOOK` because it returns TwiML on HTTP. The browser trigger has no HTTP-response phase — the client speaks WS from the start. Add a new enum value:

```java
enum TriggerType { CALLABLE, DYNAMIC_WEBHOOK, HYBRID, LISTENER, POLLING, STATIC_WEBHOOK, WEBSOCKET }
```

Appended at the end so existing ordinal-stable serialization keeps working (per the project's enum ordinal convention).

The engine treats `WEBSOCKET` triggers as: no HTTP webhook controller registration; instead, an authenticated session-token endpoint (Feature 2) bound to this trigger, plus a WS upgrade endpoint at the same `/webhooks/{id}` path that `WebhookWebSocketHandler` already serves.

### Feature 2 — Session token API

Browser cannot send credentials over the WS upgrade reliably (no headers from `WebSocket` constructor, query-string auth is fine but exposes tokens in logs). The cleaner pattern: a REST/GraphQL mutation that mints a short-lived session token, then the browser opens the WS with `?sessionToken=...`.

#### GraphQL mutation

```graphql
type Mutation {
  startBrowserVoiceSession(
    workflowExecutionId: ID!
  ): BrowserVoiceSession!
}

type BrowserVoiceSession {
  sessionId: ID!
  wsUrl: String!                # absolute wss:// URL incl. ?sessionToken=...
  sampleRate: Int!              # echoed from trigger config
  echoCancellation: Boolean!
  noiseSuppression: Boolean!
  expiresAt: String!            # 60 s from issue
}
```

#### Server behaviour

1. Resolver verifies the caller is authenticated and has access to the workflow execution.
2. Looks up the trigger; rejects if not a `WEBSOCKET`-type browser voice trigger.
3. Mints `sessionToken = UUID` + stores `{sessionToken, sessionId, userId, workflowExecutionId, expiresAt}` in a short-TTL `Cache<String, SessionTokenRecord>` (Caffeine, 60s TTL — same pattern as `WebhookWebSocketHandler.streamHandles`).
4. Returns the WS URL + audio config.

#### Token validation in the WS handler

`WebhookWebSocketHandler.afterConnectionEstablished` extracts `sessionToken` from the URI, looks it up in the cache, and:

- Token not found / expired → close with `CloseStatus.POLICY_VIOLATION`.
- Token found → consume it (single-use, invalidate after lookup), populate the session with `userId` and `workflowExecutionId`, then proceed to start the sub-workflow via the existing `startWebsocketSubflow(...)` codepath.

### Feature 3 — Authenticated WS upgrade & session identity

Tier 1 reuses ByteChef's existing JWT auth indirectly: the session token is gated by the GraphQL mutation, which already runs through the normal authentication filter chain. The WS itself doesn't carry user creds — it carries the consumed session token, which the server has already auth'd.

#### `CallSession` → `VoiceSession`

The existing `CallSessionRegistry` ([CallSessionRegistry.java](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/CallSessionRegistry.java)) keys on `callSid`. For browser sessions:

- The key is the ByteChef-issued `sessionId` (UUID), not `callSid`.
- `CallMetadata` gains optional fields: `userId`, `sessionId`, `sourceType: "twilio" | "browser"`.
- The two source types coexist; `CallSessionRegistry` becomes the unified session store. Rename to `VoiceSessionRegistry` in a separate refactor commit; alias `getSessionByCallSid` to `getSessionById` for backwards compat.

#### `${trigger.sessionId}` and `${trigger.userId}` in the sub-workflow

The sub-workflow's `inputs` map is already populated with `callSid` for Twilio. For browser, populate `sessionId` and `userId` instead (with `callSid` unset). Sub-workflow templates use whichever matches their source.

For the streaming agent action's `threadId` default: prefer `${trigger.sessionId}` when present, fall back to `${trigger.callSid}`. Same chat memory plumbing, different session key.

### Feature 4 — Browser audio envelope protocol

The Twilio path uses Twilio's envelope (`{"event":"media","media":{"payload":"<base64 μ-law>"}}`). For browser, define a leaner ByteChef-native protocol.

#### Client → server (frames the user's voice)

Two options, both supported:

**Binary frames** (preferred, lowest overhead):
- 16-bit signed PCM at the configured sample rate.
- One WebSocket binary frame per ~20 ms slice (320 samples at 16 kHz = 640 bytes).
- Server inspects `WebSocketMessage` type to distinguish text vs binary, routes binary directly to `WebSocketEmitter.handleIncomingBinary(...)`.

**Text envelope** (fallback, debug-friendly):
```json
{"type": "audio", "payload": "<base64 PCM>", "ts": 1234567890}
```

Tier 1 ships binary primary, text accepted for diagnostics.

#### Client → server (control)

```json
{"type": "speech_start"}            // optional explicit barge-in signal from client VAD
{"type": "end_session"}             // graceful hangup
{"type": "set_mute", "mute": true}  // pauses audio frame transmission
```

#### Server → client (audio playback)

Mirror of client→server: binary PCM frames at the agreed sample rate, OR base64-wrapped in a text envelope for testing.

#### Server → client (control)

```json
{"event": "session_started", "sessionId": "..."}
{"event": "agent_speaking", "turnId": "..."}      // optional, for client UX (e.g., animation)
{"event": "agent_idle"}
{"event": "transcript", "text": "...", "role": "user|assistant"}   // optional, for live captions
{"event": "clear_playback"}                       // barge-in equivalent of Twilio's "clear"
{"event": "session_ended", "reason": "..."}
{"event": "error", "message": "..."}
```

`clear_playback` is what the TTS task emits (Feature 4 of the voice-agent runtime spec) when `cancelTurn` propagates. The client flushes its playback buffer (drops queued audio nodes) and starts listening.

### Feature 5 — Frontend voice client

A React component in `client/src/shared/components/voice/` providing the user-facing voice session UI.

#### Component shape

```tsx
interface VoiceSessionPanelPropsI {
    workflowExecutionId: string;
    onSessionEnd?: () => void;
}

// Internal state:
// - state: 'idle' | 'requesting_permission' | 'connecting' | 'active' | 'ended' | 'error'
// - micMuted: boolean
// - agentSpeaking: boolean
// - transcripts: Array<{role: 'user'|'assistant', text: string}>

export function VoiceSessionPanel({workflowExecutionId, onSessionEnd}: VoiceSessionPanelPropsI) { ... }
```

#### Audio capture pipeline

```
navigator.mediaDevices.getUserMedia({
    audio: {echoCancellation, noiseSuppression, sampleRate, channelCount: 1}
})
  → MediaStreamAudioSourceNode (AudioContext at sampleRate)
  → AudioWorkletNode running a downsample + PCM-encode worklet
  → port.onmessage → ws.send(int16ArrayBuffer)
```

`AudioWorkletNode` rather than `MediaRecorder` because we need 16-bit signed PCM at a known sample rate (MediaRecorder gives Opus-in-WebM which would need server-side decode). The worklet's source lives at `client/src/shared/components/voice/audio-worklets/pcm-encoder.js` — vendored, not bundled through Vite's main pipeline.

#### Audio playback pipeline

```
ws.onmessage (binary)
  → push Int16Array into a circular queue
  → AudioWorkletNode draining the queue at sampleRate
  → AudioContext.destination
```

Symmetric playback worklet at `pcm-decoder.js`. The queue lives in the worklet itself so playback is gapless even when the WS message rate fluctuates.

#### State management

Zustand store `useVoiceSessionStore`, scoped per `workflowExecutionId`. Stores connection state, mic mute, agent speaking state, optional live transcript array. Following CLAUDE.md conventions: reset in `beforeEach` for tests, exported store for direct manipulation, `useRef` for the worklet nodes.

#### UI affordances (Tier 1)

- Big mic button (active = pulsing color when caller is speaking based on RMS from worklet)
- Agent-speaking indicator (subtle animation; uses `agent_speaking`/`agent_idle` events)
- Live transcript (optional, gated by a setting — useful for accessibility)
- Mute toggle
- End session button → sends `{"type":"end_session"}` then closes WS
- Mic permission denied → friendly empty state with instructions

### Feature 6 — Hangup = WS close

No new component needed. End-of-session paths:

- Client clicks "End" → sends `{"type":"end_session"}`, then `ws.close(1000, 'user_ended')`.
- Agent invokes its `hangup` tool (which becomes a `voice/v1/endSession` action — sends `{"event":"session_ended","reason":"agent_hangup"}` to the client, then signals the WS handler to close).
- WS close → existing `WebhookWebSocketHandler.afterConnectionClosed` runs the normal `WorkflowContinuationHelper.createContinuationJob(...)` path. Identical to the Twilio path.

A small wrinkle: the Twilio path's `twilio/v1/hangup` from the voice-agent runtime spec is provider-specific. For Tier 1 of browser voice, add `voice/v1/endSession` as the provider-neutral version — same `delayMs` semantics, but implementation just emits the close to the active WS via the registry.

## Worked example — fastest path: Deepgram voice agent

The minimum-effort browser voice agent. Zero LLM/agent platform features required from the Tier 1 voice runtime spec.

```jsonc
{
  "label": "Browser Voice Agent (Deepgram)",
  "triggers": [{
    "name": "trigger_1",
    "type": "browser/v1/voiceSession",
    "parameters": {"sampleRate": "16000"},
    "websocketTasks": {
      "tasks": [{
        "name": "voiceAgent",
        "type": "deepgram/v1/voiceAgent",
        "parameters": {
          "language": "en",
          "prompt": "You are a helpful support agent. Keep replies under 2 sentences.",
          "greeting": "Hi! How can I help you today?",
          "audioInputEncoding": "linear16",
          "audioInputSampleRate": 16000,
          "audioOutputEncoding": "linear16",
          "audioOutputSampleRate": 16000,
          "llmProvider": "open_ai",
          "llmModel": "gpt-4o-mini",
          "ttsProvider": "deepgram",
          "ttsModel": "aura-2-thalia-en"
        }
      }]
    }
  }],
  "tasks": []
}
```

User opens this workflow in the ByteChef client's voice panel, hits the mic, talks. Done.

## Worked example — composable path: STT → LLM → TTS

Uses the voice-agent runtime Tier 1 spec features (streaming agent, inter-task wiring, turn detector, cancellation, hangup). Only the trigger differs:

```jsonc
{
  "triggers": [{
    "name": "trigger_1",
    "type": "browser/v1/voiceSession",
    "parameters": {"sampleRate": "16000"},
    "websocketTasks": {
      "tasks": [
        {"name": "stt", "type": "elevenLabs/v1/createRealtimeTranscript",
         "parameters": {"model_id": "scribe_v2_realtime", "audioFormat": "pcm_16000", "sampleRate": 16000}},
        {"name": "turn", "type": "voice/v1/turnDetector",
         "parameters": {"silenceMs": 500, "mode": "silence"}},
        {"name": "agent", "type": "ai/agent/v1/realtimeChat",
         "parameters": {
            "model": "gpt-4o-mini",
            "systemPrompt": "You are a helpful agent.",
            "threadId": "${trigger_1.sessionId}",
            "tools": [{"$ref": "voice/v1/endSession"}]
         }},
        {"name": "tts", "type": "elevenLabs/v1/createRealtimeSpeech",
         "parameters": {"voiceId": "21m00...", "model_id": "eleven_flash_v2_5", "outputFormat": "pcm_16000"}}
      ]
    }
  }]
}
```

Only differences from the Twilio composable example: trigger type, `pcm_16000` instead of `ulaw_8000`, `${trigger.sessionId}` instead of `${trigger.callSid}`, `voice/v1/endSession` instead of `twilio/v1/hangup`.

## Phasing / commit plan

| # | Commit | Notes |
|---|---|---|
| 1 | `TriggerType.WEBSOCKET` enum value + `browser/v1/voiceSession` trigger skeleton | Trigger declared, but no upgrade endpoint wired yet. Unit tests for the definition. |
| 2 | Session token GraphQL mutation + Caffeine cache | `startBrowserVoiceSession` resolver, single-use 60 s TTL. Tests cover happy path, expiry, double-consume rejection. |
| 3 | `WebhookWebSocketHandler` accepts `sessionToken` query param + binary-frame handling | Validates token, dispatches binary frames to the emitter's binary listener path. Keeps Twilio path unchanged. |
| 4 | `VoiceSessionRegistry` (rename from `CallSessionRegistry`) with `sourceType` field | Backwards-compat aliases for `getSessionByCallSid`. `CallMetadata` gains `userId`, `sessionId`, `sourceType`. |
| 5 | `voice/v1/endSession` action | Provider-neutral hangup. Reads session id from context, closes WS via registry. |
| 6 | Browser audio worklets (PCM encode/decode) and `useVoiceSessionStore` Zustand store | Vanilla JS worklets at `client/src/shared/components/voice/audio-worklets/`. Store unit tests with mock WS. |
| 7 | `VoiceSessionPanel` React component | Permission flow, capture pipeline, playback pipeline, control UI, transcript display. Tested with React Testing Library + a mock WS server. |
| 8 | Mount `VoiceSessionPanel` from the workflow detail page when a workflow has a `WEBSOCKET` trigger | Small UI add — an icon button in the workflow header that opens the voice panel sheet. |
| 9 | End-to-end quickstart doc + smoke test against the Deepgram voiceAgent | Browser → ByteChef → Deepgram, no other components. Documented mic→speaker latency baseline. |

Commits 1–5 are server work and can ship before any client work. Commits 6–8 are client work and can be developed in parallel with the server side once commit 3's protocol is locked. Commit 9 is verification.

## Tests

### Server
- `BrowserVoiceTriggerDefinitionTest` — `WEBSOCKET` type, required properties.
- `StartBrowserVoiceSessionResolverTest` — auth check, token issuance, expiry math, workflow ownership check.
- `WebhookWebSocketHandlerBrowserPathTest` — token-valid happy path, token-expired close, token-already-consumed close, binary-frame dispatch to listeners.
- `VoiceSessionRegistryTest` — Twilio + browser sessions coexist, lookup by callSid and sessionId both work.
- `EndSessionActionTest` — closes WS via registry, triggers continuation job.

### Client
- `useVoiceSessionStore.test.ts` — state transitions, mock WS receives audio frames, emits control envelopes.
- `VoiceSessionPanel.test.tsx` — render with `idle` state, click mic → state `requesting_permission`, mock `getUserMedia` reject → error state, mock accept → `connecting` → `active`. Use `vi.hoisted` for the WS mock per Vitest hoisting rules.
- Audio worklet tests are out of scope (worklets run in a separate global) — covered manually in the smoke test.

### Manual smoke
- Real browser → real ByteChef → real Deepgram voice agent. 3-turn conversation including an interruption. Measure mic→speaker latency p50/p95.

## Open questions

1. **WS vs HTTP/2 server-sent for downlink audio.** SSE downstream + WS upstream is sometimes simpler in restrictive networks. Tier 1 recommendation: full-duplex WS. Revisit if corporate firewall complaints arise.
2. **AudioWorklet vs MediaRecorder.** Worklet preferred (PCM at known rate, low latency). Fallback to ScriptProcessorNode for very old browsers is **not** scoped — modern Safari 17+ and Chrome 100+ are the targets.
3. **VAD on the client.** Sending silence is wasteful and confuses some server-side VAD providers. Adding browser-side `speech_start` detection (e.g., simple RMS threshold) before transmitting frames could materially reduce upstream bandwidth. Tier 1: send everything (simple); Tier 2: client-side VAD with a tunable threshold.
4. **Token transport.** Query string vs `Sec-WebSocket-Protocol` subprotocol header (the JS WebSocket constructor supports protocols but not arbitrary headers). Recommendation: query string for simplicity; document log redaction.
5. **Multi-tab session collision.** Same `(userId, workflowExecutionId)` opens two browser tabs and starts two sessions. Each gets a distinct `sessionId` and operates independently. Acceptable for Tier 1.
6. **CORS / cross-origin embeds.** If a customer embeds the ByteChef voice panel in their own page (iframe), mic permission propagates per browser policy (Permissions Policy header on the parent). Tier 1 supports embed only on same-origin or with explicit `allow="microphone"` on the iframe. Document.
7. **Recording / consent disclosure.** Even browser-based agents may need "this conversation may be recorded" disclosure depending on jurisdiction. Out of Tier 1 scope but flag in the quickstart.
8. **Sample-rate negotiation when STT/TTS providers disagree.** If `voiceAgent` wants 24 kHz but the user's mic device offers 48 kHz, downsample in the AudioContext (set `sampleRate: 24000` on AudioContext construction). Tier 1: trust the AudioContext's sample-rate parameter; some old Chromebooks ignore it and re-resample. Acceptable.
9. **Bidirectional message ordering.** If TTS audio frames and a `clear_playback` control event are both in flight, the client must apply `clear_playback` after the in-flight frames are dropped — i.e., handle control events on a separate worker thread that can flush the playback worklet's queue out-of-band. Confirm in commit 6.

## Risks

- **Latency budget.** Browser → ByteChef → Deepgram → ByteChef → Browser. Each hop is ~30–50 ms. Total mic→speaker target ~1.0 s p95 (better than Twilio's 1.5 s because we cut Twilio's hop). If actual measurement shows worse than 1.5 s, investigate AudioWorklet buffering and ws frame batching. **Mitigation:** baseline measurement is commit 9.
- **Worklet bundling fragility.** AudioWorklets are loaded via `audioContext.audioWorklet.addModule(url)` — Vite asset handling can break this. **Mitigation:** vendor the worklet files into `public/audio-worklets/` and reference them by absolute URL; do not import them as ES modules.
- **Permission UX edge cases.** User denies mic permission, or the device has no microphone, or another tab is holding the mic. The component must distinguish these and present sensible empty states. **Mitigation:** explicit state machine in `useVoiceSessionStore` for each error class.
- **Token leakage.** Query-string sessionToken appears in proxy access logs. Single-use + 60 s TTL limits damage but is not zero. **Mitigation:** document log scrubbing recommendation; consider header-based auth in Tier 2.
- **WS reconnect.** Network blips disconnect the WS mid-conversation. Tier 1 closes the session and surfaces a "connection lost" UI. Resume is Tier 2. **Mitigation:** make the failure mode obvious; do not silently drop user audio.
- **Mobile Safari quirks.** iOS Safari requires user gesture to start AudioContext and has historically eaten the first audio frame. **Mitigation:** start AudioContext inside the mic button's click handler (always a gesture); manual smoke includes Safari iOS.
- **Echo when not wearing headphones.** Browser AEC works but is imperfect; speaker output can leak into the mic. **Mitigation:** UI hint "headphones recommended"; for laptop integrated mic+speaker, AEC defaults should be on.

## Glossary

- **`sessionId`** — ByteChef-issued UUID identifying a browser voice session. Analogue of Twilio's `callSid`.
- **`sessionToken`** — short-lived single-use credential issued by GraphQL, consumed by the WS upgrade. 60 s TTL.
- **`AudioWorklet`** — modern Web Audio API primitive running audio code off the main thread. Replaces `ScriptProcessorNode` (deprecated).
- **`linear16` / PCM 16-bit** — the lingua-franca audio format for browser voice in Tier 1. 16-bit signed integers, little-endian, configurable sample rate.
- **AEC** — acoustic echo cancellation; browser feature exposed via `getUserMedia({audio: {echoCancellation: true}})`.
