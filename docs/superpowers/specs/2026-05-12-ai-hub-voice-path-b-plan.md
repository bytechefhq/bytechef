# AI Hub voice Path B — implementation plan

**Status:** Plan | **Owner:** Ivica | **Created:** 2026-05-12 | **Last updated:** 2026-05-12

## Goal

Implement Path B from [2026-05-12-ai-hub-voice-tts-strategy.md](2026-05-12-ai-hub-voice-tts-strategy.md): AI Hub server owns its own voice WebSocket endpoint, proxies audio to a configured voice provider, and uses AI Hub's existing agent state (tools, memory, personal-agent overlay, system prompt) to drive the LLM portion of the conversation.

Result: voice in AI Hub is **zero-config for users** once workspace voice provider settings are configured. The mic just works; the same agent that powers text conversations also powers voice. No workflow-webhook indirection.

## Why Path B exists

Path A (shipped in v1) routes voice through a customer-built workflow. The workflow's agent runs the LLM during voice mode — AI Hub's tools, memory, MCP servers, and personal-agent overlays are bypassed. This is a real architectural compromise that customers will notice once they try to use voice for the same conversations they have via text.

Path B fixes that by making AI Hub server first-class in the audio loop. It costs ~1 week of focused engineering but delivers the experience customers expect from a 2026 voice product.

## Non-goals (defer to Tier 2)

- **Multi-provider routing per call.** Each workspace picks one voice provider; switching providers mid-conversation is out of scope.
- **Custom voice cloning.** Use provider-managed voice IDs (Deepgram Aura, OpenAI voices, ElevenLabs voices) — no custom-trained voices.
- **Recording / transcript export.** Transcripts continue to flow into the AI Hub task thread for visibility; persistence beyond the chat thread is Tier 2.
- **TTS streaming during agent generation.** AI Hub's agent today streams tokens. Path B v1 will buffer the full assistant response and TTS the complete sentence at once (lower complexity, ~500ms-1s extra latency vs token-by-token TTS). Streaming TTS is a Path B v1.1 optimization.
- **Per-call cost metering for billing.** Metrics counters land in v1 (already shipped); user-visible billing dashboards are Tier 2.
- **Voice provider failover.** If Deepgram is down, voice is down for that workspace. Multi-provider fallback is Tier 2.

## Scope

Six phases, each shippable independently. Total estimate: ~1 engineer-week.

### Phase 1 — Workspace settings schema extension (~half a day)

Add the provider/model/connection/voiceId fields to `ai_hub_workspace_settings`. **Do not** wire them into any frontend yet — Phase 1 is pure schema + GraphQL surface.

**Liquibase migration** `20260513000001_ai_hub_workspace_settings_add_provider_fields.xml`:

```xml
<changeSet id="20260513000001-1" author="Ivica Cardic">
    <addColumn tableName="ai_hub_workspace_settings">
        <column name="voice_provider" type="VARCHAR(64)"/>
        <column name="voice_connection_id" type="BIGINT"/>
        <column name="voice_model" type="VARCHAR(128)"/>
        <column name="voice_id" type="VARCHAR(128)"/>
    </addColumn>
</changeSet>
```

**Entity fields** on `AiHubWorkspaceSettings`:

```java
@Column("voice_provider")
private @Nullable String voiceProvider;  // "DEEPGRAM" | "OPENAI_REALTIME" | "ELEVENLABS"

@Column("voice_connection_id")
private @Nullable Long voiceConnectionId;  // ref to a connection row with the provider's API key

@Column("voice_model")
private @Nullable String voiceModel;  // "gpt-4o-realtime", "eleven_turbo_v2_5", etc.

@Column("voice_id")
private @Nullable String voiceId;  // provider-specific TTS voice id
```

**GraphQL schema extension** on the existing `AiHubWorkspaceSettings` type:

```graphqls
type AiHubWorkspaceSettings {
    workspaceId: ID!
    voiceWebhookUrl: String       # Path A — kept for backwards compat
    voiceProvider: VoiceProvider  # Path B
    voiceConnectionId: ID
    voiceModel: String
    voiceId: String
}

enum VoiceProvider {
    DEEPGRAM
    OPENAI_REALTIME
    ELEVENLABS
}
```

New mutation:

```graphqls
extend type Mutation {
    updateAiHubVoiceProvider(input: UpdateAiHubVoiceProviderInput!): AiHubWorkspaceSettings
}

input UpdateAiHubVoiceProviderInput {
    workspaceId: ID!
    voiceProvider: VoiceProvider
    voiceConnectionId: ID
    voiceModel: String
    voiceId: String
}
```

The existing `updateAiHubVoiceWebhookUrl` mutation stays — Path A and Path B are mutually exclusive (validation: settings have either `voiceWebhookUrl` or `voiceProvider` non-null, not both).

