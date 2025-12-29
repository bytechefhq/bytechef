import {useEffect, useMemo, useRef, useState} from 'react';

type EventHandlersType = Record<string, (data: unknown) => void>;

const EVENT_PREFIX = 'event:';
const DATA_PREFIX = 'data:';

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

function parseAndDispatchSSE(
    chunk: string,
    onDefaultMessage: (data: unknown) => void,
    customHandlers?: EventHandlersType
) {
    // SSE events are separated by blank lines. Lines can be: event:, data:, id:, retry:
    const events = chunk.split(/\n\n+/);

    for (const event of events) {
        if (!event.trim()) {
            continue;
        }

        const dataLines: string[] = [];
        let eventType = 'message';
        const lines = event.split(/\n/);

        for (const line of lines) {
            if (line.startsWith(EVENT_PREFIX)) {
                eventType = line.slice(EVENT_PREFIX.length).trim();
            } else if (line.startsWith(DATA_PREFIX)) {
                const dataLine = line.slice(DATA_PREFIX.length);

                dataLines.push(dataLine);
            }
        }

        let data: unknown = dataLines.join('\n');

        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch {
                // ignored
            }
        }

        if (customHandlers && customHandlers[eventType]) {
            try {
                customHandlers[eventType](data);
            } catch (error) {
                console.error('Error in SSE event handler for event type:', eventType, error);
            }
        } else {
            onDefaultMessage(data);
        }
    }
}

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
                        break;
                    }

                    buffer += decoder.decode(value, {stream: true});

                    // Process complete events separated by double newlines; keep trailing partial in the buffer
                    const parts = buffer.split(/\n\n/);

                    buffer = parts.pop() || '';

                    for (const part of parts) {
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
                if (buffer.trim()) {
                    parseAndDispatchSSE(
                        buffer,
                        (data) => {
                            setData(data as T);
                        },
                        handlersRef.current
                    );
                }

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
