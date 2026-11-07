import {BrowserVoiceSession} from './BrowserVoiceSession';
import {createVoiceSession} from '@assistant-ui/react';

import {voiceActivityStore} from './useVoiceActivityStore';

import type {RealtimeVoiceAdapter} from '@assistant-ui/react';

interface AdapterConfigI {
    sampleRate?: 16000 | 24000;
    tokenUrl: string;
}

function deriveWsUrl(tokenUrl: string): string {
    let baseUrl: URL;

    try {
        baseUrl = new URL(tokenUrl);
    } catch {
        baseUrl = new URL(tokenUrl, window.location.origin);
    }

    let pathname = baseUrl.pathname.replace(/\/+$/, '');

    if (pathname.endsWith('/voice-session-token')) {
        pathname = pathname.slice(0, -'/voice-session-token'.length);
    }

    baseUrl.pathname = `${pathname}/wss`;
    baseUrl.protocol = baseUrl.protocol === 'https:' ? 'wss:' : 'ws:';

    return baseUrl.toString();
}

export class ByteChefRealtimeVoiceAdapter implements RealtimeVoiceAdapter {
    constructor(private readonly config: AdapterConfigI) {}

    connect(options: {abortSignal?: AbortSignal}): RealtimeVoiceAdapter.Session {
        const generation = voiceActivityStore.beginSession();

        return createVoiceSession(options, async (helpers) => {
            const mintToken = async (): Promise<string> => {
                const response = await fetch(this.config.tokenUrl, {method: 'POST'});

                if (!response.ok) throw new Error(`Voice token request failed: ${response.statusText}`);

                const {token} = (await response.json()) as {token: string};

                return token;
            };

            const session = new BrowserVoiceSession({
                mintToken,
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

                    if (event.type === 'tool_call') {
                        voiceActivityStore.setActivity({tool: String(event.name ?? '')}, generation);
                    }

                    if (event.type === 'tool_result') {
                        voiceActivityStore.setActivity('idle', generation);
                    }

                    if (event.type === 'session_end') {
                        voiceActivityStore.setEndReason(
                            typeof event.reason === 'string' ? event.reason : null,
                            generation
                        );

                        helpers.end('finished');
                    }
                },
                onSpeakingChange: (isAssistantSpeaking) =>
                    helpers.emitMode(isAssistantSpeaking ? 'speaking' : 'listening'),
                onStatusChange: (status) => {
                    if (status === 'active') {
                        helpers.setStatus({type: 'running'});
                        voiceActivityStore.setActivity('idle', generation);
                    }

                    if (status === 'reconnecting') {
                        helpers.setStatus({type: 'running'});
                        helpers.emitMode('listening');
                        voiceActivityStore.setActivity('reconnecting', generation);
                    }

                    if (status === 'error' || status === 'closed') {
                        voiceActivityStore.setActivity('idle', generation);
                    }

                    if (status === 'error') helpers.end('error');
                },
                onVolume: (level) => helpers.emitVolume(level),
                sampleRate: this.config.sampleRate ?? 16000,
                url: deriveWsUrl(this.config.tokenUrl),
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

export function createWebhookVoiceAdapter(webhookUrl: string, sampleRate?: 16000 | 24000): RealtimeVoiceAdapter {
    return new ByteChefRealtimeVoiceAdapter({sampleRate, tokenUrl: `${webhookUrl}/voice-session-token`});
}
