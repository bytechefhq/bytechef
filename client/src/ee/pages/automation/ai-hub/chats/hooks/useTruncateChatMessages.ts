import {reportMutationError} from '@/shared/error/useReportQueryError';
import {useTruncateAiHubChatMessagesMutation as useGeneratedTruncateChatMessagesMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

import {AiHubChatsKeys} from './useChats';

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
