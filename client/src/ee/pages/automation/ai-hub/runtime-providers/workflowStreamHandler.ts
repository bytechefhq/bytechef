import {extractStreamChunk} from '@/shared/util/stream-utils';

export interface WorkflowStreamErrorI {
    cause?: unknown;
    message: string;
}

/**
 * - `completed`: server closed cleanly with at least one chunk delivered.
 * - `empty`: server closed cleanly without delivering any chunks. Promoted to a distinct kind so the type system
 *   forces every consumer to handle the no-output case explicitly — a `kind === 'completed'` check then cannot
 *   accidentally treat a no-output close as success.
 * - `aborted`: caller aborted via AbortController (chat switch, panel unmount). Do NOT surface as failure.
 */
export type WorkflowStreamCompleteType =
    {chunkCount: number; kind: 'completed'} | {kind: 'empty'} | {chunkCount: number; kind: 'aborted'};

interface WorkflowStreamHandlerDepsI {
    /** Sink for each streamed text chunk. Implementations MUST treat this as an append, not a replace. */
    onChunk: (chunk: string) => void;
    onComplete: (info: WorkflowStreamCompleteType) => void;
    onError?: (error: WorkflowStreamErrorI) => void;
    /** Optional signal so the caller can cancel mid-stream (e.g. on chat switch or panel unmount). */
    signal?: AbortSignal;
}

const reportError = (
    contextLabel: string,
    error: WorkflowStreamErrorI,
    onError: ((error: WorkflowStreamErrorI) => void) | undefined
): void => {
    // Toasting is the caller's responsibility (via onError) so the user does not see two notifications
    // for the same failure when a higher layer also surfaces a retry banner.
    console.error(`${contextLabel}:`, error.message, error.cause);

    if (onError) {
        onError({...error, message: `${contextLabel}: ${error.message}`});
    }
};

const READER_RELEASED_PATTERN = /released|locked|invalid state/i;

/**
 * TypeError on cancel is the spec-defined response when the reader was already released by controller.abort()
 * — the socket is gone, so swallow it. Narrow by both type AND message pattern so a future
 * `TypeError: undefined is not a function` from a refactor surfaces on the console instead of being silently
 * eaten alongside the spec-defined release/lock/state errors.
 */
function reportUnexpectedReaderCancelRejection(cancelError: unknown): void {
    if (cancelError instanceof TypeError && READER_RELEASED_PATTERN.test(cancelError.message)) {
        return;
    }

    console.warn('reader.cancel() rejected unexpectedly', cancelError);
}

/**
 * Opens the webhook SSE stream for a streaming chat workflow. Uses fetch-based SSE (same pattern as useSSE.ts) so
 * that credentials and custom headers are forwarded correctly. Pipes each `stream` event chunk to the injected
 * `onChunk` sink (the caller — typically AiHubRuntimeProvider — appends to the last assistant message) and
 * resolves when the stream closes.
 *
 * Event types mirror ChatRuntimeProvider.tsx:
 *   stream  — incremental text chunk
 *   result  — final full-text override (overrides accumulated streamed content)
 *   error   — stream-level error; surfaces via onError at stream close
 *   (close) — AbortController abort / server-side done
 *
 * Error semantics:
 *   - HTTP 4xx/5xx response: onError is invoked, onComplete is NOT called.
 *   - Stream-level error event(s) collected during the stream: onError is invoked at stream close,
 *     onComplete is NOT called.
 *   - Stream-level fetch/read exception (excluding AbortError): onError is invoked, onComplete is NOT called.
 *   - Caller-side abort (AbortError): onComplete is called with `{kind: 'aborted', chunkCount}` so consumers
 *     can clean up tool-call cards. Without this, an in-flight stream cancelled on chat switch would
 *     leave its tool-call card stuck in the "running" state forever.
 *   - Normal close with at least one chunk: onComplete is called with `{chunkCount, kind: 'completed'}`.
 *   - Normal close with no chunks: onComplete is called with `{kind: 'empty'}`.
 */
export async function openWorkflowSseStream({
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
                    // Not JSON — keep as string. Log at WARN so a webhook-serializer regression is grep-able
                    // instead of producing garbled chat output with zero ops signal. console.debug is filtered
                    // out by default browser consoles in production, defeating the purpose of the log.
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
                        // Surface the raw payload so the failure is at least debuggable from logs.
                        try {
                            errorMessage = `Workflow stream reported an error: ${JSON.stringify(data)}`;
                        } catch (stringifyError) {
                            // JSON.stringify can fail on circular references, BigInt, etc. Without this log
                            // the user sees only "unserializable payload" with no breadcrumb to reproduce
                            // the offending shape — defeating the stated rationale of the surrounding code.
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

        // Flush any remaining bytes that were held back across UTF-8 boundaries.
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

            // controller.abort() rejects the reader's pending read() but does not release the stream lock or
            // signal the underlying socket to close. Cancel explicitly so the upstream connection is freed.
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

    // controller.abort() rejects the reader's pending read() but does not release the stream lock or signal the
    // underlying socket to close. Cancel explicitly so the upstream connection is freed.
    if (reader) {
        reader.cancel().catch(reportUnexpectedReaderCancelRejection);
    }

    if (signal) {
        signal.removeEventListener('abort', onCallerAbort);
    }

    if (aborted) {
        // Abort is part of the normal lifecycle (chat switch / panel unmount). Tell consumers so they
        // can mark tool-call cards complete instead of leaving them spinning forever.
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

/**
 * Fetches the non-streaming webhook response and appends the full text to the
 * active assistant message. The same POST format used by ChatRuntimeProvider.tsx
 * (form data with an empty message field so the trigger fires immediately).
 *
 * Error semantics:
 *   - HTTP 4xx/5xx: onError is invoked, onComplete is NOT called.
 *   - Network/parse exception: onError is invoked, onComplete is NOT called.
 *   - Caller-side abort (AbortError): onComplete is called with `{kind: 'aborted',
 *     chunkCount: 0}` so consumers can mark tool-call cards complete.
 *   - Successful response with text: onComplete is called with `{chunkCount: 1, kind: 'completed'}`.
 *   - Successful response with empty body: onComplete is called with `{kind: 'empty'}`.
 */
export async function fetchWorkflowResponse({
    onChunk,
    onComplete,
    onError,
    responseUrl,
    signal,
}: {responseUrl: string} & WorkflowStreamHandlerDepsI): Promise<void> {
    try {
        const response = await fetch(responseUrl, {
            body: new FormData(),
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
            // Abort is part of the normal lifecycle (chat switch / panel unmount). Tell consumers so
            // they can mark tool-call cards complete instead of leaving them spinning forever.
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
