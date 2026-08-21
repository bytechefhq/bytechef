# Browser voice Tier 1 — Deepgram-backed implementation plan

**Status:** Plan | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Ship a working browser voice agent in ~2 weeks instead of ~6 by **deferring the streaming-agent / turn-detector / cancellation work to Tier 2** and using the existing `deepgram/v1/voiceAgent` action as the in-workflow agent. This plan implements the **A1 + A3** subset from the broader voice work — the actual platform unlock that the client code shipped in earlier commits depends on.

## Why this scope and not the full Tier 1

The originally-drafted voice runtime Tier 1 has five features (streaming agent, inter-task wiring, turn detector, turn-scoped cancellation, hangup) and a 4-6 week timeline. The browser-voice spec explicitly calls out that **for browser-only Tier 1, all five are optional** if you use `deepgram/v1/voiceAgent` — a single component that handles STT, LLM, and TTS in one WS connection.

So this plan ships:

1. **A1.1** — `TriggerType.WEBSOCKET` enum value
2. **A1.2** — `browser/v1/voiceSession` trigger
3. **A1.3** — `POST /webhooks/{id}/voice-session-token` mint endpoint + service
4. **A1.4** — Token validation on WS upgrade in `WebhookWebSocketHandler`
5. **A3** — Inbound WS → embedded sub-workflow's `WebSocketEmitter` audio bridge

Nothing else. Customer workflows wire `deepgram/v1/voiceAgent` directly inside the browser trigger's `websocketTasks` extension. The browser sends audio → `WebhookWebSocketHandler` receives binary frames → bridges them to the running `voiceAgent` task's emitter → Deepgram does STT+LLM+TTS → audio flows back through the existing outbound emitter path → browser plays.

## Non-goals (defer to Tier 2)

- `ai/agent/v1/realtimeChat` streaming action — not needed when the agent loop lives at Deepgram.
- `voice/v1/turnDetector` — Deepgram has built-in VAD; turn-taking happens at the Deepgram WS endpoint.
- `EmitterChannelRegistry` / inter-task `subscribesTo`/`publishesTo` — single-task pipelines (one `voiceAgent` task) work with the existing trigger-emitter-everywhere model.
- `WebSocketEmitter.cancelTurn(turnId)` — barge-in is handled inside the Deepgram session.
- `twilio/v1/hangup` — irrelevant for browser path (WS close == hangup).
- Recording, transcript persistence, PII redaction.
- Per-tenant concurrent-call quotas.
- EE worker affinity (Tier 1 pins to coordinator for simplicity).
- Workspace-settings GraphQL field for AI Hub (separate spec / separate work).

## Design

### A1.1 — `TriggerType.WEBSOCKET` enum value

Append to the existing enum at `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/TriggerDefinition.java:37`:

```java
enum TriggerType {
    CALLABLE,
    DYNAMIC_WEBHOOK,
    HYBRID,
    LISTENER,
    POLLING,
    STATIC_WEBHOOK,
    WEBSOCKET
}
```

Appended at the end to preserve ordinal stability (project convention pinned by `EnumOrdinalStabilityTest`).

Engine treatment: `WEBSOCKET` triggers register **no** HTTP webhook controller — their public surface is just `POST /webhooks/{id}/voice-session-token` + WS upgrade at `/webhooks/{id}/wss`. The existing `WebhookWebSocketHandler` already serves the WS upgrade; the new trigger just needs to coexist without an HTTP webhook controller trying to register for it.

Audit:
- `WebhookTriggerController` and similar — branch on `TriggerType.WEBSOCKET` and skip registration.
- `WebhookWorkflowExecutor` — same.
- `WorkflowTrigger.of(...)` — no change (operates on configuration, not type).

### A1.2 — `browser/v1/voiceSession` trigger

New component module at `server/libs/modules/components/browser/`.

```java
public class BrowserVoiceSessionTrigger {
    public static final String SUB_WORKFLOW = "subWorkflow";
    public static final String SAMPLE_RATE = "sampleRate";
    public static final String ECHO_CANCELLATION = "echoCancellation";
    public static final String NOISE_SUPPRESSION = "noiseSuppression";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("voiceSession")
        .title("Browser Voice Session")
        .description("Triggers when a browser opens a voice session against this workflow.")
        .type(TriggerType.WEBSOCKET)
        .properties(
            string(SUB_WORKFLOW).label("Real-Time Workflow").required(true),
            string(SAMPLE_RATE)
                .label("Audio Sample Rate")
                .options(option("16 kHz", "16000"), option("24 kHz", "24000"))
                .defaultValue("16000")
                .required(false),
            bool(ECHO_CANCELLATION).defaultValue(true).required(false),
            bool(NOISE_SUPPRESSION).defaultValue(true).required(false))
        .output(outputSchema(object()
            .properties(
                string("sessionId").description("ByteChef-issued session id (UUID)"),
                string("startedAt").description("ISO-8601 session start timestamp"))));
}
```

