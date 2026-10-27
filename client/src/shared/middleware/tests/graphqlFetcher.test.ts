import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {GraphQLResponseError, USER_GRAPHQL_OPERATION_NAMES, fetcher, getOperationNames} from '../graphqlFetcher';

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

    it('sends the user schema header for a user operation after a fragment', async () => {
        await fetcher(
            'fragment UserFields on AdminUser { login }\nquery users { users { content { ...UserFields } } }'
        )();

        const [, init] = fetchMock.mock.calls[0];

        expect(init.headers['X-ByteChef-GraphQL-Schema']).toBe('user');
    });

    it('does not send the header when a document mixes user and other operations', async () => {
        await fetcher('query users { users { content { login } } }\nquery projects { projects { id } }')();

        const [, init] = fetchMock.mock.calls[0];

        expect(init.headers['X-ByteChef-GraphQL-Schema']).toBeUndefined();
    });

    it('reads every top-level operation name and skips fields, strings and comments', () => {
        const document = `
            # query commented { x }
            fragment ProjectFields on Project { query users }
            query projects($name: String = "query inner { x }") { projects { query deleteUser } }
            mutation updateUser { updateUser { id } }
            subscription jobs { jobs { id } }
            { anonymous }
        `;

        expect(getOperationNames(document)).toEqual(['projects', 'updateUser', 'jobs']);
    });

    it('lists every operation in src/graphql/platform/user', () => {
        const operationNames = Object.values(userOperationDocuments).flatMap((document) =>
            getOperationNames(document)
        );

        expect(new Set(operationNames)).toEqual(USER_GRAPHQL_OPERATION_NAMES);
    });
});
