import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNode from './WorkflowNode';

// Mutable slice of the editor store so each test can toggle which node is being renamed.
const {dataStoreState, directionStoreState, editorStoreState, recordedContextMenuProps} = vi.hoisted(() => ({
    dataStoreState: {definition: '{}'},
    directionStoreState: {layoutDirection: 'TB'},
    editorStoreState: {renamingNodeName: undefined as string | undefined},
    recordedContextMenuProps: {value: undefined as Record<string, unknown> | undefined},
}));

// Render the context menu as a passthrough so the node content (and its rename input) is asserted directly.
vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu', () => ({
    default: ({children, ...contextMenuProps}: {children: ReactNode}) => {
        recordedContextMenuProps.value = contextMenuProps;

        return <div>{children}</div>;
    },
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        cancelWorkflowQueries: vi.fn(),
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: {mutate: vi.fn()},
    }),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getNodeLabel', () => ({
    getNodeLabel: () => 'Approval',
}));

vi.mock('@/shared/queries/platform/workflowNodeDescriptions.queries', () => ({
    useGetWorkflowNodeDescriptionQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetClusterElementDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('../hooks/useNodeClick', () => ({
    default: () => vi.fn(),
}));

vi.mock('../../cluster-element-editor/utils/clusterElementsUtils', () => ({
    calculateNodeWidth: () => 200,
    convertNameToCamelCase: (value: string) => value,
    getFilteredClusterElementTypes: () => [],
    getHandlePosition: () => 0,
}));

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector(directionStoreState),
}));

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentNode: undefined, setCurrentNode: vi.fn(), workflowNodeDetailsPanelOpen: false}),
}));

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            incrementLayoutResetCounter: vi.fn(),
            workflow: {definition: dataStoreState.definition, id: 'workflow-1', tasks: [], triggers: []},
        }),
}));

vi.mock('../stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            clusterElementsCanvasOpen: true,
            copiedNode: undefined,
            copiedWorkflowId: undefined,
            mainClusterRootComponentDefinition: undefined,
            nestedClusterRootsComponentDefinitions: {},
            renamingNodeName: editorStoreState.renamingNodeName,
            rootClusterElementNodeData: undefined,
            setCopiedNode: vi.fn(),
            setCopiedWorkflowId: vi.fn(),
            setRenamingNodeName: vi.fn(),
            setRootClusterElementNodeData: vi.fn(),
            workflowTestNodeStates: {},
        }),
}));

const NESTED_CLUSTER_ROOT_DATA = {
    clusterElementName: 'approval',
    clusterElementType: 'approval',
    componentName: 'approval',
    isNestedClusterRoot: true,
    label: 'Approval',
    name: 'approval_1',
    operationName: 'requestApproval',
    version: 1,
    workflowNodeName: 'approval_1',
} as unknown as NodeDataType;

function renderNode(data: NodeDataType = NESTED_CLUSTER_ROOT_DATA, id: string = 'approval_1') {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <ReactFlowProvider>
                <WorkflowNode data={data} id={id} />
            </ReactFlowProvider>
        </QueryClientProvider>
    );
}

