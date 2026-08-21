# Automation chat widget — voice support design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Let customers' end-users **talk** to a workflow embedded in a ByteChef-built chat widget on a customer site. The widget today is text-only (SSE chat over an HTTP webhook). With browser voice support landing platform-wide ([2026-05-12-browser-voice-runtime-tier1-design.md](2026-05-12-browser-voice-runtime-tier1-design.md)), the widget gets a mic toggle that opens a WebSocket to the same workflow's `/webhooks/{id}/wss` voice endpoint and runs a full bidirectional audio session.

This is the **customer-facing** voice path. It mirrors the editor's voice spec ([2026-05-12-workflow-test-chat-voice-design.md](2026-05-12-workflow-test-chat-voice-design.md)) but ships in a published widget bundle and connects to a **published** webhook URL, not a test endpoint.

## Non-goals

- **Server-side wire-protocol changes.** The browser-voice spec already defines the wire protocol and session-token API.
- **Worklet served from customer's CDN.** The widget bundle ships the worklet **inline** as a Blob URL constructed at runtime, so customers do not need to host an extra static asset.
- **Custom mic/speaker UI overrides.** The widget ships a default mic button styled the same way as its existing trigger button. Theming is via the customer's existing CSS-vars / class overrides.
- **Resume + voice cross-product.** If a voice workflow suspends with `ask_user_question`, the user must answer **by voice** (which works through the streaming agent) or close voice mode and answer via text. The widget does not synthesize a special UI for "answer this question by voice or text" — Tier 2.
- **Mobile / iOS Safari polish.** The browser-voice spec sets Safari 17+ as the floor; voice may have audio-routing quirks in iOS PWAs that we'll surface as known issues, not fix here.
- **Recording / saving the voice conversation.** Server-side concern; same as Twilio-path non-goal.

## Background

- The widget [`AutomationChatModal`](../../sdks/frontend/automation/chat/library/src/components/AutomationChatModal.tsx) accepts a `webhookUrl`. SSE mode is auto-detected by trailing `/sse`. We extend this with a new property and a new code path.
- The platform's [browser-voice spec](2026-05-12-browser-voice-runtime-tier1-design.md) defines:
  - Trigger type `browser/v1/voiceSession` with a webhook id.
  - Session-token endpoint: `POST /webhooks/{id}/voice-session-token` → `{ token, expiresInSeconds }`.
  - WS upgrade: `wss://.../webhooks/{id}/wss?sessionToken=...&sampleRate=...`.
  - JSON event envelope: `transcript_interim`, `transcript_final`, `assistant_text`, `error`, plus binary PCM16 audio frames in both directions.
- The editor spec's `BrowserVoiceSession` class is **directly reusable** in shape. The widget gets a sibling copy of the class (no shared import from `@/shared`).
- The widget already has a `useSSE` hook; the new voice mode runs in **parallel** with SSE, not as a replacement. Switching modes ends the current one before starting the next.

## Design

### Feature 1 — `voiceWebhookUrl` config field

Extend `AutomationChatConfig`:

```ts
export interface AutomationChatConfig {
    webhookUrl: string;                  // existing — text webhook
    voiceWebhookUrl?: string;            // new — base URL like https://.../webhooks/<id>
    // ...existing title/description/suggestions
}
```

When `voiceWebhookUrl` is set, the widget renders a mic button. The voice token-mint endpoint and the WS upgrade path are **derived**:

