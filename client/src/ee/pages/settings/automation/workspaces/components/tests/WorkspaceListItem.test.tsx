import WorkspaceListItem from '@/ee/pages/settings/automation/workspaces/components/WorkspaceListItem';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: undefined, isError: false, isLoading: false})),
}));

vi.mock('@/ee/shared/mutations/automation/workspaces.mutations', () => ({
    useDeleteWorkspaceMutation: vi.fn(() => ({mutate: hoisted.deleteMutate})),
}));

// Partial mock: everything else in the generated module stays real, so only the permission query is observable.
vi.mock(import('@/shared/middleware/graphql'), async (importOriginal) => ({
    ...(await importOriginal()),
    useMyWorkspaceScopesQuery: hoisted.useMyWorkspaceScopesQuery as never,
}));

const WORKSPACE_ID = 7;

const workspace = {id: WORKSPACE_ID, name: 'Payments'} as Workspace;

// An Enterprise member who is NOT a tenant admin and holds no built-in workspace role — the shape a custom-role
// membership produces, because PermissionServiceImpl.getMyWorkspaceRole answers null for one. Their scopes are loaded
// and include member management.
function setCustomRoleMember(): void {
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], id: 1, login: 'lead'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {
            [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKSPACE_MEMBER_MANAGE'], status: 'loaded'}},
        },
    });
}

const openRowMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: `Workspace actions for ${workspace.name}`}));
};

describe('WorkspaceListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        vi.clearAllMocks();
        setCustomRoleMember();
    });

    it('offers every row action without consulting a built-in workspace role', async () => {
        // The route is ADMIN-only, so the removed role gates were unconditionally true in production; a member with no
        // built-in role is the case those gates would have silently hidden everything from if the route were opened.
        render(<WorkspaceListItem workspace={workspace} />);

        await openRowMenu();

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Members'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('fires no per-row permission query', () => {
        // One permission fetch per workspace, each keyed by a distinct id react-query cannot dedupe, for an answer the
        // tenant-admin short-circuit would discard before reading it.
        render(<WorkspaceListItem workspace={workspace} />);

        expect(hoisted.useMyWorkspaceScopesQuery).not.toHaveBeenCalled();
    });

    it('names the workspace in the delete confirmation', async () => {
        // The copy said "This will permanently delete the connection" on a dialog that deletes a workspace and
        // everything inside it.
        render(<WorkspaceListItem workspace={workspace} />);

        await openRowMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(screen.getByRole('alertdialog')).toHaveTextContent(
            'This action cannot be undone. This will permanently delete the workspace Payments and everything in it.'
        );
        expect(screen.queryByText(/delete the connection/i)).not.toBeInTheDocument();
    });

    it('deletes the workspace once confirmed', async () => {
        render(<WorkspaceListItem workspace={workspace} />);

        await openRowMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith(WORKSPACE_ID);
    });
});
