# AI Hub voice — UX, i18n, docs polish design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

The smaller v1.3+ items that didn't fit into v1.3 (UX gaps), production-readiness (engineering), or cost+compliance (GA scope). Polish work — none of these block GA, but they make voice feel finished instead of "MVP-shaped." Each item is half-a-day to a day; ship as a batch when there's calendar space.

## UX gaps

### UX1 — Mode indicator in composer

**Today.** A user looking at the mic button can't tell whether they're getting Path A (workflow webhook), Path B passthrough (provider runs LLM), or Path B native (AI Hub runs LLM). All three look identical at the UI surface.

**Change.** Small badge or tooltip on the mic button that shows the active mode:

- Path A → "Workflow voice" + small icon.
- Path B passthrough → "Deepgram voice" (or active provider name).
- Path B native LLM → "AI Hub voice (LLM)" — distinct because this is the one where workspace LLM provider applies.

Inline in the existing `voiceUnsupportedReason` tooltip area. Use `useMemo` to compute the label once from `voiceProvider + voiceMode + voiceWebhookUrl`.

**Effort.** ~0.25 day.

### UX2 — Live transcript view

**Today.** [BrowserSessionListener.onTranscript](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/voice/AiHubVoiceWebSocketHandler.java) emits `transcript_final` JSON events to the browser, but the AI Hub chat UI doesn't render them during the voice session. Transcripts only appear after the assistant turn completes, persisted to chat memory.

**Change.** While a voice session is active, show a transient "pill" above the composer with the most recent user transcript ("You said: …"). Replaces itself on each new `transcript_final` event; fades out 3 s after the assistant starts responding.

Optional polish: also show the assistant's current sentence-buffer in a separate pill below ("AI: …") so the user can read along. Useful for accessibility (deaf users can use voice mic + read responses).

**Effort.** ~0.5 day.

### UX3 — Mute/pause without ending the session

**Today.** The mic-active button toggles the whole session. Stopping = closing the WS = losing context. There's no "I want to think for 30 seconds" middle state.

**Change.** Two buttons:

- **Mute mic** (microphone-slash icon) — stop sending inbound audio to the provider, but keep the WS + provider session alive. Useful for "let me check something" pauses.
- **End session** (square icon, current behaviour) — close WS.

