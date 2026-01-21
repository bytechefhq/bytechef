import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useEditUserDialogStore} from '../useEditUserDialogStore';

describe('useEditUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useEditUserDialogStore.getState().handleClose();
        });
    });

    describe('initial state', () => {
        it('has loginToEdit as null', () => {
            const state = useEditUserDialogStore.getState();

            expect(state.loginToEdit).toBeNull();
        });

        it('has editRole as null', () => {
            const state = useEditUserDialogStore.getState();

            expect(state.editRole).toBeNull();
        });
    });

    describe('handleOpen', () => {
        it('sets loginToEdit to the provided login', () => {
            act(() => {
                useEditUserDialogStore.getState().handleOpen('user@example.com');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.loginToEdit).toBe('user@example.com');
        });
    });

    describe('handleRoleChange', () => {
        it('sets editRole to the provided role', () => {
            act(() => {
                useEditUserDialogStore.getState().handleOpen('user@example.com');
            });

            act(() => {
                useEditUserDialogStore.getState().handleRoleChange('ROLE_ADMIN');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.editRole).toBe('ROLE_ADMIN');
        });

        it('updates editRole when called multiple times', () => {
            act(() => {
                useEditUserDialogStore.getState().handleRoleChange('ROLE_ADMIN');
            });

            act(() => {
                useEditUserDialogStore.getState().handleRoleChange('ROLE_USER');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.editRole).toBe('ROLE_USER');
        });
    });

    describe('handleClose', () => {
        it('resets all state to initial values', () => {
            act(() => {
                useEditUserDialogStore.getState().handleOpen('user@example.com');
            });

            act(() => {
                useEditUserDialogStore.getState().handleRoleChange('ROLE_ADMIN');
            });

            act(() => {
                useEditUserDialogStore.getState().handleClose();
            });

            const state = useEditUserDialogStore.getState();

            expect(state.loginToEdit).toBeNull();
            expect(state.editRole).toBeNull();
        });
    });
});
