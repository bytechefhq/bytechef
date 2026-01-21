import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useInviteUserDialogStore} from '../useInviteUserDialogStore';

vi.mock('@/pages/settings/platform/users/util/password-utils', () => ({
    generatePassword: vi.fn(() => 'generated-password-123'),
}));

describe('useInviteUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useInviteUserDialogStore.getState().reset();
        });
    });

    describe('initial state', () => {
        it('has open as false', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('has empty inviteEmail', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.inviteEmail).toBe('');
        });

        it('has generated invitePassword', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.invitePassword).toBe('generated-password-123');
        });

        it('has inviteRole as null', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.inviteRole).toBeNull();
        });
    });

    describe('setOpen', () => {
        it('sets open to true', () => {
            act(() => {
                useInviteUserDialogStore.getState().setOpen();
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.open).toBe(true);
        });
    });

    describe('setInviteEmail', () => {
        it('sets inviteEmail to the provided value', () => {
            act(() => {
                useInviteUserDialogStore.getState().setInviteEmail('newuser@example.com');
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.inviteEmail).toBe('newuser@example.com');
        });
    });

    describe('setInviteRole', () => {
        it('sets inviteRole to the provided value', () => {
            act(() => {
                useInviteUserDialogStore.getState().setInviteRole('ROLE_ADMIN');
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.inviteRole).toBe('ROLE_ADMIN');
        });
    });

    describe('regeneratePassword', () => {
        it('regenerates the password', () => {
            act(() => {
                useInviteUserDialogStore.getState().regeneratePassword();
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.invitePassword).toBe('generated-password-123');
        });
    });

    describe('reset', () => {
        it('resets all state and closes dialog', () => {
            act(() => {
                useInviteUserDialogStore.getState().setOpen();
            });

            act(() => {
                useInviteUserDialogStore.getState().setInviteEmail('user@example.com');
            });

            act(() => {
                useInviteUserDialogStore.getState().setInviteRole('ROLE_ADMIN');
            });

            act(() => {
                useInviteUserDialogStore.getState().reset();
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.open).toBe(false);
            expect(state.inviteEmail).toBe('');
            expect(state.inviteRole).toBeNull();
            expect(state.invitePassword).toBe('generated-password-123');
        });
    });
});
