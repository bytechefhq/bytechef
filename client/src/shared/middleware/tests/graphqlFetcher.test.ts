import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {GraphQLResponseError, USER_GRAPHQL_OPERATION_NAMES, fetcher} from '../graphqlFetcher';

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

const OPERATION_NAME_PATTERN = /^\s*(?:query|mutation)\s+(\w+)/;

const userOperationDocuments = import.meta.glob<string>('/src/graphql/platform/user/*.graphql', {
    eager: true,
    import: 'default',
    query: '?raw',
});

describe('graphqlFetcher schema header', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockResolvedValue({
            json: async () => ({data: {}}),
            ok: true,
        });

        vi.stubGlobal('fetch', fetchMock);
    });

    afterEach(() => {
        fetchMock.mockReset();

        vi.unstubAllGlobals();
    });

    it('sends the user schema header for user operations', async () => {
        await fetcher('query users { users { content { login } } }')();

        const [url, init] = fetchMock.mock.calls[0];

        expect(url).toBe('/graphql');
        expect(init.headers['X-ByteChef-GraphQL-Schema']).toBe('user');
    });

    it('does not send the header for other operations', async () => {
        await fetcher('query projects { projects { id } }')();

        const [, init] = fetchMock.mock.calls[0];

        expect(init.headers['X-ByteChef-GraphQL-Schema']).toBeUndefined();
    });

    it('lists every operation in src/graphql/platform/user', () => {
        const operationNames = Object.values(userOperationDocuments).map(
            (document) => OPERATION_NAME_PATTERN.exec(document)?.[1]
        );

        expect(new Set(operationNames)).toEqual(USER_GRAPHQL_OPERATION_NAMES);
    });
});
