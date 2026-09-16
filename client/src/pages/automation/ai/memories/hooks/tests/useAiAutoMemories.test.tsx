import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

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
const {toast} = await import('sonner');

import {
    AiAutoMemoriesKeys,
    useAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery,
    useAiAutoMemoryQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} from '../useAiAutoMemories';

const mockUseGeneratedMemoriesQuery = vi.mocked(useGeneratedMemoriesQuery);
const mockUseGeneratedPrincipalsQuery = vi.mocked(useGeneratedPrincipalsQuery);
const mockUseGeneratedMemoryQuery = vi.mocked(useGeneratedMemoryQuery);
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
    content: 'Alice prefers concise replies.',
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
    workspaceId: '7',
};

const MAPPED_MEMORY = {
    content: 'Alice prefers concise replies.',
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
    workspaceId: 7,
};

beforeEach(() => {
    vi.mocked(toast.error).mockClear();
    vi.spyOn(console, 'error').mockImplementation(() => {});

    mockUseGeneratedMemoriesQuery.mockReset();
    mockUseGeneratedPrincipalsQuery.mockReset();
    mockUseGeneratedMemoryQuery.mockReset();
    mockUseGeneratedDeleteMutation.mockReset();
    mockUseGeneratedUpdateMutation.mockReset();
});