Like `TwilioInboundCallTrigger`, the trigger carries no `webhookValidate` or `webhookRequest` — those are HTTP-shaped and don't apply to WS upgrades.

Component handler boilerplate same as other components: `@AutoService(ComponentHandler.class)` + `ComponentDefinition` + `@AutoService` registration.

`websocketTasks` extension is read at WS-upgrade time (existing flow in `WebhookWebSocketHandler.getWebsocketSubflowDefinition`) just like the Twilio inbound trigger.

### A1.3 — Voice-session-token endpoint + service

Mirrors the existing `WorkflowTestVoiceSessionTokenService` I shipped for the test path, but keyed by **webhookId** and gated by the customer's session (not internal). For now we keep it inside `platform-websocket-webhook-rest` next to the existing classes.

```java
@Service
public class BrowserVoiceSessionTokenService {
    private static final long TOKEN_TTL_SECONDS = 60L;
    private final Cache<String, String> tokenToWebhookId = …;

    public Token issue(String webhookId) { … }
    public boolean consume(String token, String webhookId) { … }

    public record Token(String token, long expiresInSeconds) {}
}
```

```java
@RestController
@RequestMapping("${openapi.openAPIDefinition.platform:}/webhooks/{webhookId}")
public class BrowserVoiceSessionTokenController {
    @PostMapping("/voice-session-token")
    public Token issue(@PathVariable String webhookId) { … }
}
```

**Auth model:**
- `/webhooks/**` is permit-all today (no session required). For browser voice, the customer's site embedding the chat widget MAY or MAY NOT be authenticated to ByteChef.
- **Tier 1 trade-off:** the token endpoint is permit-all to match the existing webhook security model. Real security comes from the workflow's webhook signature configuration (out of scope) and the single-use token consumption on WS upgrade.
- Future-tier improvement: scope tokens to a customer session token issued during embedding.

**CORS:** the existing `WebhookWebSocketHandler` already sets `setAllowedOriginPatterns("*")` for WS upgrades. The token endpoint needs the same `*` CORS treatment — add it via the existing webhook CORS configurer or a `@CrossOrigin` on the controller.

### A1.4 — Token validation on WS upgrade in `WebhookWebSocketHandler`

Today the handler accepts any WS upgrade carrying a `callSid` query param (Twilio path). For the browser path the URL is `wss://host/webhooks/{id}/wss?sessionToken=…&sampleRate=…` — no callSid.

Update `afterConnectionEstablished` to:

1. Inspect the trigger by `webhookId`. If its trigger type is `WEBSOCKET` (the new browser path), require `sessionToken` and validate it via `BrowserVoiceSessionTokenService.consume(token, webhookId)`. Reject otherwise.
2. Generate a synthetic `callSid` like `browser-<uuid>` for the session, register in `CallSessionRegistry`, and proceed with the existing `startWebsocketSubflow` flow.
3. For the Twilio path (existing `callSid` query param present), behavior is unchanged.

The session lifecycle (`afterConnectionClosed` → continuation, stop subJob, etc.) reuses the existing logic since `CallSessionRegistry` doesn't care what shape the `callSid` is.

### A3 — Inbound WS → emitter audio bridge

This is the most novel piece. Today `WebhookWebSocketHandler.handleBinaryMessage` is missing — the handler extends `TextWebSocketHandler` which only delivers text. For binary audio we need:

1. Change `WebhookWebSocketHandler` to extend `AbstractWebSocketHandler` (handles both text and binary).
2. Add `handleBinaryMessage(WebSocketSession session, BinaryMessage message)`:
   - Look up the `callSid` for this session via `sessionIdToCallSid`.
   - Look up the registered `WebSocketEmitter` for this callSid. (Where does this live? See below.)
   - Forward `message.getPayload()` (a `ByteBuffer`) into the emitter's inbound listener queue so the running sub-workflow task (e.g. `deepgram/v1/voiceAgent`) sees the bytes.
3. Add `handleTextMessage` JSON event routing: for control frames like `{"type":"control","action":"end"}`, close the session; otherwise forward into the emitter's text listener queue.

**Where does the `WebSocketEmitter` live?** Per the existing voice-agent runtime spec, the worker-side `WebSocketStreamTaskExecutionPostOutputProcessor` constructs a `WebSocketEmitter` when a task that returned a `WebSocketHandler` starts. The emitter is stored… somewhere. We need to:

