# Testing voice workflows in the editor

The workflow editor's **Playground** panel includes a mic button when the open workflow has a voice
trigger. Click it to speak directly to your workflow without going through the embedded chat widget or
AI Hub — useful for iterating on prompts, models, and turn-taking behavior while building the workflow.

## When the mic button appears

The mic icon only renders when the open workflow has at least one trigger carrying a `websocketTasks`
extension — i.e. the embedded sub-workflow that runs during the voice session. Practically:

- Open a workflow with a `browser/v1/voiceSession` trigger → mic appears
- Open a workflow with no voice trigger → no mic
- Open a brand-new workflow → no mic until you add the trigger and save

Adding the trigger does NOT require deploying the workflow. The editor's test path resolves the **draft**
workflow definition directly via the workflow service, so you can iterate on the trigger configuration
and click mic without publishing.

## How testing differs from production voice

The editor's test path uses a separate WebSocket endpoint:

```
production:   wss://host/api/automation/webhooks/{webhookId}/wss
editor test:  wss://host/api/platform/internal/workflow-tests/{workflowId}/wss
```

Both terminate in the same handler class but key off different identifiers. The editor path:

- Is gated by your ByteChef session cookie (no cross-origin embedding)
- Reads the workflow's draft definition (no publish required)
- Mints session tokens via `POST /api/platform/internal/workflow-tests/{workflowId}/voice-session-token`
- Runs the same audio bridge / inter-task chain wiring as production

What you test in the editor is what your customers will get from the embedded widget or AI Hub.

## Click flow

1. Click the mic icon in the Playground header. The icon flips to a red mic-off when active.
2. The browser prompts for microphone permission. Grant it. (If you've denied it before, you'll need to
   re-grant via browser site settings — see Troubleshooting below.)
3. A status banner appears: "Connecting microphone…"
4. Once the WS is open and Deepgram has accepted the audio: the banner flips to "🎙 Listening… click mic
   to end." (Or "🔊 Assistant is speaking…" once it responds.)
5. Speak. Transcripts and assistant responses appear in the chat thread as they arrive.
6. Click the mic again (now showing the mic-off icon) to end the session.

## Iteration loop

Voice testing is the fastest way to evaluate prompt + model changes:

1. Edit the system prompt or LLM model in the Deepgram voiceAgent task
2. Save the workflow
3. Click mic, speak, listen
4. Repeat

Each click of the mic starts a fresh session — the previous Deepgram WS closes, a new sub-workflow job
spawns, and chat memory resets (unless you've configured a fixed `threadId` on a streaming agent task).
This is the right semantics for prompt iteration; you don't want the previous test's context bleeding
into the next.

For multi-turn conversation memory across sessions, set a stable `threadId` on the agent task (e.g.
`${callSid}` interpolates per-session; `'editor-test'` reuses across all editor sessions).

## Cost during testing

Every editor voice session bills you for real Deepgram usage. A 1-minute test = ~$0.13. The 30-minute
max-session-duration cap applies in the editor too. For prompt iteration, keep sessions short and exit
explicitly via the mic-off button.

## Browser requirements

Same as production voice: Chrome 66+, Firefox 76+, or Safari 14.1+. The mic button is disabled with a
tooltip explaining why on unsupported browsers — no silent failures.

## Troubleshooting

### Mic button doesn't appear

The workflow doesn't have a voice trigger. Add a `browser/v1/voiceSession` trigger with an embedded
sub-workflow under `websocketTasks`. See the [voice quickstart](./quickstart.md).

### Mic button greyed out

Either the browser is unsupported (hover for the tooltip explaining why) or the workflow hasn't been
saved yet (the test path requires a persisted workflow id).

### "Workflow has no websocketTasks defined on its trigger"

The trigger exists but the `websocketTasks` extension is missing or empty. Re-open the trigger config
and add the sub-workflow under "Real-Time Workflow".

### Audio works but no response

Almost always a sample-rate mismatch. The trigger's `sampleRate` (16000 by default) must match the
Deepgram task's `audioInputSampleRate`. The output sample rate (`audioOutputSampleRate`, typically
24000) is independent.

### Permission denied / mic button does nothing

The browser remembers permission decisions per origin. Reset via:
- Chrome: chrome://settings/content/microphone
- Firefox: about:preferences#privacy → Permissions → Microphone
- Safari: Safari → Settings → Websites → Microphone

Reload after resetting.

### Session closes after 30 minutes

The server enforces a 30-minute max-session-duration to bound Deepgram cost. If you're testing long
conversations, close and re-open the mic between iterations.

## What's next

- [Voice quickstart](./quickstart.md) — initial workflow setup
- [Multi-provider chains](./multi-provider-chain.md) — split STT/agent/TTS across providers
