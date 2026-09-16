import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAutoMemoriesQuery: vi.fn(),
    useAiAutoMemoryPrincipalsQuery: vi.fn(),
    useAiAutoMemoryQuery: vi.fn(),
    useDeleteAiAutoMemoryMutation: vi.fn(),
    useUpdateAiAutoMemoryMutation: vi.fn(),
}));

const {
    useAiAutoMemoriesQuery: useGeneratedMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery: useGeneratedPrincipalsQuery,
    useAiAutoMemoryQuery: useGeneratedMemoryQuery,
    useDeleteAiAutoMemoryMutation: useGeneratedDeleteMutation,
    useUpdateAiAutoMemoryMutation: useGeneratedUpdateMutation,
} = await import('@/shared/middleware/graphql');

import {
    AiAutoMemoriesKeys,
    AiAutoMemoryI,
    useAiAutoMemoriesQuery,
    useAiAutoMemoryDetailQuery,
    useAiAutoMemoryPrincipalsQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} from '../useAiAutoMemories';

const mockUseGeneratedMemoriesQuery = vi.mocked(useGeneratedMemoriesQuery);
const mockUseGeneratedMemoryQuery = vi.mocked(useGeneratedMemoryQuery);
const mockUseGeneratedPrincipalsQuery = vi.mocked(useGeneratedPrincipalsQuery);
const mockUseGeneratedDeleteMutation = vi.mocked(useGeneratedDeleteMutation);
const mockUseGeneratedUpdateMutation = vi.mocked(useGeneratedUpdateMutation);

const makeQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {retry: false},
        },
    });

const wrap = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

const GRAPHQL_MEMORY = {
    createdAt: '1767225600000',
    description: null,
    environmentId: '1',
    id: '5',
    memoryType: 'FEEDBACK',
    name: 'user_profile',
    principalId: '9',
    principalType: 'PROJECT_DEPLOYMENT',
    title: 'User profile',
    updatedAt: null,
    version: '3',
    workspaceId: '7',
};

const MAPPED_MEMORY = {
    createdAt: '2026-01-01T00:00:00.000Z',
    description: null,
    environmentId: 1,
    id: 5,
    memoryType: 'FEEDBACK',
    name: 'user_profile',
    principalId: 9,
    principalType: 'PROJECT_DEPLOYMENT',
    title: 'User profile',
    updatedAt: '',
    version: 3,
    workspaceId: 7,
};

beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    mockUseGeneratedMemoriesQuery.mockReset();
    mockUseGeneratedPrincipalsQuery.mockReset();
    mockUseGeneratedDeleteMutation.mockReset();
    mockUseGeneratedUpdateMutation.mockReset();
});

describe('AiAutoMemoriesKeys', () => {
    it('keys list scoped by workspaceId, environmentId, memoryType, and principal', () => {
        expect(AiAutoMemoriesKeys.list(7, 0)).toEqual([
            'aiAutoMemories',
            'list',
            7,
            0,
            'ALL',
            'ALL_OWNERS',
            'ALL_OWNERS',
        ]);
        expect(AiAutoMemoriesKeys.list(7, 1, 'FEEDBACK')).toEqual([
            'aiAutoMemories',
            'list',
            7,
            1,
            'FEEDBACK',
            'ALL_OWNERS',
            'ALL_OWNERS',
        ]);
        expect(AiAutoMemoriesKeys.list(7, 1, undefined, 'PROJECT_DEPLOYMENT', 9)).toEqual([
            'aiAutoMemories',
            'list',
            7,
            1,
            'ALL',
            'PROJECT_DEPLOYMENT',
            9,
        ]);
    });
});

describe('useAiAutoMemoriesQuery', () => {
    it('passes workspaceId, environment, memoryType to the generated query', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(7, 1, 'FEEDBACK'), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedMemoriesQuery).toHaveBeenCalledWith(
            {environment: 1, memoryType: 'FEEDBACK', workspaceId: '7'},
            expect.objectContaining({
                enabled: true,
                queryKey: AiAutoMemoriesKeys.list(7, 1, 'FEEDBACK'),
            })
        );
    });

    it('disables the query when workspaceId is 0', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(0, 0), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedMemoriesQuery).toHaveBeenCalledWith(
            expect.any(Object),
            expect.objectContaining({enabled: false})
        );
    });

    it('forwards the principal to the generated query', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(7, 1, undefined, 'PROJECT_DEPLOYMENT', 9), {
            wrapper: wrap(makeQueryClient()),
        });

        expect(mockUseGeneratedMemoriesQuery).toHaveBeenCalledWith(
            {
                environment: 1,
                memoryType: undefined,
                principal: {principalId: 9, principalType: 'PROJECT_DEPLOYMENT'},
                workspaceId: '7',
            },
            expect.objectContaining({
                queryKey: AiAutoMemoriesKeys.list(7, 1, undefined, 'PROJECT_DEPLOYMENT', 9),
            })
        );
    });

    it('sends no principal for the All scope', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(7, 1), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedMemoriesQuery).toHaveBeenCalledWith(
            expect.objectContaining({principal: undefined}),
            expect.anything()
        );
    });
});

