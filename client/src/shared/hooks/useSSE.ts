import {useEffect, useMemo, useRef, useState} from 'react';

type EventHandlersType = Record<string, (data: string) => void>;

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
    onDefaultMessage: (data: string) => void,
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
            if (line.startsWith('event:')) {
                eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
                const dataLine = line.slice(5);

                dataLines.push(dataLine);
            }
        }

        let data = dataLines.join('\n');

        if (data.startsWith(' ')) {
            const trimmedData = data.slice(1);

            try {
                JSON.parse(trimmedData);

                data = trimmedData;
            } catch {
                // ignore
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
    const [connectionState, setConnectionState] = useState<'CONNECTING' | 'CONNECTED' | 'ERROR' | 'CLOSED'>(
        'CONNECTING'
    );

    const abortControllerRef = useRef<AbortController | null>(null);
    const handlersRef = useRef<EventHandlersType | undefined>(options.eventHandlers);

    useEffect(() => {
        handlersRef.current = options.eventHandlers;
    }, [options.eventHandlers]);

    const stableRequest = useMemo(() => {
        if (request == null) {
            return request;
        }

        return {init: request.init, url: request.url};
    }, [request]);

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
                            (dataStr) => {
                                try {
                                    const parsed = JSON.parse(dataStr);
                                    setData(parsed as T);
                                } catch {
                                    setData(dataStr);
                                }
                            },
                            handlersRef.current
                        );
                    }
                }

                // flush any remaining buffered event
                if (buffer.trim()) {
                    parseAndDispatchSSE(
                        buffer,
                        (dataStr) => {
                            try {
                                const parsed = JSON.parse(dataStr);

                                setData(parsed as T);
                            } catch {
                                setData(dataStr);
                            }
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

    const close = () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setConnectionState('CLOSED');
        }
    };

    return {close, connectionState, data, error};
};
