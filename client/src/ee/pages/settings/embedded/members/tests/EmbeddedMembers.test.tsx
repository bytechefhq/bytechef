import {act, mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedMembers from '../EmbeddedMembers';

const hoisted = vi.hoisted(() => ({
    addEmbeddedUserByEmailMutate: vi.fn(),
    captureAddOnSuccess: vi.fn(),
    captureRemoveEnvironmentRoleOnSuccess: vi.fn(),
    captureSetEnvironmentRoleOnSuccess: vi.fn(),
    embeddedUsers: [] as unknown[],
    invalidateQueries: vi.fn(),
    memberViewGranted: true,
    removeEmbeddedUserMutate: vi.fn(),
    removeEnvironmentRoleMutate: vi.fn(),
    scopes: ['EMBEDDED_MEMBER_MANAGE', 'EMBEDDED_VIEW'] as string[],
    scopesByEnvironment: {} as Record<string, string[]>,
    setEnvironmentRoleMutate: vi.fn(),
    updateEmbeddedUserRoleMutate: vi.fn(),
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: () => hoisted.memberViewGranted,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    EnvironmentEnum: {Development: 'DEVELOPMENT', Production: 'PRODUCTION', Staging: 'STAGING'},
    WorkspaceRole: {Admin: 'ADMIN', Editor: 'EDITOR', Viewer: 'VIEWER'},
    useAddEmbeddedUserByEmailMutation: vi.fn((options: {onSuccess: () => void}) => {
        hoisted.captureAddOnSuccess(options.onSuccess);

        return {mutate: hoisted.addEmbeddedUserByEmailMutate};
    }),
    useEmbeddedUsersQuery: vi.fn(() => ({data: {embeddedUsers: hoisted.embeddedUsers}, isLoading: false})),
    useMyEmbeddedScopesQuery: vi.fn((variables: {environment?: string}) => ({
        data: {myEmbeddedScopes: hoisted.scopesByEnvironment[variables.environment ?? ''] ?? hoisted.scopes},
        isLoading: false,
    })),
    useRemoveEmbeddedUserEnvironmentRoleMutation: vi.fn((options: {onSuccess: () => void}) => {
        hoisted.captureRemoveEnvironmentRoleOnSuccess(options.onSuccess);

        return {mutate: hoisted.removeEnvironmentRoleMutate};
    }),
    useRemoveEmbeddedUserMutation: vi.fn(() => ({mutate: hoisted.removeEmbeddedUserMutate})),
    useSetEmbeddedUserEnvironmentRoleMutation: vi.fn((options: {onSuccess: () => void}) => {
        hoisted.captureSetEnvironmentRoleOnSuccess(options.onSuccess);

        return {mutate: hoisted.setEnvironmentRoleMutate};
    }),
    useUpdateEmbeddedUserRoleMutation: vi.fn(() => ({mutate: hoisted.updateEmbeddedUserRoleMutate})),
}));

vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: hoisted.invalidateQueries}) as never),
}));

const user = {email: 'member@example.com', firstName: 'Mem', lastName: 'Ber'};

const member = {
    customRoleId: null,
    embeddedRole: 'EDITOR',
    environment: null,
    id: '1',
    inherited: false,
    user,
    userId: '10',
};

const developmentRole = {...member, embeddedRole: 'EDITOR', environment: 'DEVELOPMENT', id: '2'};

const inheritedAdmin = {
    customRoleId: null,
    embeddedRole: 'ADMIN',
    environment: null,
    id: null,
    inherited: true,
    user: {email: 'owner@example.com', firstName: 'Ten', lastName: 'Ant'},
    userId: '99',
};

