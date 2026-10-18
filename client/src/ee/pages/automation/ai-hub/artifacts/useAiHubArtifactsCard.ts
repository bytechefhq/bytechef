import {AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatArtifactsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';

export interface AiHubArtifactsCardStateI {
    artifacts: AiHubChatArtifactI[];
    visible: boolean;
    workspaceId: number;
}

export default function useAiHubArtifactsCard(): AiHubArtifactsCardStateI {
    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const enabled = currentChatId != null && isWorkspaceIdKnown(currentWorkspaceId) && !rightPanelOpen;

    const {data: artifacts} = useAiHubChatArtifactsQuery(currentChatId, currentWorkspaceId, enabled);

    return {
        artifacts: artifacts ?? [],
        visible: enabled && !!artifacts && artifacts.length > 0,
        workspaceId: currentWorkspaceId,
    };
}
