import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {useEffect, useRef} from 'react';

import {aiHubChatsStore} from '../chats/stores/useAiHubChatsStore';
import {aiHubComposerStore} from '../composer/stores/useAiHubComposerStore';
import {aiHubStore} from '../stores/useAiHubStore';
import {aiHubTabsStore} from '../stores/useAiHubTabsStore';

/**
 * Resets every AI Hub store when the active workspace changes. The stores are module-scoped singletons, so without
 * this the tabs, chat selection and retry banner would leak across workspace switches. Skips the initial mount.
 */
export function useResetAiHubStoresOnWorkspaceChange(): void {
    const isFirstRunRef = useRef(true);
    const previousWorkspaceIdRef = useRef<number | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    useEffect(() => {
        if (isFirstRunRef.current) {
            isFirstRunRef.current = false;
            previousWorkspaceIdRef.current = currentWorkspaceId;

            return;
        }

        const previousWorkspaceId = previousWorkspaceIdRef.current;

        previousWorkspaceIdRef.current = currentWorkspaceId;

        if (previousWorkspaceId === currentWorkspaceId) {
            return;
        }

        aiHubComposerStore.getState().clear();
        aiHubChatsStore.getState().reset();
        aiChatRetryableErrorStore.getState().clearError();
        aiHubTabsStore.getState().reset();
        aiChatToolCallStore.getState().reset();
        aiChatAskedQuestionsStore.getState().reset();
        aiHubStore.getState().resetMessages();
        aiHubStore.getState().generateChatId();
    }, [currentWorkspaceId]);
}
