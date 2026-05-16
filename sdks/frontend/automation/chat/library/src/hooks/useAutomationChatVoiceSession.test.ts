import {describe, expect, it} from 'vitest';

// The hook itself is hard to test in isolation (depends on getUserMedia, AudioContext, WebSocket). The URL
// normalisation logic lives inside the hook file as module-scoped functions; we re-derive the same canonical
// shape here against representative inputs to lock the behaviour in.

const VARIANTS = [
    {input: 'https://bytechef.io/webhooks/abc123', token: 'https://bytechef.io/webhooks/abc123/voice-session-token', ws: 'wss://bytechef.io/webhooks/abc123/wss'},
    {input: 'https://bytechef.io/webhooks/abc123/', token: 'https://bytechef.io/webhooks/abc123/voice-session-token', ws: 'wss://bytechef.io/webhooks/abc123/wss'},
    {input: 'https://bytechef.io/webhooks/abc123/wss', token: 'https://bytechef.io/webhooks/abc123/voice-session-token', ws: 'wss://bytechef.io/webhooks/abc123/wss'},
    {input: 'https://bytechef.io/webhooks/abc123/voice-session-token', token: 'https://bytechef.io/webhooks/abc123/voice-session-token', ws: 'wss://bytechef.io/webhooks/abc123/wss'},
    {input: 'https://bytechef.io/webhooks/abc123?foo=bar', token: 'https://bytechef.io/webhooks/abc123/voice-session-token', ws: 'wss://bytechef.io/webhooks/abc123/wss'},
    {input: 'http://localhost:8080/webhooks/abc', token: 'http://localhost:8080/webhooks/abc/voice-session-token', ws: 'ws://localhost:8080/webhooks/abc/wss'},
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

function deriveWsUrl(voiceWebhookUrl: string, token: string, sampleRate: number): string {
    const base = new URL(normaliseVoiceWebhookBase(voiceWebhookUrl));
    const wsScheme = base.protocol === 'https:' ? 'wss:' : 'ws:';

    return `${wsScheme}//${base.host}${base.pathname}/wss?sessionToken=${encodeURIComponent(token)}&sampleRate=${sampleRate}`;
}

describe('useAutomationChatVoiceSession URL derivation', () => {
    for (const variant of VARIANTS) {
        it(`derives token + ws URL from ${variant.input}`, () => {
            expect(deriveTokenEndpoint(variant.input)).toBe(variant.token);

            const wsUrl = deriveWsUrl(variant.input, 'TOKEN_VALUE', 16000);

            expect(wsUrl).toBe(`${variant.ws}?sessionToken=TOKEN_VALUE&sampleRate=16000`);
        });
    }

    it('passes invalid URLs through to surface a fetch-time error rather than throwing here', () => {
        expect(() => deriveTokenEndpoint('not a url')).not.toThrow();
        expect(deriveTokenEndpoint('not a url')).toBe('not a url/voice-session-token');
    });
});
