import {getCookie} from '@/shared/util/cookie-utils';
import {useCallback, useRef, useState} from 'react';

interface UsePushToTalkPropsI {
    locale?: string;
    onError?: (error: Error) => void;
    onTranscript: (text: string) => void;
}

/**
 * Push-to-talk speech-to-text: records a short audio clip via {@code MediaRecorder} while held, uploads it to the
 * app-user transcribe endpoint (`POST /api/platform/internal/ai/transcribe`), and reports the transcript so a composer
 * can inject it as text. Unlike the full realtime voice mode, this is a one-shot request/response with no live socket.
 */
export const usePushToTalk = ({locale, onError, onTranscript}: UsePushToTalkPropsI) => {
    const [isRecording, setIsRecording] = useState(false);
    const [isTranscribing, setIsTranscribing] = useState(false);

    const chunksRef = useRef<Blob[]>([]);
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const streamRef = useRef<MediaStream | null>(null);

    const stopStream = useCallback(() => {
        streamRef.current?.getTracks().forEach((track) => track.stop());

        streamRef.current = null;
    }, []);

    const transcribe = useCallback(
        async (blob: Blob) => {
            setIsTranscribing(true);

            try {
                const formData = new FormData();

                formData.append('file', blob, 'audio.webm');

                if (locale) {
                    formData.append('locale', locale);
                }

                const response = await fetch('/api/platform/internal/ai/transcribe', {
                    body: formData,
                    headers: {'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''},
                    method: 'POST',
                });

                if (!response.ok) {
                    throw new Error(`Transcription failed with status ${response.status}`);
                }

                const result = (await response.json()) as {text?: string};

                if (result.text) {
                    onTranscript(result.text);
                }
            } catch (error) {
                onError?.(error as Error);
            } finally {
                setIsTranscribing(false);
            }
        },
        [locale, onError, onTranscript]
    );

    const start = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({audio: true});

            streamRef.current = stream;

            const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
                ? 'audio/webm;codecs=opus'
                : MediaRecorder.isTypeSupported('audio/webm')
                  ? 'audio/webm'
                  : 'audio/mp4';

            const mediaRecorder = new MediaRecorder(stream, {mimeType});

            chunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    chunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = () => {
                stopStream();

                const blob = new Blob(chunksRef.current, {type: mimeType});

                chunksRef.current = [];

                void transcribe(blob);
            };

            mediaRecorderRef.current = mediaRecorder;

            mediaRecorder.start();

            setIsRecording(true);
        } catch (error) {
            stopStream();

            onError?.(error as Error);
        }
    }, [onError, stopStream, transcribe]);

    const stop = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
        }

        setIsRecording(false);
    }, []);

    return {isRecording, isTranscribing, start, stop};
};