- Verify whether a `WebSocketEmitter` registry exists today. If yes, just look it up by callSid. If not, **add one** as part of this work: a `Map<String, WebSocketEmitter> activeEmitters` keyed by callSid, populated by the worker-side processor when a task starts and cleared when it ends.
- This is the smallest version of the `EmitterChannelRegistry` from the voice-agent-runtime-tier1 spec — without the multi-task wiring. Tier 1 only supports one emitter per session (the `deepgram/v1/voiceAgent` task), so a flat map by callSid is enough.

**Emitter API check.** Confirm `WebSocketEmitter` exposes inbound listener registration (e.g. `addBinaryMessageListener(Consumer<ByteBuffer>)`). The Deepgram action's `handle(emitter)` callback should already be subscribing inbound listeners; we just need a way to deliver the data.

### Wire summary

```
Browser mic
  ↓ PCM16 binary frames
WS /webhooks/{id}/wss
  ↓ WebhookWebSocketHandler.handleBinaryMessage
WebSocketEmitter (registry lookup by callSid)
  ↓ emitter.dispatchInboundBinary(payload)
deepgram/v1/voiceAgent (running task subscribed to inbound)
  ↓ forwards to Deepgram WS
[Deepgram does STT → LLM → TTS]
  ↓ Deepgram WS sends audio back
deepgram/v1/voiceAgent
  ↓ emitter.send(audioBytes)
WebSocketEmitter (outbound listener)
  ↓ worker-side bridge → WS session.sendMessage(BinaryMessage)
Browser playback (PCM16 → AudioBuffer)
```

The only links that don't exist today: the inbound binary handler in the WS handler, and the emitter-by-callSid registry.

## Out-of-scope and accepted limitations

- **Single agent task per session.** Multi-task pipelines (STT branched to both agent and logger) require `EmitterChannelRegistry` — deferred to Tier 2.
- **No turn-scoped cancellation.** Deepgram handles barge-in internally; the platform doesn't need to.
- **No client-visible reconnect.** WS drop → session ends, browser shows error. User reopens voice mode.
- **No metrics.** `bytechef_voice_*` counters deferred.

## Test plan

Server (in `platform-websocket-webhook-rest-impl/src/test/`):

- `BrowserVoiceSessionTokenServiceTest` — issue, consume, replay rejection, expiry.
- `BrowserVoiceSessionTokenControllerIntTest` — POST returns token, missing webhook → 4xx (verified against route registration).
- `WebhookWebSocketHandlerBrowserVoiceIntTest` — WS upgrade with valid token registers a session; with invalid token closes `POLICY_VIOLATION`; with no token but Twilio callSid behaves as before.
- `WebhookWebSocketHandlerBinaryFrameTest` — binary frame forwarded to a mock `WebSocketEmitter` registered for the callSid.

Component (in `browser/src/test/`):

- `BrowserVoiceSessionTriggerTest` — definition snapshot (auto-generated JSON).

Client:

- The existing `useWorkflowTestVoiceSession`, `useAutomationChatVoiceSession`, and `useAiHubVoiceSession` hooks should all "just work" once the server side lands. Add an end-to-end smoke story in Storybook if time permits.

## Implementation sequence

1. **A1.1** — `TriggerType.WEBSOCKET` enum value + audit downstream switches. ~2-4 hours.
2. **A1.2** — `browser/v1/voiceSession` trigger component + scaffold module. ~4-6 hours.
3. **A1.3** — Token service + controller, mirroring `WorkflowTestVoiceSessionTokenService`. ~2-3 hours.
4. **A1.4** — Update `WebhookWebSocketHandler.afterConnectionEstablished` to branch on trigger type and validate tokens. ~3-4 hours.
5. **A3** — `WebSocketEmitterRegistry` (simple flat map), `handleBinaryMessage` forwarding, audit worker-side bridge to populate the registry. ~6-8 hours.
6. **Tests** — write the tests listed above as each piece lands, not at the end. ~1-2 days total.

Realistic estimate: **5-7 engineer-days** from spec to working browser voice agent against a Deepgram-backed workflow.

## What's working when this lands

- A workspace admin creates a workflow with a `browser/v1/voiceSession` trigger + a `deepgram/v1/voiceAgent` action in `websocketTasks`.
- The workflow's published `webhookUrl` is dropped into AI Hub's voice settings popover (or AutomationChatModal's `voiceWebhookUrl` config).
- User opens the chat surface, clicks the mic, grants permission, speaks. Agent responds via voice. All three client paths (editor test panel, widget, AI Hub) work against the same backend.
