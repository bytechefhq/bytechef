# AI Hub voice — lifecycle, UX gaps, architectural decisions design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

After the four post-v1.2 specs (v1.3 polish, production-readiness, cost+compliance, widget, UX+i18n+docs), an audit surfaced more items in three categories: **lifecycle bugs/gaps** likely lurking today, **UX problems** specific to voice that other specs didn't catch, and **architectural decisions** worth making before they get permanently baked in. This spec captures them. None of these items individually are large; collectively they're the difference between "voice mostly works" and "voice is solid."

Items are independent. Ship in priority order: bugs first, UX next, decisions when calendar opens.

## Bug-shaped lifecycle gaps

### B1 — Path A voice meets workflow suspend/resume

**Today.** Path A routes voice through a workflow webhook. Workflows can suspend mid-execution (waiting for an external webhook, sleep dispatcher, manual approval). If a voice-routed workflow suspends, the WS stays open but no audio flows back. The user hears silence indefinitely until the workflow resumes — they don't know whether the assistant is thinking, the system is broken, or they should hang up.

**Change.** Three pieces:

1. **Detect suspend.** The workflow execution emits a status event when it suspends. The voice WS bridge should listen for it (Atlas execution events flow through `WebSocketEmitter` already; verify suspend transitions surface there too).
2. **Audible cue.** On suspend detection, speak a brief "I'm checking on that, this may take a moment" via the configured TTS (or via a workflow-author-configurable filler string on the trigger config).
3. **Timeout escape.** If the suspend lasts more than N seconds (workspace-configurable, default 60 s), close the WS with a "this is taking too long — try again or check your dashboard" message rather than holding the user indefinitely.

**Effort.** ~1.5 days (mostly testing — workflow suspend/resume paths have many shapes).

### B2 — Server graceful shutdown of active voice WSs

**Today.** Rolling deploys (k8s, blue-green, etc.) abruptly close any active voice WS. Browser sees a 1006-NO_CLOSE_FRAME with no warning; the user gets cut off mid-conversation. The provider WS (Deepgram) also gets dropped abruptly, leaving open server-side resources in their account that take a while to time out.

