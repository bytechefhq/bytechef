import {useEffect, useMemo, useRef, useState} from 'react';
import {parseAndDispatchSSE} from '@/utils/sse-parser';

type EventHandlersType = Record<string, (data: unknown) => void>;

export type UseSSEOptionsType = {
    eventHandlers?: EventHandlersType;
};

export type SSERequestType = null | {
    url: string;
    init?: RequestInit;
};

export type UseSSEResultType<T = unknown> = {
    data: T | string | null;
    error: string | null;
    connectionState: 'CONNECTING' | 'CONNECTED' | 'ERROR' | 'CLOSED';
    close: () => void;
};

export const useSSE = <T = unknown>(request: SSERequestType, options: UseSSEOptionsType = {}): UseSSEResultType<T> => {
    const [data, setData] = useState<T | string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [connectionState, setConnectionState] = useState<'CONNECTING' | 'CONNECTED' | 'ERROR' | 'CLOSED'>('CLOSED');

    const abortControllerRef = useRef<AbortController | null>(null);
    const handlersRef = useRef<EventHandlersType | undefined>(options.eventHandlers);

    const stableRequest = useMemo(() => {
        if (request == null) {
            return request;
        }

        return {init: request.init, url: request.url};
    }, [request]);

    const close = () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setConnectionState('CLOSED');
        }
    };

    useEffect(() => {
        handlersRef.current = options.eventHandlers;
    }, [options.eventHandlers]);

    useEffect(() => {
        if (!stableRequest) {
            return;
        }

        const {init, url} = stableRequest as {url: string; init?: RequestInit};
        const controller = new AbortController();

        abortControllerRef.current = controller;

        (async () => {
            try {
                setConnectionState('CONNECTING');

                const headers: Record<string, string> = {
                    Accept: 'text/event-stream',
                    ...(init?.headers as Record<string, string>),
                };

                const response = await fetch(url, {
                    body: init?.body,
                    credentials: init?.credentials,
                    headers,
                    method: init?.method ?? (init?.body ? ('POST' as const) : undefined),
                    signal: controller.signal,
                });

                if (!response.ok || !response.body) {
                    setError(`HTTP ${response.status}`);
                    setConnectionState('ERROR');

                    return;
                }

                setError(null);
                setConnectionState('CONNECTED');

                const reader = response.body.getReader();
                const decoder = new TextDecoder('utf-8');
                let buffer = '';

                while (true) {
                    const {done, value} = await reader.read();

                    if (done) {
                        console.log('[useSSE] Stream ended, done=true');
                        break;
                    }

                    const chunk = decoder.decode(value, {stream: true});
                    console.log('[useSSE] Received raw chunk:', JSON.stringify(chunk));
                    buffer += chunk;

                    // Process complete events separated by double newlines; keep trailing partial in the buffer
                    const parts = buffer.split(/\r?\n\r?\n/);

                    buffer = parts.pop() || '';
                    console.log(
                        '[useSSE] Split into',
                        parts.length,
                        'parts, remaining buffer:',
                        JSON.stringify(buffer)
                    );

                    for (const part of parts) {
                        console.log('[useSSE] Processing part:', JSON.stringify(part));
                        parseAndDispatchSSE(
                            part,
                            (data) => {
                                setData(data as T);
                            },
                            handlersRef.current
                        );
                    }
                }

                // flush any remaining buffered event
                console.log('[useSSE] Flushing remaining buffer:', JSON.stringify(buffer));
                if (buffer.trim()) {
                    parseAndDispatchSSE(
                        buffer,
                        (data) => {
                            setData(data as T);
                        },
                        handlersRef.current
                    );
                }

                console.log('[useSSE] Closing connection');
                setConnectionState('CLOSED');
            } catch (error) {
                if ((error as Error)?.name !== 'AbortError') {
                    setError('Connection error occurred');
                    setConnectionState('ERROR');
                }
            }
        })();

        return () => {
            controller.abort();
            setConnectionState('CLOSED');
        };
    }, [stableRequest]);

    return {close, connectionState, data, error};
};
