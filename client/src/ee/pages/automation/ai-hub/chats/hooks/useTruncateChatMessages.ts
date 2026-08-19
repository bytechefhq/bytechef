import {reportMutationError} from '@/shared/error/useReportQueryError';
import {useTruncateAiHubChatMessagesMutation as useGeneratedTruncateChatMessagesMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

import {AiHubChatsKeys} from './useChats';

/**
 * Edit-and-resend support: truncates the chat-memory history of a chat at the supplied message index so
 * the next {@code runAgent} call re-runs from the edit point. The mutation is the server-side leg; the UI leg
 * (pencil icon on user messages, inline editor, re-send) consumes this hook + the regular runAgent path.
 *
 * <p>
 * Invalidates the chat's loaded-messages cache on success so the message list refetches and the edited
 * branch surfaces immediately. Without the invalidation the client's cached message list still shows the deleted
 * messages until the next chat switch.
 * </p>
 */
export function useTruncateAiHubChatMessagesMutation() {
    const queryClient = useQueryClient();

    return useGeneratedTruncateChatMessagesMutation({
        onError: (error) => reportMutationError('Edit and resend', error as Error),
        onSuccess: (_data, variables) => {
            const truncateVars = variables as {fromMessageIndex: number; id: string; workspaceId: string};

            queryClient.invalidateQueries({
                queryKey: AiHubChatsKeys.messages(Number(truncateVars.id), Number(truncateVars.workspaceId)),
            });
        },
    });
}
