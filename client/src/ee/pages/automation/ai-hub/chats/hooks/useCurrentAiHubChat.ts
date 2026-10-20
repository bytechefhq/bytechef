import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export default function useCurrentAiHubChat(): AiHubChatI | undefined {
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: activeChats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');
    const {data: archivedChats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ARCHIVED');

    if (currentChatId == null) {
        return undefined;
    }

    return [...(activeChats ?? []), ...(archivedChats ?? [])].find((chat) => chat.id === currentChatId);
}
