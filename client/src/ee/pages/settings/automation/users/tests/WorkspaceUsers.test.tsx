import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {act, mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceUsers from '../WorkspaceUsers';

const hoisted = vi.hoisted(() => ({
    addWorkspaceUserMutate: vi.fn(),
    assignCustomRoleMutate: vi.fn(),
    authorities: ['ROLE_USER'] as string[],
    captureUpdateOnError: vi.fn(),
    customRoles: [{id: '900', name: 'Deployer', scopes: ['WORKFLOW_VIEW']}] as unknown[],
    inviteWorkspaceUserMutate: vi.fn(),
    removeWorkspaceUserMutate: vi.fn(),
    scopes: ['WORKSPACE_MEMBER_MANAGE'] as string[],
    updateWorkspaceUserRoleMutate: vi.fn(),
    workspaceUsers: [] as unknown[],
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(() => ({account: {authorities: hoisted.authorities, id: 1}})),
}));

vi.mock('zustand/react/shallow', () => ({
    useShallow: vi.fn((selector: unknown) => selector),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    WorkspaceRole: {Admin: 'ADMIN', Editor: 'EDITOR', Viewer: 'VIEWER'},
    useAddWorkspaceUserMutation: vi.fn(() => ({mutate: hoisted.addWorkspaceUserMutate})),
    useAssignWorkspaceUserCustomRoleMutation: vi.fn(() => ({mutate: hoisted.assignCustomRoleMutate})),
    useCustomRolesQuery: vi.fn(() => ({data: {customRoles: hoisted.customRoles}})),
    useInviteWorkspaceUserMutation: vi.fn(() => ({mutate: hoisted.inviteWorkspaceUserMutate})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}, isLoading: false})),
    useRemoveWorkspaceUserMutation: vi.fn(() => ({mutate: hoisted.removeWorkspaceUserMutate})),
    useUpdateWorkspaceUserRoleMutation: vi.fn((options: {onError: (error: Error) => void}) => {
        hoisted.captureUpdateOnError(options.onError);

        return {mutate: hoisted.updateWorkspaceUserRoleMutate};
    }),
    useUsersQuery: vi.fn(() => ({data: {users: {content: [{email: 'existing@example.com', id: 5}]}}})),
    useWorkspaceUsersQuery: vi.fn(() => ({data: {workspaceUsers: hoisted.workspaceUsers}, isLoading: false})),
}));

// Partial mock: the shared render helper builds a real QueryClient, so only useQueryClient is replaced.
vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()}) as never),
}));

const member = {
    id: '1',
    inherited: false,
    user: {email: 'member@example.com', firstName: 'Mem', lastName: 'Ber'},
    userId: '10',
    workspaceId: '7',
    workspaceRole: 'EDITOR',
};

const inheritedAdmin = {
    id: null,
    inherited: true,
    user: {email: 'owner@example.com', firstName: 'Ten', lastName: 'Ant'},
    userId: '99',
    workspaceId: '7',
    workspaceRole: 'ADMIN',
};

describe('WorkspaceUsers', () => {
    beforeEach(() => {
        // Radix Select needs both: jsdom implements neither pointer capture nor ResizeObserver.
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();
        hoisted.authorities = ['ROLE_USER'];
        hoisted.scopes = ['WORKSPACE_MEMBER_MANAGE'];
        hoisted.workspaceUsers = [member];
        hoisted.customRoles = [{id: '900', name: 'Deployer', scopes: ['WORKFLOW_VIEW']}];

        // The real store, driven through setState, rather than a mock returning a bare number. A mocked hook keeps
        // passing if the state shape changes underneath the page's selector; this breaks, which is the point.
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('renders for a workspace admin who is not a tenant admin', () => {
        // The whole point of the page: reachable on the workspace scope, not ROLE_ADMIN. Every sibling settings
        // route is ROLE_ADMIN-gated, so this is the property most likely to be "fixed" back into uselessness.
        render(<WorkspaceUsers />);

        expect(screen.getByRole('button', {name: 'Invite User'})).toBeInTheDocument();
        expect(screen.getByText('member@example.com')).toBeInTheDocument();
    });

    it('refuses the page without the member-manage scope', () => {
        hoisted.scopes = ['WORKSPACE_VIEW'];

        render(<WorkspaceUsers />);

        expect(screen.getByText(/do not have permission/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Invite User'})).not.toBeInTheDocument();
    });

    it('invites by email', async () => {
        render(<WorkspaceUsers />);

        await userEvent.click(screen.getByRole('button', {name: 'Invite User'}));
        await userEvent.type(screen.getByPlaceholderText('colleague@example.com'), 'new@example.com');
        await userEvent.click(screen.getByRole('button', {name: 'Invite'}));

        expect(hoisted.inviteWorkspaceUserMutate).toHaveBeenCalledWith({
            email: 'new@example.com',
            role: 'EDITOR',
            workspaceId: '7',
        });
    });

    it('hides the add-existing-user tab from a workspace admin', async () => {
        // Enumerating every tenant account is ROLE_ADMIN-only and exposes the whole organisation's user list, so a
        // workspace admin adds an existing colleague through the email path instead.
        render(<WorkspaceUsers />);

        await userEvent.click(screen.getByRole('button', {name: 'Invite User'}));

        expect(screen.queryByRole('tab', {name: 'Add existing user'})).not.toBeInTheDocument();
    });

    it('offers the add-existing-user tab to a tenant admin', async () => {
        hoisted.authorities = ['ROLE_ADMIN'];

        render(<WorkspaceUsers />);

        await userEvent.click(screen.getByRole('button', {name: 'Invite User'}));

        expect(screen.getByRole('tab', {name: 'Add existing user'})).toBeInTheDocument();
    });

    it('locks an inherited row', () => {
        hoisted.workspaceUsers = [member, inheritedAdmin];

        render(<WorkspaceUsers />);

        expect(screen.getByText('owner@example.com')).toBeInTheDocument();
        expect(screen.getByText(/inherited from tenant admin/)).toBeInTheDocument();

        // One remove button, for the real member only — an inherited entry has no row to remove.
        expect(screen.getAllByRole('button', {name: ''})).toHaveLength(1);
    });

    it('removes a stored member', async () => {
        render(<WorkspaceUsers />);

        await userEvent.click(screen.getAllByRole('button', {name: ''})[0]);

        expect(hoisted.removeWorkspaceUserMutate).toHaveBeenCalledWith({userId: '10', workspaceId: '7'});
    });

    it('offers custom roles alongside the built-in ones', async () => {
        render(<WorkspaceUsers />);

        // Two comboboxes: the invite-role picker and the member row's. The member's is last.
        const comboboxes = screen.getAllByRole('combobox');

        await userEvent.click(comboboxes[comboboxes.length - 1]);

        expect(screen.getByRole('option', {name: 'Deployer'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Editor'})).toBeInTheDocument();
    });

    it('renders a membership error inline rather than only as a toast', async () => {
        // The typed errors — last admin, self-demotion, inherited entry — answer what the operator just tried, so
        // they belong beside the control. A toast is gone before they can read it against the row.
        render(<WorkspaceUsers />);

        expect(screen.queryByRole('alert')).not.toBeInTheDocument();

        const onError = hoisted.captureUpdateOnError.mock.calls.at(-1)?.[0] as (error: Error) => void;

        await act(async () => {
            onError(new Error('Cannot remove the last admin of workspace 7'));
        });

        expect(screen.getByRole('alert')).toHaveTextContent('Cannot remove the last admin');
    });
});
