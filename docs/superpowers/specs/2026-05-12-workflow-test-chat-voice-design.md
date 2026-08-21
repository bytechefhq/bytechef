# Workflow test chat — WebSocket / voice support design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Give workflow authors a way to **test voice workflows directly from the workflow editor**, the same way they test chat workflows today. Today the editor's `WorkflowTestChatPanel` only speaks SSE — the user types, the server streams text back. With voice workflows ([2026-05-12-voice-agent-runtime-tier1-design.md](2026-05-12-voice-agent-runtime-tier1-design.md), [2026-05-12-browser-voice-runtime-tier1-design.md](2026-05-12-browser-voice-runtime-tier1-design.md)) landing, the editor needs a way to:

1. **Speak** to a voice workflow over WebSocket — mic capture, audio playback, mute/end.
2. **See** the live transcript (both user side and assistant side) in the same chat thread, so a single panel reads naturally whether the test is text or voice.
3. **Switch modes**: a workflow author should be able to toggle between text and voice on the same panel without juggling two UIs.

The matching server-side gap: there is no **test** WebSocket endpoint for the workflow editor — `/webhooks/{id}/wss` requires a published workflow with a registered webhook id, but a workflow being edited has neither. We need an editor-scoped WS endpoint analogous to `POST /internal/workflows/{id}/tests`.

## Non-goals

- **Embedded customer voice chat.** The external widget (`AutomationChatModal`) is a separate spec.
- **AI Hub voice.** Separate spec.
- **Phone-call testing.** Twilio inbound requires a public URL; out of scope for editor preview. Voice testing is browser-only here.
- **Recording / transcript export.** Tier 2.
- **Mic level meter / audio waveform visualization.** Tier 2 polish; Tier 1 ships a simple "speaking" indicator.
- **Multi-language / locale UI**. English-only Tier 1.
- **Reconnect-with-resume on WS drop.** On disconnect, panel shows an error and the user re-opens voice mode.

## Background

- The panel today uses [`useWorkflowTestStream`](../../client/src/shared/hooks/useWorkflowTestStream.ts) → `POST /internal/workflows/{id}/tests` (returns SSE). The runtime provider is [`WorkflowTestChatRuntimeProvider`](../../client/src/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider.tsx) which wires `@assistant-ui/react`'s `useExternalStoreRuntime` to the chat store.
- The browser-voice spec defines a session-token endpoint (`POST /webhooks/{id}/voice-session-token`) and a WS upgrade at `/webhooks/{id}/wss?sessionToken=…`. That endpoint is **per published webhook** — it won't work for an unsaved workflow under edit.
- The platform already has `WebhookWebSocketHandler` at `/webhooks/*/wss`. We can model the test endpoint after it but key it by workflow id + an editor session token instead of webhook id + call sid.
- The chat panel store ([`useWorkflowTestChatStore`](../../client/src/pages/platform/workflow-editor/stores/useWorkflowTestChatStore.ts)) already has `resumeUrl` plumbing (resume-question workflows). Voice mode will add a parallel `voiceSessionState` slice.

## Design

### Feature 1 — Test WebSocket endpoint `/internal/workflow-tests/{id}/wss`

A new endpoint that accepts a WS upgrade for an in-editor workflow and runs the workflow's voice sub-workflow against the live audio stream — without requiring the workflow to be saved/published or have a webhook id.

**Path:** `/internal/workflow-tests/{workflowId}/wss?sessionToken=…&sampleRate=16000`

**Authentication:** Same pattern as the test SSE endpoint — it lives under `/internal` which is gated by the normal session cookie. The `sessionToken` query parameter is **not** an auth token; it's a one-shot UUID minted by a companion REST endpoint and used by the browser to disambiguate concurrent voice sessions on the same workflow.

**Companion endpoint:** `POST /internal/workflow-tests/{workflowId}/voice-session-token` → `{ sessionToken: "<uuid>" }`. Mints a short-lived (60 s) token bound to the calling user. Token is consumed on first WS upgrade; replays are rejected. This pattern matches the browser-voice spec's session-token API except keyed by workflow id, not webhook id.

**Wire protocol:** Same envelope as the published browser-voice trigger:

```
client → server: binary frames (raw PCM16, little-endian, sampleRate from query) — mic audio
client → server: {"type":"control","action":"mute"|"end"} text frames
server → client: binary frames (raw PCM16) — TTS audio
server → client: {"type":"transcript_interim","text":"…","turnId":"…"} text frames
server → client: {"type":"transcript_final","text":"…","turnId":"…"} text frames
server → client: {"type":"assistant_text","text":"…","turnId":"…","done":false|true} text frames
server → client: {"type":"error","message":"…"} text frames
```

