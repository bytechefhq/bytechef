import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, fireEvent, render, screen} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    isMutating: vi.fn(() => 0),
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
}));

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({agents: []}),
}));

vi.mock('@/pages/automation/project/components/project-header/hooks/useProjectHeader', () => ({
    useProjectHeader: () => ({
        handleProjectWorkflowValueChange: vi.fn(),
        handlePublishProjectSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        hasUnpublishedChanges: true,
        project: {id: 5, name: 'Brokers'},
        projectWorkflows: [{label: 'Workflow 1', projectWorkflowId: 11}],
        publishProjectMutationIsPending: false,
    }),
}));

vi.mock('@/pages/automation/project/components/project-header/components/LeftSidebarButton', () => ({
    default: () => <button>See projects</button>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/ProjectBreadcrumb', () => ({
    default: ({itemSelect}: {itemSelect: React.ReactNode}) => <nav>{itemSelect}</nav>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowActionsButton', () => ({
    default: () => <button>Test</button>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/PublishPopover', () => ({
    default: () => <button>Publish</button>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/DeployButton', () => ({
    default: ({
        onDeploymentSaved,
    }: {
        onDeploymentSaved: (projectDeploymentId: number, environmentId?: number) => void;
    }) => (
        <>
            <button>Deploy</button>

            <button onClick={() => onDeploymentSaved(42, 0)}>Save deployment</button>

            <button onClick={() => onDeploymentSaved(43, 2)}>Save production deployment</button>
        </>
    ),
}));

vi.mock('@/pages/automation/project/components/project-header/components/ProjectDeploymentsButton', () => ({
    default: ({onClick}: {onClick: () => void}) => <button onClick={onClick}>Project Deployments</button>,
}));

vi.mock('@/pages/automation/project/components/ProjectDeploymentsSheet', () => ({
    default: ({
        expandedProjectDeploymentId,
        onSheetOpenChange,
    }: {
        expandedProjectDeploymentId?: number;
        onSheetOpenChange: (open: boolean) => void;
    }) => (
        <div>
            <span>{`Deployments sheet ${expandedProjectDeploymentId ?? 'none'}`}</span>

            <button onClick={() => onSheetOpenChange(false)}>Close sheet</button>
        </div>
    ),
}));

vi.mock('@/pages/automation/project/components/project-header/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu', () => ({
    default: () => <button>Settings</button>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
}));

const renderProjectHeader = (
    embeddedProps: Partial<
        Pick<
            React.ComponentProps<typeof ProjectHeader>,
            'embedded' | 'onWorkflowChange' | 'showPublishDeploy' | 'showWorkflowSelect'
        >
    > = {}
) =>
    render(
        <TooltipProvider>
            <ProjectHeader
                bottomResizablePanelRef={{current: null}}
                projectId={5}
                projectWorkflowId={11}
                runDisabled={false}
                updateWorkflowMutation={{} as UpdateWorkflowMutationType}
                {...embeddedProps}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    hoisted.isMutating.mockReturnValue(0);

    onlineManager.setOnline(true);
});

afterEach(() => {
    onlineManager.setOnline(true);
});

it('orders the header actions Test, Publish, Deploy, Output, Settings', () => {
    renderProjectHeader();

    const actionLabels = screen
        .getAllByRole('button')
        .map((button) => button.textContent)
        .filter((label) => ['Deploy', 'Output', 'Publish', 'Settings', 'Test'].includes(label ?? ''));

    expect(actionLabels).toEqual(['Test', 'Publish', 'Deploy', 'Output', 'Settings']);
});

it('shows the current workflow in the breadcrumb switcher', () => {
    useWorkflowDataStore.setState({workflow: {...useWorkflowDataStore.getState().workflow, label: 'Workflow 1'}});

    renderProjectHeader();

    expect(screen.getByLabelText('Project item select')).toHaveTextContent('Workflow 1');
});

it('hides the save indicator while nothing is being saved and the app is online', () => {
    renderProjectHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('shows the save indicator while a mutation is saving', () => {
    hoisted.isMutating.mockReturnValue(1);

    renderProjectHeader();

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();
});

it('shows the save indicator as soon as the app goes offline', () => {
    renderProjectHeader();

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(false);
    });

    expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();

    act(() => {
        onlineManager.setOnline(true);
    });

    expect(screen.queryByLabelText('Loading indicator')).not.toBeInTheDocument();
});

it('opens the project deployments sheet from the deployments button and closes it', () => {
    renderProjectHeader();

    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('Project Deployments'));

    expect(screen.getByText('Deployments sheet none')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Close sheet'));

    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});

it('opens the project deployments sheet on the saved deployment after deploying', () => {
    renderProjectHeader();

    fireEvent.click(screen.getByText('Save deployment'));

    expect(screen.getByText('Deployments sheet 42')).toBeInTheDocument();
});

it('opens the deployments page in production after deploying to production', () => {
    environmentStore.setState({currentEnvironmentId: 0});
    hoisted.navigate.mockReset();

    renderProjectHeader();

    fireEvent.click(screen.getByText('Save production deployment'));

    expect(environmentStore.getState().currentEnvironmentId).toBe(2);
    expect(hoisted.navigate).toHaveBeenCalledWith(expect.stringContaining('/automation/deployments?projectId='), {
        state: {projectDeploymentId: 43},
    });
    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});

it('shows the sidebar toggle, breadcrumb, Publish, Deploy and Settings on the project page', () => {
    renderProjectHeader();

    expect(screen.getByRole('button', {name: 'See projects'})).toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Settings'})).toBeInTheDocument();
});

it('hides the sidebar toggle, breadcrumb, Publish, Deploy and Settings when embedded', () => {
    renderProjectHeader({embedded: true});

    expect(screen.queryByRole('button', {name: 'See projects'})).not.toBeInTheDocument();
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Publish'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Settings'})).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Test'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Output'})).toBeInTheDocument();
});

it('shows Publish and Deploy when embedded with showPublishDeploy', () => {
    renderProjectHeader({embedded: true, showPublishDeploy: true});

    expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Settings'})).not.toBeInTheDocument();
});

it('shows the workflow select without the breadcrumb when embedded with showWorkflowSelect', () => {
    useWorkflowDataStore.setState({workflow: {...useWorkflowDataStore.getState().workflow, label: 'Workflow 1'}});

    renderProjectHeader({embedded: true, onWorkflowChange: vi.fn(), showWorkflowSelect: true});

    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Project item select')).toHaveTextContent('Workflow 1');
});

it('hides the workflow select when embedded without showWorkflowSelect', () => {
    renderProjectHeader({embedded: true});

    expect(screen.queryByLabelText('Project item select')).not.toBeInTheDocument();
});
