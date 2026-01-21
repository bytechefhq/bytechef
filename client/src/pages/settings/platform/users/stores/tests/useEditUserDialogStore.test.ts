import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useEditUserDialogStore} from '../useEditUserDialogStore';

describe('useEditUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useEditUserDialogStore.getState().clearLoginToEdit();
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

    describe('setLoginToEdit', () => {
        it('sets loginToEdit to the provided login', () => {
            act(() => {
                useEditUserDialogStore.getState().setLoginToEdit('user@example.com');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.loginToEdit).toBe('user@example.com');
        });
    });

    describe('setEditRole', () => {
        it('sets editRole to the provided role', () => {
            act(() => {
                useEditUserDialogStore.getState().setLoginToEdit('user@example.com');
            });

            act(() => {
                useEditUserDialogStore.getState().setEditRole('ROLE_ADMIN');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.editRole).toBe('ROLE_ADMIN');
        });

        it('updates editRole when called multiple times', () => {
            act(() => {
                useEditUserDialogStore.getState().setEditRole('ROLE_ADMIN');
            });

            act(() => {
                useEditUserDialogStore.getState().setEditRole('ROLE_USER');
            });

            const state = useEditUserDialogStore.getState();

            expect(state.editRole).toBe('ROLE_USER');
        });
    });

    describe('clearLoginToEdit', () => {
        it('resets all state to initial values', () => {
            act(() => {
                useEditUserDialogStore.getState().setLoginToEdit('user@example.com');
            });

            act(() => {
                useEditUserDialogStore.getState().setEditRole('ROLE_ADMIN');
            });

            act(() => {
                useEditUserDialogStore.getState().clearLoginToEdit();
            });

            const state = useEditUserDialogStore.getState();

            expect(state.loginToEdit).toBeNull();
            expect(state.editRole).toBeNull();
        });
    });
});