The same JSON-event shape on the server-to-client side is what the published browser-voice path will use, so the frontend client component is shared between this test endpoint and the published path (Feature 4 below). Only the URL and auth differ.

**Server implementation:**

- New `WorkflowTestWebSocketHandler` in `platform-workflow-test-rest`, registered at `/internal/workflow-tests/*/wss`. Mirrors `WebhookWebSocketHandler`'s structure.
- On WS upgrade: validate `sessionToken`, resolve workflow definition from `WorkflowService` (uses the **draft** workflow JSON, not the published one — this is the test path), extract the voice sub-workflow's `websocketTasks` (browser-voice trigger or twilio-inbound trigger — both share the same `websocketTasks` extension shape), spawn the embedded sub-workflow via `JobFacade.createJob(...)`. Same `WebSocketEmitter`/`WebSocketHandler` machinery used in production paths.
- On WS close: cancel running tasks, emit a synthetic `end` event to the SSE side of the chat panel (so the chat history shows "Voice session ended") via the existing `SseStreamBridge` if one is open.

**Why not reuse `/webhooks/{id}/wss`?** Because an in-editor workflow has no published webhook id, and there's no `WorkflowTrigger` registered for it server-side. The test endpoint reads the workflow from the **draft** (`getWorkflow(id)` returns the currently saved-but-maybe-unpublished version), constructs a `WorkflowTrigger` on the fly, and runs the embedded `websocketTasks` directly.

**Token endpoint:** `POST /internal/workflow-tests/{workflowId}/voice-session-token` → `{ sessionToken: "<uuid>", expiresInSeconds: 60 }`. Tokens live in a `Cache<String, Long>` (token → userId) in the controller, evicted after 60 s or on first use.

### Feature 2 — `WorkflowTestChatPanel` voice mode toggle

The panel header gains a mic button. Clicking it:

1. Mints a voice session token (`POST /internal/workflow-tests/{workflowId}/voice-session-token`).
2. Opens a WS to `/internal/workflow-tests/{workflowId}/wss?sessionToken=…&sampleRate=16000`.
3. Requests mic permission via `navigator.mediaDevices.getUserMedia({audio: {echoCancellation: true, noiseSuppression: true}})`.
4. Sets `useWorkflowTestChatStore.voiceSessionState = "active"`.

While voice mode is active:

- The text composer is **disabled** (mic input is the input channel). A small banner reads "🎙 Listening… click mic to end."
- Mic audio is captured via `AudioWorklet` → PCM16 → WS as binary frames.
- Incoming TTS audio frames are queued onto an `AudioBufferSourceNode` chain for gapless playback.
- Incoming transcript/assistant-text JSON frames are routed to the existing chat store as if they were SSE events (`setMessage`, `appendToLastAssistantMessage`), so the same `Thread` UI renders both modalities.

Clicking the mic again sends `{"type":"control","action":"end"}` and closes the WS.

### Feature 3 — Voice session store slice

Add to `useWorkflowTestChatStore`:

```ts
voiceSessionState: 'idle' | 'requesting-permission' | 'active' | 'ending' | 'error';
voiceSessionError: string | null;
setVoiceSessionState: (s: VoiceSessionState) => void;
setVoiceSessionError: (e: string | null) => void;
```

State machine:

```
idle ──user clicks mic──> requesting-permission ──getUserMedia ok──> active
                                              │
                                              └── permission denied ──> error
active ──user clicks mic again──> ending ──WS close──> idle
active ──WS error / close──> error
error ──user dismisses──> idle
```

### Feature 4 — `useWorkflowTestVoiceSession` hook

A new hook in `client/src/shared/hooks/useWorkflowTestVoiceSession.ts` that encapsulates the WS lifecycle, mic capture, and audio playback. Shape:

```ts
function useWorkflowTestVoiceSession(workflowId: string): {
    state: VoiceSessionState;
    error: string | null;
    start: () => Promise<void>;
    stop: () => void;
    isAssistantSpeaking: boolean;
};
```

Internals:

