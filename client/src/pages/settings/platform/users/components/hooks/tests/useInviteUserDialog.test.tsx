import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useInviteUserDialog from '../useInviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        inviteUserMutate: vi.fn(),
        storeState: {
            handleClose: vi.fn(),
            handleEmailChange: vi.fn(),
            handleOpen: vi.fn(),
            handleRegeneratePassword: vi.fn(),
            handleRoleChange: vi.fn(),
            inviteEmail: '',
            invitePassword: 'GeneratedPass1',
            inviteRole: null as string | null,
            open: false,
        },
    };
});

vi.mock('@/pages/settings/platform/users/stores/useInviteUserDialogStore', () => ({
    useInviteUserDialogStore: vi.fn(() => {
        return {
            handleClose: () => {
                hoisted.storeState.inviteEmail = '';
                hoisted.storeState.invitePassword = 'GeneratedPass1';
                hoisted.storeState.inviteRole = null;
                hoisted.storeState.open = false;
                hoisted.storeState.handleClose();
            },
            handleEmailChange: (email: string) => {
                hoisted.storeState.inviteEmail = email;
                hoisted.storeState.handleEmailChange(email);
            },
            handleOpen: () => {
                hoisted.storeState.open = true;
                hoisted.storeState.handleOpen();
            },
            handleRegeneratePassword: () => {
                hoisted.storeState.handleRegeneratePassword();
            },
            handleRoleChange: (role: string) => {
                hoisted.storeState.inviteRole = role;
                hoisted.storeState.handleRoleChange(role);
            },
            inviteEmail: hoisted.storeState.inviteEmail,
            invitePassword: hoisted.storeState.invitePassword,
            inviteRole: hoisted.storeState.inviteRole,
            open: hoisted.storeState.open,
        };
    }),
}));

vi.mock('@/pages/settings/platform/users/util/password-utils', () => ({
    generatePassword: vi.fn(() => 'GeneratedPass1'),
    isValidPassword: vi.fn((password: string) => password.length >= 8 && /[A-Z]/.test(password) && /\d/.test(password)),
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

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: vi.fn(),
    })),
}));

describe('useInviteUserDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.inviteEmail = '';
        hoisted.storeState.invitePassword = 'GeneratedPass1';
        hoisted.storeState.inviteRole = null;
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
            expect(result.current.invitePassword).toBe('GeneratedPass1');
            expect(result.current.inviteRole).toBeNull();
        });

        it('returns inviteDisabled as true initially', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            expect(result.current.inviteDisabled).toBe(true);
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
    });

    describe('handle invite', () => {
        it('calls invite mutation with correct parameters', () => {
            hoisted.storeState.open = true;
            hoisted.storeState.inviteEmail = 'newuser@test.com';
            hoisted.storeState.inviteRole = 'ROLE_ADMIN';
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleInvite();
            });

            expect(hoisted.inviteUserMutate).toHaveBeenCalledWith({
                email: 'newuser@test.com',
                password: 'GeneratedPass1',
                role: 'ROLE_ADMIN',
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
