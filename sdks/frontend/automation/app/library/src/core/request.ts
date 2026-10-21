import {joinUrl, type ResolvedConfig} from './config';
import {BytechefAppError} from './errors';

export const API_PREFIX = '/api/app/v1';

export interface RequestOptions {
    body?: unknown;
    headers?: Record<string, string>;
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT';
    path: string;
    query?: Record<string, string | string[]>;
    /**
     * Resolves with the answer's `data`, `headers` and `status` whatever the status, instead of throwing on a non-2xx.
     * A body that is not JSON is returned as text. Timeouts and transport failures still throw.
     */
    rawResponse?: boolean;
    signal?: AbortSignal;
}

export interface RequestResult<T> {
    data: T;
    /** Set only when `rawResponse` was requested. */
    headers?: Record<string, string>;
    status: number;
}

export interface Requester {
    <T>(options: RequestOptions): Promise<RequestResult<T>>;
}

interface ErrorBody {
    code?: unknown;
    message?: unknown;
}

export function abortError(signal: AbortSignal): BytechefAppError {
    return signal.reason instanceof BytechefAppError
        ? signal.reason
        : new BytechefAppError('ABORTED', 'The ByteChef call was aborted', {cause: signal.reason});
}

export function untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {
    if (signal.aborted) {
        promise.catch(() => undefined);

        return Promise.reject(abortError(signal));
    }

    return new Promise<T>((resolve, reject) => {
        const onAbort = () => reject(abortError(signal));

        signal.addEventListener('abort', onAbort, {once: true});

        promise.then(
            (value) => {
                signal.removeEventListener('abort', onAbort);
                resolve(value);
            },
            (error: unknown) => {
                signal.removeEventListener('abort', onAbort);
                reject(error);
            }
        );
    });
}

function parseErrorBody(text: string): ErrorBody | undefined {
    if (!text) {
        return undefined;
    }

    try {
        const parsed: unknown = JSON.parse(text);

        return typeof parsed === 'object' && parsed !== null ? (parsed as ErrorBody) : undefined;
    } catch {
        return undefined;
    }
}

function withQuery(url: string, query: Record<string, string | string[]> | undefined): string {
    if (!query) {
        return url;
    }

    const params = new URLSearchParams();

    for (const [name, value] of Object.entries(query)) {
        for (const item of Array.isArray(value) ? value : [value]) {
            params.append(name, item);
        }
    }

    const queryString = params.toString();

    return queryString === '' ? url : `${url}?${queryString}`;
}

function parseRawBody(text: string): unknown {
    if (text === '') {
        return undefined;
    }

    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

export function createRequester(getConfig: () => ResolvedConfig): Requester {
    return async <T>({
        body,
        headers: extraHeaders,
        method,
        path,
        query,
        rawResponse,
        signal,
    }: RequestOptions): Promise<RequestResult<T>> => {
        if (signal?.aborted) {
            throw new BytechefAppError('ABORTED', 'The ByteChef call was aborted', {cause: signal.reason});
        }

        const config = getConfig();
        const payload = body === undefined ? undefined : JSON.stringify(body);
        const headers: Record<string, string> = {accept: 'application/json'};

        if (payload !== undefined) {
            headers['content-type'] = 'application/json';
        }

        for (const [name, value] of Object.entries(extraHeaders ?? {})) {
            headers[name.toLowerCase()] = value;
        }

        headers['authorization'] = `Bearer ${config.appKey}`;

        const controller = new AbortController();
        const onAbort = () =>
            controller.abort(new BytechefAppError('ABORTED', 'The ByteChef call was aborted', {cause: signal?.reason}));
        const timeout = setTimeout(
            () =>
                controller.abort(
                    new BytechefAppError('TIMEOUT', `ByteChef did not answer within ${config.timeoutMs} ms`)
                ),
            config.timeoutMs
        );

        signal?.addEventListener('abort', onAbort, {once: true});

        try {
            let response: Response;

            try {
                response = await config.fetch(withQuery(joinUrl(config.baseUrl, `${API_PREFIX}${path}`), query), {
                    body: payload,
                    headers,
                    method,
                    signal: controller.signal,
                });
            } catch (error) {
                if (controller.signal.aborted) {
                    throw abortError(controller.signal);
                }

                throw new BytechefAppError(
                    'NETWORK_ERROR',
                    `ByteChef could not be reached: ${(error as Error).message}`,
                    {
                        cause: error,
                    }
                );
            }

            let text: string;

            try {
                text = await untilAborted(response.text(), controller.signal);
            } catch (error) {
                if (error instanceof BytechefAppError) {
                    throw error;
                }

                if (response.ok) {
                    throw new BytechefAppError(
                        'NETWORK_ERROR',
                        `Reading the ByteChef answer failed: ${(error as Error).message}`,
                        {
                            cause: error,
                            status: response.status,
                        }
                    );
                }

                text = '';
            }

            if (rawResponse) {
                const responseHeaders: Record<string, string> = {};

                response.headers.forEach((value, name) => {
                    responseHeaders[name] = value;
                });

                return {data: parseRawBody(text) as T, headers: responseHeaders, status: response.status};
            }

            if (!response.ok) {
                const errorBody = parseErrorBody(text);
                const code = typeof errorBody?.code === 'string' ? errorBody.code : 'HTTP_ERROR';
                const message =
                    typeof errorBody?.message === 'string' ? errorBody.message : `ByteChef answered ${response.status}`;

                throw new BytechefAppError(code, message, {status: response.status});
            }

            if (text === '') {
                return {data: undefined as T, status: response.status};
            }

            try {
                return {data: JSON.parse(text) as T, status: response.status};
            } catch (error) {
                throw new BytechefAppError(
                    'INVALID_RESPONSE',
                    `ByteChef answered ${response.status} with a body that is not JSON; check that BYTECHEF_URL points at the ByteChef server`,
                    {cause: error, status: response.status}
                );
            }
        } finally {
            clearTimeout(timeout);
            signal?.removeEventListener('abort', onAbort);
        }
    };
}
