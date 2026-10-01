import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import useClusterElementsViewModeStore from '@/pages/platform/workflow-editor/stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ClusterElementsCanvasDialog from './ClusterElementsCanvasDialog';
import {useClusterElementsCanvasDialogStore} from './stores/useClusterElementsCanvasDialogStore';

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useParams: () => ({projectId: '1', projectWorkflowId: '1'}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPostTurnRegistry', () => ({
    default: {getState: () => ({register: () => () => {}})},
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    ProjectWorkflowKeys: {projectWorkflow: () => ['projectWorkflow']},
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor', () => ({
    default: () => <div>canvas view</div>,
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor', () => ({
    default: ({onClose}: {onClose?: () => void}) => (
        <div>
            ai agent editor
            {onClose && <button onClick={onClose}>close ai agent editor</button>}
        </div>
    ),
}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel',
    () => ({default: () => null})
);

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals', () => ({
    default: () => <div>dialog-own-evals-overlay</div>,
}));

vi.mock('@/pages/platform/cluster-element-editor/data-stream-editor/DataStreamEditor', () => ({
    default: () => <div>data stream editor</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel', () => ({default: () => null}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({dataPillPanelOpen: false}),
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons', () => ({
    DataPillPanelSkeleton: () => null,
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => null}));

const AI_AGENT_ROOT_DATA = {
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

const DATA_STREAM_ROOT_DATA = {
    componentName: 'dataStream',
    name: 'dataStream_1',
    type: 'dataStream/v1',
    workflowNodeName: 'dataStream_1',
} as unknown as NodeDataType;

const renderDialog = (onOpenChange: () => void = vi.fn()) =>
    render(
        <ClusterElementsCanvasDialog
            onOpenChange={onOpenChange}
            open
            previousComponentDefinitions={[]}
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            updateWorkflowMutation={{} as any}
            workflowNodeOutputs={[]}
            workflowReferenceId={1}
        />
    );

describe('ClusterElementsCanvasDialog - evals reachability and cleanup', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({workflow: {definition: undefined, id: 'workflow-1', nodeNames: [], version: 1}});
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false, rootClusterElementNodeData: undefined});
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
        useClusterElementsCanvasDialogStore.setState({
            copilotPanelOpen: false,
            editorPreferences: {},
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        });
        useAiAgentEvalsStore.setState({
            evalsPanelOpen: false,
            evalsTab: 'tests',
            selectedRunId: null,
            selectedTestId: null,
        });
        useWorkflowNodeDetailsPanelStore.getState().reset();
    });

    it("renders the AI Agent editor branch, not the dialog's own duplicate evals overlay, when evalsPanelOpen is set for an agent root", () => {
        useWorkflowEditorStore.setState({rootClusterElementNodeData: AI_AGENT_ROOT_DATA});
        useAiAgentEvalsStore.setState({evalsPanelOpen: true});

        renderDialog();

        expect(screen.getByText('ai agent editor')).toBeInTheDocument();
        expect(screen.queryByText('dialog-own-evals-overlay')).not.toBeInTheDocument();
        expect(screen.queryByText('canvas view')).not.toBeInTheDocument();
    });

    it('clears evalsPanelOpen when the dialog closes, so it cannot leak into a differently-typed root reopened after it', async () => {
        const user = userEvent.setup();
        const onOpenChange = vi.fn();

        useWorkflowEditorStore.setState({rootClusterElementNodeData: AI_AGENT_ROOT_DATA});
        useAiAgentEvalsStore.setState({evalsPanelOpen: true});

        useClusterElementsCanvasDialogStore.setState({editorPreferences: {'1:dataStream_1': false}});

        const {rerender} = renderDialog(onOpenChange);

        expect(screen.getByText('ai agent editor')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'close ai agent editor'}));

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(useAiAgentEvalsStore.getState().evalsPanelOpen).toBe(false);

        useWorkflowEditorStore.setState({rootClusterElementNodeData: DATA_STREAM_ROOT_DATA});

        rerender(
            <ClusterElementsCanvasDialog
                onOpenChange={onOpenChange}
                open
                previousComponentDefinitions={[]}
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                updateWorkflowMutation={{} as any}
                workflowNodeOutputs={[]}
                workflowReferenceId={1}
            />
        );

        expect(await screen.findByText('canvas view')).toBeInTheDocument();
        expect(screen.queryByText('dialog-own-evals-overlay')).not.toBeInTheDocument();
        expect(screen.queryByText('ai agent editor')).not.toBeInTheDocument();
        expect(screen.queryByText('data stream editor')).not.toBeInTheDocument();
    });
});
