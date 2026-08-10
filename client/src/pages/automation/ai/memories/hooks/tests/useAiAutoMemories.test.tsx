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

beforeEach(() => {
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

    it('detail key uses memoryId + workspaceId + environmentId', () => {
        // Same reason as the list key: the single fetch is environment-scoped server side, so the cached
        // detail of a development memory must not answer a production read of the same id.
        expect(AiAutoMemoriesKeys.detail(3, 7, 0)).toEqual(['aiAutoMemories', 'detail', 3, 7, 0]);
        expect(AiAutoMemoriesKeys.detail(3, 7, 1)).toEqual(['aiAutoMemories', 'detail', 3, 7, 1]);
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
