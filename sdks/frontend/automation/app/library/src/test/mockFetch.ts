import {vi} from 'vitest';

export interface MockResponseSpec {
    body?: unknown;
    headers?: Record<string, string>;
    status?: number;
    text?: string;
}

export function jsonResponse(spec: MockResponseSpec = {}): Response {
    const status = spec.status ?? 200;
    const headers = new Headers({'content-type': 'application/json', ...spec.headers});

    if (spec.text !== undefined) {
        return new Response(spec.text, {headers, status});
    }

    return new Response(spec.body === undefined ? null : JSON.stringify(spec.body), {headers, status});
}

export function mockFetch(
    ...responses: Array<
        Response | Error | ((input: RequestInfo | URL, init?: RequestInit) => Response | Promise<Response>)
    >
) {
    const calls: Array<{input: RequestInfo | URL; init?: RequestInit}> = [];
    let index = 0;

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        calls.push({init, input});

        const next = responses[Math.min(index, responses.length - 1)];

        index += 1;

        if (next instanceof Error) {
            throw next;
        }

        if (typeof next === 'function') {
            return next(input, init);
        }

        return next!.clone();
    });

    return {calls, fetch: fetchMock as unknown as typeof fetch};
}

export function lastRequest(calls: Array<{input: RequestInfo | URL; init?: RequestInit}>) {
    const last = calls[calls.length - 1]!;
    const url =
        typeof last.input === 'string' ? last.input : last.input instanceof URL ? last.input.href : last.input.url;
    const headers = new Headers(last.init?.headers);

    return {body: last.init?.body, headers, method: last.init?.method ?? 'GET', url};
}
