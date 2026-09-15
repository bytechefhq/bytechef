import {TooltipProvider} from '@/components/ui/tooltip';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockOnPublishProjectSubmit = vi.fn();

const WORKSPACE_ID = 1049;

// Publishing needs BOTH scopes: ProjectFacadeImpl.publishProject is annotated WORKFLOW_EDIT and the
// ProjectServiceImpl.publishProject / ProjectWorkflowServiceImpl.publishWorkflow calls it makes are annotated
// PROJECT_PUBLISH. Grant both to a plain Enterprise member so the popover behaves as it did before the gate existed;
// each single-scope denial is asserted at the bottom of this file.
const PUBLISH_SCOPES = ['PROJECT_PUBLISH', 'WORKFLOW_EDIT'];

function setEnterpriseMemberScopes(scopes: string[]): void {
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes, status: 'loaded'}}},
    });
}

beforeEach(() => {
    useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

    setEnterpriseMemberScopes(PUBLISH_SCOPES);
});

const renderPublishPopover = (disabled = false) => {
    render(
        <TooltipProvider>
            <PublishPopover disabled={disabled} isPending={false} onPublishProjectSubmit={mockOnPublishProjectSubmit} />
        </TooltipProvider>
    );
};

it('should render the publish popover button', () => {
    renderPublishPopover();

    expect(screen.getByText('Publish')).toBeInTheDocument();
});

it('should open the publish popover on click', () => {
    renderPublishPopover();

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.getByText('Publish Project')).toBeInTheDocument();
});

it('should call the onPublishProjectSubmit function with the correct description on submit', async () => {
    renderPublishPopover();

    fireEvent.click(screen.getByText('Publish'));

    fireEvent.input(screen.getByLabelText('Description'), {target: {value: 'Project description'}});

    expect(screen.getByLabelText('Publish button')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Publish button'));

    await waitFor(() => {
        expect(mockOnPublishProjectSubmit).toHaveBeenCalledWith({
            description: 'Project description',
            onSuccess: expect.any(Function),
        });
    });
});

it('should disable the publish button when there is nothing to publish', () => {
    renderPublishPopover(true);

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('should not open the publish popover when the publish button is disabled', () => {
    renderPublishPopover(true);

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.queryByText('Publish Project')).not.toBeInTheDocument();
});

it('disables publishing for an Enterprise member holding neither publish scope', () => {
    // The server refuses ProjectServiceImpl.publishProject without this scope, so offering an enabled button only
    // buys the user a rejected request.
    setEnterpriseMemberScopes(['WORKFLOW_VIEW']);

    renderPublishPopover();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.queryByText('Publish Project')).not.toBeInTheDocument();
});

it('disables publishing for a member holding PROJECT_PUBLISH but not WORKFLOW_EDIT', () => {
    // The combination a custom role makes reachable. ProjectFacadeImpl.publishProject is the outermost gate and it
    // asks for WORKFLOW_EDIT, so an enabled button here ends in a 403 after the user has written a description.
    setEnterpriseMemberScopes(['PROJECT_PUBLISH']);

    renderPublishPopover();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.queryByText('Publish Project')).not.toBeInTheDocument();
});

it('disables publishing for a member holding WORKFLOW_EDIT but not PROJECT_PUBLISH', () => {
    setEnterpriseMemberScopes(['WORKFLOW_EDIT']);

    renderPublishPopover();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('does not call an entitled member unauthorized while the scopes are still loading', () => {
    // useLoadWorkspaceScopes writes {status: 'loading'} on every project open. The button stays disabled — fail
    // closed — but the tooltip must not claim a refusal that has not happened; getDisabledControlTooltip decides
    // that and is asserted directly in util/tests/permission-tooltip-utils.test.ts.
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}},
    });

    renderPublishPopover();

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('keeps publishing available on Community, where members have no distinct permissions', () => {
    setEnterpriseMemberScopes([]);
    applicationInfoStore.setState({application: {edition: EditionType.CE}});
    permissionStore.setState({workspaceScopeStates: {}});

    renderPublishPopover();

    expect(screen.getByRole('button', {name: 'Publish'})).not.toBeDisabled();
});