- **Mic capture**: `AudioContext` at `sampleRate=16000`, custom `AudioWorklet` (`mic-worklet.js`) that downsamples / packs Float32 → Int16 LE, posts 20 ms frames to main thread which sends them as binary WS frames.
- **Playback**: WS binary frames decoded as PCM16 LE at 16 kHz, wrapped in `AudioBuffer`, played through a `gain` node so we can mute (during barge-in or end). Frames are concatenated into a small ring buffer; we keep ≤ 200 ms ahead of "now" to bound latency.
- **JSON event routing**: text frames `JSON.parse` and dispatched to a caller-supplied `onEvent` callback. The hook does **not** own the chat store — the panel wires `onEvent` to `setMessage` / `appendToLastAssistantMessage`.
- **Cleanup**: `stop()` posts a close-control frame, stops mic tracks, suspends the AudioContext, closes the WS.

The hook also exposes `isAssistantSpeaking` (true while playback buffer non-empty) so the panel can render a speaking indicator.

### Feature 5 — Shared `BrowserVoiceSession` core

The hook above will be **shared** between this spec, the external widget voice spec, and the AI Hub voice spec. We extract a pure (no-React) class:

```ts
// client/src/shared/lib/browser-voice/BrowserVoiceSession.ts
export class BrowserVoiceSession {
    constructor(opts: { url: string; sampleRate: number; onEvent: (e: VoiceEvent) => void });
    start(): Promise<void>;
    stop(): void;
    mute(muted: boolean): void;
    on(event: 'speaking-change' | 'error' | 'closed', cb: (...args) => void): () => void;
}
```

The React hook is a thin wrapper that exposes lifecycle as state. The same class will be wrapped by a React hook in the external widget bundle (no shared cross-package import; the class file gets **copied** into the chat-library bundle because external customers can't reach into `@/shared`). Code duplication is intentional and small (~200 lines).

## Migration / compatibility

- The chat panel's existing SSE path is **unchanged**. Voice mode is purely additive — a new button, a new state slice, a new hook.
- The new test WS endpoint sits next to the existing test SSE endpoint; no breaking change to either.
- No new dependencies in the client bundle (`AudioContext`, `AudioWorklet`, `WebSocket` are all platform).
- The worklet file (`mic-worklet.js`) ships as a static asset under `client/public/` — loaded via `audioContext.audioWorklet.addModule('/mic-worklet.js')`.

## Open questions / risks

- **Worker WS lifetime in EE.** The browser-voice spec calls out worker affinity as a Tier 2 concern. For the editor test path the same risk applies, but is bounded — the test path is single-user and short-lived, so a coordinator-pinned WS handler is acceptable. If we later move test execution to workers, the test WS handler needs the same affinity work.
- **Auth on the WS URL.** Query-string `sessionToken` is convention; if cookies-on-WS work reliably (they do in modern Chrome / Safari for same-origin requests), we can drop the token entirely. Keep the token for simplicity and so we can support cross-origin client embedding in the future.
- **`getUserMedia` permission UX.** A user who denies mic permission stays in the error state until they explicitly re-enable. Tier 1 surfaces the OS-level instructions in the error banner; we do not try to auto-recover.
- **Mic worklet on Safari.** `AudioWorklet` ships on Safari 14.1+; the floor for ByteChef client is Safari 17 per the browser-voice spec. Safe.

## Test plan

- Unit test `BrowserVoiceSession` mocked WS round-trip (text + binary).
- Unit test `useWorkflowTestVoiceSession` state machine transitions (denied permission, server-closed-on-error, manual stop).
- Integration test (server) for `WorkflowTestWebSocketHandler` token validation, replay rejection, and sub-workflow spawn.
- Manual smoke: open a voice workflow in the editor, click mic, speak, verify transcript appears + TTS plays.

## Implementation plan

1. **Server: test WS endpoint** — new `WorkflowTestWebSocketHandler` + `WorkflowTestWebSocketConfiguration` + token mint endpoint on `WorkflowTestApiController`. Routes registered at `/internal/workflow-tests/*/wss` and `POST /internal/workflow-tests/{id}/voice-session-token`.
2. **Client: shared session class** — `client/src/shared/lib/browser-voice/BrowserVoiceSession.ts` + `client/public/mic-worklet.js`.
3. **Client: hook** — `client/src/shared/hooks/useWorkflowTestVoiceSession.ts`.
4. **Client: store slice** — extend `useWorkflowTestChatStore` with voice session state.
5. **Client: panel UI** — mic button in `WorkflowTestChatPanel` header, banner while active, disabled text composer while active.
6. **Client: routing** — wire `onEvent` in the panel to `setMessage` / `appendToLastAssistantMessage` so transcripts land in the same chat thread.
