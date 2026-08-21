# Voice agent runtime — Tier 1 design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

ByteChef has the transport (`platform-websocket-webhook-rest`), the edge codecs (ElevenLabs realtime STT / TTS), the Twilio inbound-call trigger with `callSid` plumbing, and the embedded WS sub-workflow (`websocketTasks`) parsed into a first-class `Workflow`. What's missing is the **agent loop** that turns these primitives into a working voice agent.

This spec defines the five runtime additions that, together, take us from "two parallel codecs hanging off Twilio" to "STT → LLM → TTS as a wired streaming graph with usable turn-taking, barge-in, and an agent that can end its own calls." Everything else from the gap analysis (provider abstraction, recording/PII, EE worker affinity, canvas UX, save-time validation) is deferred to a follow-up Tier 2 spec.

The five pieces:

1. **Streaming agent action** — `ai/agent/v1/realtimeChat` that consumes a transcript stream and emits a token stream, with chat memory bound to `callSid` and tool-calling.
2. **Inter-task stream wiring** — explicit upstream/downstream subscription between WS tasks, so `STT → LLM → TTS` composes.
3. **Turn-detector primitive** — `voice/v1/turnDetector` that consumes interim+final transcripts and emits only final turns on silence/EoU.
4. **Turn-scoped cancellation** — `WebSocketEmitter.cancelTurn()` propagating downstream so the LLM and TTS can be interrupted without tearing down the job.
5. **Hangup action** — `twilio/v1/hangup` that the agent can invoke as a tool to end its own call. Without this the agent is one-sided: it can listen and talk, but only the caller can hang up.

## Background — what exists today

- `WebhookWebSocketHandler` ([server/libs/platform/platform-webhook/platform-websocket-webhook-rest/](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/)) accepts a WS upgrade keyed by `callSid`, registers a `CallSession`, and spawns the embedded sub-workflow via `jobFacade.createJob(...)`. WS lifecycle drives `WorkflowContinuationHelper.createContinuationJob(...)` on close.
- `ActionDefinition.WebSocketHandler` and `WebSocketEmitter` ([sdks/backend/java/component-api/.../ActionDefinition.java](../../sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ActionDefinition.java)) expose `addMessageListener`, `addBinaryMessageListener`, `addCloseListener`, `addTimeoutListener`. The worker-side adapter in `platform-job-sync` also has `addOutboundListener` / `addOutboundBinaryListener` (platform-internal).
- `WebSocketStreamTaskExecutionPostOutputProcessor` (worker) + `WebSocketEmitter` (job-sync) bridge each `perform` that returns a `WebSocketHandler` onto the live WS session.
- ElevenLabs `createRealtimeTranscript` and `createRealtimeSpeech` follow the pattern: `perform(...)` returns a `WebSocketHandler` that opens a provider WS, then wires `webSocketEmitter` listeners to forward data both ways.
- The Twilio inbound-call trigger ([server/libs/modules/components/twilio/.../TwilioInboundCallTrigger.java](../../server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/trigger/TwilioInboundCallTrigger.java)) returns TwiML with `<Stream>` pointing at `wss://.../webhooks/{id}`, then exposes a `subWorkflow` property and a stringified `websocketTasks` extension that the WS handler parses into a real `Workflow` ([WebhookWebSocketHandler.java:365](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/WebhookWebSocketHandler.java:365)).
- Chat memory infrastructure already exists (`SPRING_AI_CHAT_MEMORY`) and AI Hub conversations bind a `threadId` per session. Voice can reuse it without inventing a new memory store.

### What the existing sample workflow looks like

```jsonc
{
  "triggers": [{
    "type": "twilio/v1/inboundCall",
    "websocketTasks": "{\"tasks\":[
      {\"name\":\"stt\",\"type\":\"elevenLabs/v1/createRealtimeTranscript\", ...},
      {\"name\":\"tts\",\"type\":\"elevenLabs/v1/createRealtimeSpeech\", ...}
    ]}"
  }]
}
```

