import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useTruncateAiHubTaskMessagesMutation: vi.fn(),
}));

const {useTruncateAiHubTaskMessagesMutation: useGeneratedTruncateMutation} =
    await import('@/shared/middleware/graphql');

import {AiHubTasksKeys} from '../useTasks';
import {useTruncateAiHubTaskMessagesMutation} from '../useTruncateTaskMessages';

const mockUseGeneratedMutation = vi.mocked(useGeneratedTruncateMutation);

beforeEach(() => {
    mockUseGeneratedMutation.mockReset();
});

const wrap = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

describe('useTruncateAiHubTaskMessagesMutation', () => {
    it('invalidates the task messages cache key on success', async () => {
        // The hook's job after the mutation lands is to evict the cached message list so the UI shows the
        // truncated history immediately. Without this invalidation the user clicks "Save and resend" and
        // sees the OLD messages still rendered until they switch away and back.
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

        // Capture the onSuccess option the hook passes to react-query so we can fire it manually with the
        // shape the generated mutation would produce. Mocking the full @tanstack/react-query useMutation
        // would be overkill; this captures the contract the hook owns (the onSuccess invalidation) without
        // dragging in the full mutation lifecycle.
        let capturedOnSuccess: ((data: unknown, variables: unknown) => void) | undefined;

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseGeneratedMutation.mockImplementation((options: any) => {
            capturedOnSuccess = options.onSuccess;

            return {
                isError: false,
                isIdle: true,
                isPending: false,
                isSuccess: false,
                mutate: vi.fn(),
                mutateAsync: vi.fn(),
                reset: vi.fn(),
                status: 'idle',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any;
        });

        renderHook(() => useTruncateAiHubTaskMessagesMutation(), {wrapper: wrap(queryClient)});

        // Fire the captured onSuccess with the variables shape the generated mutation passes back. The hook
        // reads {fromMessageIndex, id, workspaceId} from variables and converts both ids to numbers.
        act(() => {
            capturedOnSuccess?.(undefined, {fromMessageIndex: 3, id: '42', workspaceId: '7'});
        });

        // Pin the exact key shape — same prefix the task-message query uses, with id+workspaceId
        // both coerced to number. A regression in the key shape would silently miss the invalidation.
        expect(invalidateSpy).toHaveBeenCalledWith({
            queryKey: AiHubTasksKeys.messages(42, 7),
        });
    });
});
