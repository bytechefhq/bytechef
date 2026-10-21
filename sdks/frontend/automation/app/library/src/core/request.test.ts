import {afterEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import type {ResolvedConfig} from './config';
import {BytechefAppError} from './errors';
import {createRequester} from './request';

function configWith(fetchImpl: typeof fetch, timeoutMs = 70_000): () => ResolvedConfig {
    return () => ({appKey: 'key-1', baseUrl: 'https://bc.example', fetch: fetchImpl, timeoutMs});
}

describe('request', () => {
    it('sends the bearer key, JSON headers and body, and returns status and parsed data', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: {result: 1}, status: 202}));
        const request = createRequester(configWith(fetch));

        const response = await request<{result: number}>({body: {input: {}}, method: 'POST', path: '/workflows/k'});

        expect(response).toEqual({data: {result: 1}, status: 202});

        const sent = lastRequest(calls);

        expect(sent.url).toBe('https://bc.example/api/app/v1/workflows/k');
        expect(sent.method).toBe('POST');
        expect(sent.headers.get('authorization')).toBe('Bearer key-1');
        expect(sent.headers.get('accept')).toBe('application/json');
        expect(sent.headers.get('content-type')).toBe('application/json');
        expect(sent.body).toBe('{"input":{}}');
    });

    it('throws BytechefAppError with the server code and message on a JSON error body', async () => {
        const {fetch} = mockFetch(
            jsonResponse({body: {code: 'APP_OPERATION_NOT_ALLOWED', message: 'no'}, status: 403})
        );
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({
            code: 'APP_OPERATION_NOT_ALLOWED',
            message: 'no',
            status: 403,
        });
    });

    it('throws HTTP_ERROR with the status on a non-JSON error body', async () => {
        const {fetch} = mockFetch(
            jsonResponse({headers: {'content-type': 'text/html'}, status: 502, text: '<html>bad gateway</html>'})
        );
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({code: 'HTTP_ERROR', status: 502});
    });

    it('throws HTTP_ERROR on an empty 401 body', async () => {
        const {fetch} = mockFetch(jsonResponse({status: 401, text: ''}));
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({code: 'HTTP_ERROR', status: 401});
    });

    it('maps a thrown fetch to NETWORK_ERROR without a status', async () => {
        const {fetch} = mockFetch(new TypeError('fetch failed'));
        const request = createRequester(configWith(fetch));

        const error = await request({method: 'GET', path: '/tools'}).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(BytechefAppError);
        expect(error).toMatchObject({code: 'NETWORK_ERROR', status: undefined});
    });

    it('aborts with TIMEOUT when the timeout elapses', async () => {
        const {fetch} = mockFetch(
            (_input, init) =>
                new Promise((_resolve, reject) => {
                    init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                })
        );
        const request = createRequester(configWith(fetch, 20));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({code: 'TIMEOUT'});
    });

    it('propagates a caller abort as ABORTED', async () => {
        const controller = new AbortController();
        const {fetch} = mockFetch(
            (_input, init) =>
                new Promise((_resolve, reject) => {
                    init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                })
        );
        const request = createRequester(configWith(fetch));
        const pending = request({method: 'GET', path: '/tools', signal: controller.signal});

        controller.abort('user left');

        const error = await pending.catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(BytechefAppError);
        expect(error).toMatchObject({cause: 'user left', code: 'ABORTED'});
    });

    it('returns data undefined for a 204', async () => {
        const {fetch} = mockFetch(new Response(null, {status: 204}));
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).resolves.toEqual({data: undefined, status: 204});
    });

    it('throws INVALID_RESPONSE with the status when a 2xx body is not JSON', async () => {
        const {fetch} = mockFetch(
            jsonResponse({headers: {'content-type': 'text/html'}, status: 200, text: '<!doctype html><html></html>'})
        );
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });

    it('does not call fetch when the caller signal is already aborted', async () => {
        const controller = new AbortController();
        const {calls, fetch} = mockFetch(jsonResponse({body: []}));
        const request = createRequester(configWith(fetch));

        controller.abort();

        await expect(request({method: 'GET', path: '/tools', signal: controller.signal})).rejects.toMatchObject({
            code: 'ABORTED',
        });
        expect(calls).toHaveLength(0);
    });

    it('applies the timeout to reading the response body', async () => {
        const {fetch} = mockFetch(
            () =>
                new Response(new ReadableStream({start() {}}), {
                    headers: {'content-type': 'application/json'},
                    status: 200,
                })
        );
        const request = createRequester(configWith(fetch, 20));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({code: 'TIMEOUT'});
    });

    it('aborts with ABORTED when the caller aborts while the body is being read', {timeout: 2000}, async () => {
        const controller = new AbortController();
        const {fetch} = mockFetch(() => {
            setTimeout(() => controller.abort(), 5);

            return new Response(new ReadableStream({start() {}}), {
                headers: {'content-type': 'application/json'},
                status: 200,
            });
        });
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools', signal: controller.signal})).rejects.toMatchObject({
            code: 'ABORTED',
        });
    });

    it('maps a failing body read on a 2xx answer to NETWORK_ERROR with the status', async () => {
        const response = Object.assign(new Response('x', {status: 200}), {
            text: () => Promise.reject(new TypeError('terminated')),
        });
        const {fetch} = mockFetch(() => response);
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({
            code: 'NETWORK_ERROR',
            status: 200,
        });
    });

    it('maps a failing body read on an error answer to HTTP_ERROR with the status', async () => {
        const response = Object.assign(new Response('x', {status: 500}), {
            text: () => Promise.reject(new TypeError('terminated')),
        });
        const {fetch} = mockFetch(() => response);
        const request = createRequester(configWith(fetch));

        await expect(request({method: 'GET', path: '/tools'})).rejects.toMatchObject({
            code: 'HTTP_ERROR',
            status: 500,
        });
    });

    it('throws the serialisation TypeError without calling fetch for a circular body', async () => {
        const circular: Record<string, unknown> = {};

        circular.self = circular;

        const {calls, fetch} = mockFetch(jsonResponse({body: {}}));
        const request = createRequester(configWith(fetch));

        await expect(request({body: circular, method: 'POST', path: '/workflows/k'})).rejects.toBeInstanceOf(TypeError);
        expect(calls).toHaveLength(0);
    });

    describe('cleanup', () => {
        afterEach(() => {
            vi.useRealTimers();
        });

        it('releases the timeout timer after a successful request', async () => {
            vi.useFakeTimers();

            const {fetch} = mockFetch(jsonResponse({body: {}}));
            const request = createRequester(configWith(fetch));

            await request({method: 'GET', path: '/tools'});

            expect(vi.getTimerCount()).toBe(0);
        });

        it('releases the timeout timer after an HTTP error', async () => {
            vi.useFakeTimers();

            const {fetch} = mockFetch(jsonResponse({body: {code: 'X', message: 'no'}, status: 400}));
            const request = createRequester(configWith(fetch));

            await request({method: 'GET', path: '/tools'}).catch(() => undefined);

            expect(vi.getTimerCount()).toBe(0);
        });

        it('removes the caller abort listener it added', async () => {
            const controller = new AbortController();
            const addSpy = vi.spyOn(controller.signal, 'addEventListener');
            const removeSpy = vi.spyOn(controller.signal, 'removeEventListener');
            const {fetch} = mockFetch(jsonResponse({body: {}}));
            const request = createRequester(configWith(fetch));

            await request({method: 'GET', path: '/tools', signal: controller.signal});

            const added = addSpy.mock.calls.find(([type]) => type === 'abort');

            expect(added).toBeDefined();
            expect(removeSpy).toHaveBeenCalledWith('abort', added![1]);
        });
    });

    describe('methods, headers, query and rawResponse', () => {
        it.each(['DELETE', 'PATCH', 'PUT'] as const)('sends %s', async (method) => {
            const {calls, fetch} = mockFetch(jsonResponse({body: {}}));
            const request = createRequester(configWith(fetch));

            await request({method, path: '/api/v1/a/b'});

            expect(lastRequest(calls).method).toBe(method);
        });

        it('returns data, headers and status without throwing on a non-2xx when rawResponse is set', async () => {
            const {fetch} = mockFetch(jsonResponse({body: {code: 'X'}, headers: {'x-a': '1'}, status: 422}));
            const request = createRequester(configWith(fetch));

            const result = await request({method: 'GET', path: '/api/v1/a/b', rawResponse: true});

            expect(result.status).toBe(422);
            expect(result.data).toEqual({code: 'X'});
            expect(result.headers?.['x-a']).toBe('1');
        });

        it('keeps the bearer key when a caller header names authorization', async () => {
            const {calls, fetch} = mockFetch(jsonResponse({body: {}}));
            const request = createRequester(configWith(fetch));

            await request({headers: {authorization: 'evil'}, method: 'GET', path: '/x', query: {a: '1'}});

            expect(lastRequest(calls).headers.get('authorization')).toBe('Bearer key-1');
            expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/x?a=1');
        });
    });
});
