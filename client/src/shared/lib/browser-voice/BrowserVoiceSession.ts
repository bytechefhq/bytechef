/**
 * Pure (no-React) session class that bridges a browser mic to a ByteChef voice WebSocket.
 *
 * Responsibilities:
 *   - request mic access (`getUserMedia`)
 *   - load the PCM-16 downsampler worklet and stream audio frames as WS binary messages
 *   - decode incoming binary frames as PCM16 LE and play them through an AudioContext
 *   - route incoming text frames as JSON events to the caller
 *   - tear everything down on stop()
 *
 * Used by:
 *   - the workflow editor's `WorkflowTestChatPanel` (via `useWorkflowTestVoiceSession`)
 *   - the AI Hub (via `useAiHubVoiceSession`)
 *   - the external chat widget bundle (a sibling copy lives in
 *     `sdks/frontend/automation/chat/library/src/lib/BrowserVoiceSession.ts`)
 *
 * The code duplication into the widget bundle is intentional: customers cannot reach into `@/shared` from the
 * published npm package. The two files must stay in sync.
 */

export type VoiceEventType =
    'connected' | 'transcript_interim' | 'transcript_final' | 'assistant_text' | 'speech_start' | 'error';

export interface VoiceEventI {
    type: VoiceEventType | string;
    text?: string;
    turnId?: string;
    done?: boolean;
    message?: string;
    [k: string]: unknown;
}

export type VoiceSessionStatusType = 'idle' | 'connecting' | 'active' | 'ending' | 'closed' | 'error';

export interface BrowserVoiceSessionOptionsI {
    url: string;
    sampleRate?: number;
    workletPath?: string;
    onEvent?: (event: VoiceEventI) => void;
    onStatusChange?: (status: VoiceSessionStatusType) => void;
    onSpeakingChange?: (speaking: boolean) => void;
    onVolume?: (level: number) => void;
}

const DEFAULT_SAMPLE_RATE = 16000;
const PLAYBACK_LEAD_AHEAD_SECONDS = 0.02;
const SPEAKING_HYSTERESIS_MS = 300;
const SPEAKING_CHECK_INTERVAL_MS = 100;
const VOLUME_EMIT_THROTTLE_MS = 50;

/**
 * Returns null when voice is supported in this browser, or a human-readable reason when it is not.
 *
 * <p>
 * Voice requires:
 *   - {@code AudioContext} (Chrome 35+, Firefox 25+, Safari 14.1+)
 *   - {@code AudioWorklet} (Chrome 66+, Firefox 76+, Safari 14.1+)
 *   - {@code navigator.mediaDevices.getUserMedia} (all modern browsers but requires HTTPS / localhost)
 *   - {@code WebSocket} (universal in modern browsers)
 *
 * Without this check, calling {@code start()} on an unsupported browser fails silently at the worklet-load
 * step with a generic Promise rejection that's hard to surface to the user. UI consumers should call this at
 * mount time and disable the mic button with the returned reason when non-null.
 */
export function checkVoiceSupport(): string | null {
    if (typeof window === 'undefined') {
        return 'Voice is not available outside a browser environment.';
    }

    if (
        typeof window.AudioContext === 'undefined' &&
        typeof (window as {webkitAudioContext?: unknown}).webkitAudioContext === 'undefined'
    ) {
        return 'Your browser does not support the Web Audio API. Please use Chrome 35+, Firefox 25+, or Safari 14.1+.';
    }

    if (typeof window.AudioWorklet === 'undefined') {
        return 'Your browser does not support AudioWorklet (required for voice). Please use Chrome 66+, Firefox 76+, or Safari 14.1+.';
    }

    if (
        typeof navigator === 'undefined' ||
        !navigator.mediaDevices ||
        typeof navigator.mediaDevices.getUserMedia !== 'function'
    ) {
        return 'Your browser does not support microphone access (getUserMedia). Please use Chrome, Firefox, or Safari.';
    }

    if (typeof WebSocket === 'undefined') {
        return 'Your browser does not support WebSocket (required for voice). Please use Chrome, Firefox, or Safari.';
    }

    // getUserMedia requires a secure context (HTTPS or localhost). Browsers don't expose this as a
    // first-class check, but window.isSecureContext is the canonical signal.
    if (typeof window.isSecureContext !== 'undefined' && !window.isSecureContext) {
        return 'Voice requires a secure (https://) context. Browsers block microphone access on insecure origins.';
    }

    return null;
}

export class BrowserVoiceSession {
    private readonly url: string;
    private readonly sampleRate: number;
    private readonly workletPath: string;
    private readonly onEvent?: (event: VoiceEventI) => void;
    private readonly onStatusChange?: (status: VoiceSessionStatusType) => void;
    private readonly onSpeakingChange?: (speaking: boolean) => void;
    private readonly onVolume?: (level: number) => void;

