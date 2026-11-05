import {type AiHubTabType, useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {AiHubChatArtifactKind, useRecordReferencedAiHubChatArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';

import {isWorkspaceIdKnown} from '../isWorkspaceIdKnown';
import {AiHubChatsKeys} from './useChats';

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
    }, [enabled, openTabs, chatId, tabsActiveChatId, workspaceId]);
}

function toWorkflowMetadata(tab: AiHubTabType): {projectId: string; projectWorkflowId: number} {
    return {projectId: tab.projectId, projectWorkflowId: tab.projectWorkflowId};
}
