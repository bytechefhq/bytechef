import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNode from './WorkflowNode';

// Mutable slice of the editor store so each test can toggle which node is being renamed.
const {directionStoreState, editorStoreState} = vi.hoisted(() => ({
    directionStoreState: {layoutDirection: 'TB'},
    editorStoreState: {renamingNodeName: undefined as string | undefined},
}));

// Render the context menu as a passthrough so the node content (and its rename input) is asserted directly.
vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
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
            workflow: {definition: '{}', id: 'workflow-1', tasks: [], triggers: []},
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
