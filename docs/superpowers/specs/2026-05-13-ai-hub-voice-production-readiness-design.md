# AI Hub voice — production-readiness design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

v1.2 shipped the real Path B but left several production-readiness items open. Two are already fixed in this branch (max session duration + max concurrent sessions per workspace). Four remain:

1. Reconnection support (browser → server)
2. Provider WS health watchdog
3. Connection-parameter encryption confirmation
4. Integration test coverage

Independent items, can ship in any order. Total effort estimate: ~5–7 days.

## Item 1 — Reconnection support

**Today.** If the browser WS drops (network blip, laptop wake from sleep, mobile context switch), the user's voice session is lost. The next mic-button press mints a new token and opens a new WS, but:
- The provider session is gone — no audio playback queue continuity.
- The AI Hub task's `threadId` is still the same, so SPRING_AI_CHAT_MEMORY persists, but transcripts mid-flight at the moment of disconnect are dropped.
- Any TTS audio buffered for the user but not yet emitted is lost.

**Change.** Three layers:

1. **Client-side reconnect.** Extend `BrowserVoiceSession` to detect `onclose` events that aren't user-initiated, schedule an exponential backoff retry (1 s → 2 s → 4 s → 8 s, max 3 attempts), mint a fresh session token via the existing endpoint, and reopen the WS with the same taskId + sampleRate. Surface reconnect state via `onStatusChange('reconnecting')`.

2. **Server-side session resumability.** The current WS handler treats each connection as a fresh session — no resumption state is kept across drops. Two options:
   - **Option A (simpler):** Each reconnect starts a fresh provider session bound to the same threadId. Chat memory continues; transient audio is lost. ~1 day.
   - **Option B (richer):** Reserve a brief grace window (~10 s) after a WS drops where a reconnect to the same taskId restores the original provider session. Requires keeping the `AiHubVoiceProviderSession` alive across browser absence. ~3 days. Tricky: provider WSs may have their own idle timers.

   Recommend **Option A** for v1.3 — most reconnects come from short network flaps that the provider WS will outlive. Document Option B as a v1.4 polish.

3. **Browser playback queue.** The AudioWorklet's PCM playback buffer needs a "drain and resume" semantic — discard everything queued at drop, accept new audio cleanly after reconnect. Likely already correct from Path A work but verify.

**Acceptance.** Toggle Wi-Fi off and on during a voice session; the assistant resumes within 5 s and the next user turn flows normally. Chat memory in text mode reflects everything spoken before the drop.

**Effort.** ~1.5 days (Option A).

## Item 2 — Provider WS health watchdog

**Today.** If Deepgram's STT or TTS WS returns 4xx at open time, `AiHubVoiceProviderException` is thrown and the browser WS closes with a clear error. But:
- If the WS opens successfully then stalls (e.g. Deepgram-side stuck), nothing detects it. The user speaks; STT never emits transcripts; the agent never runs.
- If TTS WS stops responding mid-utterance, audio just stops.

**Change.** Per-session watchdog timer in `DeepgramSttTtsSession`:

- **STT activity check.** Track last-audio-received timestamp. If no audio comes from the provider within 10 s of activity from the browser (i.e. the user is actively speaking), treat the STT WS as stalled. Close the WS, call `listener.onError(new StallException("STT WS stalled"))`, which propagates to the browser as `error` JSON + a clean close.

- **TTS speak ACK check.** When the WS handler calls `speak(text, turnId)`, expect at least one audio frame within 5 s. If none arrives, treat as stalled.

- **WS handler retry.** On stall, the browser sees the error + can retry (Item 1 reconnect logic).

**Provider-specific note.** Each provider has different latency characteristics. The watchdog timeouts need to be tunable per provider (Deepgram is fast; ElevenLabs Flash is faster; ElevenLabs Multilingual v2 is slower). Hardcode reasonable defaults in v1.3; add a `voice_watchdog_timeout_ms` workspace setting if customers report false positives.

**Acceptance.** Simulate a stalled provider WS (e.g. firewall-drop after handshake). Browser sees a clear error within 15 s instead of hanging silently.

**Effort.** ~1 day.

## Item 3 — Connection-parameter encryption confirmation

**Today.** Path B fetches the provider's API key via `connectionService.getConnection(connectionId).getParameters()`. The `ConnectionService` layer encrypts/decrypts parameters at rest via `EncryptionService`, but this needs explicit verification for the voice connection path — there's no test pinning the behaviour, and a change to ConnectionService's internal encryption could silently regress us to plaintext-at-rest.

