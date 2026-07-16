# Voice quickstart

Set up your first voice workflow in ~5 minutes. You'll end up with a workflow that lets you (or your customers) speak to a Deepgram-powered AI assistant directly from the browser.

## What you'll build

```
microphone → ByteChef WebSocket → Deepgram voiceAgent (STT + LLM + TTS) → speaker
```

The Deepgram **voiceAgent** action is an all-in-one component: speech-to-text, LLM reasoning, and text-to-speech all happen at Deepgram over a single WebSocket. ByteChef proxies the audio bytes; you don't need to wire STT, agent, and TTS as separate tasks.

This is the recommended v1 setup. If you want to split the chain across providers (e.g. Deepgram STT + Anthropic LLM + ElevenLabs TTS), see the [multi-provider chain doc](./multi-provider-chain.md) — but stick with all-in-one for your first voice workflow.

## Prerequisites

- ByteChef Automation Pro or Enterprise (browser voice is not in Community Edition today)
- A Deepgram account with an API key — get one free at [console.deepgram.com](https://console.deepgram.com). The free tier covers ~45 minutes of voice agent usage.
- A modern browser: Chrome 66+, Firefox 76+, or Safari 14.1+ (older browsers don't support the `AudioWorklet` API)

## Step 1 — Connect Deepgram

In ByteChef, go to **Connections** → **New Connection** → search for **Deepgram**. Paste your API key. Save.

You'll need the connection ID in step 3. You can find it in the URL of the connection's detail page.

## Step 2 — Import the example workflow

The repo ships a working example at `docs/examples/voice/deepgram-voiceagent.json`. Either copy that file's contents into a new workflow in ByteChef, or create the workflow manually:

1. **Workflows** → **New Workflow**
2. Add a trigger: search for **Browser** → **Browser Voice Session**
3. Configure the trigger:
   - **Real-Time Workflow**: any name (e.g. `voice-subflow-1`)
   - **Audio Sample Rate**: 16 kHz
   - **Echo Cancellation**: on
   - **Noise Suppression**: on
4. In the trigger's "Real-Time Workflow" editor (the embedded sub-workflow), add **Deepgram → Voice Agent** as the only task:
   - **System Prompt**: defines the agent's behavior. Example: "You are a helpful assistant for ByteChef. Keep responses concise and natural for spoken delivery."
   - **Greeting**: the assistant's opening line (e.g. "Hi! How can I help?")
   - **LLM Provider** + **LLM Model**: e.g. OpenAI / gpt-4o-mini, or Anthropic / claude-3-5-sonnet
   - **TTS Provider** + **TTS Voice**: e.g. Deepgram / aura-asteria-en (a friendly female voice)
   - **Audio Input/Output Encoding**: linear16 (must match the trigger's sample rate)
   - **Connection**: the Deepgram connection you created in step 1

## Step 3 — Test from the workflow editor

The fastest way to verify the workflow works:

1. Open the workflow in the editor
2. Click the **mic icon** in the Playground panel header (top-right). The mic icon only appears when the workflow has a `websocketTasks` extension — you'll see it once you've added the trigger + agent above.
3. Grant microphone permission
4. Wait for the assistant's greeting (1-2 seconds)
5. Speak naturally — the assistant should respond

If you don't hear anything:

- Check the browser's developer console for errors
- Check the trigger's `audioInputSampleRate` matches the trigger's `sampleRate` (both should be 16000)
- Check the Deepgram connection has a valid API key
- Verify your browser is one of the supported versions

## Step 4 — Deploy

Once the workflow works in the editor, publish it. You'll get a webhook URL like:

```
https://your-bytechef-instance.com/api/automation/webhooks/abc123
```

That URL is what you'll paste into the embedded chat widget, AI Hub voice settings, or any other frontend that needs voice.

## Step 5 — Wire up a frontend

Three places voice can run:

### Workflow editor (already working from step 3)

You're done. The editor's test panel uses the workflow's draft definition directly.

### Embedded chat widget on your customer's site

```tsx
import {AutomationChatModal} from '@bytechef/automation-chat';

<AutomationChatModal
    config={{
        webhookUrl: 'https://your-bytechef-instance.com/api/automation/webhooks/abc123',
        voiceWebhookUrl: 'https://your-bytechef-instance.com/api/automation/webhooks/abc123',
    }}
/>
```

If you embed the widget inside an `<iframe>`, the iframe element MUST have `allow="microphone"` or `getUserMedia` will be silently denied:

```html
<iframe src="https://your-site.com/chat" allow="microphone"></iframe>
```

See the [widget README](../../sdks/frontend/automation/chat/library/README.md) for the full config reference.

### AI Hub composer

AI Hub voice ships with **two routing paths**, chosen per-workspace by the admin. Both are mutually exclusive — setting one clears the other server-side.

#### Path A — Workflow webhook (the v1 default)

1. Workspace **Admin** → **AI Hub Settings** (gear icon in composer) → select **Workflow webhook (Path A)**
2. Paste the webhook URL from step 4
3. Save
4. Open any AI Hub task and click the mic button

Voice runs through YOUR workflow's agent — the system prompt, model, and tools you configured in step 2. AI Hub's own chat agent (with its built-in tools and memory) is **not** active during voice mode on Path A. Recommended when you want voice to share the exact behaviour and tools of an existing browser-voice workflow.

#### Path B — Native AI Hub voice

1. Create a **Deepgram** connection holding the API key (Connections → New Connection)
2. Workspace **Admin** → **AI Hub Settings** → select **Native AI Hub voice (Path B)**
3. Pick **Deepgram** from the provider dropdown
4. Paste the Connection ID from step 1
5. Optional: override **Voice ID** (defaults to `aura-asteria-en`)
6. Save
7. Open any AI Hub task and click the mic button

How it works: Deepgram does STT and TTS only; AI Hub's routing agent runs the LLM in between. Your workspace's configured LLM provider (set in admin → AI gateway settings), personal-agent overlay, tools, and chat memory continuity all apply to voice turns the same way they apply to text turns. The session is bound to the AI Hub task's threadId so voice and text turns share the same chat memory in `SPRING_AI_CHAT_MEMORY` — ask the same task something in voice mode, then switch to text, and the LLM remembers the voice turn.

> **Want Deepgram's all-in-one voiceAgent (STT + LLM + TTS) instead?** Use Path A with a Deepgram voiceAgent component in your workflow. That gives you full pipeline control and is the supported way to run "provider runs everything" voice.

Current limitations:
- Sentence-buffered TTS, not token-by-token (v1.3 work).
- No barge-in interruption when the user starts speaking while the assistant is still talking (v1.3 work — Deepgram emits the signal, we just don't act on it yet).
- Only Deepgram is wired; ElevenLabs is planned for v1.3.

## Costs

Deepgram voiceAgent is billed per-minute by Deepgram (not by ByteChef). At time of writing:

- Voice Agent end-to-end: ~$0.13/minute
- Just STT (if you split the chain): ~$0.0043/minute (Nova-2)
- Just TTS (Aura): ~$0.015/minute

A 5-minute voice call = ~$0.65. Production deployments should set per-workspace concurrent-session limits (see [operational docs](./operations.md)) to prevent runaway costs.

ByteChef does not currently meter or charge for voice minutes — your Deepgram bill is your own to manage. Per-workspace billing is planned for v1.1.

## Troubleshooting

### Mic button doesn't appear

Voice requires a `websocketTasks` extension on the workflow trigger. Make sure you added the embedded sub-workflow with the Deepgram task in step 2.

### Mic button is disabled with a tooltip about browser support

Your browser doesn't support `AudioWorklet`. Upgrade to Chrome 66+, Firefox 76+, or Safari 14.1+.

### "Voice requires https://" error

The browser blocks `getUserMedia` on insecure origins. Either run ByteChef behind HTTPS (production) or use `http://localhost` (development).

### Mic permission denied

The browser remembers permission decisions per origin. Reset via the browser's site settings (chrome://settings/content/microphone or equivalent) and reload the page.

### "Invalid or expired session token" on WS upgrade

The voice-session-token is single-use and TTL 60 seconds. If you see this, you're likely opening the WS more than once with the same token, or your client clock is significantly skewed. The hook handles this automatically by minting a fresh token per session start.

### No audio response from the assistant

Most often a mismatch between the trigger's `sampleRate` and the Deepgram action's `audioInputSampleRate` / `audioOutputSampleRate`. Both sides should agree (typically 16000 for input, 24000 for output).

### Session ends after 30 minutes

Server-side max-duration enforcement kicks in at 30 minutes per session. This is a cost-protection guard. Configurable in `WebhookWebSocketHandler` if you self-host.

## Next steps

- [Multi-provider voice chains](./multi-provider-chain.md) — split STT, LLM, and TTS across different providers
- [Voice operations guide](./operations.md) — observability, rate limits, concurrent-session quotas
- [TTS strategy decision doc](../superpowers/specs/2026-05-12-ai-hub-voice-tts-strategy.md) — why AI Hub voice supports both Path A and Path B, and the v1.1 → v1.2 roadmap
- [Path B plan spec](../superpowers/specs/2026-05-12-ai-hub-voice-path-b-plan.md) — six-phase implementation plan for native AI Hub voice (shipped in v1.1)
