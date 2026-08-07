import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubTaskArtifactI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {useAiHubTaskArtifactsQuery, useAiHubTasksQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export interface AiHubArtifactsCardStateI {
    artifacts: AiHubTaskArtifactI[];
    visible: boolean;
    workspaceId: number;
}

/**
 * Whether the {@link AiHubArtifactsCard} shows, and the artifacts it shows.
 *
 * <p>Extracted from the card so {@link AiHubPanel} can answer the same question without rendering it: the
 * card floats over the transcript, so the thread insets its content by the card's width while it is up.
 * Both callers hit the same two react-query keys, so the second call is a cache read, not a fetch.</p>
 *
 * <p>The card is hidden whenever the resource panel is open — the panel's tab strip already lists what has
 * been opened, and the chat pane narrows to ~38% in that layout, leaving no room for a 256px overlay — for
 * WORKFLOW_CHAT tasks (webhook-routed, so they never accrue artifacts), on the home view, and when the
 * task has no artifacts at all. A floating "No artifacts yet" card would be pure noise over the
 * transcript; that message lives in the resource panel's + menu instead, which the user opens
 * deliberately.</p>
 */
export default function useAiHubArtifactsCard(): AiHubArtifactsCardStateI {
    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // Cache lookup, not a fetch — the sidebar already loaded this list under the same query key.
    const {data: tasks} = useAiHubTasksQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');

    const currentTask = tasks?.find((task) => task.id === currentTaskId);
    // Workflow chats route through a webhook rather than the LLM agent, so they never accrue artifacts.
    const isWorkflowChat = currentTask?.kind === 'WORKFLOW_CHAT';

    const enabled = currentTaskId != null && currentWorkspaceId != null && !rightPanelOpen && !isWorkflowChat;

    const {data: artifacts} = useAiHubTaskArtifactsQuery(currentTaskId, currentWorkspaceId ?? 0, enabled);

    return {
        artifacts: artifacts ?? [],
        visible: enabled && !!artifacts && artifacts.length > 0,
        workspaceId: currentWorkspaceId,
    };
}
