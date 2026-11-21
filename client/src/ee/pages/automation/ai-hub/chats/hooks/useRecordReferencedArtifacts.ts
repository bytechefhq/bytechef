import {
    type AiHubTabType,
    type AiHubWorkflowTabI,
    getTabGenericId,
    useAiHubTabsStore,
} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubChatArtifactKind, useRecordReferencedAiHubChatArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';

import {isWorkspaceIdKnown} from '../isWorkspaceIdKnown';
import {AiHubChatsKeys} from './useChats';

/**
 * Watches the right-panel tabs store and records each open workflow tab as a {@code WORKFLOW_REFERENCED}, and each open
 * AI Agent tab as an {@code AI_AGENT_REFERENCED}, {@code ai_hub_chat_artifact} of the active chat, so a resource the user
 * opened shows up in the chat's artifact list.
 *
 * <p>Idempotency lives at two layers: the server short-circuits when a {@code (chatId, kind, artifactId)} row already
 * exists, and a per-mount seen set keeps a single open tab from re-firing the mutation across re-renders; the set is
 * cleared when the chat switches.</p>
 *
 * <p>Skips the home view, skips until the workspace store holds a real workspace id, and skips while the tabs store
 * still mirrors another chat: on a switch this hook sees the new {@code chatId} before the tabs store has swapped in
 * that chat's tabs, and recording then would file the previous chat's tabs under the new one.</p>
 *
 * <p>Records nothing while {@code enabled} is false, which the page passes for a workflow chat: its turns run a
 * workflow, not the hub agent, so a tab open beside it is not one of its artifacts.</p>
 */
export default function useRecordReferencedArtifacts(
    chatId: number | undefined,
    workspaceId: number | undefined,
    enabled = true
) {
    const seenRef = useRef<Set<string>>(new Set());
    const previousChatIdRef = useRef<number | undefined>(chatId);

    const openTabs = useAiHubTabsStore((state) => state.openTabs);
    const tabsActiveChatId = useAiHubTabsStore((state) => state.activeChatId);

    const queryClient = useQueryClient();

    const recordMutation = useRecordReferencedAiHubChatArtifactMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: [...AiHubChatsKeys.all, 'artifacts']});
        },
    });

    useEffect(() => {
        if (previousChatIdRef.current !== chatId) {
            seenRef.current = new Set();
            previousChatIdRef.current = chatId;
        }

        if (!enabled || chatId == null || tabsActiveChatId !== chatId || !isWorkspaceIdKnown(workspaceId)) {
            return;
        }

        for (const tab of openTabs) {
            const artifactId = getTabGenericId(tab);
            const kind = toArtifactKind(tab);

            const dedupeKey = `${chatId}:${kind}:${artifactId}`;

            if (seenRef.current.has(dedupeKey)) {
                continue;
            }

            seenRef.current.add(dedupeKey);

            recordMutation.mutate({
                input: {
                    artifactId,
                    artifactName: tab.name,
                    chatId: String(chatId),
                    kind,
                    metadataJson: tab.kind === 'workflow' ? JSON.stringify(toWorkflowMetadata(tab)) : undefined,
                    workspaceId: String(workspaceId),
                },
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [enabled, openTabs, chatId, tabsActiveChatId, workspaceId]);
}

function toArtifactKind(tab: AiHubTabType): AiHubChatArtifactKind {
    return tab.kind === 'aiAgent' ? AiHubChatArtifactKind.AiAgentReferenced : AiHubChatArtifactKind.WorkflowReferenced;
}

function toWorkflowMetadata(tab: AiHubWorkflowTabI): {projectId: string; projectWorkflowId: number} {
    return {projectId: tab.projectId, projectWorkflowId: tab.projectWorkflowId};
}
