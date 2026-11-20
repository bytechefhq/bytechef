import {afterEach, describe, expect, it, vi} from 'vitest';

import {GraphQLResponseError, fetcher} from '../graphqlFetcher';

const respondWith = (status: number, body: unknown) =>
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), {status}));

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
});
