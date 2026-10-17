import {useEffect, useRef, useState} from 'react';

type EventHandlersType = Record<string, (data: unknown) => void>;

const EVENT_PREFIX = 'event:';
const DATA_PREFIX = 'data:';
const SPACE = ' ';

export type SSERequestType = null | {
    url: string;
    init?: RequestInit;
};

export type UseSSEOptionsType = {
    eventHandlers?: EventHandlersType;
    onClose?: () => void;

    onRequestAbort?: (request: SSERequestType) => void;

    onRequestError?: (status: number | null, request: SSERequestType) => void;

    onRequestSuccess?: (request: SSERequestType) => void;
};

export type UseSSEResultType<T = unknown> = {
    data: T | string | null;
    error: string | null;
    errorStatus: number | null;
    connectionState: 'CONNECTING' | 'CONNECTED' | 'ERROR' | 'CLOSED';
    close: () => void;
};

function parseAndDispatchSSE(
    chunk: string,
    onDefaultMessage: (data: unknown) => void,
    customHandlers?: EventHandlersType
) {
    // SSE events are separated by blank lines. Lines can be: event:, data:, id:, retry:
    const events = chunk.split(/\r?\n\r?\n+/);

    for (const event of events) {
        if (!event.trim()) {
            continue;
        }

        const dataLines: string[] = [];
        let eventType = 'message';
        const lines = event.split(/\r?\n/);

        for (const line of lines) {
            if (line.startsWith(EVENT_PREFIX)) {
                let value = line.slice(EVENT_PREFIX.length);

                if (value.startsWith(SPACE)) {
                    value = value.slice(SPACE.length);
                }

                eventType = value;
            } else if (line.startsWith(DATA_PREFIX)) {
                let dataLine = line.slice(DATA_PREFIX.length);

                if (dataLine.startsWith(SPACE)) {
                    dataLine = dataLine.slice(SPACE.length);
                }

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
    const [errorStatus, setErrorStatus] = useState<number | null>(null);
    const [connectionState, setConnectionState] = useState<'CONNECTING' | 'CONNECTED' | 'ERROR' | 'CLOSED'>('CLOSED');

    const abortControllerRef = useRef<AbortController | null>(null);
    const handlersRef = useRef<EventHandlersType | undefined>(options.eventHandlers);
    const onCloseRef = useRef<(() => void) | undefined>(options.onClose);
    const onRequestAbortRef = useRef<UseSSEOptionsType['onRequestAbort']>(options.onRequestAbort);
    const onRequestErrorRef = useRef<UseSSEOptionsType['onRequestError']>(options.onRequestError);
    const onRequestSuccessRef = useRef<UseSSEOptionsType['onRequestSuccess']>(options.onRequestSuccess);

    const close = () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setConnectionState('CLOSED');
        }
    };

    useEffect(() => {
        handlersRef.current = options.eventHandlers;
        onRequestAbortRef.current = options.onRequestAbort;
        onRequestErrorRef.current = options.onRequestError;
        onRequestSuccessRef.current = options.onRequestSuccess;
    }, [options.eventHandlers, options.onRequestAbort, options.onRequestError, options.onRequestSuccess]);

    useEffect(() => {
        onCloseRef.current = options.onClose;
    }, [options.onClose]);

    useEffect(() => {
        if (!request) {
            return;
        }

        const {init, url} = request;
        const controller = new AbortController();

        abortControllerRef.current = controller;

        (async () => {
            try {
                setError(null);
                setErrorStatus(null);
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

                if (controller.signal.aborted || abortControllerRef.current !== controller) {
                    onRequestAbortRef.current?.(request);

                    return;
                }

                if (!response.ok || !response.body) {
                    setError(`HTTP ${response.status}`);
                    setErrorStatus(response.status);
                    setConnectionState('ERROR');

                    onRequestErrorRef.current?.(response.status, request);

                    return;
                }

                setConnectionState('CONNECTED');

                onRequestSuccessRef.current?.(request);

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
                    const parts = buffer.split(/\r?\n\r?\n/);

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

                if (controller.signal.aborted || abortControllerRef.current !== controller) {
                    onRequestAbortRef.current?.(request);

                    return;
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

                onCloseRef.current?.();
            } catch (error) {
                if ((error as Error)?.name === 'AbortError') {
                    onRequestAbortRef.current?.(request);
                } else {
                    setError('Connection error occurred');
                    setConnectionState('ERROR');

                    onRequestErrorRef.current?.(null, request);
                }
            }
        })();

        return () => {
            controller.abort();
            setConnectionState('CLOSED');
        };
    }, [request]);

    return {close, connectionState, data, error, errorStatus};
};
