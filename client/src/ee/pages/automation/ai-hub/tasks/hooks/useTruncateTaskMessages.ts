import {reportMutationError} from '@/shared/error/useReportQueryError';
import {useTruncateAiHubTaskMessagesMutation as useGeneratedTruncateTaskMessagesMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

import {AiHubTasksKeys} from './useTasks';

/**
 * Edit-and-resend support: truncates the chat-memory history of a task at the supplied message index so
 * the next {@code runAgent} call re-runs from the edit point. The mutation is the server-side leg; the UI leg
 * (pencil icon on user messages, inline editor, re-send) consumes this hook + the regular runAgent path.
 *
 * <p>
 * Invalidates the task's loaded-messages cache on success so the message list refetches and the edited
 * branch surfaces immediately. Without the invalidation the client's cached message list still shows the deleted
 * messages until the next task switch.
 * </p>
 */
export function useTruncateAiHubTaskMessagesMutation() {
    const queryClient = useQueryClient();

    return useGeneratedTruncateTaskMessagesMutation({
        onError: (error) => reportMutationError('Edit and resend', error as Error),
        onSuccess: (_data, variables) => {
            const truncateVars = variables as {fromMessageIndex: number; id: string; workspaceId: string};

            queryClient.invalidateQueries({
                queryKey: AiHubTasksKeys.messages(Number(truncateVars.id), Number(truncateVars.workspaceId)),
            });
        },
    });
}
