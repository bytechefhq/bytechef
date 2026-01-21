import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useDeleteUserDialogStore} from '../useDeleteUserDialogStore';

describe('useDeleteUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useDeleteUserDialogStore.getState().clearLoginToDelete();
        });
    });

    describe('initial state', () => {
        it('has loginToDelete as null', () => {
            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });
    });

    describe('setLoginToDelete', () => {
        it('sets loginToDelete to the provided login', () => {
            act(() => {
                useDeleteUserDialogStore.getState().setLoginToDelete('user@example.com');
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBe('user@example.com');
        });

        it('allows null value for login', () => {
            act(() => {
                useDeleteUserDialogStore.getState().setLoginToDelete(null);
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });

        it('updates loginToDelete when called multiple times', () => {
            act(() => {
                useDeleteUserDialogStore.getState().setLoginToDelete('user1@example.com');
            });

            act(() => {
                useDeleteUserDialogStore.getState().setLoginToDelete('user2@example.com');
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBe('user2@example.com');
        });
    });

    describe('clearLoginToDelete', () => {
        it('resets loginToDelete to null', () => {
            act(() => {
                useDeleteUserDialogStore.getState().setLoginToDelete('user@example.com');
            });

            act(() => {
                useDeleteUserDialogStore.getState().clearLoginToDelete();
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });
    });
});
