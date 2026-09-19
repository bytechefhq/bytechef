import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
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

// Duplicate is gated on WORKFLOW_CREATE (ProjectWorkflowServiceImpl.addWorkflow) and Delete on WORKFLOW_DELETE
// (ProjectWorkflowServiceImpl.delete). Enterprise + plain member is the only configuration in which the gating is
// observable: on Community and for tenant admins useHasWorkspaceScope short-circuits to granted.
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
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

        setEnterpriseMemberScopes(['WORKFLOW_CREATE', 'WORKFLOW_DELETE']);
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
        setEnterpriseMemberScopes(['WORKFLOW_CREATE']);

        renderWorkflowTabButtons();

        expect(screen.getByText('Duplicate')).toBeInTheDocument();
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
