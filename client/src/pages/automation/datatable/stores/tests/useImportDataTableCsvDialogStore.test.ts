import {beforeEach, describe, expect, it} from 'vitest';

import {useImportDataTableCsvDialogStore} from '../useImportDataTableCsvDialogStore';

describe('useImportDataTableCsvDialogStore', () => {
    beforeEach(() => {
        useImportDataTableCsvDialogStore.getState().clearDialog();
    });

    describe('initial state', () => {
        it('should have open as false initially', () => {
            const state = useImportDataTableCsvDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });

    describe('setOpen', () => {
        it('should set open to true when called with true', () => {
            useImportDataTableCsvDialogStore.getState().setOpen(true);

            const state = useImportDataTableCsvDialogStore.getState();

            expect(state.open).toBe(true);
        });

        it('should set open to false when called with false', () => {
            useImportDataTableCsvDialogStore.getState().setOpen(true);
            useImportDataTableCsvDialogStore.getState().setOpen(false);

            const state = useImportDataTableCsvDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('should toggle open state correctly', () => {
            const store = useImportDataTableCsvDialogStore.getState();

            store.setOpen(true);

            expect(useImportDataTableCsvDialogStore.getState().open).toBe(true);

            store.setOpen(false);

            expect(useImportDataTableCsvDialogStore.getState().open).toBe(false);

            store.setOpen(true);

            expect(useImportDataTableCsvDialogStore.getState().open).toBe(true);
        });
    });

    describe('clearDialog', () => {
        it('should set open to false', () => {
            useImportDataTableCsvDialogStore.getState().setOpen(true);

            expect(useImportDataTableCsvDialogStore.getState().open).toBe(true);

            useImportDataTableCsvDialogStore.getState().clearDialog();

            const state = useImportDataTableCsvDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('should have no effect when already closed', () => {
            const initialState = useImportDataTableCsvDialogStore.getState();

            expect(initialState.open).toBe(false);

            useImportDataTableCsvDialogStore.getState().clearDialog();

            const state = useImportDataTableCsvDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });
});
