# AI Hub voice — embedded widget for Path B

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

Path B v1.2 added native AI Hub voice for one surface: the AI Hub composer in the same-origin authenticated UI. The embedded `AutomationChatModal` widget — used when AI Hub is exposed to external users (partner/contractor portals, customer-facing AI assistants) — only supports Path A today.

**Note on scope.** This spec is widget-specific. Twilio phone-number voice was originally bundled with this spec but doesn't fit AI Hub's positioning — Twilio voice is fundamentally a customer-facing channel that should be designed as a Path A workflow with full control over persona, scripts, and guardrails. Internal-AI-assistant tooling like AI Hub doesn't belong on a phone line.

## What's missing

[useAutomationChatVoiceSession](../../../sdks/frontend/automation/chat/library/...) in the widget hits the customer's workflow webhook for voice (Path A). It doesn't have any knowledge of AI Hub. Adding Path B requires:

1. A way for the widget consumer to say "use AI Hub voice for this workspace" — a config option on the widget.
2. A way for the widget (running on the customer's site, cross-origin) to authenticate against AI Hub's `/api/automation/internal/ai-hub/voice/*/voice-session-token` endpoint, which is currently session-cookie-gated and not cross-origin-friendly.

## Two viable approaches

### Approach A — Public widget-token endpoint

A new endpoint `POST /api/automation/widget/ai-hub/voice/{taskId}/voice-session-token` that:

- Doesn't require session cookies.
- Accepts an embedded API key (the same one the widget uses for the rest of its requests).
- Validates the API key + workspace scope.
- Mints the same single-use 60s session token as the internal endpoint.
- Subject to stricter rate limiting + audit logging than the internal endpoint.

The widget then uses the same WS upgrade flow as the composer (with the embedded API key validated server-side on the WS upgrade).

### Approach B — Server-mediated session

The widget never talks to AI Hub voice directly. Instead:

- The customer's site has an existing server that talks to AI Hub.
- The widget asks the customer's server to mint a voice session.
- The customer's server (using its admin credentials) calls the internal endpoint and returns the token to the widget.
- The widget then opens the WS with that token.

Pushes the auth complexity onto the customer but stays cross-origin-clean.

### Recommended

**Approach A** — better DX for customers, the API key flow is already well-trodden in the widget. Approach B is documented as a fallback for customers who can't or won't expose an AI-Hub-talking endpoint.

## Changes

1. **New REST endpoint** `POST /api/automation/widget/ai-hub/voice/{taskId}/voice-session-token`. Same token-mint logic as the internal controller, but authentication is API-key-based instead of session-cookie-based. Mirrors the existing widget endpoints under `/api/automation/widget/**`.

2. **WS handler tolerance.** `AiHubVoiceWebSocketHandler` doesn't actually authenticate the WS upgrade beyond the session-token-in-query-param check — the token IS the auth. So no WS-layer changes are needed; just verify the token-mint endpoint is API-key-protected.

3. **Widget config.** New optional prop on `AutomationChatModal`:

   ```tsx
   <AutomationChatModal
     config={{
       webhookUrl: '...',
       aiHubVoice: {
         workspaceId: 'wks_123',
         taskId: 'task_456',  // null = create-on-first-mic-click
       },
     }}
   />
   ```

   When `aiHubVoice` is present and `voiceWebhookUrl` is null, the widget routes voice through AI Hub's native endpoint.

4. **Widget voice hook.** Mirror `useAiHubVoiceSession` (already in the composer) but using the widget's API-key auth instead of session cookies. Likely a thin wrapper over `BrowserVoiceSession` parametrized by the appropriate endpoint URLs. The two hooks could share a lower-level primitive if the duplication starts to bite.

5. **CORS.** The token-mint endpoint needs proper CORS headers when called from a customer's domain. Add CORS config for `/api/automation/widget/**` if not already permissive.

6. **Mode awareness.** The widget should honor the workspace's `voiceMode` setting the same way the composer does. The mode comes back as part of the workspace settings query (already exposed via GraphQL); the widget needs to either pull the same settings or have the server pass it down via the token-mint response.

## Approach B follow-up (if Approach A blocked)

If we can't ship a public token endpoint (security review concerns), document the server-mediated pattern in the widget README and provide a reference implementation (Node.js Express middleware sample). Effort: ~1 day docs + ~0.5 day reference impl. The widget itself still needs the `aiHubVoice` config + voice hook changes — only the auth half differs.

## Threat model

**Public token endpoint — abuse vectors:**

- **Token exhaustion DoS.** Rate-limit by API key + workspace. Same limits as other widget endpoints.
- **Cross-workspace token mint.** API key scope check must happen at mint time. The API key is scoped to a workspace; the `taskId` parameter must belong to that workspace. Reject otherwise with 403 (not 404 — leaking task existence is fine in this scope since the caller already authenticated).
- **Replay.** Tokens are already single-use 60s, fine.
- **Cap evasion.** The max-concurrent-sessions-per-workspace cap (shipped in production-readiness) applies regardless of which endpoint mints the token, so widget traffic counts against the same workspace pool as composer traffic.

## Acceptance

A customer embeds the widget on `https://customer-site.com` configured with their AI Hub workspace. The user opens the chat, clicks mic, and gets a Path B voice session with AI Hub LLM in the loop. CORS works. The session counts against the workspace's concurrent-session cap. Mode toggle (VOICE_AGENT_PASSTHROUGH vs STT_TTS_NATIVE_LLM) honored.

## Cross-cutting

### Metrics

Extend the existing path counter with a `surface` tag (or split into a parallel counter, depending on cardinality concerns):

- `bytechef_voice_session_surface_total{outcome,surface=composer|widget,provider}` — distinguishes widget traffic from composer traffic. Useful for "is anyone actually using the widget" + cost-attribution per surface.

Cardinality stays bounded: 3 outcomes × 2 surfaces × small-N providers.

### Docs

- New section in widget README: "Use AI Hub voice from the widget."
- Migration guide for customers currently on Path A with `voiceWebhookUrl` who want to switch to Path B.
- Brief note in the v1.2 design spec / TTS strategy doc that widget Path B exists.

### Sequencing

The work is mostly in the widget package, with a small server-side endpoint addition. Order:

1. New server endpoint + tests (~1 day).
2. Widget voice hook for Path B + config plumbing (~1.5 days).
3. CORS + integration testing (~0.5 day).
4. Docs (~0.5 day).

**Total effort: ~3–4 days.**

Ship as one cohesive PR. The endpoint isn't useful without the widget changes and vice versa.

## When to prioritise this

Currently a hypothetical feature — we don't have customers asking for it. The trigger to build it is:

- A specific customer wanting AI Hub voice in their embedded widget, OR
- A strategic decision that AI Hub-on-the-widget is a positioning play we want to make.

Until one of those, this spec sits as a ready-to-execute plan.
