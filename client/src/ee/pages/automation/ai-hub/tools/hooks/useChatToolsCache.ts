import {useQueryClient} from '@tanstack/react-query';

export default function useChatToolsCache() {
    const queryClient = useQueryClient();

    return {
        invalidate: (chatId: string | number, workspaceId: string | number) => {
            queryClient.invalidateQueries({
                predicate: (query) => {
                    const key = query.queryKey;

                    if (!Array.isArray(key) || key.length === 0) {
                        return false;
                    }

                    const operationName = key[0];

                    return operationName === 'aiHubChatTools';
                },
            });

            void chatId;
            void workspaceId;
        },
    };
}
