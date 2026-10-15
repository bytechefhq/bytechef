import {TooltipProvider} from '@/components/ui/tooltip';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsViewModeStore from '../stores/useClusterElementsViewModeStore';
import useClusterFrameCollapsedStore from '../stores/useClusterFrameCollapsedStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import AiAgentNode from './AiAgentNode';

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    Handle: ({type}: {type: string}) => <div data-testid={`handle-${type}`} />,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu', () => ({
    default: () => null,
}));

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        cancelWorkflowQueries: vi.fn(),
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: {mutate: vi.fn()},
    }),
}));

vi.mock('@/shared/queries/platform/workflowNodeDescriptions.queries', () => ({
    useGetWorkflowNodeDescriptionQuery: () => ({data: undefined}),
}));

vi.mock('../hooks/useNodeClick', () => ({
    default: () => vi.fn(),
}));

vi.mock('./ClusterFrameShell', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

const AI_AGENT_DATA = {
    clusterElements: {},
    clusterRoot: true,
    componentName: 'aiAgent',
    label: 'AI Agent',
    name: 'aiAgent_1',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

const FRAMED_AI_AGENT_DATA = {
    ...AI_AGENT_DATA,
    clusterFrame: {clusterRootId: 'aiAgent_1', contentOrigin: {x: 32, y: 40}, height: 320, width: 640},
} as unknown as NodeDataType;

function renderNode(data: NodeDataType) {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <AiAgentNode data={data} id="aiAgent_1" />
            </TooltipProvider>
        </QueryClientProvider>
    );
}

describe('AiAgentNode', () => {
    beforeEach(() => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});
        useClusterFrameCollapsedStore.setState({collapsedByWorkflowId: {}});
        useWorkflowDataStore.setState({
            workflow: {definition: '{}', id: 'workflow-1', nodeNames: [], tasks: [], triggers: [], version: 1},
        });
    });

    it('expands a collapsed cluster root back into its frame', async () => {
        useClusterFrameCollapsedStore.setState({collapsedByWorkflowId: {'workflow-1': {aiAgent_1: true}}});

        renderNode(AI_AGENT_DATA);

        await userEvent.click(screen.getByRole('button', {name: 'Expand cluster elements'}));

        expect(useClusterFrameCollapsedStore.getState().collapsedByWorkflowId['workflow-1']?.aiAgent_1).toBeUndefined();
    });

    it('offers no expand control in dialog mode, even for a collapsed root', () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
        useClusterFrameCollapsedStore.setState({collapsedByWorkflowId: {'workflow-1': {aiAgent_1: true}}});

        renderNode(AI_AGENT_DATA);

        expect(screen.queryByRole('button', {name: 'Expand cluster elements'})).not.toBeInTheDocument();
    });

    it('offers no expand control for a root that is not collapsed', () => {
        renderNode(AI_AGENT_DATA);

        expect(screen.queryByRole('button', {name: 'Expand cluster elements'})).not.toBeInTheDocument();
    });

    it('draws its own chain handles as a compact card', () => {
        renderNode(AI_AGENT_DATA);

        expect(screen.getByTestId('handle-target')).toBeInTheDocument();
        expect(screen.getByTestId('handle-source')).toBeInTheDocument();
    });

    it('leaves the chain handles to the frame when drawn as an inline frame', () => {
        renderNode(FRAMED_AI_AGENT_DATA);

        expect(screen.queryByTestId('handle-target')).not.toBeInTheDocument();
        expect(screen.queryByTestId('handle-source')).not.toBeInTheDocument();
    });
});
