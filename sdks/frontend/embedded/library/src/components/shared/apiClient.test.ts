import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiResponseError, createApiClient} from './apiClient';

const response = (status: number, body: unknown, contentLength: string | null = null) => ({
    headers: {get: (name: string) => (name === 'content-length' ? contentLength : null)},
    json: () => (body instanceof Error ? Promise.reject(body) : Promise.resolve(body)),
    ok: status < 400,
    status,
});

describe('createApiClient', () => {
    const {fetch: apiFetch} = createApiClient('https://api.example.com', 'PRODUCTION', 'ey');

    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('sends the auth and environment headers and returns the JSON body', async () => {
        global.fetch = vi.fn().mockResolvedValue(response(200, [1, 2]));

        await expect(apiFetch<number[]>('/items')).resolves.toEqual([1, 2]);

        expect(global.fetch).toHaveBeenCalledWith(
            'https://api.example.com/items',
            expect.objectContaining({
                headers: expect.objectContaining({Authorization: 'Bearer ey', 'X-Environment': 'PRODUCTION'}),
                method: 'GET',
            })
        );
    });

    it.each([
        ['204', response(204, new SyntaxError('no body'))],
        ['an empty content length', response(200, new SyntaxError('no body'), '0')],
    ])('resolves with undefined for a POST answered with %s', async (_case, emptyResponse) => {
        global.fetch = vi.fn().mockResolvedValue(emptyResponse);

        await expect(apiFetch('/enable', {method: 'POST'})).resolves.toBeUndefined();
    });

    it('rejects a successful response whose body is not JSON instead of inventing an empty object', async () => {
        global.fetch = vi.fn().mockResolvedValue(response(200, new SyntaxError('Unexpected token <')));

        await expect(apiFetch('/items')).rejects.toBeInstanceOf(SyntaxError);
    });

    it('rejects a non-2xx response with its status and body', async () => {
        global.fetch = vi.fn().mockResolvedValue(response(409, {missingConnectionComponentName: 'slack'}));

        const error = await apiFetch('/provision', {method: 'POST'}).catch((caught: unknown) => caught);

        expect(error).toBeInstanceOf(ApiResponseError);
        expect(error).toMatchObject({
            body: {missingConnectionComponentName: 'slack'},
            name: 'ApiResponseError',
            status: 409,
        });
    });
});
