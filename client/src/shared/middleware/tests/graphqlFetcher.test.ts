import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {USER_GRAPHQL_OPERATION_NAMES, fetcher, getOperationNames} from '../graphqlFetcher';

const userOperationDocuments = import.meta.glob<string>('/src/graphql/platform/user/*.graphql', {
    eager: true,
    import: 'default',
    query: '?raw',
});

describe('graphqlFetcher', () => {
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