describe('useAiAutoMemoriesQuery select', () => {
    it('maps Long ids and epoch timestamps and keeps a missing description null', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(7, 1), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {select} = mockUseGeneratedMemoriesQuery.mock.lastCall![1] as any;

        expect(select({aiAutoMemories: [GRAPHQL_MEMORY]})).toEqual([MAPPED_MEMORY]);
    });
});

describe('useAiAutoMemoryPrincipalsQuery', () => {
    it('maps the Long principalId to a number and keeps the server-resolved label untouched', async () => {
        mockUseGeneratedPrincipalsQuery.mockImplementation((_variables, options) => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const select = (options as any).select;

            return {
                data: select({
                    aiAutoMemoryPrincipals: [
                        {label: 'My memories', memoryCount: 2, principalId: '42', principalType: 'USER'},
                        {
                            label: 'Support triage deployment',
                            memoryCount: 5,
                            principalId: '9',
                            principalType: 'PROJECT_DEPLOYMENT',
                        },
                    ],
                }),
                error: null,
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any;
        });

        const {result} = renderHook(() => useAiAutoMemoryPrincipalsQuery(7, 1), {wrapper: wrap(makeQueryClient())});

        await waitFor(() => expect(result.current.data).toHaveLength(2));

        expect(result.current.data).toEqual([
            {label: 'My memories', memoryCount: 2, principalId: 42, principalType: 'USER'},
            {label: 'Support triage deployment', memoryCount: 5, principalId: 9, principalType: 'PROJECT_DEPLOYMENT'},
        ]);
    });

    it('disables the query when workspaceId is 0', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedPrincipalsQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoryPrincipalsQuery(0, 0), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedPrincipalsQuery).toHaveBeenCalledWith(
            {environment: 0, workspaceId: '0'},
            expect.objectContaining({enabled: false, queryKey: AiAutoMemoriesKeys.principals(0, 0)})
        );
    });
});

function seedMemoryCache(queryClient: QueryClient) {
    const keys = {
        foreignWorkspaceList: AiAutoMemoriesKeys.list(8, 1),
        foreignWorkspacePrincipals: AiAutoMemoriesKeys.principals(8, 1),
        otherEnvironmentList: AiAutoMemoriesKeys.list(7, 2),
        ownerList: AiAutoMemoriesKeys.list(7, 1, 'FEEDBACK', 'PROJECT_DEPLOYMENT', 9),
        principals: AiAutoMemoriesKeys.principals(7, 1),
        unfilteredList: AiAutoMemoriesKeys.list(7, 1),
    };

    for (const key of Object.values(keys)) {
        queryClient.setQueryData(key, {});
    }

    return keys;
}

const isInvalidated = (queryClient: QueryClient, queryKey: readonly unknown[]) =>
    queryClient.getQueryState(queryKey)?.isInvalidated;

describe('useUpdateAiAutoMemoryMutation', () => {
    it("refreshes every cached list and the owners of the memory's workspace after an update", async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const keys = seedMemoryCache(queryClient);

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSettled} = mockUseGeneratedUpdateMutation.mock.lastCall![0] as any;

        await onSettled(undefined, null, {input: {environment: 1, id: '5', workspaceId: '7'}});

        expect(isInvalidated(queryClient, keys.unfilteredList)).toBe(true);
        expect(isInvalidated(queryClient, keys.ownerList)).toBe(true);
        expect(isInvalidated(queryClient, keys.otherEnvironmentList)).toBe(true);
        expect(isInvalidated(queryClient, keys.principals)).toBe(true);
        expect(isInvalidated(queryClient, keys.foreignWorkspaceList)).toBe(false);
        expect(isInvalidated(queryClient, keys.foreignWorkspacePrincipals)).toBe(false);
    });

    it('refreshes the cached lists when an update is rejected because the memory changed meanwhile', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const keys = seedMemoryCache(queryClient);

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSettled} = mockUseGeneratedUpdateMutation.mock.lastCall![0] as any;

        await onSettled(undefined, new Error('BAD_REQUEST'), {input: {environment: 1, id: '5', workspaceId: '7'}});

        expect(isInvalidated(queryClient, keys.unfilteredList)).toBe(true);
        expect(isInvalidated(queryClient, keys.principals)).toBe(true);
    });

    it('leaves reporting a failed update to the caller', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedUpdateMutation.mock.lastCall![0] as any).onError).toBeUndefined();
    });
});

