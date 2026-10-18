import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useTruncateAiHubChatMessagesMutation: vi.fn(),
}));

const {useTruncateAiHubChatMessagesMutation: useGeneratedTruncateMutation} =
    await import('@/shared/middleware/graphql');

import {AiHubChatsKeys} from '../useChats';
import {useTruncateAiHubChatMessagesMutation} from '../useTruncateChatMessages';

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

describe('useTruncateAiHubChatMessagesMutation', () => {
    it('invalidates the chat messages cache key on success', () => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

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

        renderHook(() => useTruncateAiHubChatMessagesMutation(), {wrapper: wrap(queryClient)});

        act(() => {
            capturedOnSuccess?.(undefined, {fromMessageIndex: 3, id: '42', workspaceId: '7'});
        });

        expect(invalidateSpy).toHaveBeenCalledWith({
            queryKey: AiHubChatsKeys.messages(42, 7),
        });
    });
});
