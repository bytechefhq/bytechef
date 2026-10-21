import {describe, expect, it} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createApiApi} from './api';
import {BytechefAppError} from './errors';
import {createRequester} from './request';

function apiWith(...responses: Parameters<typeof mockFetch>) {
    const {calls, fetch} = mockFetch(...responses);
    const config = {appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 30_000};

    return {api: createApiApi(createRequester(() => config)), calls};
}

describe('api.call', () => {
    it('sends method, path, query and body', async () => {
        const {api, calls} = apiWith(jsonResponse({body: {id: 1}, headers: {'x-trace': 't1'}, status: 201}));

        const response = await api.call<{id: number}>({
            body: {name: 'Ada'},
            headers: {'x-custom': 'v'},
            method: 'POST',
            path: 'v1/crm/contacts',
            query: {tag: ['a', 'b'], team: 'x y'},
        });

        expect(response.status).toBe(201);
        expect(response.body).toEqual({id: 1});
        expect(response.headers['x-trace']).toBe('t1');

        const sent = lastRequest(calls);

        expect(sent.method).toBe('POST');
        expect(sent.url).toBe('https://bc.example/api/app/v1/api/v1/crm/contacts?tag=a&tag=b&team=x+y');
        expect(sent.headers.get('x-custom')).toBe('v');
        expect(sent.headers.get('authorization')).toBe('Bearer k');
        expect(sent.body).toBe('{"name":"Ada"}');
    });

    it('keeps slashes literal and encodes the reserved characters of each segment', async () => {
        const {api, calls} = apiWith(jsonResponse({body: {}}));

        await api.call({method: 'GET', path: 'v1/crm/a b/c?d'});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/api/v1/crm/a%20b/c%3Fd');
    });

    it('strips a leading slash', async () => {
        const {api, calls} = apiWith(jsonResponse({body: {}}));

        await api.call({method: 'GET', path: '/v1/crm/contacts'});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/api/v1/crm/contacts');
    });

    it('returns a workflow-chosen 400 without throwing', async () => {
        const {api} = apiWith(jsonResponse({body: {code: 'BAD', message: 'invalid'}, status: 400}));

        await expect(api.call({method: 'GET', path: 'v1/crm/contacts'})).resolves.toMatchObject({
            body: {code: 'BAD', message: 'invalid'},
            status: 400,
        });
    });

    it('returns a non-JSON body as text and an empty body as null', async () => {
        const text = apiWith(jsonResponse({headers: {'content-type': 'text/plain'}, text: 'hello'}));
        const empty = apiWith(jsonResponse({status: 204}));

        await expect(text.api.call({method: 'GET', path: 'v1/a/b'})).resolves.toMatchObject({body: 'hello'});
        await expect(empty.api.call({method: 'DELETE', path: 'v1/a/b'})).resolves.toMatchObject({
            body: null,
            status: 204,
        });
    });

    it.each(['APP_API_ENDPOINT_NOT_FOUND', 'APP_API_COLLECTION_UNAVAILABLE', 'APP_DISABLED', 'UNAUTHORIZED'])(
        'throws BytechefAppError on %s',
        async (code) => {
            const status = code === 'APP_API_ENDPOINT_NOT_FOUND' ? 404 : 409;
            const {api} = apiWith(
                jsonResponse({body: {code, message: 'nope'}, headers: {'X-ByteChef-App-Error': 'true'}, status})
            );

            const error = await api.call({method: 'GET', path: 'v1/a/b'}).catch((caught: unknown) => caught);

            expect(error).toBeInstanceOf(BytechefAppError);
            expect(error).toMatchObject({code, message: 'nope', status});
        }
    );

    it('returns a workflow-chosen 401 with an UNAUTHORIZED body as data', async () => {
        const {api} = apiWith(jsonResponse({body: {code: 'UNAUTHORIZED', message: 'log in'}, status: 401}));

        await expect(api.call({method: 'GET', path: 'v1/a/b'})).resolves.toMatchObject({
            body: {code: 'UNAUTHORIZED'},
            status: 401,
        });
    });

    it('throws UNAUTHORIZED on a 401 marked as an app error', async () => {
        const {api} = apiWith(
            jsonResponse({
                body: {code: 'UNAUTHORIZED', message: 'bad key'},
                headers: {'X-ByteChef-App-Error': 'true'},
                status: 401,
            })
        );

        await expect(api.call({method: 'GET', path: 'v1/a/b'})).rejects.toMatchObject({
            code: 'UNAUTHORIZED',
            status: 401,
        });
    });

    it('returns a workflow-chosen empty 401 as data', async () => {
        const {api} = apiWith(jsonResponse({status: 401}));

        await expect(api.call({method: 'GET', path: 'v1/a/b'})).resolves.toMatchObject({body: null, status: 401});
    });

    it.each([
        ['ACCESS_DENIED', 403],
        ['APP_EXECUTION_FAILED', 500],
        ['APP_EXECUTION_TIMEOUT', 504],
        ['COMPONENT_ERROR', 422],
        ['PROVIDER_ERROR', 422],
    ])('throws BytechefAppError on a marked %s', async (code, status) => {
        const {api} = apiWith(
            jsonResponse({body: {code, message: 'failed'}, headers: {'X-ByteChef-App-Error': 'true'}, status})
        );

        const error = await api.call({method: 'GET', path: 'v1/a/b'}).catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(BytechefAppError);
        expect(error).toMatchObject({code, message: 'failed', status});
    });

    it('throws HTTP_ERROR on a marked error without a code', async () => {
        const {api} = apiWith(jsonResponse({headers: {'X-ByteChef-App-Error': 'true'}, status: 500}));

        await expect(api.call({method: 'GET', path: 'v1/a/b'})).rejects.toMatchObject({
            code: 'HTTP_ERROR',
            message: 'ByteChef answered 500',
            status: 500,
        });
    });

    it.each(['v1/crm/../x', 'v1/./x', '/..'])('rejects the dot segment path %s before any request', async (path) => {
        const {api, calls} = apiWith(jsonResponse({body: {}}));

        await expect(api.call({method: 'GET', path})).rejects.toMatchObject({code: 'INVALID_PATH'});
        expect(calls).toHaveLength(0);
    });

    it('encodes a hash in a segment', async () => {
        const {api, calls} = apiWith(jsonResponse({body: {}}));

        await api.call({method: 'GET', path: 'v1/crm/a#b'});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/api/v1/crm/a%23b');
    });

    it('throws BytechefAppError on a transport failure', async () => {
        const {api} = apiWith(new Error('down'));

        await expect(api.call({method: 'GET', path: 'v1/a/b'})).rejects.toMatchObject({code: 'NETWORK_ERROR'});
    });
});

describe('api.list', () => {
    it('returns collections', async () => {
        const collection = {
            apiCollectionId: 1,
            available: true,
            collectionVersion: 1,
            contextPath: 'crm',
            endpoints: [],
            name: 'CRM',
        };
        const {api, calls} = apiWith(jsonResponse({body: [collection]}));

        await expect(api.list()).resolves.toEqual([collection]);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/api-collections');
    });
});