    private ws: WebSocket | null = null;
    private audioContext: AudioContext | null = null;
    private mediaStream: MediaStream | null = null;
    private workletNode: AudioWorkletNode | null = null;
    private playbackCursorSeconds = 0;
    private scheduledPlaybackSources: AudioBufferSourceNode[] = [];
    private speaking = false;
    private status: VoiceSessionStatusType = 'idle';
    private lastServerError: string | null = null;
    private lastAssistantFrameAtRef = 0;
    private speakingChangeIntervalId: ReturnType<typeof setInterval> | null = null;
    private muted = false;
    private lastVolumeEmitAt = 0;

    constructor(options: BrowserVoiceSessionOptionsI) {
        this.url = options.url;
        this.sampleRate = options.sampleRate ?? DEFAULT_SAMPLE_RATE;
        this.workletPath = options.workletPath ?? '/mic-worklet.js';
        this.onEvent = options.onEvent;
        this.onStatusChange = options.onStatusChange;
        this.onSpeakingChange = options.onSpeakingChange;
        this.onVolume = options.onVolume;
    }

    getStatus(): VoiceSessionStatusType {
        return this.status;
    }

    /**
     * Mute or unmute the outgoing mic stream.
     *
     * While muted, captured mic frames are dropped before being forwarded to the WebSocket. The session stays
     * fully open and the server keeps receiving silence (no frames), so assistant audio playback continues
     * uninterrupted. Toggling has no effect on incoming audio.
     */
    setMuted(muted: boolean): void {
        this.muted = muted;
    }

