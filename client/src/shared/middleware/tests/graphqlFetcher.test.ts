import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {USER_GRAPHQL_OPERATION_NAMES, fetcher} from '../graphqlFetcher';

const OPERATION_NAME_PATTERN = /^\s*(?:query|mutation)\s+(\w+)/;

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

    it('lists every operation in src/graphql/platform/user', () => {
        const operationNames = Object.values(userOperationDocuments).map(
            (document) => OPERATION_NAME_PATTERN.exec(document)?.[1]
        );

        expect(new Set(operationNames)).toEqual(USER_GRAPHQL_OPERATION_NAMES);
    });
});
