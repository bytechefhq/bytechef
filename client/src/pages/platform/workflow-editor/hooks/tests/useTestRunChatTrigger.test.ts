import useTestRunChatTrigger from '@/pages/platform/workflow-editor/hooks/useTestRunChatTrigger';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const workflow = {
    triggers: [
        {name: 'trigger_1', type: 'webhook/v1/autoRespondWithHTTP200'},
        {name: 'trigger_2', type: 'chat/v1/newChatRequest'},
    ],
};

describe('useTestRunChatTrigger', () => {
    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, workflowNodeDetailsPanelOpen: false});
        useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: false});
    });

    it('is false when the first trigger is not a chat trigger and no trigger is open', () => {
        const {result} = renderHook(() => useTestRunChatTrigger(workflow));

        expect(result.current).toBe(false);
    });

    it('is true when the open trigger is a chat trigger', () => {
        const {result} = renderHook(() => useTestRunChatTrigger(workflow));

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
                workflowNodeDetailsPanelOpen: true,
            });
        });

        expect(result.current).toBe(true);
    });

    it('is false when the open trigger is not a chat trigger', () => {
        const {result} = renderHook(() =>
            useTestRunChatTrigger({
                triggers: [
                    {name: 'trigger_1', type: 'chat/v1/newChatRequest'},
                    {name: 'trigger_2', type: 'webhook/v1/autoRespondWithHTTP200'},
                ],
            })
        );

        expect(result.current).toBe(true);

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {name: 'trigger_2', trigger: true, workflowNodeName: 'trigger_2'} as NodeDataType,
                workflowNodeDetailsPanelOpen: true,
            });
        });

        expect(result.current).toBe(false);
    });

    it('stays true while the test chat panel is open', () => {
        const {result} = renderHook(() => useTestRunChatTrigger(workflow));

        act(() => {
            useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: true});
        });

        expect(result.current).toBe(true);
    });
});
