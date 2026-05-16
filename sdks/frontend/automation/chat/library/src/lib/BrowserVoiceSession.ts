/**
 * Sibling copy of `client/src/shared/lib/browser-voice/BrowserVoiceSession.ts` published inside the widget bundle.
 *
 * The widget cannot import from the platform's `@/shared` path because the widget is published as an external npm
 * package consumed by customer sites. Bytes here must stay in lockstep with the platform copy; future Tier 2
 * candidate: lift this into a shared `@bytechef/browser-voice` package consumed by both.
 *
 * Key difference from the platform copy: the AudioWorklet source is inlined as a string and registered via a
 * Blob URL, so customers do not need to host an extra static asset.
 */

export type VoiceEventTypeT =
    | 'connected'
    | 'transcript_interim'
    | 'transcript_final'
    | 'assistant_text'
    | 'speech_start'
    | 'error';

export interface VoiceEventI {
    type: VoiceEventTypeT | string;
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
 * Returns null when voice is supported in this browser, or a human-readable reason when it is not. See
 * {@code client/src/shared/lib/browser-voice/BrowserVoiceSession.ts} for the platform-side equivalent — these
 * two implementations must stay in sync.
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

    if (typeof window.isSecureContext !== 'undefined' && !window.isSecureContext) {
        return 'Voice requires a secure (https://) context. Browsers block microphone access on insecure origins.';
    }

    return null;
}

// Keep this string byte-for-byte in sync with client/public/mic-worklet.js
const WORKLET_SOURCE = `class Pcm16DownsamplerProcessor extends AudioWorkletProcessor {
    constructor(options) {
        super();
        const params = (options && options.processorOptions) || {};
        this.targetSampleRate = params.targetSampleRate || 16000;
        this.frameMs = params.frameMs || 20;
        this.frameSize = Math.max(1, Math.floor((this.targetSampleRate * this.frameMs) / 1000));
        this.acc = [];
    }
    process(inputs) {
        const input = inputs[0];
        if (!input || input.length === 0) return true;
        const channel = input[0];
        if (!channel || channel.length === 0) return true;
        const ratio = sampleRate / this.targetSampleRate;
        const out = new Int16Array(Math.floor(channel.length / ratio));
        for (let i = 0; i < out.length; i++) {
            const sourceIndex = Math.floor(i * ratio);
            const sample = Math.max(-1, Math.min(1, channel[sourceIndex] || 0));
            out[i] = sample < 0 ? sample * 0x8000 : sample * 0x7fff;
        }
        for (let i = 0; i < out.length; i++) this.acc.push(out[i]);
        while (this.acc.length >= this.frameSize) {
            const frame = new Int16Array(this.frameSize);
            for (let i = 0; i < this.frameSize; i++) frame[i] = this.acc[i];
            this.acc.splice(0, this.frameSize);
            this.port.postMessage(frame.buffer, [frame.buffer]);
        }
        return true;
    }
}
registerProcessor('pcm16-downsampler', Pcm16DownsamplerProcessor);
`;

export class BrowserVoiceSession {
    private readonly url: string;
    private readonly sampleRate: number;
    private readonly onEvent?: (event: VoiceEventI) => void;
    private readonly onStatusChange?: (status: VoiceSessionStatusType) => void;
    private readonly onSpeakingChange?: (speaking: boolean) => void;
    private readonly onVolume?: (level: number) => void;

    private ws: WebSocket | null = null;
    private audioContext: AudioContext | null = null;
    private mediaStream: MediaStream | null = null;
    private workletNode: AudioWorkletNode | null = null;
    private workletUrl: string | null = null;
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

            const blob = new Blob([WORKLET_SOURCE], {type: 'application/javascript'});

            this.workletUrl = URL.createObjectURL(blob);

            await this.audioContext.audioWorklet.addModule(this.workletUrl);

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

        if (this.workletUrl) {
            URL.revokeObjectURL(this.workletUrl);
            this.workletUrl = null;
        }

        this.ws = null;
        this.scheduledPlaybackSources = [];
        this.playbackCursorSeconds = 0;
        this.lastAssistantFrameAtRef = 0;
        this.lastVolumeEmitAt = 0;
        this.setSpeaking(false);
    }

    private setStatus(next: VoiceSessionStatusType): void {
        if (this.status === next) return;
        this.status = next;
        this.onStatusChange?.(next);
    }

    private setSpeaking(next: boolean): void {
        if (this.speaking === next) return;
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

    private enqueuePlayback(buffer: ArrayBuffer): void {
        // Update the assistant-frame timestamp regardless of whether the AudioContext exists — the
        // speaking-change watcher relies on this and should still fire even if audio playback is
        // unavailable for some reason.
        this.lastAssistantFrameAtRef = performance.now();

        if (!this.audioContext) return;

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