```
tokenEndpoint = `${voiceWebhookUrl}/voice-session-token`
wsUrl         = `${wsScheme}//${host}/webhooks/${webhookId}/wss?sessionToken=...&sampleRate=16000`
```

We derive `webhookId` from the last segment of `voiceWebhookUrl`. If the customer passes a malformed URL, the widget logs a warning and disables the mic button.

### Feature 2 — Sibling `BrowserVoiceSession` in the widget bundle

The class from `client/src/shared/lib/browser-voice/BrowserVoiceSession.ts` is copy-pasted into `sdks/frontend/automation/chat/library/src/lib/BrowserVoiceSession.ts`. Two reasons for duplication:

1. The widget package is published as `@bytechef/automation-chat`; it cannot import from the platform client.
2. Customers should not need to install or configure anything beyond `npm install`. The widget bundle ships the worklet **inline**.

The class is functionally identical except for the worklet path: instead of fetching `/mic-worklet.js`, it constructs a Blob URL at runtime:

```ts
const workletSource = `class Pcm16DownsamplerProcessor extends AudioWorkletProcessor { … }
registerProcessor('pcm16-downsampler', Pcm16DownsamplerProcessor);`;
const workletBlob = new Blob([workletSource], {type: 'application/javascript'});
const workletUrl = URL.createObjectURL(workletBlob);
```

The Blob URL is created once per session and revoked on cleanup. The worklet source is the **same string** as `client/public/mic-worklet.js`; we keep the two in lockstep (a small unit test asserts byte-equivalence).

### Feature 3 — `useAutomationChatVoiceSession` hook

A thin React wrapper around the bundled `BrowserVoiceSession`:

```ts
function useAutomationChatVoiceSession(opts: {
    voiceWebhookUrl: string;
    onEvent?: (event: VoiceEventI) => void;
}): {
    status: VoiceSessionStatusType;
    error: string | null;
    isAssistantSpeaking: boolean;
    start: () => Promise<void>;
    stop: () => void;
};
```

Internally calls the customer's `/voice-session-token` endpoint, then opens the WS via `BrowserVoiceSession`. The token endpoint URL is **derived** from `voiceWebhookUrl` by appending `/voice-session-token`.

### Feature 4 — Mic button + chat-thread splicing

Above the message composer, the modal gets a mic button. When voice mode is active:

- Modal header shows "🎙 Listening…" or "🔊 Assistant speaking…".
- Text composer is **hidden** (mic is the input channel).
- `transcript_final` events from the server are spliced into the chat thread as `{role: 'user', content: text}` — so the user sees what they said.
- `assistant_text` events with `done: false` are routed to `appendToLastAssistantMessage`; the panel renders streamed assistant text in real time.

Clicking the mic again closes the WS and restores the text composer.

### Feature 5 — Mode mutual exclusion

The widget supports SSE chat **and** voice but not both at once. Opening voice while an SSE stream is running cancels the SSE stream (`setStreamRequest(null)`); opening voice closes any existing WS. The store gains a `mode: 'text' | 'voice'` slice that the rendering branches on.

### Feature 6 — Resume + voice edge cases

If voice mode is active and the server emits an `ask_user_question` over the WS JSON channel (the platform doesn't do this today, but it's possible later), we surface the question as a text message and **leave voice mode active**. The user can answer by voice (which the running streaming agent will pick up naturally) or click mic to end voice and then type — at which point the existing resume path handles it. This is documented as "voice + resume works but is best-effort"; the rich UI is Tier 2.

## Migration / compatibility

- **No new props on existing components.** The new `voiceWebhookUrl` is optional in `AutomationChatConfig`. Customers who don't set it see no mic button.
- **Bundle size.** Adds ~5 KB minified (the `BrowserVoiceSession` class + the worklet source string).
- **No new runtime dependencies.** All browser-native APIs.
- **TypeScript exports.** `BrowserVoiceSession`, `VoiceSessionStatusType`, `VoiceEventI` are re-exported from the package index so customers building custom UIs can use the class directly.

## Open questions / risks

- **Worklet inline vs separate asset.** Inline keeps the package zero-config but bloats the JS bundle by ~1.5 KB. Acceptable.
- **CORS on the token endpoint.** Customer sites embedding the widget on a different origin need CORS on `/webhooks/{id}/voice-session-token`. The browser-voice spec already requires this for the WS upgrade.
- **`getUserMedia` permission UX in iframes.** If the customer embeds the widget inside an iframe, `allow="microphone"` is required on the iframe element. We document this.
- **Bundle duplication with editor.** The `BrowserVoiceSession` class lives in two places. Tier 2 candidate: publish the class as a shared npm package, both consumed by editor and widget. Tier 1 keeps the source duplication.

## Test plan

- Unit test inline `BrowserVoiceSession` with mock WebSocket.
- Unit test `useAutomationChatVoiceSession` token mint + WS-URL construction.
- Manual smoke: build a voice workflow, embed the widget on a static HTML page with `voiceWebhookUrl` set, click mic, verify mic→transcript→assistant→TTS round-trip.
- Lockstep test: assert the inline worklet source string equals the platform's `client/public/mic-worklet.js` byte-for-byte.

## Implementation plan

1. **Bundle copy** — `sdks/frontend/automation/chat/library/src/lib/BrowserVoiceSession.ts` (copy of the editor version, with inline worklet).
2. **Hook** — `sdks/frontend/automation/chat/library/src/hooks/useAutomationChatVoiceSession.ts`.
3. **Config type** — extend `AutomationChatConfig` with `voiceWebhookUrl?: string`.
4. **Store slice** — add `mode: 'text' | 'voice'` to `useChatStore`.
5. **Provider wiring** — `AutomationChatProvider` consumes the hook when `voiceWebhookUrl` is present.
6. **UI** — mic button + status banner inside the `Thread` shell, hidden composer while voice active.
7. **Index re-exports** — expose `BrowserVoiceSession`, `VoiceEventI`, `VoiceSessionStatusType`.