describe('WorkflowNode', () => {
    beforeEach(() => {
        dataStoreState.definition = '{}';
        directionStoreState.layoutDirection = 'TB';
        editorStoreState.renamingNodeName = undefined;
    });

    it('renders a rename input for a nested cluster root that is being renamed', () => {
        editorStoreState.renamingNodeName = 'approval_1';

        renderNode();

        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('does not render a rename input for a nested cluster root that is not being renamed', () => {
        editorStoreState.renamingNodeName = undefined;

        renderNode();

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('warns that a single referenced disabled node will not resolve', () => {
        dataStoreState.definition = JSON.stringify({
            tasks: [{disabled: true, name: 'action_1', parameters: {}, type: 'test/v1/action'}],
        });

        renderNode(
            {
                componentName: 'test',
                name: 'action_2',
                parameters: {value: '${action_1.body}'},
                workflowNodeName: 'action_2',
            } as unknown as NodeDataType,
            'action_2'
        );

        expect(
            screen.getByTitle('References disabled node action_1 — it will not run, so this value will not resolve')
        ).toBeInTheDocument();
    });

    it('warns in the plural when several referenced nodes are disabled', () => {
        dataStoreState.definition = JSON.stringify({
            tasks: [
                {disabled: true, name: 'action_1', parameters: {}, type: 'test/v1/action'},
                {disabled: true, name: 'action_3', parameters: {}, type: 'test/v1/action'},
            ],
        });

        renderNode(
            {
                componentName: 'test',
                name: 'action_2',
                parameters: {value: '${action_1} and ${action_3}'},
                workflowNodeName: 'action_2',
            } as unknown as NodeDataType,
            'action_2'
        );

        expect(
            screen.getByTitle(
                'References disabled nodes action_1, action_3 — they will not run, so this value will not resolve'
            )
        ).toBeInTheDocument();
    });

    it('rotates LR condition labels and keeps the pair on one vertical axis', () => {
        directionStoreState.layoutDirection = 'LR';

        renderNode({
            componentName: 'condition',
            name: 'condition_1',
            taskDispatcher: true,
            workflowNodeName: 'condition_1',
        } as unknown as NodeDataType);

        const trueLabel = screen.getByText('TRUE');
        const falseLabel = screen.getByText('FALSE');

        // equal fixed widths + centered text are what keep the two rotated
        // labels on the same vertical axis despite different text lengths
        for (const label of [trueLabel, falseLabel]) {
            expect(label.className).toContain('-rotate-90');
            expect(label.className).toContain('w-14');
            expect(label.className).toContain('text-center');
        }
    });

    it('rotates LR on-error labels and keeps the pair on one vertical axis', () => {
        directionStoreState.layoutDirection = 'LR';

        renderNode({
            componentName: 'on-error',
            name: 'on-error_1',
            taskDispatcher: true,
            workflowNodeName: 'on-error_1',
        } as unknown as NodeDataType);

        const tryLabel = screen.getByText('TRY');
        const catchLabel = screen.getByText('CATCH');

        for (const label of [tryLabel, catchLabel]) {
            expect(label.className).toContain('-rotate-90');
            expect(label.className).toContain('w-14');
            expect(label.className).toContain('text-center');
        }
    });

    it('keeps TB condition labels horizontal', () => {
        renderNode({
            componentName: 'condition',
            name: 'condition_1',
            taskDispatcher: true,
            workflowNodeName: 'condition_1',
        } as unknown as NodeDataType);

        expect(screen.getByText('TRUE').className).not.toContain('-rotate-90');
        expect(screen.getByText('FALSE').className).not.toContain('-rotate-90');
    });
});

describe('WorkflowNode graph transition handles', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
        editorStoreState.renamingNodeName = undefined;
    });

    function renderMember(overrides: Partial<NodeDataType> = {}, id: string = 'task_1') {
        return renderNode(
            {
                componentName: 'httpClient',
                graphData: {graphId: 'graph_1', index: 0},
                name: id,
                workflowNodeName: id,
                ...overrides,
            } as unknown as NodeDataType,
            id
        );
    }

    it('renders no transition handles on a node that is not a graph member', () => {
        const {container} = renderNode(
            {
                componentName: 'httpClient',
                name: 'task_1',
                workflowNodeName: 'task_1',
            } as unknown as NodeDataType,
            'task_1'
        );

        expect(container.querySelector('[data-handleid*="graph-transition"]')).toBeNull();
    });

    it('puts graph member transition handles on the main axis in TB', () => {
        const {container} = renderMember();

        expect(container.querySelector('[data-handleid="task_1-graph-transition-target"]')?.className).toContain(
            'react-flow__handle-top'
        );
        expect(container.querySelector('[data-handleid="task_1-graph-transition-source"]')?.className).toContain(
            'react-flow__handle-bottom'
        );
    });

    it('centres graph member transition handles on the box rather than the label-widened element', () => {
        const {container} = renderMember();

        const sourceHandle = container.querySelector<HTMLElement>('[data-handleid="task_1-graph-transition-source"]');

        expect(sourceHandle!.style.left).toBe('36px');
    });

    it('moves graph member transition handles onto the horizontal axis in LR', () => {
        directionStoreState.layoutDirection = 'LR';

        const {container} = renderMember();

        expect(container.querySelector('[data-handleid="task_1-graph-transition-target"]')?.className).toContain(
            'react-flow__handle-left'
        );
        expect(container.querySelector('[data-handleid="task_1-graph-transition-source"]')?.className).toContain(
            'react-flow__handle-right'
        );
    });

    it('makes graph member transition handles connectable so transitions can be drawn', () => {
        const {container} = renderMember();

        const targetHandle = container.querySelector('[data-handleid="task_1-graph-transition-target"]');
        const sourceHandle = container.querySelector('[data-handleid="task_1-graph-transition-source"]');

        expect(targetHandle?.classList.contains('connectable')).toBe(true);
        expect(sourceHandle?.classList.contains('connectable')).toBe(true);
    });

    it('keeps the dynamic stub anchor on the source side and never connectable', () => {
        const {container} = renderMember();

        const dynamicHandle = container.querySelector('[data-handleid="task_1-graph-transition-dynamic"]');

        expect(dynamicHandle?.className).toContain('react-flow__handle-bottom');
        expect(dynamicHandle?.classList.contains('connectable')).toBe(false);
    });

    // A task-dispatcher member is one unit inside the frame, and `parameters.transitions[*].from`
    // names the dispatcher task itself — so its outgoing transition anchors on the dispatcher's own
    // node, not on its bottom ghost bar (which the retired lane model used).
    it('anchors a task-dispatcher member transitions on the dispatcher node itself', () => {
        const {container} = renderMember(
            {componentName: 'loop', name: 'loop_1', taskDispatcher: true, workflowNodeName: 'loop_1'},
            'loop_1'
        );

        expect(container.querySelector('[data-handleid="loop_1-graph-transition-source"]')).not.toBeNull();
        expect(container.querySelector('[data-handleid="loop_1-graph-transition-target"]')).not.toBeNull();
    });
});

describe('WorkflowNode reset position action', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
        editorStoreState.renamingNodeName = undefined;
        recordedContextMenuProps.value = undefined;
    });

    it('offers the reset position action on an ordinary task that carries a saved position', () => {
        renderNode(
            {
                componentName: 'httpClient',
                metadata: {ui: {nodePosition: {x: 10, y: 20}}},
                name: 'task_1',
                workflowNodeName: 'task_1',
            } as unknown as NodeDataType,
            'task_1'
        );

        expect(recordedContextMenuProps.value?.hasSavedPosition).toBe(true);
    });

    // Inside a graph frame a position IS the model rather than a pin override, so there is nothing
    // for a reset to fall back to and the action is hidden.
    it('hides the reset position action on a graph member that carries a saved position', () => {
        renderNode(
            {
                componentName: 'httpClient',
                graphData: {graphId: 'graph_1', index: 0},
                metadata: {ui: {nodePosition: {x: 10, y: 20}}},
                name: 'task_1',
                workflowNodeName: 'task_1',
            } as unknown as NodeDataType,
            'task_1'
        );

        expect(recordedContextMenuProps.value?.hasSavedPosition).toBe(false);
    });
});
