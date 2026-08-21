# AI Hub — voice support design

**Status:** Draft | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Wire the AI Hub composer's already-present "Voice input (coming soon)" mic button — the disabled stub at [AiHubChatComposer.tsx:303](../../client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx:303) — into a working voice mode that streams the user's voice into AI Hub and plays back the assistant's spoken response.

AI Hub today is text-only via AG-UI (`@ag-ui/client` → SSE). This spec adds a **parallel** voice channel: when the user clicks the mic, AI Hub opens a WebSocket to a workspace-configured browser-voice webhook, captures mic audio, plays back TTS audio, and splices the resulting transcripts into the same AG-UI message stream so the UI rendering stays unchanged.

## Non-goals

- **Agent-level voice tools.** The AI Hub agent today is a thinking-and-tool-calling LLM; making it natively voice-aware (e.g., specifying voice-output preferences in tool results) is Tier 2.
- **Per-task voice configuration.** Tier 1 uses one workspace-level `voiceWebhookUrl`. Per-task voice (e.g., this task is voice-only, that one is text-only) is Tier 2.
- **Voice in workflow-chat tasks.** Workflow-chat tasks (`kind: WORKFLOW_CHAT`) route through `WebhookBridgeAgent`, not the LLM agent. They get their own voice path later — out of scope here. The mic button is disabled for workflow-chat task rows.
- **Streaming user-turn → agent without going through transcription.** We use the existing browser-voice runtime which gives us transcripts; AI Hub treats those transcripts as a user message and runs the standard agent loop. End-to-end speech-to-speech (no text intermediate) is Tier 2.
- **Multi-modal LLM input.** No image-from-voice, no document-from-voice. Voice is text-mode-equivalent.
- **Voice cancellation of running agent.** Pressing the mic while the agent is producing a long response stops the playback but does not cancel the agent — the user has to click the existing stop button. Tier 2 candidate: link mic toggle to `threadRuntime.cancelRun()`.

## Background

- The AI Hub composer ([AiHubChatComposer.tsx](../../client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx)) already has a stub mic button styled correctly; we just need to enable it.
- AI Hub runtime ([AiHubRuntimeProvider.tsx](../../client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx)) bridges AG-UI to `useExternalStoreRuntime`. The message store is shared via `useAiHubStore`. Adding a voice-sourced user message uses the same store APIs (`setMessage`, `appendToLastAssistantMessage`).
- The browser-voice spec ([2026-05-12-browser-voice-runtime-tier1-design.md](2026-05-12-browser-voice-runtime-tier1-design.md)) provides the transport. AI Hub reuses the `BrowserVoiceSession` class from `client/src/shared/lib/browser-voice/`.
- Workspace settings already exist — adding a `voiceWebhookUrl` field is a small migration on `workspace_settings` (or whatever table holds workspace-scoped config; we follow the existing convention).
- The AI Hub agent ID is `AI_HUB` (hardcoded in `AiHubRuntimeProvider`). The voice path opens a WS to the workspace's configured `voiceWebhookUrl` regardless of agent — the agent is the LLM at the other end of that WS.

## Design

### Feature 1 — Workspace voice configuration

Add a workspace-level setting `aiHubVoiceWebhookUrl: string | null` (server-side: a column on whichever existing workspace_settings table holds workspace-scoped AI Hub config). Surfaced in the AI Hub workspace settings page as "Voice agent webhook URL" with helper text:

> Optional. Base URL of a ByteChef browser-voice webhook to use when the user enables voice input
> in AI Hub. Leave empty to disable voice support.

GraphQL: extend the `WorkspaceSettings` type with `aiHubVoiceWebhookUrl`. Mutation: `updateWorkspaceAiHubVoiceWebhookUrl(workspaceId, url)` — admin-only via `@PreAuthorize`.

### Feature 2 — `useAiHubVoiceSession` hook

A new hook in `client/src/pages/automation/ai-hub/hooks/useAiHubVoiceSession.ts`:

```ts
function useAiHubVoiceSession(opts: {
    voiceWebhookUrl: string | null;
    onTranscript: (text: string) => void;
    onAssistantText: (delta: string) => void;
}): {
    status: VoiceSessionStatusType;
    error: string | null;
    isAssistantSpeaking: boolean;
    start: () => Promise<void>;
    stop: () => void;
};
```

Internals wrap `BrowserVoiceSession` (from `@/shared/lib/browser-voice/BrowserVoiceSession`). Token mint follows the browser-voice spec — POST to `${voiceWebhookUrl}/voice-session-token`. WS upgrade derives the URL from the same base.

`onTranscript` fires on `transcript_final` events; the caller pushes the text into AI Hub as a user message and triggers the standard agent loop (so the AG-UI agent ALSO sees it and runs the standard LLM/tool path). Tier 1 trade-off: the AG-UI agent and the voice agent at the WS endpoint may double-respond. We mitigate by:
- **Path A (recommended Tier 1):** voiceWebhookUrl points to a workflow that does **only STT** (no LLM); AG-UI takes the transcript and runs the standard AI Hub agent. The TTS is generated as a separate response stage by an HTTP-call-to-TTS step at the end of the workflow.
- **Path B (Tier 2):** voiceWebhookUrl points to a full STT→LLM→TTS workflow; AG-UI is **disabled** while voice is active. Needs runtime-mode toggle in `AiHubRuntimeProvider`.

