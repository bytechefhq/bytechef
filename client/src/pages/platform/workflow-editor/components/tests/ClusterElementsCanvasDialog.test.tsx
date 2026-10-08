import ClusterElementsCanvasDialog from '@/pages/platform/workflow-editor/components/ClusterElementsCanvasDialog';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    isAiAgentClusterRoot: true,
}));

vi.mock('@/pages/platform/workflow-editor/components/hooks/useClusterElementsCanvasDialog', () => ({
    default: () => ({
        copilotEnabled: false,
        handleClose: vi.fn(),
        handleCloseTestingPanel: vi.fn(),
        handleCopilotClick: vi.fn(),
        handleCopilotClose: vi.fn(),
        handleOpenChange: vi.fn(),
        handlePointerDownOutside: vi.fn(),
        handleTestClick: vi.fn(),
        handleToggleEditor: vi.fn(),
        isAiAgentClusterRoot: hoisted.isAiAgentClusterRoot,
        isDataStreamClusterRoot: false,
        isDataStreamSimpleModeAvailable: false,
    }),
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader', () => ({
    default: ({showTestButton}: {showTestButton: boolean}) => (showTestButton ? <button>Test agent</button> : null),
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel',
    () => ({
        default: () => <div>Agent Playbook</div>,
    })
);

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAiAgentEvals', () => ({
    default: () => ({handleClose: vi.fn()}),
}));

vi.mock('@/pages/platform/cluster-element-editor/data-stream-editor/DataStreamEditor', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({
    default: () => null,
}));

const renderDialog = (readOnly: boolean) =>
    render(
        <MemoryRouter>
            <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
                <ClusterElementsCanvasDialog
                    onOpenChange={vi.fn()}
                    open
                    previousComponentDefinitions={[]}
                    updateWorkflowMutation={{} as never}
                    workflowNodeOutputs={[]}
                />
            </WorkflowEditorReadOnlyContext.Provider>
        </MemoryRouter>
    );

afterEach(() => {
    hoisted.isAiAgentClusterRoot = true;

    useClusterElementsCanvasDialogStore.getState().reset();

    resetAll();
});

describe('ClusterElementsCanvasDialog', () => {
    it('offers Test agent on an AI Agent cluster root when the workflow is editable', () => {
        renderDialog(false);

        expect(screen.getByRole('button', {name: 'Test agent'})).toBeInTheDocument();
    });

    it('hides Test agent from a viewer who cannot edit the workflow', () => {
        renderDialog(true);

        expect(screen.queryByRole('button', {name: 'Test agent'})).not.toBeInTheDocument();
    });

    it('does not offer Test agent on a cluster root that is not an AI Agent', () => {
        hoisted.isAiAgentClusterRoot = false;

        renderDialog(false);

        expect(screen.queryByRole('button', {name: 'Test agent'})).not.toBeInTheDocument();
    });

    it('shows an open testing panel when the workflow is editable', () => {
        useClusterElementsCanvasDialogStore.setState({testingPanelOpen: true});

        renderDialog(false);

        expect(screen.getByText('Agent Playbook')).toBeInTheDocument();
    });

    it('hides an open testing panel from a viewer who cannot edit the workflow', () => {
        useClusterElementsCanvasDialogStore.setState({testingPanelOpen: true});

        renderDialog(true);

        expect(screen.queryByText('Agent Playbook')).not.toBeInTheDocument();
    });
});
