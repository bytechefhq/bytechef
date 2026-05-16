import {BrowserVoiceSession} from './BrowserVoiceSession';
import {createVoiceSession} from '@assistant-ui/react';

import type {RealtimeVoiceAdapter} from '@assistant-ui/react';

interface AdapterConfigI {
    sampleRate?: 16000 | 24000;
    tokenUrl: string;
}

/**
 * Derives the WebSocket URL from the token endpoint URL.
 *
 * Given a tokenUrl like {@code http://host/webhooks/{id}/voice-session-token}, returns
 * {@code ws://host/webhooks/{id}/wss?sessionToken=<token>}.
 */
function deriveWsUrl(tokenUrl: string, token: string, sampleRate: number): string {
    let baseUrl: URL;

    try {
        baseUrl = new URL(tokenUrl);
    } catch {
        // Relative URL — use current origin
        baseUrl = new URL(tokenUrl, window.location.origin);
    }

    let pathname = baseUrl.pathname.replace(/\/+$/, '');

    if (pathname.endsWith('/voice-session-token')) {
        pathname = pathname.slice(0, -'/voice-session-token'.length);
    }

    const wsScheme = baseUrl.protocol === 'https:' ? 'wss:' : 'ws:';

    return `${wsScheme}//${baseUrl.host}${pathname}/wss?sessionToken=${encodeURIComponent(token)}&sampleRate=${sampleRate}`;
}

export class ByteChefRealtimeVoiceAdapter implements RealtimeVoiceAdapter {
    constructor(private readonly config: AdapterConfigI) {}

    connect(options: {abortSignal?: AbortSignal}): RealtimeVoiceAdapter.Session {
        return createVoiceSession(options, async (helpers) => {
            const response = await fetch(this.config.tokenUrl, {method: 'POST'});

            if (!response.ok) throw new Error(`Voice token request failed: ${response.statusText}`);

            const {token} = (await response.json()) as {token: string};

            const sampleRate = this.config.sampleRate ?? 16000;
            const wsUrl = deriveWsUrl(this.config.tokenUrl, token, sampleRate);

            const session = new BrowserVoiceSession({
                onEvent: (event) => {
                    if (event.type === 'transcript_interim') {
                        helpers.emitTranscript({isFinal: false, role: 'user', text: event.text ?? ''});
                    }

                    if (event.type === 'transcript_final') {
                        helpers.emitTranscript({isFinal: true, role: 'user', text: event.text ?? ''});
                    }

                    if (event.type === 'assistant_text') {
                        helpers.emitTranscript({
                            isFinal: !!event.done,
                            role: 'assistant',
                            text: event.text ?? '',
                        });
                    }
                },
                onSpeakingChange: (isAssistantSpeaking) =>
                    helpers.emitMode(isAssistantSpeaking ? 'speaking' : 'listening'),
                onStatusChange: (status) => {
                    if (status === 'active') helpers.setStatus({type: 'running'});

                    if (status === 'closed') helpers.end('finished');

                    if (status === 'error') helpers.end('error');
                },
                onVolume: (level) => helpers.emitVolume(level),
                sampleRate,
                url: wsUrl,
            });

            await session.start();

            return {
                disconnect: () => session.stop(),
                mute: () => session.setMuted(true),
                unmute: () => session.setMuted(false),
            };
        });
    }
}

export function createWebhookVoiceAdapter(webhookUrl: string): RealtimeVoiceAdapter {
    return new ByteChefRealtimeVoiceAdapter({tokenUrl: `${webhookUrl}/voice-session-token`});
}
