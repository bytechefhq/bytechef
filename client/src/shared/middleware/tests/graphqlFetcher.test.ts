import {afterEach, describe, expect, it, vi} from 'vitest';

import {GraphQLResponseError, fetcher} from '../graphqlFetcher';

const respondWith = (status: number, body: unknown) =>
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
        new Response(JSON.stringify(body), {headers: {'Content-Type': 'application/json'}, status})
    );

describe('graphqlFetcher', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('throws a GraphQLResponseError with the message of a GraphQL error in the response', async () => {
        respondWith(200, {data: null, errors: [{message: 'Workflow Notify needs a value for accountId'}]});

        const error = await fetcher('query {}')().catch((caughtError: unknown) => caughtError);

        expect(error).toBeInstanceOf(GraphQLResponseError);
        expect(error).toHaveProperty('message', 'Workflow Notify needs a value for accountId');
    });

    it('throws a GraphQLResponseError with the server message of a failed response', async () => {
        respondWith(400, {errors: [{message: 'Configuration id=1 was not found'}]});

        const error = await fetcher('query {}')().catch((caughtError: unknown) => caughtError);

        expect(error).toBeInstanceOf(GraphQLResponseError);
        expect(error).toHaveProperty('message', 'Configuration id=1 was not found');
    });

    it('throws a plain error for a failed response that carries no server message', async () => {
        respondWith(502, {});

        const error = await fetcher('query {}')().catch((caughtError: unknown) => caughtError);

        expect(error).not.toBeInstanceOf(GraphQLResponseError);
        expect(error).toHaveProperty('message', 'GraphQL request failed with status 502');
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

        await expect(rejection).rejects.toBeInstanceOf(GraphQLResponseError);
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
