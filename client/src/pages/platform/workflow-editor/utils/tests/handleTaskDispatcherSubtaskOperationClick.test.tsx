import useWorkflowDataStore, {WorkflowDataType} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ActionDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {
    ClickedOperationType,
    NodeDataType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import handleTaskDispatcherSubtaskOperationClick from '../handleTaskDispatcherSubtaskOperationClick';

const saveWorkflowDefinitionMock = vi.hoisted(() => vi.fn());

vi.mock('../saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

const OPERATION: ClickedOperationType = {
    componentLabel: 'AI Agent',
    componentName: 'aiAgent',
    operationName: 'chat',
    type: 'aiAgent/v1/chat',
    version: 1,
};

const TASK_DISPATCHER_CONTEXT: TaskDispatcherContextType = {
    conditionCase: 'caseTrue',
    conditionId: 'condition_1',
    index: 0,
    taskDispatcherId: 'condition_1',
};

const callHandler = (clusterRoot?: boolean) =>
    handleTaskDispatcherSubtaskOperationClick({
        clusterRoot,
        operation: OPERATION,
        operationDefinition: {properties: []} as unknown as ActionDefinition,
        queryClient: new QueryClient(),
        taskDispatcherContext: TASK_DISPATCHER_CONTEXT,
        updateWorkflowMutation: {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType,
        workflow: {
            definition: JSON.stringify({tasks: []}),
            id: 'workflow_1',
            tasks: [],
        } as unknown as Workflow & WorkflowDataType,
    });

describe('handleTaskDispatcherSubtaskOperationClick', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockClear();

        useWorkflowDataStore.setState({
            nodes: [],
            workflow: {
                definition: JSON.stringify({tasks: []}),
                id: 'workflow_1',
            } as unknown as Workflow & WorkflowDataType,
        });

        useWorkflowNodeDetailsPanelStore.setState({
            activeTab: 'description',
            currentNode: undefined,
            pendingSaveNodeNames: new Set<string>(),
            workflowNodeDetailsPanelOpen: false,
        });
    });

    it('should open the node details panel for a regular component added inside a task dispatcher', () => {
        callHandler(false);

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);
    });

    it('should not open the node details panel for a cluster root added inside a task dispatcher', () => {
        callHandler(true);

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
    });

    it('should persist an empty clusterElements map for a cluster root added inside a task dispatcher', () => {
        callHandler(true);

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0] as {nodeData: NodeDataType};

        expect(nodeData.clusterElements).toEqual({});
    });

    it('closes the optimistically opened panel when the add fails', () => {
        callHandler(false);

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);

        const {onError} = saveWorkflowDefinitionMock.mock.calls[0][0] as {onError: () => void};

        onError();

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
    });
});
