import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useInviteUserDialog from '../useInviteUserDialog';

interface WorkspaceAssignmentI {
    roleName: string;
    workspaceId: string;
}

const hoisted = vi.hoisted(() => {
    return {
        inviteUserMutate: vi.fn(),
        storeState: {
            inviteEmail: '',
            inviteRole: null as string | null,
            inviteWorkspaces: [] as WorkspaceAssignmentI[],
            open: false,
            reset: vi.fn(),
            setInviteEmail: vi.fn(),
            setInviteRole: vi.fn(),
            setInviteWorkspaceRole: vi.fn(),
            setOpen: vi.fn(),
            toggleInviteWorkspace: vi.fn(),
        },
    };
});

vi.mock('@/pages/settings/platform/users/stores/useInviteUserDialogStore', () => ({
    useInviteUserDialogStore: vi.fn(() => {
        return {
            inviteEmail: hoisted.storeState.inviteEmail,
            inviteRole: hoisted.storeState.inviteRole,
            inviteWorkspaces: hoisted.storeState.inviteWorkspaces,
            open: hoisted.storeState.open,
            reset: () => {
                hoisted.storeState.inviteEmail = '';
                hoisted.storeState.inviteRole = null;
                hoisted.storeState.inviteWorkspaces = [];
                hoisted.storeState.open = false;
                hoisted.storeState.reset();
            },
            setInviteEmail: (email: string) => {
                hoisted.storeState.inviteEmail = email;
                hoisted.storeState.setInviteEmail(email);
            },
            setInviteRole: (role: string) => {
                hoisted.storeState.inviteRole = role;
                hoisted.storeState.setInviteRole(role);
            },
            setInviteWorkspaceRole: (workspaceId: string, roleName: string) => {
                hoisted.storeState.setInviteWorkspaceRole(workspaceId, roleName);
            },
            setOpen: () => {
                hoisted.storeState.open = true;
                hoisted.storeState.setOpen();
            },
            toggleInviteWorkspace: (workspaceId: string, roleName: string) => {
                hoisted.storeState.toggleInviteWorkspace(workspaceId, roleName);
            },
        };
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAuthoritiesQuery: vi.fn(() => ({
        data: {authorities: ['ROLE_ADMIN', 'ROLE_USER']},
    })),
    useInviteUserMutation: vi.fn((options: {onSuccess: () => void}) => ({
        mutate: (vars: unknown) => {
            hoisted.inviteUserMutate(vars);
            options.onSuccess();
        },
    })),
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: vi.fn(() => ({
        data: [
            {id: 1, name: 'Engineering'},
            {id: 2, name: 'Marketing'},
        ],
    })),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(() => ({account: {id: 7}})),
}));

vi.mock('zustand/react/shallow', () => ({
    useShallow: vi.fn((selector: unknown) => selector),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: vi.fn(),
    })),
}));

describe('useInviteUserDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.inviteEmail = '';
        hoisted.storeState.inviteRole = null;
        hoisted.storeState.inviteWorkspaces = [];
        hoisted.storeState.open = false;
    });

    describe('initial state', () => {
        it('returns authorities from query', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            expect(result.current.authorities).toEqual(['ROLE_ADMIN', 'ROLE_USER']);
        });

        it('returns initial invite state as closed', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            expect(result.current.open).toBe(false);
            expect(result.current.inviteEmail).toBe('');
            expect(result.current.inviteRole).toBeNull();
            expect(result.current.inviteWorkspaces).toEqual([]);
        });

        it('returns inviteDisabled as true initially', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            expect(result.current.inviteDisabled).toBe(true);
        });

        it('maps the workspaces the admin can assign', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            expect(result.current.workspaces).toEqual([
                {id: '1', name: 'Engineering'},
                {id: '2', name: 'Marketing'},
            ]);
        });
    });

    describe('open dialog', () => {
        it('opens dialog', () => {
            const {rerender, result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
            });

            rerender();

            expect(result.current.open).toBe(true);
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            hoisted.storeState.open = true;
            const {rerender, result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleClose();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('update fields', () => {
        it('updates invite email', () => {
            const {rerender, result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleEmailChange('newuser@test.com');
            });

            rerender();

            expect(result.current.inviteEmail).toBe('newuser@test.com');
        });

        it('updates invite role', () => {
            const {rerender, result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleRoleChange('ROLE_USER');
            });

            rerender();

            expect(result.current.inviteRole).toBe('ROLE_USER');
        });

        it('toggles a workspace at the default role', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleWorkspaceToggle('1');
            });

            expect(hoisted.storeState.toggleInviteWorkspace).toHaveBeenCalledWith('1', 'EDITOR');
        });

        it('changes a selected workspace role', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleWorkspaceRoleChange('1', 'ADMIN');
            });

            expect(hoisted.storeState.setInviteWorkspaceRole).toHaveBeenCalledWith('1', 'ADMIN');
        });
    });

    describe('handle invite', () => {
        it('calls invite mutation without a password', () => {
            hoisted.storeState.open = true;
            hoisted.storeState.inviteEmail = 'newuser@test.com';
            hoisted.storeState.inviteRole = 'ROLE_ADMIN';

            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleInvite();
            });

            expect(hoisted.inviteUserMutate).toHaveBeenCalledWith({
                email: 'newuser@test.com',
                role: 'ROLE_ADMIN',
                workspaces: [],
            });
        });

        it('sends the selected workspace assignments', () => {
            hoisted.storeState.open = true;
            hoisted.storeState.inviteEmail = 'newuser@test.com';
            hoisted.storeState.inviteRole = 'ROLE_ADMIN';
            hoisted.storeState.inviteWorkspaces = [{roleName: 'EDITOR', workspaceId: '1'}];

            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleInvite();
            });

            expect(hoisted.inviteUserMutate).toHaveBeenCalledWith({
                email: 'newuser@test.com',
                role: 'ROLE_ADMIN',
                workspaces: [{roleName: 'EDITOR', workspaceId: '1'}],
            });
        });

        it('closes dialog after successful invite', () => {
            hoisted.storeState.open = true;
            hoisted.storeState.inviteEmail = 'newuser@test.com';
            hoisted.storeState.inviteRole = 'ROLE_ADMIN';

            const {rerender, result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleInvite();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });
});
