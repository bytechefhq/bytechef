import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkspaceScopePermissionStateType, permissionStore} from '@/shared/stores/usePermissionStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {onlineManager} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

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
    default: ({readOnly}: {readOnly?: boolean}) => (readOnly ? null : <button>Test</button>),
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

vi.mock('@/pages/automation/project/components/project-header/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu', () => ({
    default: () => <button>Settings</button>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
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

const renderHeader = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <ProjectHeader
                    bottomResizablePanelRef={{current: null}}
                    projectId={5}
                    projectWorkflowId={11}
                    runDisabled={false}
                    updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
                />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

const WORKSPACE_ID = 1049;

const setWorkflowEditScopeState = (workspaceScopeState: WorkspaceScopePermissionStateType) =>
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: workspaceScopeState}},
    });

describe('ProjectHeader', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'viewer'} as never,
            authenticated: true,
        });
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        setWorkflowEditScopeState({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
        useWorkflowEditorStore.setState({workflowIsRunning: false});
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    afterEach(() => {
        resetAll();
    });

    it('offers Test and no View only badge when the workflow is editable', () => {
        renderHeader(false);

        expect(screen.getByRole('button', {name: 'Test'})).toBeInTheDocument();
        expect(screen.queryByRole('status', {name: 'View only'})).not.toBeInTheDocument();
    });

    it('shows the View only badge and no Test button in read-only mode', () => {
        renderHeader(true);

        expect(screen.getByRole('status', {name: 'View only'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test'})).not.toBeInTheDocument();
    });

    it('shows no View only badge while the scopes load, and still no Test button', () => {
        setWorkflowEditScopeState({status: 'loading'});

        renderHeader(true);

        expect(screen.queryByRole('status', {name: 'View only'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test'})).not.toBeInTheDocument();
    });

    it('shows the View only badge when the scopes failed to load', () => {
        setWorkflowEditScopeState({status: 'error'});

        renderHeader(true);

        expect(screen.getByRole('status', {name: 'View only'})).toBeInTheDocument();
    });

    it('never shows the View only badge on Community', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        setWorkflowEditScopeState({status: 'error'});

        renderHeader(true);

        expect(screen.queryByRole('status', {name: 'View only'})).not.toBeInTheDocument();
    });
});
