import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {featureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

let queryClient: QueryClient;

const WORKSPACE_ID = 1049;

// The scopes the server actually enforces for the five gated items in this menu. Edit -> WORKFLOW_EDIT
// (ProjectFacadeImpl.updateProject), Share -> PROJECT_SETTINGS (ProjectFacadeImpl.exportSharedProject),
// Delete -> PROJECT_DELETE (ProjectFacadeImpl.deleteProject), Duplicate -> PROJECT_CREATE
// (ProjectFacadeImpl.duplicateProject, which also requires WORKFLOW_VIEW) and Pull Project from Git ->
// PROJECT_PULL (ProjectGitFacadeImpl.pullProjectFromGit), and Git Configuration -> WORKSPACE_MANAGE
// (ProjectGitFacadeImpl.getRemoteBranches and ProjectGitConfigurationServiceImpl.save).
const ALL_GATED_SCOPES: WorkspaceScopeType[] = [
    'PROJECT_PULL',
    'WORKSPACE_MANAGE',
    'PROJECT_CREATE',
    'PROJECT_DELETE',
    'PROJECT_SETTINGS',
    'WORKFLOW_EDIT',
];

// Enterprise + plain member + an explicit scope set is the only configuration in which the gating is observable: on
// Community and for tenant admins useHasWorkspaceScope short-circuits to granted.
function setEnterpriseMemberScopes(scopes: WorkspaceScopeType[]): void {
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes, status: 'loaded'}}},
    });
}

// The two Git items are behind ff-1039 as well as behind EEVersion, so without turning the flag on neither the
// PROJECT_PULL gate on Pull nor the WORKSPACE_MANAGE gate on Git Configuration is observable.
function enableGitIntegrationFeatureFlag(): void {
    featureFlagsStore.setState({featureFlags: {'ff-1039': true}, loadingFlags: {}});
}

beforeEach(() => {
    queryClient = createTestQueryClient();

    useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

    // The flag store is module-level state, so a test that turns ff-1039 on would otherwise leak it into every test
    // declared after it.
    featureFlagsStore.setState({featureFlags: {}, loadingFlags: {}});

    setEnterpriseMemberScopes(ALL_GATED_SCOPES);
});

afterEach(() => {
    queryClient.clear();
});

const mockProps = {
    hiddenFileInputRef: {current: null} as React.RefObject<HTMLInputElement | null>,
    onCloseDropdownMenuClick: vi.fn(),
    onDeleteProjectClick: vi.fn(),
    onDuplicateProjectClick: vi.fn(),
    onPullProjectFromGitClick: vi.fn(),
    onShareProject: vi.fn(),
    onShowEditProjectDialogClick: vi.fn(),
    onShowProjectGitConfigurationDialog: vi.fn(),
    onShowProjectVersionHistorySheet: vi.fn(),
    projectGitConfigurationEnabled: false,
    projectId: 123,
};

const renderProjectTabButtons = (props = mockProps) => {
    render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <ProjectTabButtons {...props} />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );
};

