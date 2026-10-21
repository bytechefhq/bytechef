import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import type {Requester} from './request';
import {expectArray} from './response';
import type {AppApiCollection} from './types';

export interface ApiCallOptionsI extends CallOptions {
    body?: unknown;
    headers?: Record<string, string>;
    method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT';
    /** `v{n}/{contextPath}/{endpointPath}`; a leading slash is tolerated. */
    path: string;
    query?: Record<string, string | string[]>;
}

export interface ApiResponseI<T = unknown> {
    body: T;
    headers: Record<string, string>;
    status: number;
}

export interface ApiApi {
    /**
     * Calls an endpoint of an API collection bound to the app. The status, headers and body the endpoint's workflow
     * chose are returned as data, a 4xx or 5xx included. Throws `BytechefAppError` for every error ByteChef itself
     * answers (marked `X-ByteChef-App-Error: true`): an unknown endpoint, an unavailable collection, a disabled app, a
     * bad key, a failed or timed-out workflow, and for a transport failure.
     */
    call<T = unknown>(options: ApiCallOptionsI): Promise<ApiResponseI<T>>;
    list(options?: CallOptions): Promise<AppApiCollection[]>;
}

const APP_ERROR_HEADER = 'x-bytechef-app-error';

function encodePath(path: string): string {
    const segments = path.replace(/^\/+/, '').split('/');

    if (segments.some((segment) => segment === '.' || segment === '..')) {
        throw new BytechefAppError('INVALID_PATH', `The API path must not contain '.' or '..' segments: ${path}`);
    }

    return segments.map((segment) => encodeURIComponent(segment)).join('/');
}

/**
 * ByteChef marks its own errors, a rejected app key included, with `X-ByteChef-App-Error: true`; any other status is
 * what the endpoint's workflow chose.
 */
function appError(body: unknown, headers: Record<string, string>, status: number): BytechefAppError | undefined {
    if (headers[APP_ERROR_HEADER] !== 'true') {
        return undefined;
    }

    const {code, message} =
        typeof body === 'object' && body !== null ? (body as {code?: unknown; message?: unknown}) : {};

    return new BytechefAppError(
        typeof code === 'string' ? code : 'HTTP_ERROR',
        typeof message === 'string' ? message : `ByteChef answered ${status}`,
        {status}
    );
}

export function createApiApi(request: Requester): ApiApi {
    return {
        async call<T = unknown>({body, headers, method, path, query, signal}: ApiCallOptionsI) {
            const result = await request<T>({
                body,
                headers,
                method,
                path: `/api/${encodePath(path)}`,
                query,
                rawResponse: true,
                signal,
            });

            if (result.status >= 400) {
                const error = appError(result.data, result.headers ?? {}, result.status);

                if (error) {
                    throw error;
                }
            }

            return {
                body: (result.data === undefined ? null : result.data) as T,
                headers: result.headers ?? {},
                status: result.status,
            };
        },

        async list(options = {}) {
            const path = '/api-collections';

            return expectArray<AppApiCollection>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}
