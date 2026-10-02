import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowIssuesStore from '../stores/useWorkflowIssuesStore';
import WorkflowNode from './WorkflowNode';

// Mutable slice of the editor store so each test can toggle which node is being renamed.
const {directionStoreState, editorStoreState, popoverMenuMock} = vi.hoisted(() => ({
    directionStoreState: {layoutDirection: 'TB'},
    editorStoreState: {renamingNodeName: undefined as string | undefined},
    popoverMenuMock: vi.fn(),
}));

// Render the context menu as a passthrough so the node content (and its rename input) is asserted directly.
vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: (props: Record<string, unknown>) => {
        popoverMenuMock(props);

        return null;
    },
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

const MANUAL_TRIGGER_DATA = {
    componentName: 'manual',
    label: 'Manual',
    name: 'trigger_3',
    operationName: 'manual',
    trigger: true,
    type: 'manual/v1/manual',
    workflowNodeName: 'trigger_3',
} as unknown as NodeDataType;

function renderNode(data: NodeDataType = NESTED_CLUSTER_ROOT_DATA, id = 'approval_1') {
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

        popoverMenuMock.mockClear();
    });

    it('points the trigger replace popover at the trigger it replaces', () => {
        renderNode(MANUAL_TRIGGER_DATA, 'trigger_3');

        expect(popoverMenuMock).toHaveBeenCalledWith(
            expect.objectContaining({hideActionComponents: true, sourceNodeName: 'trigger_3'})
        );
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

    it('shows an issue badge when the issues store has an issue for this node', () => {
        useWorkflowIssuesStore.getState().setValidatorIssues([
            {
                kind: 'MISSING_REQUIRED',
                message: 'Missing required property: model',
                nodeName: 'approval_1',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
        ]);

        renderNode();

        expect(screen.getByLabelText('1 issue')).toHaveAttribute('title', 'Missing required property: model');

        useWorkflowIssuesStore.getState().reset();
    });

    it('shows no badge for a node without issues', () => {
        renderNode();

        expect(screen.queryByLabelText(/issue/)).not.toBeInTheDocument();
    });

    it('keeps TB condition labels on the node, beside the stem', () => {
        renderNode({
            componentName: 'condition',
            name: 'condition_1',
            taskDispatcher: true,
            workflowNodeName: 'condition_1',
        } as unknown as NodeDataType);

        expect(screen.getByText('TRUE')).toBeInTheDocument();
        expect(screen.getByText('FALSE')).toBeInTheDocument();
    });

    it('leaves LR condition labels to the arms past the split bar', () => {
        directionStoreState.layoutDirection = 'LR';

        renderNode({
            componentName: 'condition',
            name: 'condition_1',
            taskDispatcher: true,
            workflowNodeName: 'condition_1',
        } as unknown as NodeDataType);

        expect(screen.queryByText('TRUE')).not.toBeInTheDocument();
        expect(screen.queryByText('FALSE')).not.toBeInTheDocument();
    });
});
