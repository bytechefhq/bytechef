import {TooltipProvider} from '@/components/ui/tooltip';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import GraphStatesPanel from './GraphStatesPanel';

function renderPanel(taskDispatcherDefinition: Parameters<typeof GraphStatesPanel>[0]['taskDispatcherDefinition']) {
    return render(
        <TooltipProvider>
            <GraphStatesPanel taskDispatcherDefinition={taskDispatcherDefinition} />
        </TooltipProvider>
    );
}

const {storeState, workflowDataStoreState} = vi.hoisted(() => ({
    storeState: {
        currentNode: undefined as NodeDataType | undefined,
    },
    workflowDataStoreState: {
        workflow: {
            tasks: undefined as Array<{name: string; parameters?: Record<string, unknown>; type?: string}> | undefined,
        },
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: typeof storeState) => unknown) => selector(storeState),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof workflowDataStoreState) => unknown) => selector(workflowDataStoreState),
}));

// Isolate GraphStatesPanel's own list/badge/terminal-text logic from Property's full editor
// machinery (Tiptap, workflow editor context, ...) — asserted here only by the `path` it is
// addressed with, which is the load-bearing contract for the container-parameter save route.
vi.mock('@/pages/platform/workflow-editor/components/properties/Property', () => ({
    default: ({path}: {path: string}) => <div data-testid="next-property">{path}</div>,
}));

// Isolate GraphStatesPanel's own suggestion-list computation (which names it offers, per node,
// and whether the clobber guard is engaged) from GraphNextNodeSuggestions' own save/render
// machinery (covered by its own test suite) — asserted here only by the `nodeNames`/`path`/
// `disabled` props it is given.
vi.mock('@/pages/platform/workflow-editor/components/properties/graph/GraphNextNodeSuggestions', () => ({
    default: ({disabled, nodeNames, path}: {disabled?: boolean; nodeNames: Array<string>; path: string}) => (
        <div data-disabled={String(!!disabled)} data-node-names={nodeNames.join(',')} data-testid="next-suggestions">
            {path}
        </div>
    ),
}));

const NEXT_PROPERTY_DEFINITION: PropertyAllType = {
    controlType: 'FORMULA_MODE',
    label: 'Next',
    name: 'next',
    type: 'STRING',
};

const TASK_DISPATCHER_DEFINITION = {
    name: 'graph',
    outputDefined: false,
    taskProperties: [
        {
            items: [
                {
                    properties: [
                        {name: 'name', type: 'STRING'},
                        NEXT_PROPERTY_DEFINITION,
                        {name: 'tasks', type: 'ARRAY'},
                    ],
                    type: 'OBJECT',
                },
            ],
            name: 'nodes',
            type: 'ARRAY',
        },
    ],
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
} as any;

