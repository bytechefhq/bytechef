import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useDeleteUserDialogStore} from '../useDeleteUserDialogStore';

describe('useDeleteUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useDeleteUserDialogStore.getState().handleClose();
        });
    });

    describe('initial state', () => {
        it('has loginToDelete as null', () => {
            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });
    });

    describe('handleOpen', () => {
        it('sets loginToDelete to the provided login', () => {
            act(() => {
                useDeleteUserDialogStore.getState().handleOpen('user@example.com');
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBe('user@example.com');
        });

        it('allows null value for login', () => {
            act(() => {
                useDeleteUserDialogStore.getState().handleOpen(null);
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });

        it('updates loginToDelete when called multiple times', () => {
            act(() => {
                useDeleteUserDialogStore.getState().handleOpen('user1@example.com');
            });

            act(() => {
                useDeleteUserDialogStore.getState().handleOpen('user2@example.com');
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBe('user2@example.com');
        });
    });

    describe('handleClose', () => {
        it('resets loginToDelete to null', () => {
            act(() => {
                useDeleteUserDialogStore.getState().handleOpen('user@example.com');
            });

            act(() => {
                useDeleteUserDialogStore.getState().handleClose();
            });

            const state = useDeleteUserDialogStore.getState();

            expect(state.loginToDelete).toBeNull();
        });
    });
});