describe('ProjectTabButtons Export Functionality', () => {
    beforeEach(() => {
        // Mock window.location.href assignment
        Object.defineProperty(window, 'location', {
            value: {
                href: '',
            },
            writable: true,
        });

        // Clear all mocks
        vi.clearAllMocks();
    });

    it('should render export button', () => {
        renderProjectTabButtons();

        const exportButton = screen.getByText('Export');
        expect(exportButton).toBeInTheDocument();
    });

    it('should have correct export URL when export button is clicked', async () => {
        renderProjectTabButtons();

        const exportButton = screen.getByText('Export');
        expect(exportButton).toBeInTheDocument();

        // Click the export button
        await userEvent.click(exportButton);

        // Check that window.location.href was set to the correct URL
        expect(window.location.href).toBe('/api/automation/internal/projects/123/export');
    });

    it('should use correct project ID in export URL', async () => {
        const customProps = {
            ...mockProps,
            projectId: 456,
        };

        renderProjectTabButtons(customProps);

        const exportButton = screen.getByText('Export');
        await userEvent.click(exportButton);

        expect(window.location.href).toBe('/api/automation/internal/projects/456/export');
    });

    it('should call onCloseDropdownMenuClick when export button is clicked', async () => {
        renderProjectTabButtons();

        const exportButton = screen.getByText('Export');
        await userEvent.click(exportButton);

        // The handleButtonClick function should trigger onCloseDropdownMenuClick
        expect(mockProps.onCloseDropdownMenuClick).toHaveBeenCalled();
    });

    it('should render other action buttons correctly', () => {
        renderProjectTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Export')).toBeInTheDocument();
        expect(screen.getByText('Project History')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('should call appropriate handlers when buttons are clicked', async () => {
        renderProjectTabButtons();

        // Test Edit button
        await userEvent.click(screen.getByText('Edit'));
        expect(mockProps.onShowEditProjectDialogClick).toHaveBeenCalled();

        // Test Duplicate button
        await userEvent.click(screen.getByText('Duplicate'));
        expect(mockProps.onDuplicateProjectClick).toHaveBeenCalled();

        // Test Project History button
        await userEvent.click(screen.getByText('Project History'));
        expect(mockProps.onShowProjectVersionHistorySheet).toHaveBeenCalled();

        // Test Delete button
        await userEvent.click(screen.getByText('Delete'));
        expect(mockProps.onDeleteProjectClick).toHaveBeenCalled();
    });

    it('should not show Git-related buttons when feature flag is disabled', () => {
        renderProjectTabButtons();

        // These buttons should not be visible when gitIntegrationEnabled (ff-1039) is disabled
        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });

    it('hides Edit, Delete and Duplicate for an Enterprise member holding none of their scopes', () => {
        // Before this gating existed an EDITOR was offered "Delete Project" and the click was refused server-side.
        // WORKFLOW_VIEW stands in for "can open the project but can do nothing privileged". Share is asserted
        // separately, below.
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderProjectTabButtons();

        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
        expect(screen.queryByText('Duplicate')).not.toBeInTheDocument();
    });

    it('still offers the ungated items to an Enterprise member holding none of the gated scopes', () => {
        // Export and Project History ask only for WORKFLOW_VIEW server-side, which opening the project already
        // required. Gating them would hide functionality the server would happily serve. Duplicate used to belong in
        // this list and no longer does: duplicateProject creates a project and its workflows, so it demands
        // PROJECT_CREATE on top of the read.
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderProjectTabButtons();

        expect(screen.getByText('Export')).toBeInTheDocument();
        expect(screen.getByText('Project History')).toBeInTheDocument();
    });

    it('shows Duplicate for an Enterprise member holding PROJECT_CREATE', () => {
        // duplicateProject is gated on WORKFLOW_VIEW *and* PROJECT_CREATE. Only the create half is checked here,
        // because a member who could not read the project would never have this menu open.
        setEnterpriseMemberScopes(['PROJECT_CREATE', 'WORKFLOW_VIEW']);

        renderProjectTabButtons();

        expect(screen.getByText('Duplicate')).toBeInTheDocument();
    });

    it('shows Edit for an Enterprise member holding WORKFLOW_EDIT', () => {
        setEnterpriseMemberScopes(['WORKFLOW_EDIT']);

        renderProjectTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('shows Delete for an Enterprise member holding PROJECT_DELETE', () => {
        setEnterpriseMemberScopes(['PROJECT_DELETE']);

        renderProjectTabButtons();

        expect(screen.getByText('Delete')).toBeInTheDocument();
        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
    });

    it('offers Share to an Enterprise member holding PROJECT_SETTINGS', () => {
        // Share opens a dialog that calls exportSharedProject / deleteSharedProject, both annotated
        // hasPermission(#id, 'Project', 'PROJECT_SETTINGS').
        setEnterpriseMemberScopes(['PROJECT_SETTINGS']);

        renderProjectTabButtons();

        expect(screen.getByText('Share')).toBeInTheDocument();
    });

    it('hides Share from an Enterprise member without PROJECT_SETTINGS', () => {
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderProjectTabButtons();

        expect(screen.queryByText('Share')).not.toBeInTheDocument();
    });

    it('offers Pull Project from Git to an Enterprise member holding PROJECT_PULL', () => {
        enableGitIntegrationFeatureFlag();
        setEnterpriseMemberScopes(['PROJECT_PULL']);

        renderProjectTabButtons();

        expect(screen.getByText('Pull Project from Git')).toBeInTheDocument();
    });

    it('hides Pull Project from Git from an Enterprise member without PROJECT_PULL', () => {
        // ProjectGitFacadeImpl.pullProjectFromGit now carries hasPermission(#projectId, 'Project', 'PROJECT_PULL'),
        // so offering the item to a member without the scope means offering a click the server refuses.
        enableGitIntegrationFeatureFlag();
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderProjectTabButtons();

        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
    });

    it('offers Git Configuration to an Enterprise member holding WORKSPACE_MANAGE', () => {
        enableGitIntegrationFeatureFlag();
        setEnterpriseMemberScopes(['WORKSPACE_MANAGE']);

        renderProjectTabButtons();

        expect(screen.getByText('Git Configuration')).toBeInTheDocument();
    });

    it('hides Git Configuration from an Enterprise member without WORKSPACE_MANAGE, even one holding PROJECT_PULL', () => {
        // The dialog reads its branch list through ProjectGitFacadeImpl.getRemoteBranches and submits through
        // ProjectGitConfigurationServiceImpl.save, both gated on WORKSPACE_MANAGE. Pulling and configuring are separate
        // privileges, so holding one must not reveal the other.
        enableGitIntegrationFeatureFlag();
        setEnterpriseMemberScopes(['PROJECT_PULL']);

        renderProjectTabButtons();

        expect(screen.getByText('Pull Project from Git')).toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });

    it('keeps the Git items behind ff-1039 even for a member holding PROJECT_PULL', () => {
        setEnterpriseMemberScopes(['PROJECT_PULL']);

        renderProjectTabButtons();

        expect(screen.queryByText('Pull Project from Git')).not.toBeInTheDocument();
        expect(screen.queryByText('Git Configuration')).not.toBeInTheDocument();
    });

    it('shows every gated item on Community, where there is no boundary between workspace members', () => {
        // No scopes are stored and the account is not an admin: on CE the hook grants regardless, so a Community
        // install must not lose any control to a layer that has nothing to enforce there.
        setEnterpriseMemberScopes([]);
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        permissionStore.setState({workspaceScopeStates: {}});

        renderProjectTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
    });

    it('shows every gated item for a tenant admin with no scopes loaded', () => {
        enableGitIntegrationFeatureFlag();
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_ADMIN'], login: 'tester'} as never,
            authenticated: true,
        });
        permissionStore.setState({workspaceScopeStates: {}});

        renderProjectTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Pull Project from Git')).toBeInTheDocument();
    });
});