Both tasks receive the **same** `webSocketEmitter` (Twilio's inbound stream) and both `.send()` back on it. There is no edge from STT → TTS. The codecs run as **siblings**, not as a pipeline.

> **Superseded (2026-08-11): execution model.** This spec describes the pipeline running as an Atlas job — a
> sub-workflow spawned via `jobFacade.createJob(...)`, one task per stage. That model could not work: Atlas dispatches
> a workflow's tasks sequentially and a voice stage does not complete until the caller hangs up, so a multi-stage
> pipeline never started its second stage, and the stage that did run pinned a worker thread for the whole call.
>
> Voice sessions now run on `VoiceSessionEngine` (`platform-websocket-webhook-rest`, package
> `com.bytechef.platform.webhook.voice`): the trigger hands the pipeline to the engine without starting a job, every
> stage starts at once through `ActionDefinitionFacade.executePerform` with null job/task ids, and Atlas re-enters only
> when the session ends via `WorkflowContinuationHelper.createContinuationJob`. The authoring contract in this spec is
> unchanged — same `websocketTasks` JSON, same "stage order IS the wiring". Everything below about jobs, sub-workflow
> spawning and task dispatch describes the old runtime.

## Non-goals (Tier 2/3, separate spec)

- Telephony provider abstraction (`VoiceCallContext` / `AudioStream` for Vonage, Plivo, SIP, LiveKit). Twilio-shaped code is acceptable for Tier 1.
- Recording / transcript persistence / PII redaction policy.
- EE microservice worker affinity for the WS lifetime.
- Voice-specific observability metrics (`bytechef_voice_call_*`). Tier 1 should expose hooks; the actual counters land in Tier 2.
- Per-tenant concurrent-call quotas.
- Canvas / designer-time visual editor for the embedded `websocketTasks` pipeline.
- Save-time validation that `audioFormat` agrees between trigger and downstream STT.
- Back-pressure beyond what the existing `MAX_PENDING_EVENTS = 100` cap already provides. We will document the limitation, not fix it here.
- Outbound call-control actions other than `hangup` (i.e. transfer, DTMF send) — Tier 2. `clear` is built into the TTS barge-in path (Feature 4). `hangup` is Tier 1 (Feature 5) because a self-terminating agent is table-stakes.

## Design

### Feature 1 — Streaming agent action `ai/agent/v1/realtimeChat`

A new action in the existing `ai/agent` component that consumes a stream of finalized user turns and emits a stream of assistant tokens.

#### Surface

```java
public static final ModifiableActionDefinition ACTION_DEFINITION =
    action("realtimeChat")
        .title("Realtime Chat")
        .description("Streaming LLM turn handler. Consumes finalized user turns, " +
            "emits assistant tokens, supports tool calls and chat memory.")
        .properties(
            string(MODEL).required(true),                            // e.g. "gpt-4o-mini"
            string(SYSTEM_PROMPT).required(false),
            string(THREAD_ID).required(false)                        // defaults to ${callSid}
                .description("Chat memory thread id. Defaults to the call's callSid."),
            integer(MAX_TOKENS).required(false).defaultValue(512),
            number(TEMPERATURE).required(false).defaultValue(0.7),
            array(TOOLS).items(toolReference()).required(false),
            string(KNOWLEDGE_BASE_ID).required(false))               // optional RAG
        .perform(AiAgentRealtimeChatAction::perform);                // returns WebSocketHandler
```

`perform` returns a `WebSocketHandler` — same pattern as the ElevenLabs realtime actions. The handler:

- Subscribes to its upstream emitter (see Feature 2). Inbound payloads are `{"type":"user_turn","text":"...","turnId":"..."}`.
- On each user-turn message: append to chat memory (Spring AI `ChatMemory`, thread = `THREAD_ID`), invoke streaming LLM, on each token emit `{"type":"assistant_token","text":"...","turnId":"...","done":false}` to its outbound emitter. On completion, emit a final `{"type":"assistant_token","text":"","turnId":"...","done":true}` and append the assistant message to memory.
- On tool call: emit `{"type":"tool_call","name":"...","arguments":{...},"turnId":"..."}` to a side-channel, await the tool result (a normal ByteChef sub-task), then continue streaming.
- On `cancelTurn(turnId)` (see Feature 4): abort the in-flight LLM generation, append a partial assistant message to memory (with `[interrupted]` marker), drop unsent tokens.

#### Why a new action, not extending `ai/agent/v1/chat`

The existing `chat` action's `perform` returns a value. The streaming variant needs a `WebSocketHandler` return type. The infrastructure underneath (memory, tools, vector store, model routing) is shared via a `RealtimeChatService` extracted from the existing agent code. Mirrors the `createSpeech` vs `createRealtimeSpeech` split in ElevenLabs.

#### Tool calls

Tools run as **normal ByteChef tasks** outside the streaming graph — they're not streaming. The agent action emits a `tool_call` event, the platform spawns a one-shot sub-job for the tool, the result is pushed back into the agent via `emitter.send({"type":"tool_result", ...})`. The agent resumes streaming.

This keeps tool execution on the normal job rails (retry, timeout, errors) instead of inventing tool-streaming semantics. Cost is one extra round-trip per tool call vs. inline execution — acceptable for Tier 1.

#### Chat memory

- Default `threadId` = `${callSid}` (resolved by the existing parameter-binding code; `callSid` is already exposed on the trigger output).
- Memory uses Spring AI's `JdbcChatMemoryRepository` backed by `SPRING_AI_CHAT_MEMORY`. No schema change.
- One row per turn (user / assistant / tool). Pruning policy: keep last N turns where N is configurable per-action (default 20).

### Feature 2 — Inter-task stream wiring

Today every WS action's `perform` receives the trigger's emitter. For pipelines we need to express "task B's emitter is task A's outbound."

#### DSL — `subscribesTo` and `publishesTo`

Each task in the `websocketTasks` sub-workflow may declare:

```jsonc
{
  "name": "agent",
  "type": "ai/agent/v1/realtimeChat",
  "subscribesTo": "turn",        // name of upstream task, or "$trigger"
  "publishesTo": "tts",          // name of downstream task, or "$trigger". Optional.
  "parameters": { ... }
}
```

Defaults (so simple linear pipelines stay terse):

- If `subscribesTo` is omitted, the runtime infers it from preceding task order in the array (first task subscribes to `$trigger`, every other task subscribes to its predecessor).
- If `publishesTo` is omitted, the runtime infers it from following task order (last task publishes to `$trigger`, every other task publishes to its successor).
- `$trigger` is the reserved name for the inbound WS emitter (currently the only emitter every task gets).

A non-linear graph (e.g., "STT branches to both LLM and a logger") sets `subscribesTo` explicitly. Fan-in (multiple `subscribesTo`) is **not** in Tier 1 scope.

#### Runtime — `EmitterChannelRegistry`

A new platform component, lives in `platform-job-sync` next to `WebSocketEmitter`:

```java
public class EmitterChannelRegistry {
    // jobId -> taskName -> emitter
    private final Map<Long, Map<String, WebSocketEmitter>> emitters = new ConcurrentHashMap<>();

    void register(long jobId, String taskName, WebSocketEmitter emitter);
    WebSocketEmitter get(long jobId, String taskName);          // throws if unknown
    void wire(long jobId, String upstream, String downstream);  // pipe upstream.outbound -> downstream.inbound
    void close(long jobId);                                     // on call end
}
```

When `WebSocketStreamTaskExecutionPostOutputProcessor` starts a task that returned a `WebSocketHandler`:

1. Construct a new `WebSocketEmitter` for that task (rather than handing it the trigger emitter).
2. Register it in `EmitterChannelRegistry` under `(jobId, taskName)`.
3. Resolve `subscribesTo` — if it's `$trigger`, wire the trigger emitter's outbound to this emitter's inbound listener queue. If it's a task name, look it up in the registry and wire it the same way.
4. Resolve `publishesTo` — wire this emitter's outbound to the named downstream's inbound.
5. Invoke `handler.handle(emitter)`.

"Wire X.outbound to Y.inbound" means: for each `Consumer<Object>` on X.outboundListeners, fan into Y's `handleIncomingMessage` queue. Implementation is straightforward listener registration.

Tasks are started in DAG order (existing workflow engine behaviour). A task whose `subscribesTo` is not yet registered waits up to a small timeout (e.g. 200 ms) — but with DAG order this should never trigger.

#### Backwards compatibility

If neither `subscribesTo` nor `publishesTo` is set on **any** task in the sub-workflow, fall back to the current behaviour: every task gets the trigger emitter. Existing workflows do not break.

### Feature 3 — Turn-detector primitive `voice/v1/turnDetector`

A new lightweight component (`server/libs/modules/components/voice/`) with one action: `turnDetector`. Subscribes to a transcript stream (interim + final), emits only finalized user turns.

#### Surface

```java
action("turnDetector")
    .title("Turn Detector")
    .properties(
        integer(SILENCE_MS).defaultValue(500),               // ms of silence to consider end-of-utterance
        integer(MAX_UTTERANCE_MS).defaultValue(15000),       // hard cap
        string(MODE).options("silence", "provider", "punctuation").defaultValue("silence"))
    .perform(VoiceTurnDetectorAction::perform);
```

#### Modes

- `silence` — accumulate `transcript_interim` messages; on `SILENCE_MS` of no new message, emit `{"type":"user_turn","text":"<concat>","turnId":"..."}`. Also emits `{"type":"speech_start","turnId":"..."}` when the first interim arrives — used by Feature 4 (barge-in).
- `provider` — pass through provider-supplied `is_final=true` messages unchanged but normalize to the `user_turn` schema. Works with providers that have server VAD (Deepgram, OpenAI Whisper realtime).
- `punctuation` — emit on `.!?` plus a short follow-up grace window. Useful when the provider doesn't expose VAD.

Provider-specific quirks (ElevenLabs interim message shape, Deepgram envelope) live in the **STT components**, which normalize their output to `{"type":"transcript_interim","text":"...","ts":...}` and `{"type":"transcript_final",...}`. The turn detector is provider-agnostic.

#### Why a separate task rather than inlining into STT

- Same STT can be paired with different turn-detection strategies (silence vs. provider VAD).
- Turn detector emits `speech_start` events that need to reach the cancellation channel (Feature 4) — having it as a node in the wiring graph keeps that explicit.
- Future use cases: feeding the same transcript stream to both an agent and a live-transcript-display task with different turn semantics.

### Feature 4 — Turn-scoped cancellation

#### `WebSocketEmitter.cancelTurn(turnId)`

Add a new method to the `WebSocketEmitter` interface:

```java
interface WebSocketEmitter {
    // ...existing...
    void cancelTurn(String turnId);
    void addCancelTurnListener(Consumer<String> listener);
}
```

`cancelTurn(turnId)` propagates downstream through the wiring graph: the agent's emitter receives `cancelTurn`, which the realtime-chat action handles by aborting the LLM generation for that `turnId`. The TTS emitter receives it next and handles it by emitting a Twilio-shaped `{"event":"clear","streamSid":"..."}` to the inbound emitter (clearing playback buffer) and dropping any in-flight synthesis.

The propagation is implicit: when the registry wires `A.outbound -> B.inbound`, it also wires `A.cancelTurn -> B.cancelTurn`.

#### Wiring barge-in end-to-end

```
turnDetector emits speech_start(turnId=T_new)
  → goes upstream into the wiring graph as a control event (separate from data events)
  → control bus delivers cancelTurn(T_prev) to the agent emitter and to the tts emitter
agent emitter receives cancelTurn(T_prev) → aborts LLM generation, persists partial reply to memory
tts emitter receives cancelTurn(T_prev) → cancels in-flight ElevenLabs synthesis, sends {"event":"clear"} to Twilio
turnDetector later emits user_turn(T_new, text=...) → normal flow resumes
```

Open question: should `speech_start` and `cancelTurn` flow on the **same** stream as data, or on a separate control channel? **Recommendation: separate control channel** — keeps the data path simple, and a "data + control" dual channel is the standard reactive-streams idiom (Reactor `Sinks.Many` for data, `Sinks.One` per turn for control). To be confirmed in implementation.

#### Twilio `clear` envelope

```json
{"event": "clear", "streamSid": "MZ..."}
```

The `streamSid` must be captured from Twilio's `start` event ([WebhookWebSocketHandler.handleTextMessage](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/WebhookWebSocketHandler.java)) and surfaced via `CallSession` so the TTS task can read it. We already have a `CallMetadata` shape — add `streamSid` there.

### Feature 5 — Hangup action `twilio/v1/hangup`

A normal one-shot action (not a streaming task) that ends a Twilio call via the REST API. Designed primarily to be invoked by the agent as a **tool call**, but usable from any workflow context.

#### Surface

```java
action("hangup")
    .title("Hangup Call")
    .description("End an in-progress Twilio call.")
    .properties(
        string(CALL_SID).required(true)
            .description("The Twilio callSid of the call to end. Often ${trigger_1.callSid} " +
                "or supplied automatically when invoked as an agent tool."),
        integer(DELAY_MS).required(false).defaultValue(0)
            .description("Milliseconds to wait before issuing the hangup. Use a small delay (e.g. 1500ms) " +
                "to let the assistant's farewell TTS finish playing."))
    .perform(TwilioHangupAction::perform);
```

`perform` calls Twilio's `POST /2010-04-01/Accounts/{AccountSid}/Calls/{CallSid}.json` with `Status=completed`. Returns the updated call resource.

#### Why a normal action, not a streaming task

`hangup` is fundamentally a control plane operation — single request, single response, idempotent on `Status=completed`. Modelling it as a `WebSocketHandler` would be wrong: nothing streams. As a normal action it composes with the existing tool-call infrastructure (Feature 1's tool dispatch already runs tools as one-shot sub-jobs), and it works equally well from the outer `tasks` array if you want to hang up programmatically after some condition.

#### Drain coordination — explicit, not automatic

When the agent decides to say goodbye, the natural flow is:

1. Agent emits a goodbye token stream → TTS plays it.
2. TTS finishes playing → call should end.

Step 2 is the problem: if the agent calls `hangup` immediately after emitting the last token, TTS may still be mid-playback and the caller hears a cut-off mid-word.

Tier 1's answer is the **`DELAY_MS` parameter**. The agent's prompt or tool definition encourages "if you call hangup, pass a delay long enough for your last sentence (typically 1500–3000 ms)." This is crude but ships. It avoids inventing an "audio drain" signal between TTS and `hangup` (which would need cross-task coordination outside the streaming graph).

Open question 9 below tracks the cleaner alternative.

#### Agent tool wiring

A `hangup` tool can be declared on the `realtimeChat` action:

```jsonc
{
  "name": "agent",
  "type": "ai/agent/v1/realtimeChat",
  "parameters": {
    "tools": [
      {"$ref": "twilio/v1/hangup", "parameters": {"callSid": "${trigger_1.callSid}"}}
    ]
  }
}
```

The agent sees `hangup({delayMs?: number})` in its tool schema (with `callSid` pre-bound by the workflow). When invoked, the existing tool-call sub-job mechanism dispatches `twilio/v1/hangup`. No new infrastructure.

#### Idempotency / race notes

- `Status=completed` is idempotent on Twilio's side. Calling it on an already-ended call returns the call resource without error.
- The Twilio WS will receive a `stop` envelope and close. `WebhookWebSocketHandler.afterConnectionClosed` triggers the normal `WorkflowContinuationHelper.createContinuationJob(...)` path, so post-call cleanup tasks run as usual.

## Worked example — end-to-end voice agent

```jsonc
{
  "label": "Twilio Voice Agent",
  "triggers": [{
    "name": "trigger_1",
    "type": "twilio/v1/inboundCall",
    "parameters": {},
    "websocketTasks": {
      "tasks": [
        {
          "name": "stt",
          "type": "elevenLabs/v1/createRealtimeTranscript",
          "parameters": {"model_id": "scribe_v2_realtime", "audioFormat": "ulaw_8000", "sampleRate": 8000}
        },
        {
          "name": "turn",
          "type": "voice/v1/turnDetector",
          "parameters": {"silenceMs": 500, "mode": "silence"}
        },
        {
          "name": "agent",
          "type": "ai/agent/v1/realtimeChat",
          "parameters": {
            "model": "gpt-4o-mini",
            "systemPrompt": "You are a helpful support agent. Keep replies under 2 sentences. When the caller is done, say goodbye and call hangup with delayMs around 2000.",
            "threadId": "${trigger_1.callSid}",
            "tools": [
              {"$ref": "lookupOrder"},
              {"$ref": "createTicket"},
              {"$ref": "twilio/v1/hangup", "parameters": {"callSid": "${trigger_1.callSid}"}}
            ]
          }
        },
        {
          "name": "tts",
          "type": "elevenLabs/v1/createRealtimeSpeech",
          "parameters": {"voiceId": "21m00...", "model_id": "eleven_flash_v2_5", "outputFormat": "ulaw_8000"}
        }
      ]
    }
  }],
  "tasks": [
    {
      "name": "log_call",
      "type": "logger/v1/info",
      "parameters": {"text": "Call ${trigger_1.callSid} ended"}
    }
  ]
}
```

Note `websocketTasks` is shown as an inline object — this implies a small DSL change to allow either a stringified JSON (current) or an embedded object. The string form continues to work for backwards compatibility.

## Phasing / commit plan

| # | Commit | Notes |
|---|---|---|
| 1 | `EmitterChannelRegistry` + wiring engine in `platform-job-sync` | No behaviour change yet (registry is unused). Includes unit tests with fake handlers. |
| 2 | `subscribesTo`/`publishesTo` parsed by `WebhookWebSocketHandler`; default linear wiring | Existing sample workflow continues to work via fallback to "every task subscribes to $trigger." New 2-task linear pipeline test. |
| 3 | `voice/v1/turnDetector` component | New module under `server/libs/modules/components/voice/`. Tests with synthetic interim+final transcript streams. |
| 4 | `ai/agent/v1/realtimeChat` action | New action in existing `ai/agent` component. Streaming LLM perform returns `WebSocketHandler`. Memory + tool dispatch reuse existing services. |
| 5 | `cancelTurn` on `WebSocketEmitter` interface + propagation through registry | Plumbing only — no use sites yet. |
| 6 | Wire barge-in: `turnDetector` emits `speech_start`, agent and tts honor `cancelTurn`, tts sends Twilio `clear` | Integration test: simulated mid-utterance interruption, assert `clear` reaches Twilio mock within 200 ms. |
| 7 | `twilio/v1/hangup` action + agent-tool wiring | Normal one-shot action under `server/libs/modules/components/twilio/`. Reuses existing Twilio connection. Includes `delayMs` parameter. |
| 8 | Inline-object form for `websocketTasks` in the workflow DSL | Backwards-compatible (string form still parsed). Optional — could ship later. |
| 9 | Embed the end-to-end worked example in a `voice-agent-quickstart.md` and run it locally against Twilio + ElevenLabs sandboxes | Smoke test only — full E2E is out of CI scope. |

Commits 1–3 are independent and can land in parallel branches. Commits 4–6 sequence on top of 1–3. Commits 7 and 8 are independent. Commit 9 is verification.

## Tests

### Unit
- `EmitterChannelRegistryTest` — register/get/wire/close, two-task wiring fan-out.
- `TurnDetectorTest` — silence mode emits one `user_turn` per silence gap; provider mode passes through `is_final` only; punctuation mode emits on terminator + grace.
- `RealtimeChatTest` — given a scripted user turn, asserts emitted token stream and final memory state.
- `CancelTurnPropagationTest` — fires `cancelTurn` upstream, asserts all downstream emitters receive it in order.
- `TwilioHangupActionTest` — given a fake Twilio REST client, asserts the action POSTs `Status=completed` to the right URL, honors `delayMs`, and surfaces 4xx errors. Idempotency test: hangup-then-hangup returns success.

### Integration
- `VoiceAgentIntTest` (a `*IntTest` under `platform-webhook` or a new `platform-voice-int` module) — full sub-workflow with a fake STT (replays a transcript script), the real turn detector, a stubbed `realtimeChat` (echoes user text back), and a fake TTS (captures outbound audio frames). Asserts:
  - Linear order: text appears at TTS after the configured silence gap.
  - Barge-in: a second `speech_start` while TTS is mid-stream cancels the previous turn within 200 ms and the next `user_turn` reaches the agent.
  - WS close mid-call triggers normal `WorkflowContinuationHelper` post-call job.

### Manual smoke
- Real Twilio number → real ElevenLabs WS → real OpenAI model. Tester calls the number, has a 3-turn conversation including an interruption. Tester records mic→speaker latency manually.

## Open questions

1. **`websocketTasks` inline-object form** — worth bundling into this spec or kicking to a follow-up? Listed as commit 7 (optional).
2. **Control-channel transport** — separate Reactor `Sinks.Many` per emitter for control events, vs. typed envelopes on the data channel (`{"type":"control.cancelTurn", ...}`). Recommendation: separate channel. To be validated in commit 5.
3. **Tool-call streaming semantics** — Tier 1 spec says tool runs as a one-shot sub-job (no streaming inside tool). If a tool itself wants to stream (e.g., a long-running RAG retrieval surfaced as partial results), that's Tier 2.
4. **`callSid` parameter binding before the call exists** — `${trigger_1.callSid}` needs to resolve at task-start time inside the sub-workflow. The sub-workflow already receives `inputs.put("callSid", callSid)` in [WebhookWebSocketHandler.startWebsocketSubflow](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/WebhookWebSocketHandler.java) so this should already work — verify in commit 4.
5. **Provider parity for STT/TTS** — only ElevenLabs is in scope today. Adding Deepgram / OpenAI realtime / Cartesia is a follow-up. Their interim/final transcript shapes need to normalize into the `transcript_interim` / `transcript_final` schema expected by `turnDetector`.
6. **Memory write on cancelled turn** — when the user barges in mid-assistant-reply, do we save the partial assistant message to chat memory? Recommendation: yes, with a `[interrupted]` suffix. Confirm in commit 6.
7. **Error semantics for upstream WS close** — if the ElevenLabs STT WS dies mid-call, should the agent task error out the job, or attempt reconnect? Tier 1 recommendation: error out, rely on the existing job error path + post-call continuation. Reconnect/retry is Tier 2.
8. **Cancellation race** — `cancelTurn(T1)` while `T1` already finished (token stream done, audio playing out). The `clear` envelope to Twilio is still meaningful (stops the audio). Confirm: TTS should send `clear` even if its own work is complete, as long as Twilio hasn't acknowledged the final mark.
9. **Hangup drain coordination** — Tier 1 uses a `delayMs` parameter on `hangup` to give TTS time to finish the farewell. The cleaner alternative is for TTS to send a Twilio `mark` envelope after its final audio frame, capture the matching mark callback ([TwilioCallbackController.java](../../server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/rest/TwilioCallbackController.java) already receives Twilio status callbacks), and only then issue the hangup. Deferred to Tier 2 because it requires a new control channel between the TTS task and the (outer-workflow) hangup task, which crosses the streaming/non-streaming boundary. Document the `delayMs` workaround in the quickstart.

## Risks

- **Latency budget.** Mic→speaker target for a usable voice agent is ~1.5 s p95. The pipeline now adds: STT (~300 ms), turn detector silence wait (500 ms default), LLM first token (~300 ms), TTS first audio (~200 ms). That's already at ~1.3 s before any platform overhead. The new wiring layer must not add more than ~20 ms per hop. **Mitigation:** measure end-to-end latency in the manual smoke before declaring Tier 1 done; tune silence threshold defaults per use case.
- **Reactor learning curve.** The current `WebSocketEmitter` is callback-based; the new wiring may pull in proper Reactor sinks. Risk of inconsistent style in the codebase. **Mitigation:** keep the public emitter interface unchanged (`addMessageListener` etc.) and only use Reactor internally inside the registry.
- **Backwards compatibility.** Existing demo workflow uses sibling-codecs model. Must keep working. **Mitigation:** "no `subscribesTo` / `publishesTo` anywhere" → fall back to current behaviour. Add a regression test.
- **No back-pressure.** TTS produces audio faster than Twilio can play it. Current `MAX_PENDING_EVENTS = 100` drop-tail cap remains. We document the limitation. **Mitigation:** Tier 2 spec to add reactive demand.
- **Tool-call interaction with cancellation.** If a turn is cancelled mid-tool-call, the tool sub-job keeps running. Wastes work but doesn't break correctness. **Mitigation:** accept for Tier 1; cleaner tool cancellation is Tier 2.
- **`streamSid` capture.** TTS needs `streamSid` to send `clear`. Today the WS handler doesn't extract it from Twilio's `start` envelope. **Mitigation:** add to commit 6 — small change to `WebhookWebSocketHandler.handleTextMessage` to extract `start.streamSid` into `CallMetadata`.

## Glossary

- **`callSid`** — Twilio's unique identifier for a phone call. Already exposed by the inbound-call trigger.
- **`streamSid`** — Twilio Media Streams' identifier for the audio stream within the call. Different from `callSid`. Required to send `clear` messages.
- **Turn** — one user utterance + the assistant's reply. Each turn gets a `turnId` (UUID generated by the turn detector).
- **`$trigger`** — reserved name in `subscribesTo`/`publishesTo` referring to the inbound WS emitter (Twilio's stream).
- **End-of-utterance (EoU)** — the point at which the turn detector decides the user has finished speaking.
- **Barge-in** — the user starts speaking while the assistant is still talking; the assistant must stop and listen.