**Change.** Two pieces:

1. **Integration test pinning encrypted persistence.** New test in `automation-ai-hub-service-test-int-test`:
   - Create a connection row with `parameters.token = "secret"` via the ConnectionService.
   - Read the raw row from the `connection` table (bypass the service).
   - Assert the raw value is NOT `secret` (i.e. encrypted).
   - Call `connectionService.getConnection(id).getParameters().get("token")` and assert it equals `"secret"` (i.e. decrypted on read).

   Doesn't need to be voice-specific — it pins the contract that ALL connections encrypt parameters at rest, which our voice path relies on.

2. **Audit log.** When a voice session resolves a connection, log `connectionId` + `workspaceId` + `provider` (NOT the parameters). Used for incident response if a customer reports a leaked key — we can find which workspace held which connection.

**Acceptance.** Test exists and passes; manual inspection of `connection.parameters` raw value in dev DB confirms encryption.

**Effort.** ~0.5 day.

## Item 4 — Integration tests

**Today.** v1.2 has compile-pass-only coverage. No end-to-end smoke test verifies the path works. Specific gaps:

- `AiHubVoiceSessionTokenService.consume` — single-use semantics, taskId mismatch rejection, TTL expiry.
- `AiHubVoiceWebSocketHandler.afterConnectionEstablished` — token validation flow, workspace settings lookup, mode dispatch.
- `AiHubVoiceAgentBridge.VoiceAgentSubscriber` — sentence buffering boundary detector. The kind of code where bugs hide silently (clauses missed, mid-word splits, off-by-one).
- `DeepgramSttTtsSession.SttListener.handleTextMessage` — JSON event parsing contract with Deepgram.
- Workspace settings mutex — saving Path A clears Path B, saving Path B clears Path A.
- VoiceProviderMode ordinal stability (mirroring `EnumOrdinalStabilityTest` patterns).

**Change.** Three test classes:

1. **`AiHubVoiceSessionTokenServiceTest`** (unit, no Spring) — covers token issue + consume + replay reject + taskId-mismatch reject + TTL.
2. **`AiHubVoiceAgentBridgeTest`** (unit) — covers `VoiceAgentSubscriber` sentence buffering with parametrized tests: sentence-terminator detection, multi-sentence flushes, newline boundaries, residual flush on `RunFinishedEvent`, no-op after `closed`.
3. **`AiHubVoiceWebSocketHandlerIntTest`** (Spring `@SpringBootTest`, Testcontainers PostgreSQL) — covers session-open flow with mocked provider, max-concurrent-sessions enforcement, max-duration timer firing (test with short duration override), workspace settings mutex.

Add an `EnumOrdinalStabilityTest` for `VoiceProviderMode` in the api module.

**Acceptance.** All three test classes pass on `./gradlew check`.

**Effort.** ~2–3 days.

## Cross-cutting

### Metrics

Already covered by v1.2 — `bytechef_voice_session_native_mode_total{outcome,mode,provider}` captures opened/closed/error per Path B sub-mode. Add:

- `bytechef_voice_session_max_duration_close_total{provider}` — increments when the max-duration timer fires (distinct from user-initiated close).
- `bytechef_voice_session_concurrent_rejected_total{workspace_class=small|medium|large}` — increments when concurrent-cap rejection fires. The workspace_class bucket is `≤10`, `≤100`, `>100` users to keep cardinality bounded.
- `bytechef_voice_provider_stall_total{provider,phase=stt|tts}` — Item 2.

### Per-workspace overrides

Today both caps (duration + concurrent) are hardcoded. For workspaces with longer voice sessions (call-center workflows) or more concurrent users, expose:

- `voice_max_session_duration_seconds INT NULL` — null = global default.
- `voice_max_concurrent_sessions INT NULL` — null = global default.

Both nullable in `ai_hub_workspace_settings`. Read at session open. Defer to v1.4 if not urgent — global defaults work fine for v1.3 GA.

## Sequencing

Recommended ship order:
1. **Item 4 (tests) first** — protects everything else from regression. ~3 days.
2. **Item 3 (encryption test)** — quick win bundled into Item 4 work. ~0.5 day.
3. **Item 2 (watchdog)** — clearest user-visible improvement, low risk. ~1 day.
4. **Item 1 (reconnect)** — most complex client-side work. ~1.5 days.

Total: ~6 days. Items 4 + 3 alone are ~1 week of safety net; ship them as v1.3.0 before any feature work.
