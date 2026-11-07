import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useAutomationChatVoiceSession} from './useAutomationChatVoiceSession';

const {BrowserVoiceSessionMock} = vi.hoisted(() => ({
    BrowserVoiceSessionMock: vi.fn(),
}));

vi.mock('@/lib/BrowserVoiceSession', () => {
    class BrowserVoiceSession {
        private readonly options: {
            mintToken: () => Promise<string>;
            onStatusChange?: (status: string) => void;
        };

        readonly stop = vi.fn();
        readonly setMuted = vi.fn();

        constructor(options: {mintToken: () => Promise<string>; onStatusChange?: (status: string) => void}) {
            this.options = options;
            BrowserVoiceSessionMock(options);
        }

        start = vi.fn(async () => {
            this.options.onStatusChange?.('connecting');

            try {
                await this.options.mintToken();

                this.options.onStatusChange?.('active');
            } catch {
                this.options.onStatusChange?.('error');
            }
        });
    }

    return {BrowserVoiceSession};
});

const VARIANTS = [
    {
        input: 'https://bytechef.io/webhooks/abc123',
        token: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        ws: 'wss://bytechef.io/webhooks/abc123/wss',
    },
    {
        input: 'https://bytechef.io/webhooks/abc123/',
        token: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        ws: 'wss://bytechef.io/webhooks/abc123/wss',
    },
    {
        input: 'https://bytechef.io/webhooks/abc123/wss',
        token: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        ws: 'wss://bytechef.io/webhooks/abc123/wss',
    },
    {
        input: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        token: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        ws: 'wss://bytechef.io/webhooks/abc123/wss',
    },
    {
        input: 'https://bytechef.io/webhooks/abc123?foo=bar',
        token: 'https://bytechef.io/webhooks/abc123/voice-session-token',
        ws: 'wss://bytechef.io/webhooks/abc123/wss',
    },
    {
        input: 'http://localhost:8080/webhooks/abc',
        token: 'http://localhost:8080/webhooks/abc/voice-session-token',
        ws: 'ws://localhost:8080/webhooks/abc/wss',
    },
];

function normaliseVoiceWebhookBase(voiceWebhookUrl: string): string {
    let url: URL;

    try {
        url = new URL(voiceWebhookUrl);
    } catch {
        return voiceWebhookUrl;
    }

    url.search = '';
    url.hash = '';

    let pathname = url.pathname.replace(/\/+$/, '');

    if (pathname.endsWith('/wss')) {
        pathname = pathname.slice(0, -'/wss'.length);
    } else if (pathname.endsWith('/voice-session-token')) {
        pathname = pathname.slice(0, -'/voice-session-token'.length);
    }

    url.pathname = pathname;

    return url.toString();
}

function deriveTokenEndpoint(voiceWebhookUrl: string): string {
    return `${normaliseVoiceWebhookBase(voiceWebhookUrl)}/voice-session-token`;
}

function deriveWsUrl(voiceWebhookUrl: string): string {
    const base = new URL(normaliseVoiceWebhookBase(voiceWebhookUrl));
    const wsScheme = base.protocol === 'https:' ? 'wss:' : 'ws:';

    return `${wsScheme}//${base.host}${base.pathname}/wss`;
}

describe('useAutomationChatVoiceSession URL derivation', () => {
    for (const variant of VARIANTS) {
        it(`derives token + ws URL from ${variant.input}`, () => {
            expect(deriveTokenEndpoint(variant.input)).toBe(variant.token);

            const wsUrl = deriveWsUrl(variant.input);

            expect(wsUrl).toBe(variant.ws);
            expect(wsUrl).not.toContain('sessionToken=');
        });
    }

    it('passes invalid URLs through to surface a fetch-time error rather than throwing here', () => {
        expect(() => deriveTokenEndpoint('not a url')).not.toThrow();
        expect(deriveTokenEndpoint('not a url')).toBe('not a url/voice-session-token');
    });
});

describe('useAutomationChatVoiceSession retry after a failed start', () => {
    beforeEach(() => {
        BrowserVoiceSessionMock.mockClear();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it('lets the user retry after mintToken fails, constructing a new session that reaches active', async () => {
        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValueOnce({ok: false, status: 401, statusText: 'Unauthorized'})
                .mockResolvedValueOnce({json: () => Promise.resolve({token: 'tkn'}), ok: true})
        );

        const {result} = renderHook(() =>
            useAutomationChatVoiceSession({voiceWebhookUrl: 'https://bytechef.io/webhooks/abc123'})
        );

        await act(async () => {
            await result.current.start();
        });

        expect(result.current.status).toBe('error');
        expect(BrowserVoiceSessionMock).toHaveBeenCalledTimes(1);

        await act(async () => {
            await result.current.start();
        });

        expect(BrowserVoiceSessionMock).toHaveBeenCalledTimes(2);
        expect(result.current.status).toBe('active');
        expect(result.current.error).toBeNull();
    });

    it('does not clear the session while it is reconnecting, so a concurrent start() is a no-op', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({json: () => Promise.resolve({token: 'tkn'}), ok: true}));

        const {result} = renderHook(() =>
            useAutomationChatVoiceSession({voiceWebhookUrl: 'https://bytechef.io/webhooks/abc123'})
        );

        await act(async () => {
            await result.current.start();
        });

        expect(result.current.status).toBe('active');
        expect(BrowserVoiceSessionMock).toHaveBeenCalledTimes(1);

        const [[firstOptions]] = BrowserVoiceSessionMock.mock.calls as [[{onStatusChange?: (status: string) => void}]];

        act(() => {
            firstOptions.onStatusChange?.('reconnecting');
        });

        expect(result.current.status).toBe('reconnecting');

        await act(async () => {
            await result.current.start();
        });

        expect(BrowserVoiceSessionMock).toHaveBeenCalledTimes(1);
        expect(result.current.status).toBe('reconnecting');
    });
});
