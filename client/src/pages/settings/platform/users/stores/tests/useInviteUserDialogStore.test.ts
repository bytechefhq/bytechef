import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useInviteUserDialogStore} from '../useInviteUserDialogStore';

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
        });
    });
});
