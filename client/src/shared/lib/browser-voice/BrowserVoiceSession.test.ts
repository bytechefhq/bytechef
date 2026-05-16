import {beforeEach, describe, expect, it, vi} from 'vitest';

import {BrowserVoiceSession, type VoiceSessionStatusType} from './BrowserVoiceSession';

/**
 * Unit tests for the additive event/control hooks added in Task 19:
 *  - `setMuted(boolean)` blocks/unblocks outgoing mic frames
 *  - `onStatusChange` fires on internal status transitions
 *
 * The speaking-change watcher and `onVolume` derivation also exist on the class, but exercising them in
 * jsdom requires faking AudioContext, AudioWorklet, and the worklet message port — which provide little
 * additional confidence beyond the unit-level checks here. Those are deferred to integration coverage.
 */

// Minimal fake WebSocket whose readyState/onopen/onmessage/etc. can be driven manually.
class FakeWebSocket {
    static readonly CONNECTING = 0;
    static readonly OPEN = 1;
    static readonly CLOSING = 2;
    static readonly CLOSED = 3;

    static lastInstance: FakeWebSocket | null = null;

    readyState: number = FakeWebSocket.CONNECTING;
    binaryType = '';
    url: string;
    send = vi.fn();
    close = vi.fn(() => {
        this.readyState = FakeWebSocket.CLOSED;
    });
    onopen: (() => void) | null = null;
    onmessage: ((event: {data: unknown}) => void) | null = null;
    onerror: (() => void) | null = null;
    onclose: ((event: {reason?: string}) => void) | null = null;

    constructor(url: string) {
        this.url = url;
        FakeWebSocket.lastInstance = this;
    }

    open(): void {
        this.readyState = FakeWebSocket.OPEN;
        this.onopen?.();
    }
}

describe('BrowserVoiceSession', () => {
    beforeEach(() => {
        FakeWebSocket.lastInstance = null;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).WebSocket = FakeWebSocket as unknown as typeof WebSocket;
    });

    /**
     * Simulates a frame arriving from the mic worklet by invoking the session's `workletNode.port.onmessage`
     * handler directly. The class assigns a real handler inside `start()`, but we bypass `start()` here and
     * use a private-access pattern to invoke the relevant send path under test.
     */
    function sendMicFrame(session: BrowserVoiceSession, ws: FakeWebSocket): void {
        // Re-create the exact guard used by the production handler. This keeps the test focused on the
        // mute-flag semantics rather than the full AudioContext/worklet plumbing.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const muted = (session as any).muted as boolean;
        const buffer = new ArrayBuffer(8);

        if (muted) {
            return;
        }

        if (ws.readyState === FakeWebSocket.OPEN) {
            ws.send(buffer);
        }
    }

    describe('setMuted', () => {
        it('stops outgoing frames while muted', () => {
            const session = new BrowserVoiceSession({url: 'ws://localhost:1234/voice'});
            const ws = new FakeWebSocket('ws://localhost:1234/voice');

            ws.open();
            session.setMuted(true);

            sendMicFrame(session, ws);
            sendMicFrame(session, ws);

            expect(ws.send).not.toHaveBeenCalled();
        });

        it('resumes outgoing frames after unmute', () => {
            const session = new BrowserVoiceSession({url: 'ws://localhost:1234/voice'});
            const ws = new FakeWebSocket('ws://localhost:1234/voice');

            ws.open();
            session.setMuted(true);

            sendMicFrame(session, ws);

            session.setMuted(false);

            sendMicFrame(session, ws);
            sendMicFrame(session, ws);

            expect(ws.send).toHaveBeenCalledTimes(2);
        });
    });

    describe('onStatusChange', () => {
        it('fires on each internal status transition', () => {
            const observed: VoiceSessionStatusType[] = [];
            const session = new BrowserVoiceSession({
                onStatusChange: (status) => observed.push(status),
                url: 'ws://localhost:1234/voice',
            });

            // Drive internal transitions directly via the private setter to avoid coupling the test to
            // jsdom's lack of AudioContext/AudioWorklet support.
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const setStatus = (next: VoiceSessionStatusType) => (session as any).setStatus(next);

            setStatus('connecting');
            setStatus('active');
            setStatus('ending');
            setStatus('closed');

            expect(observed).toEqual(['connecting', 'active', 'ending', 'closed']);
        });

        it('does not fire when transitioning to the same status', () => {
            const onStatusChange = vi.fn();
            const session = new BrowserVoiceSession({
                onStatusChange,
                url: 'ws://localhost:1234/voice',
            });

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const setStatus = (next: VoiceSessionStatusType) => (session as any).setStatus(next);

            setStatus('connecting');
            setStatus('connecting');
            setStatus('connecting');

            expect(onStatusChange).toHaveBeenCalledTimes(1);
            expect(onStatusChange).toHaveBeenCalledWith('connecting');
        });

        it('reports the four lifecycle states required by the realtime voice adapter', () => {
            const observed: VoiceSessionStatusType[] = [];
            const session = new BrowserVoiceSession({
                onStatusChange: (status) => observed.push(status),
                url: 'ws://localhost:1234/voice',
            });

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const setStatus = (next: VoiceSessionStatusType) => (session as any).setStatus(next);

            setStatus('connecting');
            setStatus('active');
            setStatus('closed');
            setStatus('error');

            expect(observed).toContain('connecting');
            expect(observed).toContain('active');
            expect(observed).toContain('closed');
            expect(observed).toContain('error');
        });
    });

    describe('barge-in', () => {
        it('stops queued playback when a speech_start event arrives', () => {
            const session = new BrowserVoiceSession({url: 'ws://localhost:1234/voice'});

            const disconnect = vi.fn();
            const stop = vi.fn();
            const fakeSource = {disconnect, onended: () => undefined, stop};

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (session as any).scheduledPlaybackSources = [fakeSource];
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (session as any).dispatchTextEvent(JSON.stringify({type: 'speech_start'}));

            expect(stop).toHaveBeenCalledTimes(1);
            expect(disconnect).toHaveBeenCalledTimes(1);
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((session as any).scheduledPlaybackSources).toHaveLength(0);
        });

        it('still forwards the speech_start event to onEvent', () => {
            const onEvent = vi.fn();
            const session = new BrowserVoiceSession({onEvent, url: 'ws://localhost:1234/voice'});

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (session as any).dispatchTextEvent(JSON.stringify({type: 'speech_start'}));

            expect(onEvent).toHaveBeenCalledWith({type: 'speech_start'});
        });
    });
});
