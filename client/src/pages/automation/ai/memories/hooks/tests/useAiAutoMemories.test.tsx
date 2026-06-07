import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAutoMemoriesQuery: vi.fn(),
    useAiAutoMemoryQuery: vi.fn(),
    useDeleteAiAutoMemoryMutation: vi.fn(),
    useUpdateAiAutoMemoryMutation: vi.fn(),
}));

const {
    useAiAutoMemoriesQuery: useGeneratedMemoriesQuery,
    useAiAutoMemoryQuery: useGeneratedMemoryQuery,
    useDeleteAiAutoMemoryMutation: useGeneratedDeleteMutation,
    useUpdateAiAutoMemoryMutation: useGeneratedUpdateMutation,
} = await import('@/shared/middleware/graphql');

import {
    AiAutoMemoriesKeys,
    useAiAutoMemoriesQuery,
    useAiAutoMemoryQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} from '../useAiAutoMemories';

const mockUseGeneratedMemoriesQuery = vi.mocked(useGeneratedMemoriesQuery);
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
    mockUseGeneratedMemoryQuery.mockReset();
    mockUseGeneratedDeleteMutation.mockReset();
    mockUseGeneratedUpdateMutation.mockReset();
});

describe('AiAutoMemoriesKeys', () => {
    it('keys list scoped by workspaceId, environmentId, and memoryType', () => {
        // Probe-oracle defense for cache invalidation: the env tail of the key must
        // change when the user flips environments so the staging memory list does not
        // bleed into the production view via a stale cache hit.
        expect(AiAutoMemoriesKeys.list(7, 0)).toEqual(['aiAutoMemories', 'list', 7, 0, 'ALL']);
        expect(AiAutoMemoriesKeys.list(7, 1, 'FEEDBACK')).toEqual(['aiAutoMemories', 'list', 7, 1, 'FEEDBACK']);
    });

    it('detail key uses memoryId + workspaceId', () => {
        expect(AiAutoMemoriesKeys.detail(3, 7)).toEqual(['aiAutoMemories', 'detail', 3, 7]);
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
});

describe('useAiAutoMemoryQuery', () => {
    it('fetches a single memory by id', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null, isSuccess: true} as any);

        const {result} = renderHook(() => useAiAutoMemoryQuery(5, 7), {wrapper: wrap(makeQueryClient())});

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(mockUseGeneratedMemoryQuery).toHaveBeenCalledWith(
            {id: '5', workspaceId: '7'},
            expect.objectContaining({enabled: true})
        );
    });

    it('does not fire when memoryId is undefined', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMemoryQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiAutoMemoryQuery(undefined, 7), {wrapper: wrap(makeQueryClient())});

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