describe('GraphStatesPanel', () => {
    it('should render a helper message when the graph has no nodes', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {nodes: []},
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByText(/This graph has no nodes yet/)).toBeInTheDocument();
    });

    it('should list every declared node by name', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [
                    {name: 'node_0', next: "'node_1'", tasks: []},
                    {name: 'node_1', tasks: []},
                ],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByLabelText('node_0 state')).toBeInTheDocument();
        expect(screen.getByLabelText('node_1 state')).toBeInTheDocument();
    });

    // The panel store holds the node as it was when the panel opened, so a node added or removed on
    // the canvas afterwards was invisible here until the panel was closed and reopened.
    it('should list the nodes of the live task, not the panel snapshot', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {nodes: [{name: 'node_0', tasks: []}]},
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        workflowDataStoreState.workflow.tasks = [
            {
                name: 'graph_1',
                parameters: {
                    nodes: [
                        {name: 'node_0', tasks: []},
                        {name: 'node_1', tasks: []},
                    ],
                },
            },
        ];

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByLabelText('node_1 state')).toBeInTheDocument();

        workflowDataStoreState.workflow.tasks = undefined;
    });

    // A graph inside a loop or branch is not a top-level task, so a top-level-only lookup would fall
    // back to the stale snapshot for exactly those graphs.
    it('should find a graph nested inside another dispatcher', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {nodes: [{name: 'node_0', tasks: []}]},
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        workflowDataStoreState.workflow.tasks = [
            {
                name: 'loop_1',
                parameters: {
                    iteratee: [
                        {
                            name: 'graph_1',
                            parameters: {
                                nodes: [
                                    {name: 'node_0', tasks: []},
                                    {name: 'node_9', tasks: []},
                                ],
                            },
                            type: 'graph/v1/graph',
                        },
                    ],
                },
                type: 'loop/v1/loop',
            },
        ];

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByLabelText('node_9 state')).toBeInTheDocument();

        workflowDataStoreState.workflow.tasks = undefined;
    });

    // A graph being configured before its first save has no task yet, so the snapshot is still the
    // only source there.
    it('should fall back to the panel snapshot when the task has not been saved yet', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {nodes: [{name: 'node_0', tasks: []}]},
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        workflowDataStoreState.workflow.tasks = [];

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByLabelText('node_0 state')).toBeInTheDocument();

        workflowDataStoreState.workflow.tasks = undefined;
    });

    it("should address each node's next editor by its container-parameter path, not a subtask path", () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [
                    {name: 'node_0', next: "'node_1'", tasks: []},
                    {name: 'node_1', tasks: []},
                ],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        const nextEditors = screen.getAllByTestId('next-property');

        expect(nextEditors[0]).toHaveTextContent('nodes[0].next');
        expect(nextEditors[1]).toHaveTextContent('nodes[1].next');
    });

    it('should show terminal helper text for a node with no next expression', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {nodes: [{name: 'node_0', tasks: []}]},
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByText(/this node is terminal/)).toBeInTheDocument();
    });

    it('should not show terminal helper text for a node with a next expression', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [{name: 'node_0', next: "'node_0'", tasks: []}],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.queryByText(/this node is terminal/)).not.toBeInTheDocument();
    });

    it('should offer every declared node name as a next suggestion, including the node itself (self-loops are legal)', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [
                    {name: 'node_0', next: "'node_1'", tasks: []},
                    {name: 'node_1', tasks: []},
                ],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        const suggestionLists = screen.getAllByTestId('next-suggestions');

        // Both nodes are offered the FULL declared-name set, including their own name.
        expect(suggestionLists[0]).toHaveAttribute('data-node-names', 'node_0,node_1');
        expect(suggestionLists[1]).toHaveAttribute('data-node-names', 'node_0,node_1');
        expect(suggestionLists[0]).toHaveTextContent('nodes[0].next');
        expect(suggestionLists[1]).toHaveTextContent('nodes[1].next');
    });

    it('should disable next suggestions for a dynamic (non-bare-literal) next expression, to avoid clobbering it', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [
                    {
                        name: 'node_0',
                        next: "steps.decision.value == 'yes' ? 'approve' : 'reject'",
                        tasks: [],
                    },
                ],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByTestId('next-suggestions')).toHaveAttribute('data-disabled', 'true');
    });

    it('should keep next suggestions enabled for an empty next expression', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [{name: 'node_0', tasks: []}],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByTestId('next-suggestions')).toHaveAttribute('data-disabled', 'false');
    });

    it('should keep next suggestions enabled for a dangling bare literal (one-click repair, not a clobber)', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [{name: 'node_0', next: "'renamedAway'", tasks: []}],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByTestId('next-suggestions')).toHaveAttribute('data-disabled', 'false');
    });

    it('should render a dangling-target warning badge when a next literal references an undeclared node', () => {
        storeState.currentNode = {
            componentName: 'graph',
            name: 'graph_1',
            parameters: {
                nodes: [{name: 'node_0', next: "'renamedAway'", tasks: []}],
            },
            workflowNodeName: 'graph_1',
        } as NodeDataType;

        renderPanel(TASK_DISPATCHER_DEFINITION);

        expect(screen.getByText('renamedAway')).toBeInTheDocument();
    });
});
