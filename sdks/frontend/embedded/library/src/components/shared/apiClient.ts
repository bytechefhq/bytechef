export class ApiResponseError extends Error {
    constructor(
        readonly status: number,
        readonly body: unknown
    ) {
        super(`Response status: ${status}`);

        this.name = 'ApiResponseError';
    }
}

async function readResponseBody<T>(response: Response, method: string): Promise<T> {
    if (method !== 'GET' && (response.status === 204 || response.headers?.get('content-length') === '0')) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

export function createApiClient(baseUrl: string, environment: string, jwtToken: string) {
    const defaultHeaders = {
        Authorization: `Bearer ${jwtToken}`,
        'X-Environment': environment,
        'Content-Type': 'application/json',
    };

    return {
        /**
         * Resolves with the parsed JSON body, or `undefined` when a non-GET response has no body. Rejects with an
         * {@link ApiResponseError} for a non-2xx status and with a `SyntaxError` for a body that is not JSON.
         */
        async fetch<T>(
            endpoint: string,
            options: {
                method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
                body?: object;
                headers?: Record<string, string>;
            } = {}
        ): Promise<T> {
            const {method = 'GET', body, headers = {}} = options;
            const url = `${baseUrl}${endpoint}`;

            try {
                const response = await fetch(url, {
                    method,
                    headers: {...defaultHeaders, ...headers},
                    ...(body && {body: JSON.stringify(body)}),
                });

                if (!response.ok) {
                    const errorBody = await readResponseBody<unknown>(response, method).catch(() => undefined);

                    throw new ApiResponseError(response.status, errorBody);
                }

                return await readResponseBody<T>(response, method);
            } catch (error: unknown) {
                console.error(`API Error (${endpoint}):`, (error as Error).message);

                throw error;
            }
        },
    };
}
