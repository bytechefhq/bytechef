import {reportMutationError} from '@/shared/error/useReportQueryError';
import {useTruncateAiHubChatMessagesMutation as useGeneratedTruncateChatMessagesMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

import {AiHubChatsKeys} from './useChats';

/**
 * Edit-and-resend support: truncates a chat's history at the supplied message index so the next run starts from the
 * edit point, then invalidates the chat's loaded messages.
 */
export function useTruncateAiHubChatMessagesMutation() {
    const queryClient = useQueryClient();

    return useGeneratedTruncateChatMessagesMutation({
        onError: (error) => reportMutationError('Edit and resend', error as Error),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: AiHubChatsKeys.messages(Number(variables.id), Number(variables.workspaceId)),
            });
        },
    });
}
