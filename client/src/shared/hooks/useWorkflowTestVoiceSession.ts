import {BrowserVoiceSession, VoiceEventI, VoiceSessionStatusType} from '@/shared/lib/browser-voice/BrowserVoiceSession';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useCallback, useEffect, useRef, useState} from 'react';

interface UseWorkflowTestVoiceSessionOptionsI {
    workflowId: string;
    onEvent?: (event: VoiceEventI) => void;
}

interface UseWorkflowTestVoiceSessionResultI {
    status: VoiceSessionStatusType;
    error: string | null;
    isAssistantSpeaking: boolean;
    start: () => Promise<void>;
    stop: () => void;
}

const DEFAULT_SAMPLE_RATE = 16000;

async function fetchVoiceSessionToken(workflowId: string): Promise<string> {
    const token = await new WorkflowTestApi().issueWorkflowTestVoiceSessionToken({workflowId});

    if (!token?.token) {
        throw new Error('Voice session token response missing token field');
    }

    return token.token;
}

function buildWebSocketUrl(workflowId: string, token: string, sampleRate: number): string {
    const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = `/api/platform/internal/workflow-tests/${encodeURIComponent(workflowId)}/wss`;

    return `${scheme}//${window.location.host}${path}?sessionToken=${encodeURIComponent(token)}&sampleRate=${sampleRate}`;
}

/**
 * Lifecycle hook for an in-editor voice test session.
 *
 * Resolves a one-shot session token, opens the WS to `/internal/workflow-tests/{workflowId}/wss`, and forwards
 * incoming JSON events to the caller-supplied `onEvent` callback so the chat panel can splice transcripts and
 * assistant text into the same `Thread` it uses for SSE chat.
 */
export function useWorkflowTestVoiceSession({
    onEvent,
    workflowId,
}: UseWorkflowTestVoiceSessionOptionsI): UseWorkflowTestVoiceSessionResultI {
    const [status, setStatus] = useState<VoiceSessionStatusType>('idle');
    const [error, setError] = useState<string | null>(null);
    const [isAssistantSpeaking, setIsAssistantSpeaking] = useState(false);

    const sessionRef = useRef<BrowserVoiceSession | null>(null);
    const onEventRef = useRef(onEvent);

    useEffect(() => {
        onEventRef.current = onEvent;
    }, [onEvent]);

    const start = useCallback(async () => {
        if (sessionRef.current) {
            return;
        }

        setError(null);

        try {
            const token = await fetchVoiceSessionToken(workflowId);
            const url = buildWebSocketUrl(workflowId, token, DEFAULT_SAMPLE_RATE);

            const session = new BrowserVoiceSession({
                onEvent: (event) => {
                    if (event.type === 'error' && typeof event.message === 'string') {
                        setError(event.message);
                    }

                    onEventRef.current?.(event);
                },
                onSpeakingChange: setIsAssistantSpeaking,
                onStatusChange: setStatus,
                sampleRate: DEFAULT_SAMPLE_RATE,
                url,
            });

            sessionRef.current = session;

            await session.start();
        } catch (caught) {
            const message = caught instanceof Error ? caught.message : 'Failed to start voice session';

            setError(message);
            setStatus('error');
            sessionRef.current = null;
        }
    }, [workflowId]);

    const stop = useCallback(() => {
        const session = sessionRef.current;

        if (!session) {
            return;
        }

        session.stop();
        sessionRef.current = null;
    }, []);

    useEffect(() => {
        return () => {
            sessionRef.current?.stop();
            sessionRef.current = null;
        };
    }, []);

    return {error, isAssistantSpeaking, start, status, stop};
}