**Change.** SIGTERM handler in the WS handler (or the application's existing shutdown hook):

1. **Stop accepting new connections.** Reject new WS upgrades with 503 + `Retry-After` header during shutdown.
2. **Emit shutdown event to active sessions.** Send `{"type":"server_shutting_down","reconnect_in_seconds":5}` to every open WS. Client-side voice hook treats this as a planned disconnect and triggers the reconnection logic (production-readiness Item 1).
3. **Drain phase.** Wait up to a configurable grace period (default 10 s) for sessions to close naturally, then force-close the rest with `CloseStatus.GOING_AWAY` (1001).
4. **Provider WS close.** Each session's provider WS gets a clean `sendClose` during drain.

**Effort.** ~1 day.

### B3 — Tab-close mid-turn behaviour

**Today.** If the user closes the browser tab while the LLM is mid-generation:

- The browser WS closes immediately (CloseStatus 1001).
- `AiHubVoiceWebSocketHandler.afterConnectionClosed` runs, calls `providerSession.close()`.
- BUT the AI Hub agent (`AiHubRoutingAgent.runAgent(...)`) is running in a separate thread driven by Spring AI's reactive pipeline. It doesn't know the WS closed. It may continue to completion, emitting tokens that nobody hears.
- Whether those tokens persist to `SPRING_AI_CHAT_MEMORY` depends on whether the `PromptChatMemoryAdvisor` runs to completion. Likely yes for short turns, partial for long ones.

The result: a voice turn ends with no audio for the user, but the next time they open the task in text mode they see a (possibly partial) assistant message. Confusing.

**Change.** Two options:

1. **Cancel the agent on WS close.** `NativeLlmListener` holds a reference to `VoiceAgentSubscriber`; on `onClose` propagation, call `subscriber.cancel()` which sets the closed flag. Future token events become no-ops. Memory persistence is still automatic but may be partial — that's the tradeoff.
2. **Let it finish.** Document the behaviour as expected: half-spoken turns persist in full to text mode. User can reopen the task and see the complete response.

Recommend **Option 2** with explicit documentation. The user closed the tab; the assistant's response is salvageable in text mode. Cancelling mid-generation just to align voice with text creates a worse outcome (no record at all).

**Effort.** ~0.25 day (just write the doc note; no code change).

### B4 — Silence timeout

**Today.** No server-side timeout for prolonged inbound silence. A user who walks away mid-session leaves the WS + provider WSs open, accruing time against the 30-min cap. Cost is bounded but suboptimal; provider quota is consumed for nothing.

**Change.** Add a `lastInboundAudioAtBySessionId` tracker updated on every `handleBinaryMessage`. A scheduled task (every 30 s) closes sessions where `now - lastInbound > 3 min`. Configurable via a constant; eventually a workspace setting.

The 3-min default is generous — the user might just be thinking. Closing too aggressively interrupts legitimate use; closing too slowly wastes provider minutes. 3 minutes balances both.

**Effort.** ~0.5 day.

## UX gaps

### U1 — Tool-call audibility during STT_TTS_NATIVE_LLM

**Today.** When the AI Hub agent invokes a tool mid-turn (DB query, MCP call, RAG lookup), the user hears silence until the tool returns and the LLM resumes generating tokens. Tool calls can take 1–10 s; the user thinks the assistant froze.

**Change.** Hook into the AG-UI events that `AiHubRoutingAgent` already emits:

- `onToolCallStartEvent` → speak a short filler ("Looking that up…", "Checking…", "One moment…") with provider TTS. Randomized from a small list so it doesn't feel mechanical.
- `onToolCallEndEvent` → no additional cue; the LLM's resumed text content event drives the next speak.

The filler text should be workspace-configurable (`voice_tool_call_filler` setting with a JSON array of strings). Default to a small English list; locale-aware via the i18n work (UX/i18n/docs spec I1N3).

**Effort.** ~0.75 day.

### U2 — RAG / source citations in voice

**Today.** When the AI Hub agent invokes a knowledge-base tool and renders citations in its response (e.g. `[1]` markers with footnote links), the persisted text in chat memory includes them. Voice strips them implicitly because TTS reads `[1]` as "bracket one." The user hears a stripped-down response with no provenance signal.

**Change.** Two-layer approach:

1. **Strip citation markers at the speak boundary.** In `VoiceAgentSubscriber.onTextMessageContentEvent`, run the delta through a citation-marker stripper before sentence-buffering. The text persisted to chat memory is still the original (with markers) so the user sees citations in text mode. The text spoken is the cleaned version.
2. **Optional: speak "according to [doc title]" inline.** Workspace setting `voice_cite_sources` (boolean, default false). When true, the agent's response is augmented at the TTS layer to mention source titles. More invasive — defer to v2.

**Effort.** ~0.5 day for the strip layer; +1 day for the optional speaking layer.

### U3 — Voice-to-task auto-creation

**Today.** A new user opens AI Hub, clicks the mic button before creating a task — Path B requires a `taskId` so the WS handler rejects with "Task not found." The user has no idea why voice "doesn't work."

**Change.** When `currentTaskId` is null AND voice is enabled, mic-button click auto-creates a new AI Hub task (kind=COPILOT, default title "Voice session"), sets it as current, then opens the voice session. The user gets the same flow as if they'd created a task first.

Client-side change only — no server changes needed (the existing `createAiHubTask` mutation works).

**Effort.** ~0.5 day.

### U4 — User-facing minutes-remaining indicator

**Today.** Once metering ships (cost+compliance spec Item 1) + quotas (Item 2), the workspace admin sees usage in a dashboard. The end user clicking the mic button has no visibility into their workspace's quota state — they hit the cap mid-session and the WS closes with a generic error.

**Change.** Surface in the composer:

- Small indicator near the mic button when the workspace is within 80% of monthly quota: "240/300 monthly voice minutes used."
- Distinct indicator at 95%+: yellow text, "approaching monthly limit."
- Block voice + show explanation at 100%: "Your workspace has reached its monthly voice minutes limit. Contact your admin to raise it."

GraphQL surface: `aiHubWorkspaceVoiceQuotaStatus` query returning `{usedMinutes, quotaMinutes, percentUsed}`. Computed server-side from the metering table.

**Effort.** ~1 day (depends on cost+compliance Item 1 shipping first).

## Testing gaps

### T1 — Mobile + iOS smoke testing

**Today.** Voice has been tested on desktop Chrome and Firefox. iOS Safari + Android Chrome behaviour is unverified. Known iOS pitfalls:

- `AudioWorklet` requires user-gesture activation; mic button click should qualify.
- Background tab audio suspends after ~30 s of inactivity in some Safari versions.
- AirPods/Bluetooth route switching during a session can crash the WebRTC stack — does it crash our AudioWorklet pipeline?
- Locked screen behaviour: does the WS stay open? Does audio playback continue?

**Change.** Manual smoke test matrix on each released v1.x:

- iOS 17+ Safari, locked + unlocked, with + without external audio device.
- Android Chrome with Bluetooth headphones, background tab behaviour.
- iPad Safari (different audio routing rules).

Document the matrix in `docs/voice/mobile-support.md` with known limitations. Fix what's fixable; mark the rest as documented limitations.

**Effort.** ~1 day initial pass; recurring before each major release.

## Architectural decisions

These three items don't have a single right answer — they're forks in the road that should be decided deliberately, not by drift.

### D1 — WebRTC vs raw WebSocket transport

**Today.** Browser → AI Hub server uses raw WebSocket + AudioWorklet. We handle PCM framing ourselves. We don't get echo cancellation or noise suppression beyond what `getUserMedia` applies pre-AudioWorklet.

**Alternative.** WebRTC's RTCPeerConnection between the browser and a server-side WebRTC gateway (mediasoup, Janus, or LiveKit). Pros: built-in echo cancellation, noise suppression, automatic codec negotiation, backpressure handling, low-latency Opus codec, adaptive bitrate. Cons: significant new server-side infrastructure, new failure modes, harder to debug.

**Decision needed.** Two questions:

1. Are we hitting audio-quality complaints today that raw WS + AudioWorklet can't fix?
2. Is the operational cost of running a WebRTC gateway acceptable for the voice ICP we serve?

If yes/yes: schedule a v2.0 transport migration spec. If no/either: ship raw WS forever; revisit when complaints accumulate.

**Recommend deferring this decision** until v1.x voice has measurable user feedback. Don't speculatively rebuild.

**Effort if pursued.** ~3 weeks (architectural project, not feature work).

### D2 — Voice in the workflow-editor Copilot

**Today.** AI Hub has voice (composer). The workflow-editor Copilot panel (`CopilotPanel` in `useCopilotStore`) is a separate surface for assisting workflow authors — different agent backend, different UI, different context. Voice isn't wired there.

**Decision needed.** Does voice in the workflow editor make sense as a feature?

Arguments for:
- "Explain this task" via voice while looking at the workflow canvas.
- Voice-driven workflow assembly (dictate node parameters).
- Accessibility — keyboard-only users can dictate workflow changes.

Arguments against:
- Workflow editing is typically slow, deliberate work — voice fits poorly with click-to-select-this-node workflows.
- The Copilot agent backend isn't the same as AI Hub's; reusing the voice WS handler requires plumbing into a different agent runtime.
- Probably nobody's asking for it.

**Recommend deferring** until a specific customer or internal user expresses demand. The SPI is reusable when the time comes.

**Effort if pursued.** ~5 days (Copilot's agent runtime needs to expose the same streaming subscriber interface AI Hub's routing agent provides).

### D3 — A/B testing voice modes per user

**Today.** `voice_mode` is workspace-scoped. The entire workspace gets one of `STT_TTS_NATIVE_LLM` or `VOICE_AGENT_PASSTHROUGH`. There's no built-in way to run an experiment splitting users to compare engagement, latency feel, or cost.

**Decision needed.** Are we going to run product experiments on voice modes? If yes, the workspace-scoped setting is the wrong shape — we need per-user override or a feature-flag layer that overrides the workspace default for a sample of users.

**Recommend deferring** until we have actual user feedback to compare. Premature without baseline.

**Effort if pursued.** ~2 days (extend the resolution chain in `getEffectiveVoiceMode` to consult a feature-flag service per user).

## Cross-cutting

### Metrics additions

- `bytechef_voice_silence_timeout_total{workspace_class}` — B4.
- `bytechef_voice_shutdown_drain_total{outcome=graceful|forced}` — B2.
- `bytechef_voice_tool_call_filler_total{outcome=spoken|skipped}` — U1.
- `bytechef_voice_workflow_suspend_total{outcome=resumed|timed_out}` — B1.

### Sequencing

Priority order assuming these all eventually ship:

1. **B2 (graceful shutdown)** — operational blocker. ~1 day.
2. **U1 (tool-call audibility)** — high-frequency UX bug. ~0.75 day.
3. **B4 (silence timeout)** — cheap cost guard. ~0.5 day.
4. **U3 (voice-to-task auto-create)** — new-user UX. ~0.5 day.
5. **B1 (suspend/resume)** — needs verification before estimation; ~1.5 days if the work matches the design.
6. **U2 (citation stripping)** — small polish. ~0.5 day.
7. **U4 (minutes indicator)** — depends on cost+compliance shipping first. ~1 day.
8. **T1 (mobile testing)** — recurring; first pass ~1 day.
9. **D1, D2, D3** — defer pending evidence of need.

**Bugs subtotal (B1–B4):** ~3.25 days.
**UX subtotal (U1–U4):** ~2.75 days.
**Testing (T1):** ~1 day.

Together: **~7 days** for everything actionable. Decisions are zero-cost (no code) but should be revisited every couple of quarters.
