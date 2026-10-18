import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubChatsStore} from '../../chats/stores/useAiHubChatsStore';
import {aiHubStore} from '../../stores/useAiHubStore';
import {aiHubTabsStore} from '../../stores/useAiHubTabsStore';
import {useResetAiHubStoresOnWorkspaceChange} from '../useResetAiHubStoresOnWorkspaceChange';

const WORKFLOW = {name: 'Workflow', projectId: '1', projectWorkflowId: 2, workflowId: 'wf-1'};

describe('useResetAiHubStoresOnWorkspaceChange', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubTabsStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubStore.setState({messages: []});
    });

    it('does not reset on the initial mount', () => {
        aiHubTabsStore.getState().openWorkflowTab(WORKFLOW);
        aiHubChatsStore.getState().setCurrentChatId(5);

        renderHook(() => useResetAiHubStoresOnWorkspaceChange());

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(1);
        expect(aiHubChatsStore.getState().currentChatId).toBe(5);
    });

    it('resets the hub stores when the workspace changes', () => {
        renderHook(() => useResetAiHubStoresOnWorkspaceChange());

        aiHubTabsStore.getState().openWorkflowTab(WORKFLOW);
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubStore.setState({messages: [{content: 'hi', role: 'user'}]});
        aiChatRetryableErrorStore.getState().setError({errorMessage: 'x', lastUserMessage: 'y', toolName: 'z'});

        const previousChatId = aiHubStore.getState().chatId;

        act(() => {
            useWorkspaceStore.setState({currentWorkspaceId: 2});
        });

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
        expect(aiHubChatsStore.getState().currentChatId).toBeUndefined();
        expect(aiHubStore.getState().messages).toEqual([]);
        expect(aiHubStore.getState().chatId).not.toBe(previousChatId);
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });
});
