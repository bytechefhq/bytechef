import {extractStreamChunk} from '@/shared/util/stream-utils';

export interface WorkflowStreamErrorI {
    cause?: unknown;
    message: string;
}

export type WorkflowStreamCompleteType =
    {chunkCount: number; kind: 'completed'} | {kind: 'empty'} | {chunkCount: number; kind: 'aborted'};

interface WorkflowStreamHandlerDepsI {
    message?: string;
    onChunk: (chunk: string) => void;
    onComplete: (info: WorkflowStreamCompleteType) => void;
    onError?: (error: WorkflowStreamErrorI) => void;
    signal?: AbortSignal;
}

const reportError = (
    contextLabel: string,
    error: WorkflowStreamErrorI,
    onError: ((error: WorkflowStreamErrorI) => void) | undefined
): void => {
    console.error(`${contextLabel}:`, error.message, error.cause);

    if (onError) {
        onError({...error, message: `${contextLabel}: ${error.message}`});
    }
};

const READER_RELEASED_PATTERN = /released|locked|invalid state/i;

function reportUnexpectedReaderCancelRejection(cancelError: unknown): void {
    if (cancelError instanceof TypeError && READER_RELEASED_PATTERN.test(cancelError.message)) {
        return;
    }

    console.warn('reader.cancel() rejected unexpectedly', cancelError);
}

const toRequestBody = (message: string | undefined): FormData => {
    const formData = new FormData();

    formData.append('message', message ?? '');

    return formData;
};

export async function openWorkflowSseStream({
    message,
    onChunk,
    onComplete,
    onError,
    signal,
    streamUrl,
}: {streamUrl: string} & WorkflowStreamHandlerDepsI): Promise<void> {
    const controller = new AbortController();

    const onCallerAbort = () => controller.abort();

    if (signal) {
        if (signal.aborted) {
            controller.abort();
        } else {
            signal.addEventListener('abort', onCallerAbort, {once: true});
        }
    }

    let collectedErrorMessage: string | null = null;
    let aborted = false;
    let chunkCount = 0;
    let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;

    try {
        const response = await fetch(streamUrl, {
            body: toRequestBody(message),
            credentials: 'include',
            headers: {Accept: 'text/event-stream'},
            method: 'POST',
            signal: controller.signal,
        });

        if (!response.ok || !response.body) {
            reportError(
                'Workflow stream failed',
                {
                    cause: {body: !!response.body, status: response.status},
                    message: `HTTP ${response.status} ${response.statusText || ''}`.trim(),
                },
                onError
            );

            controller.abort();

            return;
        }

        reader = response.body.getReader();

        const decoder = new TextDecoder('utf-8');
        let buffer = '';

        const EVENT_PREFIX = 'event:';
        const DATA_PREFIX = 'data:';
        const SPACE = ' ';

        const dispatchEvent = (raw: string) => {
            const lines = raw.split(/\r?\n/);
            let eventType = 'message';
            const dataLines: string[] = [];

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
                const rawData = data;

                try {
                    data = JSON.parse(rawData);
                } catch (parseError) {
                    console.warn('SSE data not JSON', {
                        len: rawData.length,
                        message: parseError instanceof Error ? parseError.message : String(parseError),
                        sample: rawData.slice(0, 80),
                    });
                }
            }

            if (eventType === 'stream' || eventType === 'message') {
                const chunk = extractStreamChunk(data);

                if (chunk) {
                    onChunk(chunk);
                    chunkCount += 1;
                }
            } else if (eventType === 'error') {
                let errorMessage = 'Workflow stream reported an error';

                if (data && typeof data === 'object') {
                    const candidate = (data as {message?: unknown; error?: unknown}).message;
                    const fallback = (data as {message?: unknown; error?: unknown}).error;

                    if (typeof candidate === 'string') {
                        errorMessage = candidate;
                    } else if (typeof fallback === 'string') {
                        errorMessage = fallback;
                    } else {
                        try {
                            errorMessage = `Workflow stream reported an error: ${JSON.stringify(data)}`;
                        } catch (stringifyError) {
                            console.warn('SSE error payload was not serializable', {
                                message:
                                    stringifyError instanceof Error ? stringifyError.message : String(stringifyError),
                            });

                            errorMessage = 'Workflow stream reported an error (unserializable payload)';
                        }
                    }
                } else if (typeof data === 'string' && data.length > 0) {
                    errorMessage = data;
                }

                collectedErrorMessage = errorMessage;
            }
        };

        while (true) {
            const {done, value} = await reader.read();

            if (done) {
                break;
            }

            buffer += decoder.decode(value, {stream: true});

            const parts = buffer.split(/\r?\n\r?\n/);

            buffer = parts.pop() || '';

            for (const part of parts) {
                if (part.trim()) {
                    dispatchEvent(part);
                }
            }
        }

        buffer += decoder.decode();

        if (buffer.trim()) {
            dispatchEvent(buffer);
        }
    } catch (error) {
        if ((error as Error)?.name === 'AbortError') {
            aborted = true;
        } else {
            reportError(
                'Workflow stream failed',
                {
                    cause: error,
                    message: error instanceof Error ? error.message : String(error),
                },
                onError
            );
            controller.abort();

            if (reader) {
                reader.cancel().catch(reportUnexpectedReaderCancelRejection);
            }

            if (signal) {
                signal.removeEventListener('abort', onCallerAbort);
            }

            return;
        }
    }

    controller.abort();

    if (reader) {
        reader.cancel().catch(reportUnexpectedReaderCancelRejection);
    }

    if (signal) {
        signal.removeEventListener('abort', onCallerAbort);
    }

    if (aborted) {
        onComplete({chunkCount, kind: 'aborted'});

        return;
    }

    if (collectedErrorMessage !== null) {
        reportError('Workflow stream failed', {message: collectedErrorMessage}, onError);

        return;
    }

    if (chunkCount === 0) {
        onComplete({kind: 'empty'});

        return;
    }

    onComplete({chunkCount, kind: 'completed'});
}

export async function fetchWorkflowResponse({
    message,
    onChunk,
    onComplete,
    onError,
    responseUrl,
    signal,
}: {responseUrl: string} & WorkflowStreamHandlerDepsI): Promise<void> {
    try {
        const response = await fetch(responseUrl, {
            body: toRequestBody(message),
            credentials: 'include',
            method: 'POST',
            signal,
        });

        if (!response.ok) {
            reportError(
                'Workflow response failed',
                {
                    cause: {status: response.status},
                    message: `HTTP ${response.status} ${response.statusText || ''}`.trim(),
                },
                onError
            );

            return;
        }

        let text: string;

        const contentType = response.headers.get('content-type') ?? '';

        if (contentType.includes('application/json')) {
            const json = (await response.json()) as {message?: string};

            text = json.message ?? '';
        } else {
            text = await response.text();
        }

        if (text) {
            onChunk(text);
            onComplete({chunkCount: 1, kind: 'completed'});
        } else {
            onComplete({kind: 'empty'});
        }
    } catch (error) {
        if ((error as Error)?.name === 'AbortError') {
            onComplete({chunkCount: 0, kind: 'aborted'});

            return;
        }

        reportError(
            'Workflow response failed',
            {
                cause: error,
                message: error instanceof Error ? error.message : String(error),
            },
            onError
        );
    }
}
