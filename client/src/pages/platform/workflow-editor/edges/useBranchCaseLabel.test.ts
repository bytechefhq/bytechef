import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '../stores/useWorkflowTestChatStore';
import useBranchCaseLabel from './useBranchCaseLabel';

const {saveWorkflowDefinitionMock} = vi.hoisted(() => ({
    saveWorkflowDefinitionMock: vi.fn(),
}));

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('../utils/saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

const BRANCH_PARAMETERS = {
    cases: [
        {
            key: 'caseA',
            tasks: [
                {name: 'taskA', type: 'example/v1/action'},
                {
                    name: 'condition_1',
                    parameters: {caseFalse: [], caseTrue: [{name: 'nestedTask', type: 'example/v1/action'}]},
                    type: 'condition/v1',
                },
            ],
        },
        {key: 'caseB', tasks: [{name: 'taskB', type: 'example/v1/action'}]},
    ],
    default: [],
};

function createNode(id: string, data: Partial<NodeDataType>): Node {
    return {data: {name: id, ...data}, id, position: {x: 0, y: 0}};
}

function renderBranchCaseLabel(caseKey: string, targetNodeId: string) {
    return renderHook(() =>
        useBranchCaseLabel({
            caseKey,
            edgeId: `branch_1=>${targetNodeId}`,
            layoutDirection: 'TB',
            sourceX: 0,
            sourceY: 0,
            targetX: 0,
            targetY: 100,
        })
    );
}

function deleteCase(result: {current: ReturnType<typeof useBranchCaseLabel>}) {
    act(() => {
        result.current.handleDeleteButtonClick();
    });

    act(() => {
        result.current.handleDeleteButtonClick();
    });
}

function openPanelFor(nodeName: string, nodeData: Partial<NodeDataType> = {}) {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {name: nodeName, workflowNodeName: nodeName, ...nodeData} as NodeDataType,
        workflowNodeDetailsPanelOpen: true,
    });

    useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: true});
}

function expectPanelsClosed() {
    expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
    expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
    expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(false);
}

describe('useBranchCaseLabel', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowNodeDetailsPanelStore.getState().reset();

        useWorkflowEditorStore.setState({rootClusterElementNodeData: undefined});

        useWorkflowDataStore.setState({
            nodes: [
                createNode('branch_1', {componentName: 'branch', parameters: BRANCH_PARAMETERS}),
                createNode('taskA', {branchData: {branchId: 'branch_1', caseKey: 'caseA', index: 0}}),
                createNode('taskB', {branchData: {branchId: 'branch_1', caseKey: 'caseB', index: 0}}),
            ],
            workflow: {
                definition: JSON.stringify({
                    tasks: [{name: 'branch_1', parameters: BRANCH_PARAMETERS, type: 'branch/v1'}],
                }),
                id: 'workflow-1',
                nodeNames: ['branch_1', 'taskA', 'taskB'],
            },
        });
    });

    it('closes the details panel when the deleted case contains the open node', () => {
        openPanelFor('taskA');

        const {result} = renderBranchCaseLabel('caseA', 'taskA');

        deleteCase(result);

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

        expectPanelsClosed();
    });

    it('closes the details panel when the open node is nested inside a task dispatcher in the deleted case', () => {
        openPanelFor('nestedTask');

        const {result} = renderBranchCaseLabel('caseA', 'taskA');

        deleteCase(result);

        expectPanelsClosed();
    });

    it('closes the details panel when the open cluster element belongs to a task in the deleted case', () => {
        useWorkflowEditorStore.setState({rootClusterElementNodeData: {name: 'taskA'} as NodeDataType});

        openPanelFor('openAiModel', {clusterElementType: 'model', workflowNodeName: 'openAiModel'});

        const {result} = renderBranchCaseLabel('caseA', 'taskA');

        deleteCase(result);

        expectPanelsClosed();
    });

    it('keeps the details panel open when the open node is in another case', () => {
        openPanelFor('taskB');

        const {result} = renderBranchCaseLabel('caseA', 'taskA');

        deleteCase(result);

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.name).toBe('taskB');
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(true);
    });
});
