# Voice Support Redesign: Push-to-Talk + Trigger-Gated Realtime

**Date:** 2026-05-19
**Scope:** Replace the existing voice-support shape (mic button in composers triggers full realtime everywhere) with two distinct features: push-to-talk transcribe in every composer, and full realtime voice mode only in workflow-test panel and the deployed SDK widget. Adopt `assistant-ui`'s `RealtimeVoiceAdapter` + `VoiceOrb` for the realtime UI. Remove the AI Hub realtime backend.

## Overview

Today, three composers (`AiHubChatComposer`, `WorkflowTestChatPanel`, SDK widget's `thread.tsx`) put a microphone button on the composer that opens a full bidirectional realtime voice session. Two independent realtime paths sit behind it: Path A (webhook with `websocketTasks` sub-workflow, used by deployed widget and test panel) and Path B (AI Hub native, used by AI Hub chat composer with `AiHubVoiceProvider`/`DeepgramVoiceProvider`).

The redesign:

1. **Composer mic button** is now push-to-talk only. It records a single utterance via `MediaRecorder`, uploads to a new `POST /transcribe` REST endpoint, and submits the returned text via the composer runtime as if the user had typed it (`composerRuntime.setText(text); composerRuntime.send()`). This applies to all three composers.
2. **Full realtime voice mode** is only reached when the workflow's trigger is `browser/v1/voiceSession` (`TriggerType.WEBSOCKET`), and only in the workflow-test panel and the SDK widget. AI Hub never enters realtime.
3. **Realtime UI** uses `assistant-ui`'s `RealtimeVoiceAdapter` + the `@assistant-ui/react` voice primitives (`VoiceOrb`, `VoiceConnectButton`, `VoiceDisconnectButton`, `useVoiceState`, `useVoiceControls`). When in realtime mode, `<VoiceModeLayout>` replaces `<Thread>` entirely — no message list, no composer (matches the phone-call-style mockups).
4. **AI Hub realtime stack is deleted outright.** Path B (`AiHubVoiceSessionTokenController`, `AiHubVoiceWebSocketHandler`, `AiHubVoiceProvider` SPI, `DeepgramVoiceProvider`, workspace `voiceProvider` column, AI Hub voice hook) is removed. Path A is kept and reused.
5. **STT is abstracted** behind a new `SttProvider` SPI with `OpenAiSttProvider`, `ElevenLabsSttProvider`, `DeepgramSttProvider`. Default is OpenAI `gpt-4o-mini-transcribe`. The SPI is consumed only by the three new transcribe controllers; realtime never goes through it.

## Surface matrix

| Surface | Realtime voice mode | Push-to-talk in composer |
|---|---|---|
| SDK widget (`sdks/frontend/automation/chat/library`) | Yes, when workflow trigger is `browser/v1/voiceSession` | Yes, when trigger is non-voice |
| Workflow test panel (`WorkflowTestChatPanel`) | Yes, same rule | Yes, when trigger is non-voice |
| AI Hub composer (`AiHubChatComposer`) | **Never** | Always available |

## Notes on naming

Controller names in this spec (`WebhookTriggerController`, `WebhookTriggerTestController`) are the names used in the original brief. The implementation plan resolves them to the actual existing classes (e.g. the `voice-session-token` route today lives on `BrowserVoiceSessionTokenController` in `platform-websocket-webhook-rest`); the new `/transcribe` routes attach to whichever controllers serve the existing webhook and webhook-test routes for the deployed widget and test panel respectively. The intent is that the routes are exposed at the same auth boundary as the existing `voice-session-token` routes, not that any specific class name is required.

## 1. `SttProvider` SPI

### Interface

**Location:** `server/libs/platform/platform-ai/platform-ai-stt-api/src/main/java/com/bytechef/platform/ai/stt/SttProvider.java`

STT lives under `platform-ai/` because two of its three consumers are platform-side controllers (`WebhookTriggerController` in `platform-webhook`, `WebhookTriggerTestController` in `platform-configuration`). Placing the SPI in platform keeps the dependency direction clean — automation modules (the AI Hub controller) depend down into platform, not the reverse.

This is a primitive utility (audio → text), not an AI gateway feature, so it doesn't fall under the automation-ai-gateway placement rule.

```java
public interface SttProvider {
    String getKey();

    TranscriptResult transcribe(TranscribeRequest request);

    record TranscribeRequest(
        InputStream audio,
        String mimeType,
        String locale,
        Map<String, Object> connectionParameters
    ) {}

    record TranscriptResult(
        String text,
        long durationMs,
        String detectedLocale
    ) {}
}
```

`getKey()` returns the registry key (e.g. `"OPENAI_GPT_4O_MINI_TRANSCRIBE"`). Providers are Spring beans; consumers resolve via a `Map<String, SttProvider>` bean keyed by `getKey()`.

`connectionParameters` is per-workspace configuration (API key, region) loaded from `ConnectionService` by the consumer.

### Provider implementations

| Provider | Module | Default | Notes |
|---|---|---|---|
| `OpenAiSttProvider` | `server/libs/platform/platform-ai/platform-ai-stt-openai/` | Yes | Model `gpt-4o-mini-transcribe`. Calls `POST https://api.openai.com/v1/audio/transcriptions` with `multipart/form-data`. Lives in community so CE deployments work out of the box. |
| `ElevenLabsSttProvider` | `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/` | No | Calls `POST https://api.elevenlabs.io/v1/speech-to-text` (Scribe). |
| `DeepgramSttProvider` | `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/` | No | Calls `POST https://api.deepgram.com/v1/listen` with `model=nova-3`. Separate from the deleted realtime Deepgram code — REST one-shot only. |

### Provider selection

Selection is global per deployment via a Spring property:

```
bytechef.ai.stt.provider=OPENAI_GPT_4O_MINI_TRANSCRIBE
```

`TranscribeService` resolves the active provider from the Spring `Map<String, SttProvider>` bean by the configured key. No workspace-level override.

## 2. Transcribe REST endpoints

All three endpoints accept `multipart/form-data` with one file part `audio` and one optional form field `locale`. All three return:

```json
{ "text": "string", "durationMs": 0, "locale": "en-US" }
```

All three reject audio larger than 25 MB (matches OpenAI's limit) and reject `Content-Type` other than `audio/webm`, `audio/mp4`, `audio/wav`, `audio/mpeg`, `audio/ogg`.

The endpoints **do not invoke the workflow.** They return text. The client then sends the text through the normal thread message path, the same way it would send a typed message.

| Endpoint | Controller | Auth | Notes |
|---|---|---|---|
| `POST /webhooks/{webhookId}/transcribe` | `WebhookTriggerController` (`server/libs/platform/platform-webhook/.../web/rest/`) | permit-all + CORS (matches existing `voice-session-token` route on the same controller) | Rate-limited per `webhookId` via `RateLimitInterceptor` (existing). Validates `webhookId` resolves to a published workflow before transcribing. |
| `POST /workflows/{workflowId}/test/transcribe` | `WebhookTriggerTestController` (same package) | Session cookie | No CORS open; same-origin only. |
| `POST /internal/ai-hub/transcribe` | `AiHubTranscribeController` (new, `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/transcribe/`) | Session cookie + `@PreAuthorize` | New file. Resolves workspace from session; loads STT provider connection params from `ConnectionService` if the workspace overrides the default. |

All three controllers delegate to a shared service `TranscribeService` (`server/libs/platform/platform-ai/platform-ai-stt-service/`) that:

1. Validates audio size and `Content-Type`.
2. Resolves the active `SttProvider` from the Spring bean map by the `bytechef.ai.stt.provider` key.
3. Calls `provider.transcribe(...)` and returns the result.

Concrete file paths for the three transcribe routes (the existing webhook controllers are extended; the AI Hub one is new):

| Route | Controller | File |
|---|---|---|
| `POST /webhooks/{webhookId}/transcribe` | `WebhookTriggerController` (existing) | `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/WebhookTriggerController.java` |
| `POST /workflows/{workflowId}/test/transcribe` | `WebhookTriggerTestController` (existing) | `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/src/main/java/com/bytechef/platform/configuration/web/rest/WebhookTriggerTestController.java` |
| `POST /internal/ai-hub/transcribe` | `AiHubTranscribeController` (new) | `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/transcribe/AiHubTranscribeController.java` |

## 3. Push-to-talk in composers

### `usePushToTalk` hook

**Location (platform):** `client/src/shared/lib/voice/usePushToTalk.ts`
**Location (SDK widget, mirrored):** `sdks/frontend/automation/chat/library/src/lib/usePushToTalk.ts`

```ts
type PushToTalkStatus = 'idle' | 'recording' | 'transcribing' | 'error';

interface UsePushToTalkArgs {
    transcribeUrl: string;
    locale?: string;
    onTranscript: (text: string) => void;
    onError?: (message: string) => void;
}

interface UsePushToTalkResult {
    status: PushToTalkStatus;
    error: string | null;
    start(): Promise<void>;
    stop(): Promise<void>;
}

function usePushToTalk(args: UsePushToTalkArgs): UsePushToTalkResult;
```

Implementation:

- `start()` calls `navigator.mediaDevices.getUserMedia({audio: {echoCancellation: true, noiseSuppression: true}})`, then constructs a `MediaRecorder` with `mimeType: 'audio/webm;codecs=opus'` (Chromium/Firefox). For Safari (no Opus in WebM), fall back to `'audio/mp4'`. Detect via `MediaRecorder.isTypeSupported`.
- Audio chunks accumulate in a ref. On `stop()`, the hook builds a `Blob` from the chunks, stops all media tracks, transitions to `'transcribing'`, and `POST`s `FormData` containing `audio` (the blob) and optional `locale` to `transcribeUrl`.
- On success, calls `onTranscript(text)`. On failure, sets `error` and calls `onError(message)`.
- Cleanup: aborts in-flight fetch on unmount; releases media tracks on `stop()` or unmount.

### Composer integration

All three composers use the same pattern:

```tsx
const composerRuntime = useComposerRuntime();
const pushToTalk = usePushToTalk({
    transcribeUrl: TRANSCRIBE_URL,
    onTranscript: (text) => {
        composerRuntime.setText(text);
        composerRuntime.send();
    },
});

<MicButton
    status={pushToTalk.status}
    onClick={() => pushToTalk.status === 'recording' ? pushToTalk.stop() : pushToTalk.start()}
    disabled={!checkBrowserMicSupport()}
/>
```

`composerRuntime.setText(text); composerRuntime.send();` is the assistant-ui API for sending a text message — same path as if the user typed and pressed enter. The workflow sees a normal user text message.

Per-composer `TRANSCRIBE_URL`:

| Composer | URL |
|---|---|
| `AiHubChatComposer` | `/api/automation/internal/ai-hub/transcribe` |
| `WorkflowTestChatPanel` composer | `/api/automation/workflows/${workflowId}/test/transcribe` |
| SDK widget composer | derived from the widget's configured `webhookUrl` as `${webhookUrl}/transcribe` |

### MicButton visual

While `status === 'recording'`, the mic icon is replaced with a pulsing red square (recording indicator). While `status === 'transcribing'`, a small spinner. While `status === 'error'`, the mic icon with a red tooltip surfacing `error`.

`checkBrowserMicSupport()`: returns false if no `navigator.mediaDevices` or no `MediaRecorder` global. Mic button hidden in that case.

## 4. Realtime voice mode

### `ByteChefRealtimeVoiceAdapter`

**Location (platform):** `client/src/shared/lib/voice/ByteChefRealtimeVoiceAdapter.ts`
**Location (SDK widget, mirrored):** `sdks/frontend/automation/chat/library/src/lib/ByteChefRealtimeVoiceAdapter.ts`

Implements `RealtimeVoiceAdapter` from `@assistant-ui/react`. Wraps `BrowserVoiceSession` and forwards its events to `createVoiceSession` helpers.

```ts
class ByteChefRealtimeVoiceAdapter implements RealtimeVoiceAdapter {
    constructor(private readonly config: {
        tokenUrl: string;
        sampleRate?: 16000 | 24000;
    }) {}

    connect(options: { abortSignal?: AbortSignal }): RealtimeVoiceAdapter.Session {
        return createVoiceSession(options, async (helpers) => {
            const { token, wsPath } = await fetchToken(this.config.tokenUrl);
            const wsUrl = buildWsUrl(wsPath, token);

            const browserSession = new BrowserVoiceSession({
                url: wsUrl,
                sampleRate: this.config.sampleRate ?? 16000,
                onStatusChange: (s) => {
                    if (s === 'active') helpers.setStatus({ type: 'running' });
                    if (s === 'closed') helpers.end('finished');
                    if (s === 'error') helpers.end('error');
                },
                onEvent: (e) => {
                    if (e.type === 'transcript_final') {
                        helpers.emitTranscript({ role: 'user', text: e.text!, isFinal: true });
                    }
                    if (e.type === 'transcript_partial') {
                        helpers.emitTranscript({ role: 'user', text: e.text!, isFinal: false });
                    }
                    if (e.type === 'assistant_text') {
                        helpers.emitTranscript({ role: 'assistant', text: e.text!, isFinal: !!e.done });
                    }
                },
                onSpeakingChange: (isAssistantSpeaking) => {
                    helpers.emitMode(isAssistantSpeaking ? 'speaking' : 'listening');
                },
                onVolume: (v) => helpers.emitVolume(v),
            });

            await browserSession.start();

            return {
                disconnect: () => browserSession.stop(),
                mute: () => browserSession.setMuted(true),
                unmute: () => browserSession.setMuted(false),
            };
        });
    }
}

export function createWebhookVoiceAdapter(webhookUrl: string): RealtimeVoiceAdapter {
    return new ByteChefRealtimeVoiceAdapter({
        tokenUrl: `${webhookUrl}/voice-session-token`,
    });
}
```

There is no `createAiHubVoiceAdapter` — AI Hub does not use realtime.

### `BrowserVoiceSession` additions

The existing class needs three new event hooks and one method, all additive:

- `onStatusChange?: (status: 'connecting' | 'active' | 'closed' | 'error') => void`
- `onSpeakingChange?: (isAssistantSpeaking: boolean) => void` — derived from incoming audio frame activity vs outgoing mic activity, with a short hysteresis to avoid flicker
- `onVolume?: (level: number) => void` — RMS of recent samples, 0..1, emitted at ~20 Hz
- `setMuted(muted: boolean): void` — stops feeding mic frames to the WebSocket while muted; keeps the session open

The WebSocket protocol does not change. Both copies of the class (`client/src/shared/lib/browser-voice/` and `sdks/frontend/automation/chat/library/src/lib/`) get the same additions.

### `<VoiceModeLayout>` component

**Location (platform):** `client/src/shared/lib/voice/VoiceModeLayout.tsx`
**Location (SDK widget, mirrored):** `sdks/frontend/automation/chat/library/src/lib/VoiceModeLayout.tsx`

```tsx
interface VoiceModeLayoutProps {
    sessionLimitSeconds: number;
    idleLabel?: string;     // default: "Tap to start"
    activeLabel?: string;   // default: "Listening"
    speakingLabel?: string; // default: "Speaking"
}

function VoiceModeLayout(props: VoiceModeLayoutProps);
```

Layout (matches mockups):

- Full-height dark container.
- Top header: a session-limit chip (e.g. `2:30 LIMIT`) on the right; when active, a countdown chip on the left showing remaining time.
- Center: `<VoiceOrb variant="emerald" className="size-48" />` from `@assistant-ui/react`. Its state is driven automatically by `useVoiceState()` — idle/connecting/listening/speaking/muted.
- Below the orb: status text (`idleLabel` / `activeLabel` / `speakingLabel`) and a sub-label (e.g. "Tap to talk to the assistant" / "Listening..."). Picks state from `useVoiceState()`.
- Footer: `<VoiceConnectButton>` when idle, `<VoiceDisconnectButton>` (styled red, full-width) when active.

The component uses only the public hooks/components from `@assistant-ui/react`. It assumes the parent has installed a `RealtimeVoiceAdapter` on the runtime via `useChatRuntime({ adapters: { voice: ... } })`.

### Trigger-based rendering in test panel + SDK widget

Both `WorkflowTestChatPanel.tsx` and the SDK widget's panel apply this rule:

```tsx
const isVoiceOnlyWorkflow = workflow.triggers.some(t =>
    t.componentName === 'browser' && t.triggerName === 'voiceSession'
);

const runtime = useChatRuntime({
    api: '...',
    adapters: isVoiceOnlyWorkflow
        ? { voice: createWebhookVoiceAdapter(webhookUrl) }
        : undefined,
});

return (
    <AssistantRuntimeProvider runtime={runtime}>
        {isVoiceOnlyWorkflow
            ? <VoiceModeLayout sessionLimitSeconds={150} />
            : <Thread />}
    </AssistantRuntimeProvider>
);
```

The mic button (push-to-talk) is rendered inside the `<Thread />` composer slot, so it only appears in the non-voice branch. The voice branch has no composer at all.

The session limit (150s in the example) is read from the trigger parameters once a `sessionLimitSeconds` property is added to `BrowserVoiceSessionTrigger`. Until that property exists, the layout takes a fixed default. Adding the property to `BrowserVoiceSessionTrigger` is in scope for this work.

## 5. AI Hub realtime stack deletion

The following files are deleted:

**Server:**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/.../web/voice/AiHubVoiceSessionTokenController.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/.../web/voice/AiHubVoiceWebSocketHandler.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-api/.../voice/AiHubVoiceProvider.java` (interface)
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-api/.../voice/AiHubVoiceProviderSession.java` (interface)
- The whole `automation-ai-hub-voice-deepgram` module (or whichever module hosts `DeepgramVoiceProvider`)
- WebSocket handler registration in the relevant `WebSocketConfigurer`

**Client:**
- `client/src/pages/automation/ai-hub/hooks/useAiHubVoiceSession.ts`
- The voice block in `AiHubChatComposer.tsx` (imports of `useAiHubVoiceSession`, the `voice.start/stop` calls, `pendingVoiceUserMessage` drain effect, voice error toast, voice-status banner, mic-button voice branch)

**Schema:**
- New Liquibase migration drops the `voice_provider` column from the workspace settings table (`ai_hub_workspace_settings` or equivalent). Migration is forward-only; no rollback.
- The `AiHubWorkspaceSettings` JDBC entity field for `voiceProvider` and any related GraphQL/REST surface are removed.

**Routing:**
- `AiHubRoutingAgent` is inspected for voice-routing branches; any code path that depended on `voiceProvider` workspace state or live audio frames is removed. Push-to-talk transcripts go through the same text path as typed messages, so the routing agent does not need to know about them.

## 6. Trigger metadata

`BrowserVoiceSessionTrigger` (existing, `server/libs/modules/components/browser/.../trigger/BrowserVoiceSessionTrigger.java`) gains one new property:

```java
.properties(
    // ... existing: subWorkflow, sampleRate, echoCancellation, noiseSuppression
    integer("sessionLimitSeconds")
        .label("Session limit (seconds)")
        .description("Maximum duration of a voice session. 0 means no limit.")
        .defaultValue(150)
)
```

The trigger output and `WebhookWebSocketHandler` session-timeout logic already enforce a server-side cap; this property surfaces the cap to the client so `<VoiceModeLayout>` can show a countdown matching server behavior.

## Out of scope

The following are explicitly out of scope for this design and tracked separately:

- **Streaming push-to-talk** (partial transcripts during recording). Current design uploads the full utterance after `stop()`. Acceptable for utterances <30s; if the product demands longer dictation, revisit.
- **Workspace-level STT provider override.** Not planned. Provider selection is global per deployment via `bytechef.ai.stt.provider`. Adding per-workspace override would be an additive change in the future if needed.
- **Push-to-talk in the AI Hub for WORKFLOW_CHAT tasks via the webhook bridge instead of the new AI Hub endpoint.** The design routes WORKFLOW_CHAT push-to-talk through `/internal/ai-hub/transcribe`. If we later want WORKFLOW_CHAT to go through the workflow's webhook (so the workflow author's audit/logging applies), that's a follow-up.
- **Voice-aware agent / barge-in / turn detection in the realtime path.** Same status as before this design: the realtime path passes audio through to whatever the workflow's `websocketTasks` sub-workflow does. Anything smarter is the workflow author's job today.
- **Removing or consolidating the duplicated `BrowserVoiceSession` between platform and SDK widget.** They stay duplicated; the additive changes in section 4 are applied to both copies.
- **`OpenAiSttProvider` for realtime.** Only the REST one-shot Whisper-class API. Realtime OpenAI is not added; it would be a future `AiHubVoiceProvider`-style realtime SPI if AI Hub realtime returns.

## Migration / cleanup checklist

In implementation order:

1. Add `SttProvider` SPI module (`platform-ai-stt-api`) and the three provider impls (`platform-ai-stt-openai` in community; `platform-ai-stt-elevenlabs` and `platform-ai-stt-deepgram` in EE).
2. Add `TranscribeService` in `platform-ai-stt-service`. Extend `WebhookTriggerController` and `WebhookTriggerTestController` with `/transcribe` routes; add new `AiHubTranscribeController`.
3. Add `usePushToTalk` hook + `MicButton` component (platform copy; mirror into SDK widget).
4. Wire `usePushToTalk` into `AiHubChatComposer`. Verify that with the realtime block still present, both paths coexist briefly during migration.
5. Wire `usePushToTalk` into `WorkflowTestChatPanel`'s composer and the SDK widget's composer.
6. Add `BrowserVoiceSession` additive changes (`onStatusChange`, `onSpeakingChange`, `onVolume`, `setMuted`). Mirror to SDK widget.
7. Add `ByteChefRealtimeVoiceAdapter` + `createWebhookVoiceAdapter`. Mirror to SDK widget.
8. Add `<VoiceModeLayout>`. Mirror to SDK widget.
9. Wire trigger-based rendering in `WorkflowTestChatPanel` and the SDK widget's panel.
10. Add `sessionLimitSeconds` property to `BrowserVoiceSessionTrigger`.
11. Delete `useAiHubVoiceSession.ts` and the realtime block in `AiHubChatComposer.tsx`.
12. Delete `AiHubVoiceSessionTokenController`, `AiHubVoiceWebSocketHandler`, `AiHubVoiceProvider`, `DeepgramVoiceProvider`, the voice-deepgram module, WS handler registration.
13. Liquibase migration to drop the `voice_provider` column. Remove the JDBC entity field and any REST/GraphQL surface.
14. Update `AiHubRoutingAgent` to remove any voice-aware routing.
15. Tests: unit tests for `TranscribeService` and each `SttProvider` impl (mock HTTP). Integration test for at least one transcribe controller hitting a stubbed provider. Vitest tests for `usePushToTalk` (mock `MediaRecorder` and `fetch`).

## File list (new)

**Server:**
- `server/libs/platform/platform-ai/platform-ai-stt-api/src/main/java/com/bytechef/platform/ai/stt/SttProvider.java`
- `server/libs/platform/platform-ai/platform-ai-stt-service/src/main/java/com/bytechef/platform/ai/stt/service/TranscribeService.java`
- `server/libs/platform/platform-ai/platform-ai-stt-openai/src/main/java/com/bytechef/platform/ai/stt/openai/OpenAiSttProvider.java`
- `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/src/main/java/com/bytechef/ee/platform/ai/stt/elevenlabs/ElevenLabsSttProvider.java`
- `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/src/main/java/com/bytechef/ee/platform/ai/stt/deepgram/DeepgramSttProvider.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/transcribe/AiHubTranscribeController.java` (new)
- Transcribe route added to existing `WebhookTriggerController` (`server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/.../WebhookTriggerController.java`)
- Transcribe route added to existing `WebhookTriggerTestController` (`server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/.../WebhookTriggerTestController.java`)
- `build.gradle.kts` dependency additions on the two platform controller modules: `platform-ai-stt-api`, `platform-ai-stt-service`
- Liquibase changelog: drop the `voice_provider` column from the AI Hub workspace settings table

**Client (platform):**
- `client/src/shared/lib/voice/usePushToTalk.ts`
- `client/src/shared/lib/voice/MicButton.tsx`
- `client/src/shared/lib/voice/ByteChefRealtimeVoiceAdapter.ts`
- `client/src/shared/lib/voice/VoiceModeLayout.tsx`

**Client (SDK widget — mirrored copies):**
- `sdks/frontend/automation/chat/library/src/lib/usePushToTalk.ts`
- `sdks/frontend/automation/chat/library/src/lib/MicButton.tsx`
- `sdks/frontend/automation/chat/library/src/lib/ByteChefRealtimeVoiceAdapter.ts`
- `sdks/frontend/automation/chat/library/src/lib/VoiceModeLayout.tsx`
