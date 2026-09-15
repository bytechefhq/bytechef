import {TooltipProvider} from '@/components/ui/tooltip';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {Project} from '@/shared/middleware/automation/configuration';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], isFetching: false, refetch: vi.fn()}),
}));

const WORKSPACE_ID = 1049;

// A published project is the precondition for the enabled Deploy button, so the scope check is the only remaining
// reason it can be disabled.
const publishedProject = {
    id: 1,
    lastProjectVersion: 2,
    lastPublishedDate: new Date(),
    name: 'Test Project',
} as Project;

// Deploy opens a dialog that creates a project deployment, which needs BOTH scopes:
// ProjectDeploymentFacadeImpl.createProjectDeployment is annotated WORKFLOW_EDIT and the
// ProjectDeploymentServiceImpl.create it delegates to is annotated DEPLOYMENT_CREATE (as is .update).
const DEPLOY_SCOPES: WorkspaceScopeType[] = ['DEPLOYMENT_CREATE', 'WORKFLOW_EDIT'];

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

const renderDeployButton = (project = publishedProject) => {
    render(
        <MemoryRouter>
            <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                <TooltipProvider>
                    <DeployButton project={project} />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );
};

describe('DeployButton', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

        setEnterpriseMemberScopes(DEPLOY_SCOPES);
    });

    it('enables Deploy for a published project when the member holds both deploy scopes', () => {
        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).not.toBeDisabled();
    });

    it('disables Deploy for an Enterprise member holding neither deploy scope', () => {
        // The server refuses ProjectDeploymentServiceImpl.create without this scope, so an enabled button only buys
        // the user a rejected request at the end of a dialog.
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
    });

    it('disables Deploy for a member holding DEPLOYMENT_CREATE but not WORKFLOW_EDIT', () => {
        // The combination a custom role makes reachable. ProjectDeploymentFacadeImpl.createProjectDeployment is the
        // outermost gate and it asks for WORKFLOW_EDIT, so an enabled button here ends in a 403 on submit — after the
        // user has filled in the whole deployment dialog.
        setEnterpriseMemberScopes(['DEPLOYMENT_CREATE']);

        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
    });

    it('disables Deploy for a member holding WORKFLOW_EDIT but not DEPLOYMENT_CREATE', () => {
        setEnterpriseMemberScopes(['WORKFLOW_EDIT']);

        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
    });

    it('does not call an entitled member unauthorized while the scopes are still loading', () => {
        // useLoadWorkspaceScopes writes {status: 'loading'} on every project open. The button stays disabled — fail
        // closed — but the tooltip must not claim a refusal that has not happened; getDisabledControlTooltip decides
        // that and is asserted directly in util/tests/permission-tooltip-utils.test.ts.
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}},
        });

        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();
    });

    it('keeps Deploy available on Community, where members have no distinct permissions', () => {
        setEnterpriseMemberScopes([]);
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        permissionStore.setState({workspaceScopeStates: {}});

        renderDeployButton();

        expect(screen.getByRole('button', {name: 'Deploy'})).not.toBeDisabled();
    });
});
