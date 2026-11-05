import {afterEach, describe, expect, it, vi} from 'vitest';

import {GraphQlResponseError, fetcher} from '../graphqlFetcher';

const respondWith = (status: number, body: unknown) =>
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
        new Response(JSON.stringify(body), {headers: {'Content-Type': 'application/json'}, status})
    );

describe('graphqlFetcher', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns the data of a successful response', async () => {
        respondWith(200, {data: {ok: true}});

        await expect(fetcher('query Q { ok }')()).resolves.toEqual({ok: true});
    });

    it('rejects with the first error message and its error code', async () => {
        respondWith(200, {
            data: null,
            errors: [{extensions: {errorCode: 'CONFLICT'}, message: 'Approval 4 is already SUPERSEDED'}],
        });

        const rejection = fetcher('mutation M { resolve }')();

        await expect(rejection).rejects.toBeInstanceOf(GraphQlResponseError);
        await expect(rejection).rejects.toMatchObject({
            errorCode: 'CONFLICT',
            message: 'Approval 4 is already SUPERSEDED',
        });
    });

    it('rejects without an error code when the server sent none', async () => {
        respondWith(200, {data: null, errors: [{message: 'Something failed'}]});

        await expect(fetcher('query Q { ok }')()).rejects.toMatchObject({
            errorCode: undefined,
            message: 'Something failed',
        });
    });

    it('rejects a non-2xx response with its error code, or with its status when the body has no message', async () => {
        respondWith(500, {errors: [{extensions: {errorCode: 'INTERNAL'}, message: 'Server exploded'}]});

        await expect(fetcher('query Q { ok }')()).rejects.toMatchObject({
            errorCode: 'INTERNAL',
            message: 'Server exploded',
        });

        respondWith(502, {});

        await expect(fetcher('query Q { ok }')()).rejects.toMatchObject({
            message: 'GraphQL request failed with status 502',
        });
    });
});