Tier 1 ships Path A. The workspace setting documents this restriction.

### Feature 3 — Wiring the existing mic button

Replace the disabled stub in `AiHubChatComposer.tsx`:

```tsx
<Tooltip>
    <TooltipTrigger asChild>
        <Button
            aria-label="Voice input"
            className="size-8 rounded-full text-muted-foreground"
            disabled={!voiceEnabled || isWorkflowChat}
            icon={voiceActive ? <MicOffIcon /> : <MicIcon />}
            onClick={voiceActive ? voice.stop : () => void voice.start()}
            ...
        />
    </TooltipTrigger>
    <TooltipContent>
        {voiceEnabled ? (voiceActive ? 'End voice session' : 'Start voice session')
                      : 'Voice input not configured for this workspace'}
    </TooltipContent>
</Tooltip>
```

The button reads `voiceEnabled` from the workspace settings store and `voiceActive` from the session hook's status. When `isWorkflowChat` (current task is a workflow-chat) it stays disabled — voice in workflow-chat is Tier 2.

### Feature 4 — Pushing voice transcripts into AI Hub

In `AiHubRuntimeProvider`, expose an imperative `pushUserMessage(text: string)` action via the context (or via a ref-based bridge) that the composer can call when a `transcript_final` arrives. The action runs the normal `onNew` path so the message lands in the AG-UI stream and the agent runs.

The minimum-invasive route: lift `onNew` reference into a ref the composer can call. We add `useAiHubStore.pendingVoiceUserMessage: string | null` which the composer sets; the runtime provider watches it via `useEffect` and dispatches `onNew(...)` when non-null, then clears it.

### Feature 5 — Voice mode store slice

Add to `useAiHubStore`:

```ts
voiceMode: 'idle' | 'connecting' | 'active' | 'ending' | 'error';
voiceError: string | null;
voiceWebhookUrl: string | null;            // populated from workspace settings on mount
setVoiceMode: (m: VoiceModeT) => void;
setVoiceError: (e: string | null) => void;
setVoiceWebhookUrl: (url: string | null) => void;
```

### Feature 6 — Resource panel voice status indicator

While voice is active, the right side of the AI Hub composer shows a small "🎙 Listening…" / "🔊 Speaking…" pill (re-use the patterns from the editor panel and the external widget). When voice is active and the user types a text message, we close the voice WS (text wins).

## Migration / compatibility

- **No breaking changes.** The mic button is already in the UI but disabled; we change its disabled rules but its position and styling stay.
- **Workspace settings migration.** Adds one nullable column. Existing workspaces get `null`; the mic button stays disabled.
- **No new runtime deps.** `BrowserVoiceSession` is already added in the WorkflowTestChatPanel spec.

## Open questions / risks

- **Path A workflow shape.** Path A requires the voice workflow to be **STT-only** server-side. We need a documented example workflow ("STT-then-emit-transcript-and-end") and probably a starter template in the AI Hub workspace setup. Tier 1 documents the contract; the template is a follow-up.
- **Cancellation semantics.** If voice is active and the user clicks the existing stop button, we stop **both** the agent and the voice WS. The composer's `handleCancelTurn` gets one extra step (`voice.stop()`).
- **Re-entrancy.** If the user clicks mic, then quickly clicks send (text), we end up in a weird state. Resolution: text-send while voice is connecting cancels voice connection.
- **Permission UX in the AI Hub layout.** AI Hub takes the full viewport; the browser's mic-permission prompt anchors to the URL bar and is unambiguous. No special UX needed.

## Test plan

- Unit test `useAiHubVoiceSession` mocked token-mint + WS round-trip.
- Integration test for the workspace-settings mutation (admin-only).
- Manual smoke: configure workspace `aiHubVoiceWebhookUrl`, open AI Hub, click mic, speak, verify transcript appears as user message and agent responds.
- Regression: text-only AI Hub with `voiceWebhookUrl = null` should look identical to today.

## Implementation plan

1. **Server: workspace settings** — add `ai_hub_voice_webhook_url` column to whichever existing workspace_settings table; add the GraphQL field and `updateWorkspaceAiHubVoiceWebhookUrl` mutation (admin-only).
2. **Client: settings UI** — add a "Voice agent webhook URL" text input to the AI Hub workspace settings page.
3. **Client: store slice** — extend `useAiHubStore` with voice slice + `voiceWebhookUrl`.
4. **Client: hook** — `useAiHubVoiceSession.ts` wrapping `BrowserVoiceSession`.
5. **Client: composer wiring** — enable the mic button in `AiHubChatComposer.tsx`, gate on `voiceWebhookUrl != null && !isWorkflowChat`.
6. **Client: runtime provider** — react to `pendingVoiceUserMessage` by dispatching `onNew`.
7. **Client: status pill** — small floating indicator next to the composer while voice is active.

This spec defers Path B (full voice agent at the WS endpoint with AG-UI disabled during voice) — Tier 2 will revisit once Path A's UX is validated.
