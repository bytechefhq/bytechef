import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useInviteUserDialog from '../useInviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        inviteUserMutate: vi.fn(),
    };
});

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
        it('opens dialog and sets default role', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.open).toBe(true);
            expect(result.current.inviteRole).toBe('ROLE_ADMIN');
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
            });

            act(() => {
                result.current.handleClose();
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('update fields', () => {
        it('updates invite email', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.setInviteEmail('newuser@test.com');
            });

            expect(result.current.inviteEmail).toBe('newuser@test.com');
        });

        it('updates invite role', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.setInviteRole('ROLE_USER');
            });

            expect(result.current.inviteRole).toBe('ROLE_USER');
        });

        it('regenerates password', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            const initialPassword = result.current.invitePassword;

            act(() => {
                result.current.handleRegeneratePassword();
            });

            expect(result.current.invitePassword).toBe(initialPassword);
        });
    });

    describe('inviteDisabled', () => {
        it('is true when email is empty', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.inviteDisabled).toBe(true);
        });

        it('is false when all fields are valid', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
                result.current.setInviteEmail('test@test.com');
            });

            expect(result.current.inviteDisabled).toBe(false);
        });
    });

    describe('handle invite', () => {
        it('calls invite mutation with correct parameters', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
                result.current.setInviteEmail('newuser@test.com');
            });

            act(() => {
                result.current.handleInvite();
            });

            expect(hoisted.inviteUserMutate).toHaveBeenCalledWith({
                email: 'newuser@test.com',
                password: 'GeneratedPass1',
                role: 'ROLE_ADMIN',
            });
        });

        it('resets invite state after successful invite', () => {
            const {result} = renderHook(() => useInviteUserDialog());

            act(() => {
                result.current.handleOpen();
                result.current.setInviteEmail('newuser@test.com');
            });

            act(() => {
                result.current.handleInvite();
            });

            expect(result.current.open).toBe(false);
            expect(result.current.inviteEmail).toBe('');
            expect(result.current.inviteRole).toBeNull();
        });
    });
});
