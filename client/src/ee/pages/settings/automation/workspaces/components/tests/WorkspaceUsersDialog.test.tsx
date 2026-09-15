import WorkspaceUsersDialog from '@/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    addMutate: vi.fn(),
    captureRemoveOnSuccess: vi.fn(),
    invalidateQueries: vi.fn(),
    removeMutate: vi.fn(),
    updateRoleMutate: vi.fn(),
    workspaceUsers: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    WorkspaceRole: {Admin: 'ADMIN', Editor: 'EDITOR', Viewer: 'VIEWER'},
    useAddWorkspaceUserMutation: vi.fn(() => ({mutate: hoisted.addMutate})),
    useRemoveWorkspaceUserMutation: vi.fn((options: {onSuccess: () => void}) => {
        hoisted.captureRemoveOnSuccess(options.onSuccess);

        return {mutate: hoisted.removeMutate};
    }),
    useUpdateWorkspaceUserRoleMutation: vi.fn(() => ({mutate: hoisted.updateRoleMutate})),
    useUsersQuery: vi.fn(() => ({data: {users: {content: []}}})),
    useWorkspaceUsersQuery: vi.fn(() => ({data: {workspaceUsers: hoisted.workspaceUsers}, isLoading: false})),
}));

// Partial mock: the shared render helper builds a real QueryClient, so only useQueryClient is replaced.
vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: hoisted.invalidateQueries}) as never),
}));

const WORKSPACE_ID = 7;

const member = {
    createdDate: '2026-01-02',
    environment: null,
    id: '1',
    inherited: false,
    user: {email: 'member@example.com', firstName: 'Mem', lastName: 'Ber'},
    userId: '10',
    workspaceId: '7',
    workspaceRole: 'EDITOR',
};

// Synthesized rows for tenant admins: no membership row exists, so the server has no id to give them. Two of them is
// the case that used to collapse onto one React key.
const firstInheritedAdmin = {
    createdDate: null,
    environment: null,
    id: null,
    inherited: true,
    user: {email: 'owner@example.com', firstName: 'Ten', lastName: 'Ant'},
    userId: '98',
    workspaceId: '7',
    workspaceRole: 'ADMIN',
};

const secondInheritedAdmin = {
    createdDate: null,
    environment: null,
    id: null,
    inherited: true,
    user: {email: 'second.owner@example.com', firstName: 'Two', lastName: 'Admin'},
    userId: '99',
    workspaceId: '7',
    workspaceRole: 'ADMIN',
};

// An Enterprise member holding WORKSPACE_MEMBER_MANAGE through a custom role: getMyWorkspaceRole answers null for such a
// membership, so there is no entry in workspaceStates at all.
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

const renderDialog = () => render(<WorkspaceUsersDialog onClose={vi.fn()} open={true} workspaceId={WORKSPACE_ID} />);

describe('WorkspaceUsersDialog', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();
        hoisted.workspaceUsers = [member];
        setCustomRoleMember();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('locks an inherited row and offers no control that would change it', () => {
        // The row's access comes from the tenant-admin authority, not from a membership row: there is nothing for the
        // role select to update and nothing for the remove button to delete. Both were rendered anyway, because the
        // only thing they checked was `!workspaceUser.environment`, which is null on an inherited row too.
        hoisted.workspaceUsers = [member, firstInheritedAdmin];

        renderDialog();

        expect(screen.getByText(/inherited from tenant admin/)).toBeInTheDocument();

        // One select and one remove control, both belonging to the real member.
        expect(screen.getAllByRole('combobox', {name: 'Workspace role'})).toHaveLength(1);
        expect(screen.getAllByRole('button', {name: /from workspace/})).toHaveLength(1);
        expect(screen.getByRole('button', {name: 'Remove member@example.com from workspace'})).toBeInTheDocument();
    });

    it('gives each inherited row its own key', () => {
        // `key={workspaceUser.id}` is null for every synthesized row, so two tenant admins collided on one key and
        // React reconciled them against each other.
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        hoisted.workspaceUsers = [firstInheritedAdmin, secondInheritedAdmin];

        renderDialog();

        expect(screen.getByText('owner@example.com')).toBeInTheDocument();
        expect(screen.getByText('second.owner@example.com')).toBeInTheDocument();

        const duplicateKeyWarnings = consoleErrorSpy.mock.calls.filter((call) =>
            call.some((argument) => typeof argument === 'string' && argument.includes('same key'))
        );

        expect(duplicateKeyWarnings).toHaveLength(0);
    });

    it('offers the member controls to a member holding the scope through a custom role', () => {
        // The dialog gated on useHasWorkspaceRole(workspaceId, ADMIN). A custom-role membership resolves to no built-in
        // role at all, so a "Team Lead" carrying WORKSPACE_MEMBER_MANAGE got a read-only table.
        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Workspace role'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /from workspace/})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /add user/i})).toBeInTheDocument();
    });

    it("invalidates the caller's own permission queries after a membership mutation", () => {
        // Removing yourself from the workspace leaves myWorkspaceScopes cached, so every control gated on it stays
        // enabled against an answer the server no longer gives.
        renderDialog();

        const onSuccess = hoisted.captureRemoveOnSuccess.mock.calls.at(-1)?.[0] as () => void;

        onSuccess();

        const invalidatedKeys = hoisted.invalidateQueries.mock.calls.map(
            (call) => (call[0] as {queryKey: string[]}).queryKey[0]
        );

        expect(invalidatedKeys).toContain('WorkspaceUsers');
        expect(invalidatedKeys).toContain('MyWorkspaceScopes');
    });

    it('removes a stored member', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: /from workspace/}));

        expect(hoisted.removeMutate).toHaveBeenCalledWith({userId: '10', workspaceId: '7'});
    });
});
