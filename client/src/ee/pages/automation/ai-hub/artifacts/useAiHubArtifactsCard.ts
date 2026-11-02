import {AiHubChatArtifactI, isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatArtifactsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import useCurrentAiHubChat from '@/ee/pages/automation/ai-hub/chats/hooks/useCurrentAiHubChat';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';

export interface AiHubArtifactsCardStateI {
    artifacts: AiHubChatArtifactI[];
    visible: boolean;
    workspaceId: number | undefined;
}

/**
 * Whether the {@link AiHubArtifactsCard} shows, and the artifacts it shows.
 *
 * <p>Extracted from the card so {@link AiHubPanel} can answer the same question without rendering it: the card floats
 * over the transcript, so the thread insets its content by the card's width while it is up. Both callers hit the same
 * react-query key, so the second call is a cache read.</p>
 *
 * <p>The card is hidden whenever the resource panel is open (its tab strip already lists what has been opened, and the
 * chat pane is too narrow for the overlay), for a workflow chat (its turns run a workflow, not the hub agent, so it never
 * accrues artifacts), on the home view, and when the chat has no artifacts at all.</p>
 */
export default function useAiHubArtifactsCard(): AiHubArtifactsCardStateI {
    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const currentChat = useCurrentAiHubChat();

    const enabled =
        currentChatId != null &&
        isWorkspaceIdKnown(currentWorkspaceId) &&
        !rightPanelOpen &&
        !isWebhookBridgedChat(currentChat?.kind);

    const {data: artifacts} = useAiHubChatArtifactsQuery(currentChatId, currentWorkspaceId, enabled);

    return {
        artifacts: artifacts ?? [],
        visible: enabled && !!artifacts && artifacts.length > 0,
        workspaceId: currentWorkspaceId,
    };
}
