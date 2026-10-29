import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useDeleteAiHubChatMutation, usePatchAiHubChatMutation} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {
    aiHubRunStateStore,
    isChatRunning,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCancelAiHubRunMutation} from '@/shared/middleware/graphql';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

/**
 * Cancels a deleted chat's in-flight run so the server stops streaming a conversation that no longer exists. Keyed off
 * the deleted chat's thread id, because a delete can target a background (non-focused) chat. No-op when the chat is
 * not streaming; the server cancel is idempotent, so a false positive is harmless.
 */
export function cancelChatRunIfStreaming(
    chat: Pick<AiHubChatI, 'id' | 'threadId'>,
    workspaceId: number | undefined,
    cancelAiHubRun: (variables: {id: string; runId: string | undefined; workspaceId: string}) => void
): void {
    const runState = aiHubRunStateStore.getState();

    const isStreaming =
        isChatRunning(runState, chat.threadId) || aiHubChatsStore.getState().chatActivity[chat.threadId] === 'running';

    if (!isStreaming || workspaceId == null) {
        return;
    }

    cancelAiHubRun({
        id: String(chat.id),
        runId: runState.runIdByChat[chat.threadId],
        workspaceId: String(workspaceId),
    });

    runState.setChatRunning(chat.threadId, false);

    aiHubChatsStore.getState().clearActivityState(chat.threadId);
}

export interface AiHubChatActionsI {
    archiveChat: (chat: AiHubChatI) => void;
    cancelDelete: () => void;
    cancelRename: () => void;
    confirmDelete: () => void;
    deleteTarget: AiHubChatI | null;
    renameTarget: AiHubChatI | null;
    requestDelete: (chat: AiHubChatI) => void;
    requestRename: (chat: AiHubChatI) => void;
    submitRename: (title: string) => void;
    unarchiveChat: (chat: AiHubChatI) => void;
}

/**
 * The rename / archive / delete actions a chat can be put through, and the pending state of the two that need
 * confirming. The sidebar row's menu and the chat header's menu share this one implementation, so the delete path
 * (cancel the in-flight run, leave the route that is about to 404) exists once.
 *
 * Pending state is exposed rather than rendered here: the caller pairs it with AiHubChatActionDialogs.
 */
export function useAiHubChatActions(): AiHubChatActionsI {
    const [deleteTarget, setDeleteTarget] = useState<AiHubChatI | null>(null);
    const [renameTarget, setRenameTarget] = useState<AiHubChatI | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();

    const cancelAiHubRunMutation = useCancelAiHubRunMutation();
    const deleteChatMutation = useDeleteAiHubChatMutation();
    const patchChatMutation = usePatchAiHubChatMutation();

    const archiveChat = (chat: AiHubChatI) => {
        if (!isWorkspaceIdKnown(currentWorkspaceId)) {
            return;
        }

        patchChatMutation.mutate({
            chatId: chat.id,
            patch: {status: 'ARCHIVED'},
            workspaceId: currentWorkspaceId,
        });

        if (aiHubChatsStore.getState().currentChatId === chat.id) {
            aiHubChatsStore.getState().setCurrentChatId(undefined);
        }
    };

    const unarchiveChat = (chat: AiHubChatI) => {
        if (!isWorkspaceIdKnown(currentWorkspaceId)) {
            return;
        }

        patchChatMutation.mutate({
            chatId: chat.id,
            patch: {status: 'ACTIVE'},
            workspaceId: currentWorkspaceId,
        });
    };

    const submitRename = (title: string) => {
        if (!renameTarget) {
            return;
        }

        const trimmedTitle = title.trim();

        if (isWorkspaceIdKnown(currentWorkspaceId) && trimmedTitle && trimmedTitle !== (renameTarget.title ?? '')) {
            patchChatMutation.mutate({
                chatId: renameTarget.id,
                patch: {title: trimmedTitle},
                workspaceId: currentWorkspaceId,
            });
        }

        setRenameTarget(null);
    };

    const confirmDelete = () => {
        if (!deleteTarget || !isWorkspaceIdKnown(currentWorkspaceId)) {
            return;
        }

        const chat = deleteTarget;

        cancelChatRunIfStreaming(chat, currentWorkspaceId, (variables) => cancelAiHubRunMutation.mutate(variables));

        deleteChatMutation.mutate({
            chatId: chat.id,
            workspaceId: currentWorkspaceId,
        });

        if (aiHubChatsStore.getState().currentChatId === chat.id) {
            aiHubChatsStore.getState().setCurrentChatId(undefined);
            aiHubStore.getState().resetMessages();
            aiHubStore.getState().generateChatId();

            navigate('/automation/ai-hub');
        }

        setDeleteTarget(null);
    };

    return {
        archiveChat,
        cancelDelete: () => setDeleteTarget(null),
        cancelRename: () => setRenameTarget(null),
        confirmDelete,
        deleteTarget,
        renameTarget,
        requestDelete: setDeleteTarget,
        requestRename: setRenameTarget,
        submitRename,
        unarchiveChat,
    };
}