### Phase 2 — Voice Provider SPI (~1 day)

A clean interface for the audio-proxy lifecycle. One implementation per provider; the AI Hub voice WS endpoint picks the provider based on workspace settings.

**Interface** in a new module `server/ee/libs/platform/platform-ai-hub-voice/platform-ai-hub-voice-api/`:

```java
public interface AiHubVoiceProvider {

    /** Open a provider WS session bound to a ByteChef voice session. */
    AiHubVoiceProviderSession openSession(AiHubVoiceProviderConfig config);

    /** Identifies the provider in workspace settings (e.g. "DEEPGRAM"). */
    String getKey();
}

public interface AiHubVoiceProviderSession {
    /** Send a chunk of caller audio to the provider. */
    void sendAudio(byte[] pcmBytes);

    /** Hint that the user finished an utterance (used by providers without built-in VAD). */
    void endOfUtterance();

    /** Cancel an in-flight TTS turn (barge-in). */
    void cancelTurn(String turnId);

    /** Close the provider session. */
    void close();
}

public record AiHubVoiceProviderConfig(
    long workspaceId,
    String voiceModel,
    String voiceId,
    String systemPrompt,
    String conversationId,
    AiHubVoiceProviderListener listener) {
}

public interface AiHubVoiceProviderListener {
    /** Called when the provider emits a transcribed user turn. */
    void onTranscript(String text, String turnId);

    /** Called when the provider emits TTS audio bytes. */
    void onAudio(byte[] pcmBytes);

    /** Called when the provider emits an assistant text token (for visibility in chat thread). */
    void onAssistantText(String text, String turnId, boolean done);

    /** Called on provider error. */
    void onError(Throwable throwable);

    /** Called when the provider closes its WS. */
    void onClose();
}
```

**Deepgram implementation** in `platform-ai-hub-voice-deepgram/`:

Wraps Deepgram's `voiceAgent` WS. Most of the existing `DeepgramVoiceAgentAction` logic translates 1:1; it's restructured to plug into the `AiHubVoiceProvider` interface instead of being driven by the workflow runtime.

**Configuration in v1 of Path B**: Deepgram only. OpenAI Realtime and ElevenLabs are documented stubs (`OpenAiRealtimeVoiceProvider` + `ElevenLabsVoiceProvider` shells in the same module, throw `UnsupportedOperationException` until implemented). The enum exists but only DEEPGRAM is usable.

### Phase 3 — AI Hub voice WS endpoint (~1.5 days)

A new endpoint that's structurally similar to `WorkflowTestWebSocketHandler` but bound to AI Hub task semantics instead of workflow-test semantics.

**New module** `server/ee/libs/automation/automation-ai-hub-voice/` (alongside `automation-ai-hub`):

```
automation-ai-hub-voice/
├── automation-ai-hub-voice-api/        # AiHubVoiceSessionTokenService interface
├── automation-ai-hub-voice-service/    # impl + WS handler + session token controller
└── automation-ai-hub-voice-rest/       # @CrossOrigin REST controller for token mint
```

**WS endpoint**: `wss://host/api/automation/internal/ai-hub/voice/{taskId}/wss?sessionToken=…`

**Token mint**: `POST /api/automation/internal/ai-hub/voice/{taskId}/voice-session-token`
- Gated by the standard session cookie (`/internal/**`)
- Token scope-verified against the workspace's `AiHubWorkspaceSettings.voiceProvider != null`
- Same single-use semantics + 60s TTL as `BrowserVoiceSessionTokenService`

**Handler** `AiHubVoiceWebSocketHandler`:

```java
@Component
public class AiHubVoiceWebSocketHandler extends AbstractWebSocketHandler {

    public AiHubVoiceWebSocketHandler(
        AiHubVoiceSessionTokenService tokenService,
        AiHubWorkspaceSettingsService settingsService,
        AiHubTaskService taskService,
        Map<String, AiHubVoiceProvider> providersByKey,
        VoiceMetricsRecorder metricsRecorder) {
        ...
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 1. Validate sessionToken via tokenService.consume
        // 2. Resolve taskId from URL, load workspace from task
        // 3. Look up workspace settings → voiceProvider key → provider impl
        // 4. Build AiHubVoiceProviderConfig with:
        //    - systemPrompt from AI Hub's normal agent-system-prompt rendering for this task
        //      (includes personal-agent overlay if task kind = PERSONAL_AGENT)
        //    - conversationId = task's threadId (so chat memory is shared with text mode)
        //    - voiceModel, voiceId from workspace settings
        // 5. provider.openSession(config) → AiHubVoiceProviderSession
        // 6. Wire listeners:
        //    - provider.onAudio → session.sendMessage(BinaryMessage)
        //    - provider.onTranscript → persist as user message in AI Hub task thread
        //    - provider.onAssistantText → persist as assistant message in AI Hub task thread
        //    - provider.onClose / onError → close WS, metrics
        // 7. Wire inbound:
        //    - session.handleBinaryMessage → providerSession.sendAudio
        //    - session.handleTextMessage (JSON control) → providerSession.cancelTurn etc.
    }
}
```