describe('EmbeddedMembers', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();

        hoisted.embeddedUsers = [member];
        hoisted.memberViewGranted = true;
        hoisted.scopes = ['EMBEDDED_MEMBER_MANAGE', 'EMBEDDED_VIEW'];
        hoisted.scopesByEnvironment = {};
    });

    it('testAViewerSeesMembersButNoManageControls', () => {
        hoisted.scopes = ['EMBEDDED_VIEW'];

        render(<EmbeddedMembers />);

        expect(screen.getByText('member@example.com')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add member'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Remove /})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /per-environment roles/i})).not.toBeInTheDocument();
        expect(screen.getByRole('combobox')).toBeDisabled();
    });

    it('testAnAdminAddsAMemberByEmailAndTheListRefreshes', async () => {
        render(<EmbeddedMembers />);

        await userEvent.click(screen.getByRole('button', {name: 'Add member'}));
        await userEvent.type(screen.getByPlaceholderText('colleague@example.com'), 'new@example.com');
        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(hoisted.addEmbeddedUserByEmailMutate).toHaveBeenCalledWith({email: 'new@example.com', role: 'EDITOR'});

        const onSuccess = hoisted.captureAddOnSuccess.mock.calls.at(-1)?.[0] as () => void;

        act(() => onSuccess());

        const invalidatedKeys = hoisted.invalidateQueries.mock.calls.map(
            (call) => (call[0] as {queryKey: string[]}).queryKey[0]
        );

        expect(invalidatedKeys).toContain('EmbeddedUsers');
        expect(invalidatedKeys).toContain('MyEmbeddedScopes');
    });

    it('testSettingAProductionRoleTurnsTheMemberPerEnvironment', async () => {
        render(<EmbeddedMembers />);

        await userEvent.click(screen.getByRole('button', {name: /per-environment roles/i}));

        expect(
            screen.getByText(
                'They keep this role in every other environment. Change or remove each environment separately.'
            )
        ).toBeInTheDocument();
        expect(screen.queryByText(/lose access to every other environment/)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Environment to grant first'}));
        await userEvent.click(screen.getByRole('option', {name: 'Production'}));

        expect(hoisted.setEnvironmentRoleMutate).toHaveBeenCalledWith({
            environment: 'PRODUCTION',
            role: 'EDITOR',
            userId: '10',
        });
    });

    it('testTenantAdminsAreListedAsInheritedAndNotEditable', () => {
        hoisted.embeddedUsers = [member, inheritedAdmin];

        render(<EmbeddedMembers />);

        expect(screen.getByText('owner@example.com')).toBeInTheDocument();
        expect(screen.getByText(/inherited from tenant admin/)).toBeInTheDocument();
        expect(screen.getAllByRole('combobox')).toHaveLength(1);
        expect(screen.getAllByRole('button', {name: /^Remove /})).toHaveLength(1);
        expect(screen.getByRole('button', {name: 'Remove member@example.com from embedded'})).toBeInTheDocument();
    });

    it('testScopesAreInvalidatedAfterARoleChange', async () => {
        hoisted.embeddedUsers = [developmentRole];

        render(<EmbeddedMembers />);

        await userEvent.click(await screen.findByRole('combobox', {name: /development role/i}));
        await userEvent.click(screen.getByRole('option', {name: 'Viewer'}));

        expect(hoisted.setEnvironmentRoleMutate).toHaveBeenCalledWith({
            environment: 'DEVELOPMENT',
            role: 'VIEWER',
            userId: '10',
        });

        const onSuccess = hoisted.captureSetEnvironmentRoleOnSuccess.mock.calls.at(-1)?.[0] as () => void;

        act(() => onSuccess());

        const invalidatedKeys = hoisted.invalidateQueries.mock.calls.map(
            (call) => (call[0] as {queryKey: string[]}).queryKey[0]
        );

        expect(invalidatedKeys).toContain('MyEmbeddedScopes');
        expect(invalidatedKeys).toContain('EmbeddedUsers');
    });

    it('refreshes scopes and members after an environment role is removed', async () => {
        hoisted.embeddedUsers = [developmentRole, {...developmentRole, environment: 'PRODUCTION', id: '3'}];

        render(<EmbeddedMembers />);

        await userEvent.click(await screen.findByRole('button', {name: /remove development role/i}));
        await userEvent.click(screen.getByRole('button', {name: 'Remove'}));

        expect(hoisted.removeEnvironmentRoleMutate).toHaveBeenCalledWith({environment: 'DEVELOPMENT', userId: '10'});

        const onSuccess = hoisted.captureRemoveEnvironmentRoleOnSuccess.mock.calls.at(-1)?.[0] as () => void;

        act(() => onSuccess());

        const invalidatedKeys = hoisted.invalidateQueries.mock.calls.map(
            (call) => (call[0] as {queryKey: string[]}).queryKey[0]
        );

        expect(invalidatedKeys).toContain('MyEmbeddedScopes');
        expect(invalidatedKeys).toContain('EmbeddedUsers');
    });

    it('removes a stored member once confirmed', async () => {
        render(<EmbeddedMembers />);

        await userEvent.click(screen.getByRole('button', {name: 'Remove member@example.com from embedded'}));

        expect(hoisted.removeEmbeddedUserMutate).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Remove'}));

        expect(hoisted.removeEmbeddedUserMutate).toHaveBeenCalledWith({userId: '10'});
    });
});
