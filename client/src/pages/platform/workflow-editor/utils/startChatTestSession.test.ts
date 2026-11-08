import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {NodeDataType} from '@/shared/types';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import startChatTestSession from './startChatTestSession';

const editorWorkflow = useWorkflowDataStore.getState().workflow;

describe('startChatTestSession', () => {
    beforeEach(() => {
        useDataPillPanelStore.setState({dataPillPanelOpen: true});
        useWorkflowDataStore.setState({
            workflow: {
                ...editorWorkflow,
                id: 'wf-1',
                triggers: [
                    {name: 'trigger_1', type: 'chat/v1/newChatRequest'},
                    {name: 'trigger_2', type: 'chat/v1/newChatRequest'},
                ],
            },
        } as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
        useWorkflowTestChatStore.setState({
            chatTriggerName: undefined,
            messages: [{content: 'old', role: 'user'}],
            workflowTestChatPanelOpen: false,
        });
    });

    afterEach(() => {
        useWorkflowDataStore.setState({workflow: editorWorkflow});
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, workflowNodeDetailsPanelOpen: false});
    });

    it('starts the session on the chat trigger whose panel is open and opens only the chat panel', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        startChatTestSession();

        const chatState = useWorkflowTestChatStore.getState();

        expect(chatState.chatTriggerName).toBe('trigger_2');
        expect(chatState.messages).toEqual([]);
        expect(chatState.workflowTestChatPanelOpen).toBe(true);
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('starts the session on the first chat trigger when no trigger panel is open', () => {
        startChatTestSession();

        expect(useWorkflowTestChatStore.getState().chatTriggerName).toBe('trigger_1');
    });
});