**Key implementation detail — AI Hub agent integration**: the LLM portion needs to use AI Hub's agent loop, not the provider's built-in LLM. Two approaches:

**Approach A — Provider does STT only, AI Hub does LLM, provider does TTS** (the architecturally correct approach):
- Configure Deepgram for STT-only mode (no LLM, no TTS over the same WS)
- On `onTranscript`, dispatch into AI Hub's existing AG-UI agent backend (via the same path text mode uses)
- Collect the assistant response text
- Send the response text to the provider for TTS (Deepgram supports a separate TTS REST endpoint or a dedicated TTS WS)
- Forward TTS audio to the client

**Approach B — Provider does the full STT+LLM+TTS loop with AI Hub's prompt injected**:
- Configure Deepgram voiceAgent with AI Hub's system prompt + tool definitions
- The provider runs the LLM and tool-calls
- Tool calls route back to AI Hub via webhook
- Less ideal but much simpler; tool-call latency is meh

**Recommendation:** Ship Approach B first (~1.5 days), iterate to Approach A in Path B v1.1 (~3 days additional). Approach B is enough to demonstrate "AI Hub voice works without workflow indirection"; Approach A is the right end-state.

### Phase 4 — Client integration (~1 day)

When workspace settings have `voiceProvider` set, route to the native endpoint instead of the workflow webhook.

**Hook update** in `useAiHubVoiceSession`:

```ts
function useAiHubVoiceSession({voiceWebhookUrl, voiceProvider, currentTaskId, ...}) {
    const url = voiceProvider != null
        ? `/api/automation/internal/ai-hub/voice/${currentTaskId}/wss`
        : voiceWebhookUrl;  // Path A fallback

    const tokenEndpoint = voiceProvider != null
        ? `/api/automation/internal/ai-hub/voice/${currentTaskId}/voice-session-token`
        : `${voiceWebhookUrl}/voice-session-token`;  // Path A fallback

    // … rest of the hook unchanged
}
```

**Settings popover update**: when an admin opens the popover, surface a radio choice:
- "Use a voice workflow (advanced, custom voice pipelines)" → existing `voiceWebhookUrl` flow
- "Use built-in voice (recommended)" → `voiceProvider` dropdown + connection picker + model picker + voiceId

The two are mutually exclusive at save time (server-side validation rejects setting both).

**Codegen step**: regenerate `graphql.ts` after Phase 1 schema additions.

### Phase 5 — Observability (~half a day)

Extend the existing metrics in `VoiceMetricsRecorder`:

```
bytechef_voice_session_total{outcome=opened|closed|error, path=A|B}
bytechef_ai_hub_voice_provider_total{provider=DEEPGRAM|OPENAI_REALTIME|ELEVENLABS, outcome=success|error}
```

The path tag lets ops compare Path A vs Path B reliability and cost as adoption shifts.

### Phase 6 — Documentation (~half a day)

Update:
- `docs/voice/quickstart.md` — split into "Path A setup" (workflow-based) and "Path B setup" (workspace provider config)
- `docs/voice/ai-hub.md` (new) — AI Hub-specific voice walkthrough showing both paths
- `docs/superpowers/specs/2026-05-12-ai-hub-voice-tts-strategy.md` — update status to "Path B shipped in v1.1"
- Remove the "Path A only" caveat from AI Hub voice config UI

## Phasing summary

| Phase | What | Effort | Shippable? |
|---|---|---|---|
| 1 | Workspace settings schema | 0.5 day | ✓ (additive, no UI yet) |
| 2 | Voice Provider SPI + Deepgram impl | 1 day | ✓ (internal only) |
| 3 | AI Hub voice WS endpoint + token mint + agent integration (Approach B) | 1.5 days | ✓ (end-to-end voice working) |
| 4 | Client integration (settings UI + hook routing) | 1 day | ✓ (user-visible feature) |
| 5 | Observability extensions | 0.5 day | ✓ |
| 6 | Documentation | 0.5 day | ✓ |
| **Total** | | **5 days** | |

After Phase 4, AI Hub voice "just works" for admins who configure a Deepgram connection + provider settings. The mic button hooks the workspace's `voiceProvider` automatically; no workflow construction required.

## Approach A vs Approach B decision (Phase 3 detail)

