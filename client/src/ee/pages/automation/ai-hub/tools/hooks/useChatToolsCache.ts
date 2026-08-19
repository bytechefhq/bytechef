import {useQueryClient} from '@tanstack/react-query';

/**
 * Centralizes the cache invalidation keys for chat-tool mutations. Every mutation that
 * mutates the `ai_hub_chat_tool` or `ai_hub_chat_component` rows on the server should call
 * `invalidate(chatId, workspaceId)` so the chip list and toolable-components catalog both
 * refetch.
 *
 * Keeping the keys in one place avoids the typical "I added a new mutation but forgot to invalidate
 * the chip list" bug that shows up when the same mutation is called from two unrelated UI surfaces
 * (the LLM chat affordance and the plus-button menu).
 */
export default function useChatToolsCache() {
    const queryClient = useQueryClient();

    return {
        invalidate: (chatId: string | number, workspaceId: string | number) => {
            // The codegen hook generates query keys as ['<operationName>', variables]. Match by the
            // operationName prefix so we don't have to keep the variables in sync — partial matching
            // catches the chat regardless of which workspace it was last fetched for.
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

            // No-op refs to encourage callers to actually pass these — the args matter when the
            // backend grows per-workspace caches even if today's invalidation is broader.
            void chatId;
            void workspaceId;
        },
    };
}