Implementation: client-side stop sending audio frames; server-side no change (the provider just sees silence, which Deepgram handles fine — silence detection won't trigger speech-end until 300 ms+ of audio resumes).

**Effort.** ~0.5 day.

### UX4 — Mic-permission-denied recovery

**Today.** The voice hook catches the permission-denied error and surfaces a toast: `"Voice: NotAllowedError: Permission denied"`. The user has no path to fix it — browser permission state lives in browser settings.

**Change.** Replace the generic toast with a modal that explains:

> "Microphone access was blocked. To enable it: [Chrome instructions] [Firefox instructions] [Safari instructions]"

Detect the browser via `navigator.userAgent` and show the relevant snippet. Link to the canonical browser-vendor docs as a fallback.

**Effort.** ~0.5 day.

## i18n / multi-language

### I18N1 — Configurable STT language

**Today.** Both Deepgram session classes hardcode `language=en` (in voiceAgent settings) or omit the language param (in STT WS, defaulting to en). Customers serving non-English users can't use voice.

**Change.** Two pieces:

1. **Workspace setting** `voice_language VARCHAR(8) NULL` — IETF BCP 47 code (`en`, `en-US`, `es`, `fr`, `de`, `multi`). Null = English default.
2. **Provider plumbing.** Pass language to Deepgram via the existing `language` parameter in voiceAgent settings JSON and as `&language=...` query parameter on the STT WS URL. Deepgram supports auto-detect via `language=multi` (Nova-3 model only).

### I18N2 — Multilingual voice IDs

**Today.** `voice_id` is a free-form string. Defaults to `aura-asteria-en` (English voice). No guidance for customers selecting a Spanish or French voice.

**Change.** No DB change — voice IDs are already free-form. Just:

1. **Popover hint.** When `voice_language` is set, show provider-specific voice ID suggestions in the popover for that language.
2. **Provider voice catalog.** Static JSON file `client/src/.../voice-catalog.json` mapping `(provider, language) → [voiceId, displayName]` pairs. Updated manually when provider voice catalogs change.

### I18N3 — System prompt localisation

**Today.** `buildSystemPrompt()` hardcodes the English "You are the AI Hub voice assistant…" string. Non-English users hear an English prompt-shaped reply pattern.

**Change.** Resource bundles. New `voice-prompts.properties` (per language). Voice handler reads workspace's locale and picks the right bundle. For Path B passthrough mode this matters because the prompt drives the LLM's output language; for Path B native LLM the routing agent handles prompt-language itself.

**Effort (all three items together).** ~2 days.

## Testing gaps not in production-readiness

### TEST1 — Frontend tests for voice hook

**Today.** [useAiHubVoiceSession](../../../client/src/pages/automation/ai-hub/hooks/useAiHubVoiceSession.ts) has no test coverage. Bugs in the Path A/B routing logic (which URL to derive, when to fall back) would silently break voice.

**Change.** Vitest test file. Mocks for `BrowserVoiceSession` and `fetch`. Tests:

- Path B preferred when `voiceProvider` set + `aiHubTaskId` set.
- Path A used when only `voiceWebhookUrl` set.
- Disabled when both null.
- Token endpoint URL derivation correct for both paths.
- WS URL derivation correct (ws/wss scheme, sample rate query param).
- Reconnect resets state cleanly (when Item 1 from production-readiness spec ships).

**Effort.** ~0.5 day.

### TEST2 — Frontend tests for popover

**Today.** [AiHubVoiceSettingsPopover](../../../client/src/pages/automation/ai-hub/composer/AiHubVoiceSettingsPopover.tsx) has no tests. Mode toggle logic, save/clear flow, mutex behavior all uncovered.

**Change.** Vitest tests covering:

- Initial state matches persisted settings (path choice + mode + provider fields).
- Switching from Path A → Path B preserves Path B drafts.
- "Disabled" save fires both mutations to clear A and B.
- Path B save with no connection ID shows validation error.
- Mode toggle inside Path B shows/hides the LLM model field correctly.

**Effort.** ~0.5 day.

## Docs gaps

### DOCS1 — `docs/voice/operations.md`

**Today.** Referenced from the quickstart but doesn't exist.

**Change.** Write it. Should cover:

- Observability: what each metric means, how to dashboard them, alerting thresholds.
- Rate limits: session caps, concurrent caps, how to raise them.
- Cost management: provider-side billing, ByteChef metering (when it ships), per-workspace quotas.
- Incident playbook: provider WS stalls, billing exhaustion, mass disconnects, audio playback issues.

**Effort.** ~1 day.

### DOCS2 — Path B troubleshooting section

**Today.** Quickstart's troubleshooting section is Path A only. Path B specific failures (provider rejected, mode misconfiguration, connection not found, signature errors for Twilio when that ships) aren't covered.

**Change.** Add a "Path B troubleshooting" subsection to `docs/voice/quickstart.md`:

- "Workspace has no AI Hub voice configured" — admin needs to save settings.
- "Voice provider not available" — bean for the chosen `voiceProvider` enum value isn't registered (e.g. selecting `OPENAI_REALTIME` before that provider ships).
- "Voice connection missing or has no parameters" — connection was deleted or token field missing.
- "Workspace at maximum concurrent voice sessions" — cap explanation + how to override (when per-workspace overrides ship).

**Effort.** ~0.5 day.

### DOCS3 — Cost guidance

**Today.** Quickstart mentions Deepgram bills ~$0.13/min for voiceAgent. No guidance for:

- STT_TTS_NATIVE_LLM cost breakdown (STT minutes + workspace LLM cost + TTS chars).
- Picking voice IDs by cost (some are cheaper than others).
- Setting realistic monthly budgets.

**Change.** New section "Cost planning for Path B native LLM mode" in `docs/voice/quickstart.md` with a worked example: "10 users × 20 voice turns/day × 30 s/turn × 30 days × ($X STT + $Y LLM + $Z TTS) = monthly cost."

**Effort.** ~0.5 day.

### DOCS4 — Client architecture doc

**Today.** No top-level doc explaining how `BrowserVoiceSession`, `useAiHubVoiceSession`, the AudioWorklet, and `AutomationChatModal`'s voice hook all relate to each other. Engineers extending voice have to read four files to understand the shape.

**Change.** Brief architecture doc at `docs/voice/client-architecture.md` with one ASCII diagram + 200 words of prose. Link from the quickstart.

**Effort.** ~0.5 day.

## Tracking gaps

### TRACK1 — OTLP tracing for voice turns

**Today.** Voice turns aren't tied to OpenTelemetry traces. A voice-triggered tool call shows up in the OTLP dashboard with no parent trace pointing back to the voice session.

**Change.** Wrap `AiHubVoiceWebSocketHandler.afterConnectionEstablished` and each turn's agent run in a span. Tag with `voice.session_id`, `voice.task_id`, `voice.provider`, `voice.mode`. Span events for `transcript_final`, `assistant_text_done`, `turn_completed`. Trace ID propagates into the routing agent's existing OTLP integration so tool calls correlate.

**Effort.** ~1 day.

### TRACK2 — Log correlation

**Today.** Logs for a single voice turn span four classes: WS handler, agent bridge, routing agent, Deepgram session. No shared correlation id; reconstructing a single turn from logs requires reading by timestamp.

**Change.** MDC-propagated `voiceTurnId` (UUID at turn start). Set in `AiHubVoiceAgentBridge.runTurn`, cleared on subscriber completion. Logs across the four classes share the id.

**Effort.** ~0.5 day.

## Sequencing

These are batchable polish items. Recommendation: ship UX1+UX2+UX3 together (~1.5 days, all in the composer), then DOCS1+DOCS2+DOCS3 (~2 days, all docs writers). i18n and tracing as larger separate efforts when the calendar opens.

Total scope if everything: **~9 days**.

Highest-impact subset:
- UX1 (mode indicator) + DOCS2 (Path B troubleshooting): ~0.75 day. Customers stop reporting "voice doesn't work" without saying which mode.
- I18N1 (configurable STT language): ~0.75 day. Unblocks non-English workspaces — likely the most revenue-impactful single item in this spec.