The "Phase 3" entry above defers between two architectural approaches. Concrete trade-offs:

**Approach B (provider runs full LLM)** — ship first:
- Pros: ~1.5 days. Architecturally simpler. Provider's WS handles STT+LLM+TTS as one round trip.
- Cons: AI Hub's tools / MCP servers / personal-agent overlay reach the LLM only via system-prompt + tool definitions injected into Deepgram's voiceAgent config. Tool calls round-trip via webhook (slower).
- Result: voice works without a customer-built workflow. AI Hub agent intelligence applies via prompt-level integration but not deep agent-loop integration.

**Approach A (AI Hub runs the LLM)** — ship second:
- Pros: AI Hub's agent loop runs end-to-end. Same code path as text mode, same tools, same MCP integration, same memory.
- Cons: ~3 extra days. Three-WS-handoff latency (mic→Deepgram STT→AI Hub LLM→Deepgram TTS→speaker). Need careful buffering to keep round-trip under 1.5s.

**Ship order:** B in Path B v1.0, then A in v1.1 once cost/latency telemetry from B shows what we're solving for.

## Open questions

1. **Authentication on the AI Hub voice WS endpoint.** `/api/automation/internal/**` is session-cookie gated. Cross-origin embedded customer sites can't authenticate via cookie — they'd need a separate path. For Path B v1, restrict to same-origin (AI Hub composer running on bytechef.io itself). Cross-origin embedded AI Hub voice is a Tier 2 question.

2. **Personal-agent voice overlay.** When the active task kind is `PERSONAL_AGENT`, the agent's instructions should inject into the system prompt. Approach B injects this directly into Deepgram's voiceAgent config. Approach A inherits it for free since AI Hub's agent loop already applies it.

3. **Workflow-chat task voice.** When task kind is `WORKFLOW_CHAT`, the agent is the underlying workflow's `WebhookBridgeAgent`. Path B doesn't apply — voice for workflow-chat tasks routes through the workflow's own browser-voice trigger (Path A semantics). The mic button should switch behavior per task kind.

4. **Provider API key management.** `voiceConnectionId` references a `connection` row with the provider's API key. This means the customer must create a Deepgram connection (or OpenAI / ElevenLabs) via the normal Connections UI before configuring AI Hub voice. The settings popover needs a connection picker that filters to the right provider.

5. **Mutex between Path A (voiceWebhookUrl) and Path B (voiceProvider).** Server-side validation in the mutations: if `voiceProvider` is set, `voiceWebhookUrl` must be null, and vice versa. UI radio selector enforces it client-side too.

## Test plan

- Unit: provider SPI contract tests (mock provider session, verify lifecycle methods fire in order).
- Unit: token service replay rejection, scope verification (settings must have `voiceProvider` set).
- Integration: `AiHubVoiceWebSocketHandlerIntTest` with a stub `AiHubVoiceProvider` — open WS, send fake transcript, verify it lands in the AI Hub task thread; send fake audio bytes, verify they round-trip.
- Manual smoke: configure a Deepgram connection + workspace voice provider, open AI Hub, click mic, speak, hear response.
- Manual smoke: verify Path A (existing `voiceWebhookUrl` config) still works after Phase 1 schema changes.

## Migration from Path A

Existing workspaces using Path A (`voiceWebhookUrl` set) keep working — Phase 1's schema additions are purely additive. The settings popover detects which config shape is in use and renders accordingly:

- `voiceWebhookUrl != null` → Path A UI (URL input + preflight)
- `voiceProvider != null` → Path B UI (provider dropdown + connection + model)
- Both null → empty state with "Choose voice setup" radio between the two

Customers can switch between paths by clearing one config and configuring the other. No data migration required.

## Out of scope (Tier 3)

- Voice in workflow-chat tasks via Path B
- Multi-provider per workspace (e.g. Deepgram for STT, OpenAI for LLM, ElevenLabs for TTS as one config)
- Voice activity detection (VAD) configuration UI
- Custom audio pipeline filters (profanity strip, PII redaction)
- Voice cloning / fine-tuned voices
- Recording / call playback

## Bottom line

Path B is ~1 engineer-week, split across six committable phases. No phase is risky in isolation; the architectural commitment is "AI Hub server owns the voice WS, providers are pluggable, workspace settings drive the choice."

Most user-visible work is Phases 1, 3, and 4. Phases 2, 5, 6 are infrastructure and polish.

Phase 3's Approach B vs Approach A decision determines how deeply AI Hub's agent intelligence applies to voice. Approach B ships fast and gets us 80% of the value; Approach A is the right end-state but additional 3-day investment.

Recommended sequence: Phase 1 → 2 → 3 (Approach B) → 4 → 5 → 6, then iterate to Approach A in a v1.1 round.
