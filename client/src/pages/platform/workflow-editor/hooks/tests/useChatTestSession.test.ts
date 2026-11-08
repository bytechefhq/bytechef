import useChatTestSession from '@/pages/platform/workflow-editor/hooks/useChatTestSession';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const workflow = {
    triggers: [
        {name: 'trigger_1', type: 'chat/v1/newChatRequest'},
        {name: 'trigger_2', type: 'chat/v1/newChatRequest'},
    ],
};

describe('useChatTestSession', () => {
    beforeEach(() => {
        useDataPillPanelStore.setState({dataPillPanelOpen: true});
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });
        useWorkflowTestChatStore.setState({chatTriggerName: undefined, workflowTestChatPanelOpen: false});
    });

    it('starts a chat session on the open chat trigger and opens the chat panel', () => {
        const {result} = renderHook(() => useChatTestSession(false));

        act(() => {
            result.current.startChatTestSession(workflow);
        });

        expect(result.current.chatSessionRef.current).toBe(true);
        expect(useWorkflowTestChatStore.getState().chatTriggerName).toBe('trigger_2');
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(true);
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('ends an active chat session once and closes the chat panel', () => {
        const {result} = renderHook(() => useChatTestSession(false));

        act(() => {
            result.current.startChatTestSession(workflow);
        });

        let firstEnd = false;
        let secondEnd = true;

        act(() => {
            firstEnd = result.current.endChatTestSession();
            secondEnd = result.current.endChatTestSession();
        });

        expect(firstEnd).toBe(true);
        expect(secondEnd).toBe(false);
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(false);
    });

    it('does not end a session that was never started', () => {
        const {result} = renderHook(() => useChatTestSession(false));

        expect(result.current.endChatTestSession()).toBe(false);
    });
});
