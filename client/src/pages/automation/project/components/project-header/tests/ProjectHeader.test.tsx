import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkspaceScopePermissionStateType, permissionStore} from '@/shared/stores/usePermissionStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/project/components/project-header/hooks/useProjectHeader', () => ({
    useProjectHeader: () => ({
        handleProjectWorkflowValueChange: vi.fn(),
        handlePublishProjectSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        hasUnpublishedChanges: false,
        project: {id: 1, name: 'Project One'},
        projectWorkflows: undefined,
        publishProjectMutationIsPending: false,
    }),
}));

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/project/components/project-header/components/DeployButton', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/project/components/project-header/components/PublishPopover', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

const renderHeader = (readOnly: boolean) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
                <TooltipProvider>
                    <ProjectHeader
                        bottomResizablePanelRef={{current: null}}
                        projectId={1}
                        projectWorkflowId={10}
                        runDisabled={false}
                        updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
                    />
                </TooltipProvider>
            </WorkflowEditorReadOnlyContext.Provider>
        </QueryClientProvider>
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
