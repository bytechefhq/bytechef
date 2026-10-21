import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, fireEvent, render, screen} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    isMutating: vi.fn(() => 0),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
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

            <button onClick={() => onDeploymentSaved(42, 2)}>Save deployment</button>
        </>
    ),
}));

vi.mock('@/pages/automation/project/components/project-header/components/ProjectDeploymentsButton', () => ({
    default: ({onClick}: {onClick: () => void}) => <button onClick={onClick}>Project Deployments</button>,
}));

vi.mock('@/pages/automation/project/components/ProjectDeploymentsSheet', () => ({
    default: ({
        environmentId,
        expandedProjectDeploymentId,
        onSheetOpenChange,
    }: {
        environmentId?: number;
        expandedProjectDeploymentId?: number;
        onSheetOpenChange: (open: boolean) => void;
    }) => (
        <div>
            <span>{`Deployments sheet ${environmentId ?? 'current'} ${expandedProjectDeploymentId ?? 'none'}`}</span>

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

const renderProjectHeader = () =>
    render(
        <TooltipProvider>
            <ProjectHeader
                bottomResizablePanelRef={{current: null}}
                projectId={5}
                projectWorkflowId={11}
                runDisabled={false}
                updateWorkflowMutation={{} as UpdateWorkflowMutationType}
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

    expect(screen.getByText('Deployments sheet current none')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Close sheet'));

    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});

it('opens the project deployments sheet on the saved deployment after deploying', () => {
    renderProjectHeader();

    fireEvent.click(screen.getByText('Save deployment'));

    expect(screen.getByText('Deployments sheet 2 42')).toBeInTheDocument();
});
