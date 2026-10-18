import {type AiHubTabType, useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubChatArtifactKind, useRecordReferencedAiHubChatArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';

import {isWorkspaceIdKnown} from '../isWorkspaceIdKnown';
import {AiHubChatsKeys} from './useChats';

/**
 * Watches the right-panel tabs store and records each open workflow tab as a {@code WORKFLOW_REFERENCED}
 * {@code ai_hub_chat_artifact} of the active chat, so a workflow the user opened shows up in the chat's artifact list.
 *
 * <p>Idempotency lives at two layers: the server short-circuits when a {@code (chatId, kind, artifactId)} row already
 * exists, and a per-mount seen set keeps a single open tab from re-firing the mutation across re-renders; the set is
 * cleared when the chat switches.</p>
 *
 * <p>Skips the home view, skips until the workspace store holds a real workspace id, and skips while the tabs store still mirrors another chat: on a switch this hook sees the new
 * {@code chatId} before the tabs store has swapped in that chat's tabs, and recording then would file the previous
 * chat's tabs under the new one.</p>
 */
export default function useRecordReferencedArtifacts(chatId: number | undefined, workspaceId: number | undefined) {
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

        if (chatId == null || tabsActiveChatId !== chatId || !isWorkspaceIdKnown(workspaceId)) {
            return;
        }

        for (const tab of openTabs) {
            const dedupeKey = `${chatId}:${AiHubChatArtifactKind.WorkflowReferenced}:${tab.workflowId}`;

            if (seenRef.current.has(dedupeKey)) {
                continue;
            }

            seenRef.current.add(dedupeKey);

            recordMutation.mutate({
                input: {
                    artifactId: tab.workflowId,
                    artifactName: tab.name,
                    chatId: String(chatId),
                    kind: AiHubChatArtifactKind.WorkflowReferenced,
                    metadataJson: JSON.stringify(toWorkflowMetadata(tab)),
                    workspaceId: String(workspaceId),
                },
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [openTabs, chatId, tabsActiveChatId, workspaceId]);
}

function toWorkflowMetadata(tab: AiHubTabType): {projectId: string; projectWorkflowId: number} {
    return {projectId: tab.projectId, projectWorkflowId: tab.projectWorkflowId};
}
