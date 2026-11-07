import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {usePushToTalk} from './usePushToTalk';

class FakeMediaRecorder {
    static isTypeSupported = vi.fn(() => true);

    ondataavailable: ((event: {data: Blob}) => void) | null = null;
    onstop: (() => void) | null = null;
    state = 'inactive';

    constructor(
        public stream: unknown,
        public options: {mimeType: string}
    ) {}

    start(): void {
        this.state = 'recording';
    }

    stop(): void {
        this.state = 'inactive';
        this.ondataavailable?.({data: new Blob(['audio'], {type: 'audio/webm'})});
        this.onstop?.();
    }
}

describe('usePushToTalk', () => {
    beforeEach(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).MediaRecorder = FakeMediaRecorder;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis.navigator as any).mediaDevices = {
            getUserMedia: vi.fn().mockResolvedValue({getTracks: () => [{stop: vi.fn()}]}),
        };
        globalThis.fetch = vi.fn().mockResolvedValue({
            json: async () => ({text: 'hello world'}),
            ok: true,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        }) as any;
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('records, uploads the clip, and reports the transcript', async () => {
        const onTranscript = vi.fn();
        const {result} = renderHook(() => usePushToTalk({onTranscript}));

        await act(async () => {
            await result.current.start();
        });

        expect(result.current.isRecording).toBe(true);

        await act(async () => {
            result.current.stop();

            await new Promise((resolve) => setTimeout(resolve, 10));
        });

        expect(globalThis.fetch).toHaveBeenCalledWith(
            '/api/platform/internal/ai/transcribe',
            expect.objectContaining({method: 'POST'})
        );
        expect(onTranscript).toHaveBeenCalledWith('hello world');
    });

    it('reports an error when the request fails', async () => {
        globalThis.fetch = vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        }) as any;

        const onError = vi.fn();
        const onTranscript = vi.fn();
        const {result} = renderHook(() => usePushToTalk({onError, onTranscript}));

        await act(async () => {
            await result.current.start();
        });

        await act(async () => {
            result.current.stop();

            await new Promise((resolve) => setTimeout(resolve, 10));
        });

        expect(onError).toHaveBeenCalled();
        expect(onTranscript).not.toHaveBeenCalled();
    });
});