describe('useDeleteAiAutoMemoryMutation', () => {
    it("refreshes every cached list and the owners of the memory's workspace after a delete", async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const keys = seedMemoryCache(queryClient);

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSettled} = mockUseGeneratedDeleteMutation.mock.lastCall![0] as any;

        await onSettled(undefined, null, {environment: 1, id: '5', workspaceId: '7'});

        expect(isInvalidated(queryClient, keys.unfilteredList)).toBe(true);
        expect(isInvalidated(queryClient, keys.ownerList)).toBe(true);
        expect(isInvalidated(queryClient, keys.otherEnvironmentList)).toBe(true);
        expect(isInvalidated(queryClient, keys.principals)).toBe(true);
        expect(isInvalidated(queryClient, keys.foreignWorkspaceList)).toBe(false);
        expect(isInvalidated(queryClient, keys.foreignWorkspacePrincipals)).toBe(false);
    });

    it('refreshes the cached lists when a delete fails, since NotFound means the row is already gone', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const keys = seedMemoryCache(queryClient);

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSettled} = mockUseGeneratedDeleteMutation.mock.lastCall![0] as any;

        await onSettled(undefined, new Error('NOT_FOUND'), {environment: 1, id: '5', workspaceId: '7'});

        expect(isInvalidated(queryClient, keys.unfilteredList)).toBe(true);
        expect(isInvalidated(queryClient, keys.principals)).toBe(true);
    });

    it('leaves reporting a failed delete to the caller', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue({} as any);

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedDeleteMutation.mock.lastCall![0] as any).onError).toBeUndefined();
    });
});

describe('useAiAutoMemoryDetailQuery', () => {
    const LIST_MEMORY: AiAutoMemoryI = {
        createdAt: '2026-04-01T00:00:00Z',
        description: null,
        environmentId: 1,
        id: 5,
        memoryType: 'FEEDBACK',
        name: 'user_profile',
        principalId: 9,
        principalType: 'PROJECT_DEPLOYMENT',
        title: 'User profile',
        updatedAt: '2026-04-10T00:00:00Z',
        version: 3,
        workspaceId: 7,
    };

    beforeEach(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null} as any);
    });

    it('asks for the row own owner and keys the cache by it', () => {
        renderHook(() => useAiAutoMemoryDetailQuery(LIST_MEMORY, 7), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedMemoryQuery).toHaveBeenCalledWith(
            {
                environment: 1,
                id: '5',
                principal: {principalId: 9, principalType: 'PROJECT_DEPLOYMENT'},
                workspaceId: '7',
            },
            expect.objectContaining({
                enabled: true,
                queryKey: AiAutoMemoriesKeys.detail(5, 7, 1, 'PROJECT_DEPLOYMENT', 9),
            })
        );
    });

    it('stays idle without a memory, while it is not enabled, and without a workspace', () => {
        renderHook(() => useAiAutoMemoryDetailQuery(null, 7), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedMemoryQuery.mock.lastCall![1] as any).enabled).toBe(false);

        renderHook(() => useAiAutoMemoryDetailQuery(LIST_MEMORY, 7, false), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedMemoryQuery.mock.lastCall![1] as any).enabled).toBe(false);

        renderHook(() => useAiAutoMemoryDetailQuery(LIST_MEMORY, 0), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedMemoryQuery.mock.lastCall![1] as any).enabled).toBe(false);
    });

    it('maps the returned memory with its content and answers null when it is gone', () => {
        renderHook(() => useAiAutoMemoryDetailQuery(LIST_MEMORY, 7), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {select} = mockUseGeneratedMemoryQuery.mock.lastCall![1] as any;

        expect(select({aiAutoMemory: {...GRAPHQL_MEMORY, content: 'Alice prefers concise replies.'}})).toEqual({
            ...MAPPED_MEMORY,
            content: 'Alice prefers concise replies.',
        });
        expect(select({aiAutoMemory: null})).toBeNull();
    });
});
