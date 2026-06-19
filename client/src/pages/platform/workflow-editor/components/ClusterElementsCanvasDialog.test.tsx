import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ClusterElementsCanvasDialog from './ClusterElementsCanvasDialog';

// The cluster canvas renders a ReactFlow inside this dialog. ReactFlow measures handle positions
// with getBoundingClientRect, which returns scaled values while a CSS scale animation is running, so
// a scale (zoom) entry/exit animation corrupts handle bounds and renders edges non-vertical. The base
// DialogContent hardcodes zoom-in-95/zoom-out-95 and twMerge does not dedupe the tw-animate-css zoom
// utilities, so the dialog pins the enter/exit scale to 1 via inline style (inline beats the utility
// classes deterministically). These tests guard that the scale animation stays removed.

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useParams: () => ({projectId: '1', projectWorkflowId: '1'}),
}));

vi.mock('./hooks/useClusterElementsCanvasDialog', () => ({
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
        isAiAgentClusterRoot: false,
        isDataStreamClusterRoot: false,
        isDataStreamSimpleModeAvailable: false,
    }),
}));

vi.mock('./stores/useClusterElementsCanvasDialogStore', () => ({
    useClusterElementsCanvasDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            copilotPanelOpen: false,
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        }),
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore', () => ({
    useAiAgentEvalsStore: () => ({evalsPanelOpen: false, setEvalsPanelOpen: vi.fn()}),
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAiAgentEvals', () => ({
    default: () => ({handleClose: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({dataPillPanelOpen: false}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({workflowNodeDetailsPanelOpen: false}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPostTurnRegistry', () => ({
    default: {getState: () => ({register: () => () => {}})},
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', () => ({
    Source: {CLUSTER_ELEMENT: 'CLUSTER_ELEMENT'},
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    ProjectWorkflowKeys: {projectWorkflow: () => ['projectWorkflow']},
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor', () => ({default: () => null}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel',
    () => ({default: () => null})
);

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals', () => ({default: () => null}));

vi.mock('@/pages/platform/cluster-element-editor/data-stream-editor/DataStreamEditor', () => ({default: () => null}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel', () => ({default: () => null}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons', () => ({
    DataPillPanelSkeleton: () => null,
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => null}));

const renderDialog = () =>
    render(
        <ClusterElementsCanvasDialog
            onOpenChange={vi.fn()}
            open
            previousComponentDefinitions={[]}
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            updateWorkflowMutation={{} as any}
            workflowNodeOutputs={[]}
            workflowReferenceId={1}
        />
    );

describe('ClusterElementsCanvasDialog - no scale animation', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('pins the enter scale to 1 so ReactFlow never measures handles mid-scale', () => {
        renderDialog();

        const dialog = screen.getByRole('dialog') as HTMLElement;

        expect(dialog.style.getPropertyValue('--tw-enter-scale')).toBe('1');
    });

    it('pins the exit scale to 1 for the close transition', () => {
        renderDialog();

        const dialog = screen.getByRole('dialog') as HTMLElement;

        expect(dialog.style.getPropertyValue('--tw-exit-scale')).toBe('1');
    });
});
