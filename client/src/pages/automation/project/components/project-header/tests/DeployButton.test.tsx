import {Dialog, DialogContent, DialogTitle, DialogTrigger} from '@/components/Dialog';
import {TooltipProvider} from '@/components/ui/tooltip';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {WorkspaceScopeType} from '@/shared/hooks/useHasWorkspaceScope';
import {Project} from '@/shared/middleware/automation/configuration';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

function MockProjectDeploymentDialog({
    onClose,
    projectDeployment,
    triggerNode,
}: {
    onClose?: () => void;
    projectDeployment?: {name?: string};
    triggerNode?: ReactNode;
}) {
    const [initialProjectName] = useState(projectDeployment?.name);

    return triggerNode ? (
        <Dialog>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent>
                <DialogTitle>Deploy Project</DialogTitle>

                <span>Deploying {initialProjectName}</span>
            </DialogContent>
        </Dialog>
    ) : (
        <div>
            <span>Controlled Deploy Dialog</span>

            <button onClick={onClose}>Close controlled</button>
        </div>
    );
}

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: MockProjectDeploymentDialog,
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], isFetching: false, refetch: vi.fn(() => Promise.resolve())}),
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

const renderDeployButton = (
    project = publishedProject,
    controlled: {onOpenChange?: (open: boolean) => void; open?: boolean} = {}
) => {
    render(
        <MemoryRouter>
            <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                <TooltipProvider>
                    <DeployButton onOpenChange={controlled.onOpenChange} open={controlled.open} project={project} />
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

    it('shows a tooltip on hover and opens the deployment dialog on click', async () => {
        renderDeployButton();

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        await userEvent.hover(deployButton);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Deploy the published project version');

        await userEvent.click(deployButton);

        expect(await screen.findByText('Deploy Project')).toBeInTheDocument();
    });

    it('offers the current project after switching to another project', async () => {
        const {rerender} = renderDeployButton(publishedProject);

        rerender(
            <TooltipProvider>
                <DeployButton project={{...publishedProject, id: 8, name: 'Invoices'}} />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Deploy'}));

        expect(await screen.findByText('Deploying Invoices')).toBeInTheDocument();
    });

    it('explains why deploying is disabled for an unpublished project', async () => {
        renderDeployButton({id: 7, name: 'Orders'} as Project);

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        expect(deployButton).toBeDisabled();

        await userEvent.hover(deployButton.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the project before deploying it.');
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

    it('renders the deployment dialog without a click when open is true', async () => {
        renderDeployButton(publishedProject, {open: true});

        expect(await screen.findByText('Controlled Deploy Dialog')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
    });

    it('does not render the controlled deployment dialog when open is false', () => {
        renderDeployButton(publishedProject, {open: false});

        expect(screen.queryByText('Controlled Deploy Dialog')).not.toBeInTheDocument();
    });

    it('calls onOpenChange with false when the controlled deployment dialog closes', async () => {
        const onOpenChange = vi.fn();

        renderDeployButton(publishedProject, {onOpenChange, open: true});

        await userEvent.click(await screen.findByRole('button', {name: 'Close controlled'}));

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('clears the open flag and renders no dialog for an unpublished project', () => {
        const onOpenChange = vi.fn();

        renderDeployButton({id: 7, name: 'Orders'} as Project, {onOpenChange, open: true});

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(screen.queryByText('Controlled Deploy Dialog')).not.toBeInTheDocument();
    });

    it('renders no controlled deployment dialog for a member without the deploy scopes', async () => {
        setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

        renderDeployButton(publishedProject, {open: true});

        expect(await screen.findByRole('button', {name: 'Deploy'})).toBeDisabled();
        expect(screen.queryByText('Controlled Deploy Dialog')).not.toBeInTheDocument();
    });
});