    async start(): Promise<void> {
        if (this.status !== 'idle' && this.status !== 'closed' && this.status !== 'error') {
            return;
        }

        this.setStatus('connecting');

        try {
            this.mediaStream = await navigator.mediaDevices.getUserMedia({
                audio: {echoCancellation: true, noiseSuppression: true},
            });

            this.audioContext = new AudioContext();

            await this.audioContext.audioWorklet.addModule(this.workletPath);

            this.workletNode = new AudioWorkletNode(this.audioContext, 'pcm16-downsampler', {
                processorOptions: {frameMs: 20, targetSampleRate: this.sampleRate},
            });

            const sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);

            sourceNode.connect(this.workletNode);

            this.ws = new WebSocket(this.url);
            this.ws.binaryType = 'arraybuffer';

            this.workletNode.port.onmessage = (event) => {
                const buffer = event.data as ArrayBuffer;

                this.emitVolumeFromFrame(buffer);

                if (this.muted) {
                    return;
                }

                if (this.ws?.readyState === WebSocket.OPEN) {
                    this.ws.send(buffer);
                }
            };

            this.ws.onopen = () => {
                this.setStatus('active');
                this.startSpeakingChangeWatcher();
            };

            this.ws.onmessage = (event) => {
                if (typeof event.data === 'string') {
                    this.dispatchTextEvent(event.data);
                } else if (event.data instanceof ArrayBuffer) {
                    this.enqueuePlayback(event.data);
                }
            };

            this.ws.onerror = () => {
                this.onEvent?.({message: this.lastServerError ?? 'WebSocket error', type: 'error'});

                this.setStatus('error');
            };

            this.ws.onclose = (event) => {
                if (this.status !== 'ending') {
                    // Servers emit a JSON `error` frame right before closing on policy violations
                    // (bad token, missing websocketTasks, etc). lastServerError captures that frame
                    // so the consumer sees the reason instead of a bare "WebSocket error" or "closed".
                    const reason = this.lastServerError ?? event.reason;

                    if (reason) {
                        this.onEvent?.({message: reason, type: 'error'});
                        this.setStatus('error');
                    } else {
                        this.setStatus('closed');
                    }
                }

                this.cleanup();
            };
        } catch (error) {
            this.onEvent?.({
                message: error instanceof Error ? error.message : 'Failed to start voice session',
                type: 'error',
            });

            this.setStatus('error');
            this.cleanup();
        }
    }

    stop(): void {
        if (this.status === 'idle' || this.status === 'closed') {
            return;
        }

        this.setStatus('ending');

        try {
            this.ws?.send(JSON.stringify({action: 'end', type: 'control'}));
        } catch {
            // ignore — closing anyway
        }

        this.ws?.close();
        this.cleanup();
        this.setStatus('closed');
    }

    private cleanup(): void {
        this.stopSpeakingChangeWatcher();

        if (this.mediaStream) {
            for (const track of this.mediaStream.getTracks()) {
                track.stop();
            }

            this.mediaStream = null;
        }

        if (this.workletNode) {
            this.workletNode.port.onmessage = null;
            this.workletNode.disconnect();
            this.workletNode = null;
        }

        if (this.audioContext) {
            void this.audioContext.close();
            this.audioContext = null;
        }

        this.ws = null;
        this.scheduledPlaybackSources = [];
        this.playbackCursorSeconds = 0;
        this.lastAssistantFrameAtRef = 0;
        this.lastVolumeEmitAt = 0;
        this.setSpeaking(false);
    }

    private setStatus(next: VoiceSessionStatusType): void {
        if (this.status === next) {
            return;
        }

        this.status = next;

        this.onStatusChange?.(next);
    }

    private setSpeaking(next: boolean): void {
        if (this.speaking === next) {
            return;
        }

        this.speaking = next;

        this.onSpeakingChange?.(next);
    }

    private dispatchTextEvent(payload: string): void {
        try {
            const parsed = JSON.parse(payload) as VoiceEventI;

            if (parsed.type === 'error' && typeof parsed.message === 'string') {
                this.lastServerError = parsed.message;
            }

            if (parsed.type === 'speech_start') {
                this.clearPlayback();
            }

            this.onEvent?.(parsed);
        } catch {
            this.onEvent?.({message: payload, type: 'error'});
        }
    }

    private enqueuePlayback(buffer: ArrayBuffer): void {
        // Update the assistant-frame timestamp regardless of whether the AudioContext exists — the
        // speaking-change watcher relies on this and should still fire even if audio playback is
        // unavailable for some reason.
        this.lastAssistantFrameAtRef = performance.now();

        if (!this.audioContext) {
            return;
        }

        const audioContext = this.audioContext;
        const int16 = new Int16Array(buffer);
        const audioBuffer = audioContext.createBuffer(1, int16.length, this.sampleRate);
        const channel = audioBuffer.getChannelData(0);

        for (let index = 0; index < int16.length; index++) {
            channel[index] = int16[index] / 0x8000;
        }

        const source = audioContext.createBufferSource();

        source.buffer = audioBuffer;
        source.connect(audioContext.destination);

        const now = audioContext.currentTime;
        const startAt = Math.max(now + PLAYBACK_LEAD_AHEAD_SECONDS, this.playbackCursorSeconds);

        source.onended = () => {
            this.scheduledPlaybackSources = this.scheduledPlaybackSources.filter((scheduled) => scheduled !== source);
        };

        source.start(startAt);

        this.scheduledPlaybackSources.push(source);

        this.playbackCursorSeconds = startAt + audioBuffer.duration;
    }

    /**
     * Stops and discards any queued or playing assistant audio. Invoked on a `speech_start` (barge-in) event so the
     * assistant stops talking the instant the user starts speaking.
     */
    private clearPlayback(): void {
        for (const source of this.scheduledPlaybackSources) {
            source.onended = null;

            try {
                source.stop();
            } catch {
                // Source already ended; nothing to stop.
            }

            source.disconnect();
        }

        this.scheduledPlaybackSources = [];
        this.playbackCursorSeconds = this.audioContext ? this.audioContext.currentTime : 0;
    }

    private emitVolumeFromFrame(buffer: ArrayBuffer): void {
        if (!this.onVolume) {
            return;
        }

        const now = performance.now();

        if (now - this.lastVolumeEmitAt < VOLUME_EMIT_THROTTLE_MS) {
            return;
        }

        this.lastVolumeEmitAt = now;

        const samples = new Int16Array(buffer);

        if (samples.length === 0) {
            this.onVolume(0);

            return;
        }

        let sumOfSquares = 0;

        for (let index = 0; index < samples.length; index++) {
            const normalized = samples[index] / 0x8000;

            sumOfSquares += normalized * normalized;
        }

        const rms = Math.sqrt(sumOfSquares / samples.length);

        this.onVolume(rms);
    }

    private startSpeakingChangeWatcher(): void {
        if (this.speakingChangeIntervalId !== null) {
            return;
        }

        this.speakingChangeIntervalId = setInterval(() => {
            // No incoming frames yet — never speaking.
            if (this.lastAssistantFrameAtRef === 0) {
                return;
            }

            const elapsed = performance.now() - this.lastAssistantFrameAtRef;

            if (elapsed <= SPEAKING_HYSTERESIS_MS) {
                if (!this.speaking) {
                    this.setSpeaking(true);
                }
            } else if (this.speaking) {
                this.setSpeaking(false);
            }
        }, SPEAKING_CHECK_INTERVAL_MS);
    }

    private stopSpeakingChangeWatcher(): void {
        if (this.speakingChangeIntervalId !== null) {
            clearInterval(this.speakingChangeIntervalId);

            this.speakingChangeIntervalId = null;
        }
    }
}
