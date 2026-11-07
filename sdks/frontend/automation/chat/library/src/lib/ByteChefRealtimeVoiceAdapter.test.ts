import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ByteChefRealtimeVoiceAdapter} from './ByteChefRealtimeVoiceAdapter';
import {voiceActivityStore} from './useVoiceActivityStore';

import type {VoiceEventI, VoiceSessionStatusType} from './BrowserVoiceSession';
import type {RealtimeVoiceAdapter} from '@assistant-ui/react';

const {BrowserVoiceSessionMock} = vi.hoisted(() => ({
    BrowserVoiceSessionMock: vi.fn(),
}));

vi.mock('./BrowserVoiceSession', () => {
    class BrowserVoiceSession {
        readonly start = vi.fn().mockResolvedValue(undefined);
        readonly stop = vi.fn();
        readonly setMuted = vi.fn();

        constructor(options: {onStatusChange?: (status: string) => void}) {
            BrowserVoiceSessionMock(options);

            setTimeout(() => options.onStatusChange?.('active'), 0);
        }
    }

    return {BrowserVoiceSession};
});

async function connectAndAwaitActive(adapter: ByteChefRealtimeVoiceAdapter) {
    const session = adapter.connect({});
    const statuses: RealtimeVoiceAdapter.Status[] = [];

    session.onStatusChange((status) => statuses.push(status));

    await vi.waitFor(() => {
        expect(statuses).toContainEqual({type: 'running'});
    });

    const [passedOptions] = BrowserVoiceSessionMock.mock.calls.at(-1) as [
        {onEvent?: (event: VoiceEventI) => void; onStatusChange?: (status: VoiceSessionStatusType) => void},
    ];

    return {passedOptions, session};
}

describe('ByteChefRealtimeVoiceAdapter', () => {
    beforeEach(() => {
        voiceActivityStore.reset();
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({json: () => Promise.resolve({token: 'tkn'}), ok: true}));
        BrowserVoiceSessionMock.mockClear();
    });

    it('opens the socket without the token in the base url and mints lazily', async () => {
        const adapter = new ByteChefRealtimeVoiceAdapter({
            tokenUrl: 'http://host/webhooks/abc/voice-session-token',
        });

        const session = adapter.connect({});
        const statuses: RealtimeVoiceAdapter.Status[] = [];

        session.onStatusChange((status) => statuses.push(status));

        await vi.waitFor(() => {
            expect(BrowserVoiceSessionMock).toHaveBeenCalledWith(
                expect.objectContaining({mintToken: expect.any(Function), url: 'ws://host/webhooks/abc/wss'})
            );
        });

        const [[passedOptions]] = BrowserVoiceSessionMock.mock.calls;

        expect(passedOptions.url).not.toContain('sessionToken=');
        expect(passedOptions.url).not.toContain('resumeSessionId=');

        expect(global.fetch).not.toHaveBeenCalled();

        const token = await passedOptions.mintToken();

        expect(token).toBe('tkn');
        expect(global.fetch).toHaveBeenCalledWith(
            'http://host/webhooks/abc/voice-session-token',
            expect.objectContaining({method: 'POST'})
        );

        await vi.waitFor(() => {
            expect(statuses).toContainEqual({type: 'running'});
        });
    });

    it('writes tool activity to the voice activity store instead of emitting an assistant transcript', async () => {
        const adapter = new ByteChefRealtimeVoiceAdapter({tokenUrl: 'http://host/webhooks/abc/voice-session-token'});
        const {passedOptions, session} = await connectAndAwaitActive(adapter);
        const transcripts: RealtimeVoiceAdapter.TranscriptItem[] = [];

        session.onTranscript((item) => transcripts.push(item));

        passedOptions.onEvent?.({name: 'lookupOrder', type: 'tool_call'});

        expect(voiceActivityStore.getState().activity).toEqual({tool: 'lookupOrder'});
        expect(transcripts).toEqual([]);

        passedOptions.onEvent?.({name: 'lookupOrder', type: 'tool_result'});

        expect(voiceActivityStore.getState().activity).toBe('idle');
        expect(transcripts).toEqual([]);
    });

    it('resets a previous session end reason on connect, and records the reason on session_end', async () => {
        voiceActivityStore.setEndReason('previous_reason');

        const adapter = new ByteChefRealtimeVoiceAdapter({tokenUrl: 'http://host/webhooks/abc/voice-session-token'});

        const session = adapter.connect({});

        expect(voiceActivityStore.getState().endReason).toBeNull();

        const statuses: RealtimeVoiceAdapter.Status[] = [];

        session.onStatusChange((status) => statuses.push(status));

        await vi.waitFor(() => {
            expect(statuses).toContainEqual({type: 'running'});
        });

        const [[passedOptions]] = BrowserVoiceSessionMock.mock.calls as [[{onEvent?: (event: VoiceEventI) => void}]];

        passedOptions.onEvent?.({reason: 'silence_timeout', type: 'session_end'});

        expect(voiceActivityStore.getState().endReason).toBe('silence_timeout');
    });

    it('reflects reconnecting and active status changes in the activity store', async () => {
        const adapter = new ByteChefRealtimeVoiceAdapter({tokenUrl: 'http://host/webhooks/abc/voice-session-token'});
        const {passedOptions} = await connectAndAwaitActive(adapter);

        passedOptions.onStatusChange?.('reconnecting');

        expect(voiceActivityStore.getState().activity).toBe('reconnecting');

        passedOptions.onStatusChange?.('active');

        expect(voiceActivityStore.getState().activity).toBe('idle');
    });

    it('clears a stuck "reconnecting" activity once a reconnect attempt ends in error', async () => {
        const adapter = new ByteChefRealtimeVoiceAdapter({tokenUrl: 'http://host/webhooks/abc/voice-session-token'});
        const {passedOptions} = await connectAndAwaitActive(adapter);

        passedOptions.onStatusChange?.('reconnecting');

        expect(voiceActivityStore.getState().activity).toBe('reconnecting');

        passedOptions.onStatusChange?.('error');

        expect(voiceActivityStore.getState().activity).toBe('idle');
    });

    it('clears a stuck tool-call activity when the session closes, preserving the end reason', async () => {
        const adapter = new ByteChefRealtimeVoiceAdapter({tokenUrl: 'http://host/webhooks/abc/voice-session-token'});
        const {passedOptions} = await connectAndAwaitActive(adapter);

        passedOptions.onEvent?.({name: 'lookupOrder', type: 'tool_call'});

        expect(voiceActivityStore.getState().activity).toEqual({tool: 'lookupOrder'});

        passedOptions.onEvent?.({reason: 'silence_timeout', type: 'session_end'});
        passedOptions.onStatusChange?.('closed');

        expect(voiceActivityStore.getState().activity).toBe('idle');
        expect(voiceActivityStore.getState().endReason).toBe('silence_timeout');
    });
});