describe('AiAutoMemoriesKeys', () => {
    it('keys list scoped by workspaceId, environmentId, memoryType, and principal', () => {
        // Probe-oracle defense for cache invalidation: the env tail of the key must
        // change when the user flips environments so the staging memory list does not
        // bleed into the production view via a stale cache hit. The principal pair is in
        // the key for the same reason — one owner's list must not answer another's read.
        expect(AiAutoMemoriesKeys.list(7, 0)).toEqual(['aiAutoMemories', 'list', 7, 0, 'ALL', 'SELF', 'SELF']);
        expect(AiAutoMemoriesKeys.list(7, 1, 'FEEDBACK')).toEqual([
            'aiAutoMemories',
            'list',
            7,
            1,
            'FEEDBACK',
            'SELF',
            'SELF',
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

    it('detail key uses memoryId + workspaceId + environmentId + principal pair', () => {
        // Same reason as the list key: the single fetch is environment- and owner-scoped server side, so the cached
        // detail of one environment or owner must not answer a read of the same id under another.
        expect(AiAutoMemoriesKeys.detail(3, 7, 0)).toEqual(['aiAutoMemories', 'detail', 3, 7, 0, 'SELF', 'SELF']);
        expect(AiAutoMemoriesKeys.detail(3, 7, 1)).toEqual(['aiAutoMemories', 'detail', 3, 7, 1, 'SELF', 'SELF']);
        expect(AiAutoMemoriesKeys.detail(3, 7, 1, 'PROJECT_DEPLOYMENT', 9)).toEqual([
            'aiAutoMemories',
            'detail',
            3,
            7,
            1,
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

    it('forwards the principal pair to the generated query', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoriesQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiAutoMemoriesQuery(7, 1, undefined, 'PROJECT_DEPLOYMENT', 9), {
            wrapper: wrap(makeQueryClient()),
        });

        expect(mockUseGeneratedMemoriesQuery).toHaveBeenCalledWith(
            {
                environment: 1,
                memoryType: undefined,
                principalId: 9,
                principalType: 'PROJECT_DEPLOYMENT',
                workspaceId: '7',
            },
            expect.objectContaining({
                queryKey: AiAutoMemoriesKeys.list(7, 1, undefined, 'PROJECT_DEPLOYMENT', 9),
            })
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

describe('useAiAutoMemoryQuery', () => {
    it('fetches a single memory by id', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null, isSuccess: true} as any);

        const {result} = renderHook(() => useAiAutoMemoryQuery(5, 7, 1), {wrapper: wrap(makeQueryClient())});

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(mockUseGeneratedMemoryQuery).toHaveBeenCalledWith(
            {environment: 1, id: '5', workspaceId: '7'},
            expect.objectContaining({enabled: true, queryKey: AiAutoMemoriesKeys.detail(5, 7, 1)})
        );
    });

    it('keys the detail by the requested owner', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiAutoMemoryQuery(5, 7, 1, true, 'PROJECT_DEPLOYMENT', 9), {
            wrapper: wrap(makeQueryClient()),
        });

        expect(mockUseGeneratedMemoryQuery).toHaveBeenCalledWith(
            {environment: 1, id: '5', principalId: 9, principalType: 'PROJECT_DEPLOYMENT', workspaceId: '7'},
            expect.objectContaining({queryKey: AiAutoMemoriesKeys.detail(5, 7, 1, 'PROJECT_DEPLOYMENT', 9)})
        );
    });

    it('does not fire when memoryId is undefined', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiAutoMemoryQuery(undefined, 7, 0), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedMemoryQuery).toHaveBeenCalledWith(
            expect.any(Object),
            expect.objectContaining({enabled: false})
        );
    });
});

describe('useAiAutoMemoryQuery select', () => {
    it('maps a returned memory and answers null when the memory is absent', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiAutoMemoryQuery(5, 7, 1), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {select} = mockUseGeneratedMemoryQuery.mock.lastCall![1] as any;

        expect(select({aiAutoMemory: GRAPHQL_MEMORY})).toEqual(MAPPED_MEMORY);
        expect(select({aiAutoMemory: null})).toBeNull();
    });
});

describe('useUpdateAiAutoMemoryMutation', () => {
    it('delegates to the generated mutation', () => {
        const generatedMutation = {mutateAsync: vi.fn()};

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue(generatedMutation as any);

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedUpdateMutation).toHaveBeenCalled();
    });
});

describe('useDeleteAiAutoMemoryMutation', () => {
    it('delegates to the generated mutation', () => {
        const generatedMutation = {mutateAsync: vi.fn()};

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue(generatedMutation as any);

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedDeleteMutation).toHaveBeenCalled();
    });
});

describe('mutation callbacks', () => {
    it('invalidates the workspace lists and the environment-scoped detail after an update', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSuccess} = mockUseGeneratedUpdateMutation.mock.lastCall![0] as any;

        onSuccess(undefined, {input: {environment: 1, id: '5', workspaceId: '7'}});

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['aiAutoMemories', 'list', 7]});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['aiAutoMemories', 'detail', 5, 7, 1]});
    });

    it('refetches the cached detail of every owner after an update', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const ownerDetailKey = AiAutoMemoriesKeys.detail(5, 7, 1, 'PROJECT_DEPLOYMENT', 9);

        queryClient.setQueryData(ownerDetailKey, {});

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSuccess} = mockUseGeneratedUpdateMutation.mock.lastCall![0] as any;

        await onSuccess(undefined, {input: {environment: 1, id: '5', workspaceId: '7'}});

        expect(queryClient.getQueryState(ownerDetailKey)?.isInvalidated).toBe(true);
    });

    it('leaves reporting a failed update to the caller', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedUpdateMutation.mockReturnValue({} as any);

        renderHook(() => useUpdateAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedUpdateMutation.mock.lastCall![0] as any).onError).toBeUndefined();
    });

    it('invalidates the workspace lists after a delete', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue({} as any);

        const queryClient = makeQueryClient();
        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(queryClient)});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {onSuccess} = mockUseGeneratedDeleteMutation.mock.lastCall![0] as any;

        onSuccess(undefined, {id: '5', workspaceId: '7'});

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['aiAutoMemories', 'list', 7]});
    });

    it('leaves reporting a failed delete to the caller', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedDeleteMutation.mockReturnValue({} as any);

        renderHook(() => useDeleteAiAutoMemoryMutation(), {wrapper: wrap(makeQueryClient())});

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((mockUseGeneratedDeleteMutation.mock.lastCall![0] as any).onError).toBeUndefined();
    });
});
