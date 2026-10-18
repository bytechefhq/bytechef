import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    projectHeaderProps: vi.fn(),
    useGetProjectWorkflowQueryMock: vi.fn(),
    workflowEditorLayoutProps: vi.fn(),
}));

vi.mock('@/pages/automation/project/components/project-header/ProjectHeader', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.projectHeaderProps(props);

        return <header>Project header</header>;
    },
}));

vi.mock('@/pages/platform/workflow-editor/WorkflowEditorLayout', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.workflowEditorLayoutProps(props);

        return <div>Workflow editor layout</div>;
    },
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput', () => ({
    default: () => <div>Test output</div>,
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useRun', () => ({
    useRun: () => ({runDisabled: false}),
}));

vi.mock('@/shared/components/WorkflowTestRunLeaveDialog', () => ({
    default: () => null,
}));

vi.mock('@/shared/hooks/useWorkflowTestRunGuard', () => ({
    useWorkflowTestRunGuard: () => ({
        cancelLeave: vi.fn(),
        confirmLeave: vi.fn(),
        showLeaveDialog: false,
        workflowIsRunning: false,
        workflowTestExecution: undefined,
    }),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectWorkflows.queries')>()),
    useGetProjectWorkflowQuery: hoisted.useGetProjectWorkflowQueryMock,
}));

vi.mock('@/shared/queries/automation/connections.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/connections.queries')>()),
    useGetConnectionTagsQuery: () => ({data: []}),
}));

const openAllPanels = () => {
    useDataPillPanelStore.getState().setDataPillPanelOpen(true);
    useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(true);
    useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(true);
};

describe('EmbeddableWorkflowEditor', () => {
    beforeEach(() => {
        hoisted.projectHeaderProps.mockClear();
        hoisted.workflowEditorLayoutProps.mockClear();
        hoisted.useGetProjectWorkflowQueryMock.mockImplementation((projectId: number, projectWorkflowId: number) => ({
            data: {
                id: `workflow-${projectWorkflowId}`,
                label: `Workflow ${projectWorkflowId}`,
                projectId,
                projectWorkflowId,
            },
            isLoading: false,
        }));

        useWorkflowDataStore.setState({workflow: {nodeNames: []}});
    });

    it('renders the embedded header and editor for the project workflow', async () => {
        render(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} />);

        expect(screen.getByText('Project header')).toBeInTheDocument();
        expect(screen.getByText('Workflow editor layout')).toBeInTheDocument();

        expect(hoisted.useGetProjectWorkflowQueryMock).toHaveBeenCalledWith(5, 11, true, undefined, {
            staleTime: 30_000,
        });

        expect(hoisted.projectHeaderProps).toHaveBeenLastCalledWith(
            expect.objectContaining({embedded: true, projectId: 5, projectWorkflowId: 11})
        );

        expect(hoisted.workflowEditorLayoutProps).toHaveBeenLastCalledWith(
            expect.objectContaining({fitViewOnWorkflowChange: true, leftSidebarOpen: false, showCopilot: false})
        );

        await waitFor(() => expect(useWorkflowDataStore.getState().workflow.id).toBe('workflow-11'));
    });

    it('hides Publish and Deploy unless showPublishDeploy is set', () => {
        const {rerender} = render(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} />);

        expect(hoisted.projectHeaderProps).toHaveBeenLastCalledWith(
            expect.objectContaining({showPublishDeploy: undefined})
        );

        rerender(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} showPublishDeploy />);

        expect(hoisted.projectHeaderProps).toHaveBeenLastCalledWith(expect.objectContaining({showPublishDeploy: true}));
    });

    it('closes the node details, data pill and test chat panels when the project workflow changes', async () => {
        const {rerender} = render(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} />);

        openAllPanels();

        rerender(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={12} />);

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(false);
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);

        await waitFor(() => expect(useWorkflowDataStore.getState().workflow.id).toBe('workflow-12'));
    });

    it('leaves the panels alone while the project workflow stays the same', async () => {
        const {rerender} = render(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} />);

        await waitFor(() => expect(useWorkflowDataStore.getState().workflow.id).toBe('workflow-11'));

        openAllPanels();

        rerender(<EmbeddableWorkflowEditor projectId={5} projectWorkflowId={11} showPublishDeploy />);

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
        expect(useWorkflowTestChatStore.getState().workflowTestChatPanelOpen).toBe(true);
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);
    });
});
