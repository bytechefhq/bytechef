import {SPACE} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import pasteNode from './pasteNode';

let mockDataState: {
    nodes: Array<{data: Record<string, unknown>}>;
    workflow: {definition: string; id: string};
};
let mockEditorState: {
    copiedNode: NodeDataType | undefined;
    copiedWorkflowId: string | undefined;
    setResetWorkflowLayout: ReturnType<typeof vi.fn>;
};

const saveWorkflowDefinitionMock = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => mockDataState,
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: {
        getState: () => mockEditorState,
    },
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: (...args: unknown[]) => saveWorkflowDefinitionMock(...args),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getFormattedName', () => ({
    default: (itemName: string, reservedNames?: Set<string>) => {
        let counter = 1;
        let candidateName = `${itemName}_${counter}`;

        while (reservedNames?.has(candidateName)) {
            counter += 1;
            candidateName = `${itemName}_${counter}`;
        }

        return candidateName;
    },
}));

function makeGraphNodeData(): NodeDataType {
    return {
        componentName: 'graph',
        label: 'Graph',
        name: 'graph_1',
        parameters: {
            maxTransitions: 100,
            nodes: [
                {name: 'x_1', parameters: {url: 'http://example.com'}, type: 'x/v1'},
                {name: 'y_1', parameters: {}, type: 'y/v1'},
            ],
            startNode: 'x_1',
            transitions: [
                {from: 'x_1', to: 'y_1'},
                {from: 'y_1', to: '=${dynamicTarget}'},
            ],
        },
        type: 'graph/v1',
        workflowNodeName: 'graph_1',
    } as unknown as NodeDataType;
}

describe('pasteNode - graph', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('deep-clones the nodes array with no shared references to the copied task', () => {
        const copiedNode = makeGraphNodeData();

        mockEditorState = {
            copiedNode,
            copiedWorkflowId: 'workflow-1',
            setResetWorkflowLayout: vi.fn(),
        };
        mockDataState = {
            nodes: [],
            workflow: {
                definition: JSON.stringify({tasks: [{name: 'graph_1'}]}, null, SPACE),
                id: 'workflow-1',
            },
        };

        pasteNode({updateWorkflowMutation: {mutate: vi.fn()} as never});

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

        const pastedNodeData = saveWorkflowDefinitionMock.mock.calls[0][0].nodeData as NodeDataType;
        const pastedNodes = pastedNodeData.parameters?.nodes;
        const originalNodes = copiedNode.parameters?.nodes;

        expect(pastedNodes).not.toBe(originalNodes);
        expect(pastedNodes[0]).not.toBe(originalNodes[0]);

        expect(originalNodes[0].name).toBe('x_1');
        expect(originalNodes[1].name).toBe('y_1');
    });

    it('renames colliding graph node names and remaps both transitions and startNode to match', () => {
        const copiedNode = makeGraphNodeData();

        mockEditorState = {
            copiedNode,
            copiedWorkflowId: 'workflow-1',
            setResetWorkflowLayout: vi.fn(),
        };
        mockDataState = {
            nodes: [],
            workflow: {
                definition: JSON.stringify({tasks: [{name: 'graph_1'}, {name: 'x_1'}, {name: 'y_1'}]}, null, SPACE),
                id: 'workflow-1',
            },
        };

        pasteNode({updateWorkflowMutation: {mutate: vi.fn()} as never});

        const pastedNodeData = saveWorkflowDefinitionMock.mock.calls[0][0].nodeData as NodeDataType;
        const pastedNodes = pastedNodeData.parameters?.nodes;

        expect(pastedNodeData.name).toBe('graph_2');
        expect(pastedNodes[0].name).toBe('x_2');
        expect(pastedNodes[1].name).toBe('y_2');
        expect(pastedNodeData.parameters?.startNode).toBe('x_2');
        expect(pastedNodeData.parameters?.transitions).toEqual([
            {from: 'x_2', to: 'y_2'},
            {from: 'y_2', to: '=${dynamicTarget}'},
        ]);
    });

    it('leaves expression-shaped `to` targets untouched even when they name a node that gets renamed', () => {
        const copiedNode = makeGraphNodeData();

        copiedNode.parameters = {
            ...copiedNode.parameters,
            transitions: [
                {from: 'x_1', to: '=x_1'},
                {from: 'x_1', to: '${x_1}'},
            ],
        };

        mockEditorState = {
            copiedNode,
            copiedWorkflowId: 'workflow-1',
            setResetWorkflowLayout: vi.fn(),
        };
        mockDataState = {
            nodes: [],
            workflow: {
                definition: JSON.stringify({tasks: [{name: 'graph_1'}, {name: 'x_1'}]}, null, SPACE),
                id: 'workflow-1',
            },
        };

        pasteNode({updateWorkflowMutation: {mutate: vi.fn()} as never});

        const pastedNodeData = saveWorkflowDefinitionMock.mock.calls[0][0].nodeData as NodeDataType;

        expect(pastedNodeData.parameters?.nodes?.[0]?.name).toBe('x_2');
        expect(pastedNodeData.parameters?.transitions).toEqual([
            {from: 'x_2', to: '=x_1'},
            {from: 'x_2', to: '${x_1}'},
        ]);
    });
});
