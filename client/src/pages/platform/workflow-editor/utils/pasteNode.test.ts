import {SPACE} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import pasteNode from './pasteNode';

// ── Store mocks ──────────────────────────────────────────────────────

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

// A deliberately simple stand-in for the real name-generator: `${itemName}_<n>`, skipping
// any candidate already present in `reservedNames`.
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

// ── Helpers ──────────────────────────────────────────────────────────

function makeGraphNodeData(): NodeDataType {
    return {
        componentName: 'graph',
        label: 'Graph',
        name: 'graph_1',
        parameters: {
            maxTransitions: 100,
            nodes: [
                {
                    name: 'node_0',
                    next: "'node_1'",
                    tasks: [{name: 'http_1', parameters: {url: 'http://example.com'}, type: 'httpClient/v1/get'}],
                },
                {
                    name: 'node_1',
                    tasks: [{name: 'http_2', parameters: {}, type: 'httpClient/v1/get'}],
                },
            ],
        },
        type: 'graph/v1',
        workflowNodeName: 'graph_1',
    } as unknown as NodeDataType;
}

// ── Tests ────────────────────────────────────────────────────────────

describe('pasteNode — graph', () => {
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
        expect(pastedNodes[0].tasks).not.toBe(originalNodes[0].tasks);
        expect(pastedNodes[0].tasks[0]).not.toBe(originalNodes[0].tasks[0]);

        // The original, copied node's tasks must be untouched by the paste.
        expect(originalNodes[0].tasks[0].name).toBe('http_1');
        expect(originalNodes[0].name).toBe('node_0');
        expect(originalNodes[0].next).toBe("'node_1'");
    });

    it('renames every task nested inside the pasted graph nodes to avoid name collisions', () => {
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

        const pastedNodeData = saveWorkflowDefinitionMock.mock.calls[0][0].nodeData as NodeDataType;
        const pastedNodes = pastedNodeData.parameters?.nodes;

        // The graph task itself is renamed (graph_1 is reserved), and both nested tasks are
        // renamed too — node names ('node_0'/'node_1') are left alone since names are the
        // node's display/runtime identity, not reserved task names.
        expect(pastedNodeData.name).toBe('graph_2');
        expect(pastedNodes[0].name).toBe('node_0');
        expect(pastedNodes[0].tasks[0].name).not.toBe('http_1');
        expect(pastedNodes[1].tasks[0].name).not.toBe('http_2');
        expect(pastedNodes[0].tasks[0].name).not.toBe(pastedNodes[1].tasks[0].name);
    });
});
