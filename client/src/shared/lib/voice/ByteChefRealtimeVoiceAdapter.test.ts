import {describe, expect, it, vi} from 'vitest';

import {ByteChefRealtimeVoiceAdapter} from './ByteChefRealtimeVoiceAdapter';

import type {RealtimeVoiceAdapter} from '@assistant-ui/react';

vi.mock('@/shared/lib/browser-voice/BrowserVoiceSession', () => {
    class BrowserVoiceSession {
        readonly start = vi.fn().mockResolvedValue(undefined);
        readonly stop = vi.fn();
        readonly setMuted = vi.fn();

        constructor(options: {onStatusChange?: (status: string) => void}) {
            setTimeout(() => options.onStatusChange?.('active'), 0);
        }
    }

    return {BrowserVoiceSession};
});

describe('ByteChefRealtimeVoiceAdapter', () => {
    it('mints a token, opens a session, and emits running status', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            json: () => Promise.resolve({token: 'tkn'}),
            ok: true,
        });

        vi.stubGlobal('fetch', fetchMock);

        const adapter = new ByteChefRealtimeVoiceAdapter({
            tokenUrl: 'http://server/webhooks/x/voice-session-token',
        });

        const session = adapter.connect({});
        const statuses: RealtimeVoiceAdapter.Status[] = [];

        session.onStatusChange((status) => statuses.push(status));

        // The running status arrives through an async chain (token fetch -> session construction -> queued
        // status emit); a fixed sleep races it under full-suite load, so wait for the condition instead.
        await vi.waitFor(() => {
            expect(statuses).toContainEqual({type: 'running'});
        });

        expect(fetchMock).toHaveBeenCalledWith(
            'http://server/webhooks/x/voice-session-token',
            expect.objectContaining({method: 'POST'})
        );
    });
});
