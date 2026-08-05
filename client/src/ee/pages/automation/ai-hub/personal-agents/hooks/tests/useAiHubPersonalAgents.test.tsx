import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useAiHubPersonalAgentQuery: vi.fn(),
    useAiHubPersonalAgentsQuery: vi.fn(),
    useCreateAiHubPersonalAgentMutation: vi.fn(),
    useDeleteAiHubPersonalAgentMutation: vi.fn(),
    useUpdateAiHubPersonalAgentMutation: vi.fn(),
}));

const {
    useAiHubPersonalAgentQuery: useGeneratedAgentQuery,
    useAiHubPersonalAgentsQuery: useGeneratedAgentsQuery,
    useCreateAiHubPersonalAgentMutation: useGeneratedCreateMutation,
    useDeleteAiHubPersonalAgentMutation: useGeneratedDeleteMutation,
    useUpdateAiHubPersonalAgentMutation: useGeneratedUpdateMutation,
} = await import('@/shared/middleware/graphql');

import {
    AiHubPersonalAgentsKeys,
    useAiHubPersonalAgentQuery,
    useAiHubPersonalAgentsQuery,
} from '../useAiHubPersonalAgents';

const mockUseGeneratedAgentsQuery = vi.mocked(useGeneratedAgentsQuery);
const mockUseGeneratedAgentQuery = vi.mocked(useGeneratedAgentQuery);
const mockUseGeneratedCreateMutation = vi.mocked(useGeneratedCreateMutation);
const mockUseGeneratedUpdateMutation = vi.mocked(useGeneratedUpdateMutation);
const mockUseGeneratedDeleteMutation = vi.mocked(useGeneratedDeleteMutation);

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
    mockUseGeneratedAgentsQuery.mockReset();
    mockUseGeneratedAgentQuery.mockReset();
    mockUseGeneratedCreateMutation.mockReset();
    mockUseGeneratedUpdateMutation.mockReset();
    mockUseGeneratedDeleteMutation.mockReset();
});

describe('AiHubPersonalAgentsKeys', () => {
    it('keys list scoped by workspaceId + environmentId', () => {
        // Cache invalidation correctness depends on the env dimension being part of the key. Without it,
        // switching environments would surface stale agents from the previous env.
        expect(AiHubPersonalAgentsKeys.list(7, 0)).toEqual(['aiHubPersonalAgents', 'list', 7, 0]);
        expect(AiHubPersonalAgentsKeys.list(7, 1)).toEqual(['aiHubPersonalAgents', 'list', 7, 1]);
    });

    it('detail key uses agentId + workspaceId', () => {
        expect(AiHubPersonalAgentsKeys.detail(3, 7)).toEqual(['aiHubPersonalAgents', 'detail', 3, 7]);
    });
});

describe('useAiHubPersonalAgentsQuery', () => {
    it('passes workspaceId + environment to the generated query', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedAgentsQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiHubPersonalAgentsQuery(7, 1), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedAgentsQuery).toHaveBeenCalledWith(
            {environment: 1, workspaceId: '7'},
            expect.objectContaining({
                enabled: true,
                queryKey: AiHubPersonalAgentsKeys.list(7, 1),
            })
        );
    });

    it('disables the query when workspaceId is 0', () => {
        // Workspace not yet resolved on first render — the hook MUST disable rather than fire a query against
        // workspaceId=0, which would either error out or return another workspace's data depending on the
        // server-side handling.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedAgentsQuery.mockReturnValue({data: [], error: null} as any);

        renderHook(() => useAiHubPersonalAgentsQuery(0, 0), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedAgentsQuery).toHaveBeenCalledWith(
            expect.any(Object),
            expect.objectContaining({enabled: false})
        );
    });
});

describe('useAiHubPersonalAgentQuery', () => {
    it('fetches a single agent by id', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedAgentQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiHubPersonalAgentQuery(42, 7), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedAgentQuery).toHaveBeenCalledWith(
            {id: '42', workspaceId: '7'},
            expect.objectContaining({
                enabled: true,
                queryKey: AiHubPersonalAgentsKeys.detail(42, 7),
            })
        );
    });

    it('disables when agentId is undefined', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedAgentQuery.mockReturnValue({data: null, error: null} as any);

        renderHook(() => useAiHubPersonalAgentQuery(undefined, 7), {wrapper: wrap(makeQueryClient())});

        expect(mockUseGeneratedAgentQuery).toHaveBeenCalledWith(
            expect.any(Object),
            expect.objectContaining({enabled: false})
        );
    });
});
