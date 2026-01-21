import {beforeEach, describe, expect, it} from 'vitest';

import {useDeleteDataTableRowsDialogStore} from '../useDeleteDataTableRowsDialogStore';

describe('useDeleteDataTableRowsDialogStore', () => {
    beforeEach(() => {
        useDeleteDataTableRowsDialogStore.getState().clearDialog();
    });

    describe('initial state', () => {
        it('should have open set to false initially', () => {
            const state = useDeleteDataTableRowsDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });

    describe('setOpen', () => {
        it('should set open to true', () => {
            useDeleteDataTableRowsDialogStore.getState().setOpen();

            const state = useDeleteDataTableRowsDialogStore.getState();

            expect(state.open).toBe(true);
        });
    });

    describe('clearDialog', () => {
        it('should set open to false', () => {
            useDeleteDataTableRowsDialogStore.getState().setOpen();

            useDeleteDataTableRowsDialogStore.getState().clearDialog();

            const state = useDeleteDataTableRowsDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });
});
