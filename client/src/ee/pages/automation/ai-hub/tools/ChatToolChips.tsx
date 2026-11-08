import Badge from '@/components/Badge/Badge';
import useChatToolsCache from '@/ee/pages/automation/ai-hub/tools/hooks/useChatToolsCache';
import {
    useAiHubChatToolsQuery,
    useRemoveAiHubChatToolMutation,
    useSetAiHubChatToolRequiresApprovalMutation,
} from '@/shared/middleware/graphql';
import {ShieldCheckIcon, ShieldIcon, WrenchIcon, XIcon} from 'lucide-react';

interface ChatToolChipsPropsI {
    chatId: string;
    workspaceId: number;
}

const ChatToolChips = ({chatId, workspaceId}: ChatToolChipsPropsI) => {
    const {invalidate} = useChatToolsCache();

    const {data} = useAiHubChatToolsQuery({
        chatId,
        workspaceId: String(workspaceId),
    });

    const removeMutation = useRemoveAiHubChatToolMutation({
        onSuccess: () => invalidate(chatId, workspaceId),
    });

    const setRequiresApprovalMutation = useSetAiHubChatToolRequiresApprovalMutation();

    const tools = data?.aiHubChatTools ?? [];

    if (tools.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-wrap gap-1.5 px-3 pt-2" data-testid="chat-tool-chips">
            {tools.map((tool) => (
                <Badge
                    className="flex items-center gap-1 bg-surface-warning-secondary px-2 py-0.5 text-xs font-normal text-content-warning-primary"
                    data-testid="chat-tool-chip"
                    key={`tool-${tool.chatToolId}`}
                    styleType="outline-outline"
                >
                    <WrenchIcon className="size-3" />

                    <span className="font-medium">{tool.componentName}:</span>

                    <span>{tool.clusterElementName}</span>

                    <button
                        aria-label={tool.requiresApproval ? 'Approval required' : 'Require approval'}
                        className="ml-0.5 rounded hover:opacity-75"
                        disabled={setRequiresApprovalMutation.isPending}
                        onClick={() =>
                            setRequiresApprovalMutation.mutate(
                                {
                                    chatToolId: tool.chatToolId,
                                    requiresApproval: !tool.requiresApproval,
                                    workspaceId: String(workspaceId),
                                },
                                {onSuccess: () => invalidate(chatId, workspaceId)}
                            )
                        }
                        type="button"
                    >
                        {tool.requiresApproval ? (
                            <ShieldCheckIcon className="size-3" />
                        ) : (
                            <ShieldIcon className="size-3" />
                        )}
                    </button>

                    <button
                        aria-label={`Remove ${tool.componentName} ${tool.clusterElementName}`}
                        className="ml-0.5 rounded hover:opacity-75"
                        disabled={removeMutation.isPending}
                        onClick={() =>
                            removeMutation.mutate({
                                chatToolId: tool.chatToolId,
                                workspaceId: String(workspaceId),
                            })
                        }
                        type="button"
                    >
                        <XIcon className="size-3" />
                    </button>
                </Badge>
            ))}
        </div>
    );
};

export default ChatToolChips;
