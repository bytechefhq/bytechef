import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const WORKSPACE_ID = 1049;

const mockProps = {
    onCloseDropdownMenu: vi.fn(),
    onDuplicateWorkflow: vi.fn(),
    onShareWorkflow: vi.fn(),
    onShowDeleteWorkflowAlertDialog: vi.fn(),
    onShowEditWorkflowDialog: vi.fn(),
    workflowId: '1',
};

// Duplicate is gated on WORKFLOW_CREATE and WORKFLOW_VIEW and Delete on WORKFLOW_DELETE, all in Development
// (ProjectWorkflowFacadeImpl.duplicateWorkflow and deleteWorkflow). Enterprise + plain member is the only configuration in which the gating is
// observable: on Community and for tenant admins useHasWorkspaceScope short-circuits to granted.
function setEnterpriseMemberScopes(scopes: WorkspaceScopeType[], productionScopes: WorkspaceScopeType[] = []): void {
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {
            [WORKSPACE_ID]: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes, status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: productionScopes, status: 'loaded'},
            },
        },
    });
}

const renderWorkflowTabButtons = () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <WorkflowTabButtons {...mockProps} />
            </TooltipProvider>
        </MemoryRouter>
    );
};

describe('WorkflowTabButtons', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

        setEnterpriseMemberScopes(['WORKFLOW_CREATE', 'WORKFLOW_DELETE', 'WORKFLOW_VIEW']);
    });

    it('offers Duplicate and Delete to an Enterprise member holding both scopes', () => {
        renderWorkflowTabButtons();

        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('hides Duplicate and Delete from an Enterprise member holding neither scope', () => {
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderWorkflowTabButtons();

        expect(screen.queryByText('Duplicate')).not.toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('hides only Delete from a member who may create but not delete', () => {
        setEnterpriseMemberScopes(['WORKFLOW_CREATE', 'WORKFLOW_VIEW']);

        renderWorkflowTabButtons();

        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('hides Duplicate from a member holding WORKFLOW_CREATE without WORKFLOW_VIEW', () => {
        setEnterpriseMemberScopes(['WORKFLOW_CREATE']);

        renderWorkflowTabButtons();

        expect(screen.queryByText('Duplicate')).not.toBeInTheDocument();
    });

    it('answers from Development while Production is selected', () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        setEnterpriseMemberScopes(['WORKFLOW_CREATE', 'WORKFLOW_DELETE', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        renderWorkflowTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('ignores workflow scopes held only in the selected Production environment', () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        setEnterpriseMemberScopes([], ['WORKFLOW_CREATE', 'WORKFLOW_DELETE', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        renderWorkflowTabButtons();

        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
        expect(screen.queryByText('Duplicate')).not.toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('offers Edit and Share to an Enterprise member holding WORKFLOW_EDIT', () => {
        setEnterpriseMemberScopes(['WORKFLOW_EDIT']);

        renderWorkflowTabButtons();

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Share')).toBeInTheDocument();
    });

    it('hides Edit and Share from an Enterprise member without WORKFLOW_EDIT but keeps Export', () => {
        setEnterpriseMemberScopes(['WORKFLOW_CREATE', 'WORKFLOW_DELETE']);

        renderWorkflowTabButtons();

        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
        expect(screen.queryByText('Share')).not.toBeInTheDocument();
        expect(screen.getByText('Export')).toBeInTheDocument();
    });

    it('offers every item on Community, where members have no distinct permissions', () => {
        setEnterpriseMemberScopes([]);
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        permissionStore.setState({workspaceScopeStates: {}});

        renderWorkflowTabButtons();

        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Share')).toBeInTheDocument();
    });
});
