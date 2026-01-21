import {beforeEach, describe, expect, it} from 'vitest';

import {useAddDataTableColumnDialogStore} from '../useAddDataTableColumnDialogStore';

describe('useAddDataTableColumnDialogStore', () => {
    beforeEach(() => {
        useAddDataTableColumnDialogStore.getState().clearDialog();
    });

    describe('initial state', () => {
        it('should have open as false initially', () => {
            const state = useAddDataTableColumnDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });

    describe('setOpen', () => {
        it('should set open to true when called with true', () => {
            useAddDataTableColumnDialogStore.getState().setOpen(true);

            const state = useAddDataTableColumnDialogStore.getState();

            expect(state.open).toBe(true);
        });

        it('should set open to false when called with false', () => {
            useAddDataTableColumnDialogStore.getState().setOpen(true);
            useAddDataTableColumnDialogStore.getState().setOpen(false);

            const state = useAddDataTableColumnDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('should toggle open state correctly', () => {
            const store = useAddDataTableColumnDialogStore.getState();

            store.setOpen(true);

            expect(useAddDataTableColumnDialogStore.getState().open).toBe(true);

            store.setOpen(false);

            expect(useAddDataTableColumnDialogStore.getState().open).toBe(false);

            store.setOpen(true);

            expect(useAddDataTableColumnDialogStore.getState().open).toBe(true);
        });
    });

    describe('clearDialog', () => {
        it('should set open to false', () => {
            useAddDataTableColumnDialogStore.getState().setOpen(true);

            expect(useAddDataTableColumnDialogStore.getState().open).toBe(true);

            useAddDataTableColumnDialogStore.getState().clearDialog();

            const state = useAddDataTableColumnDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('should have no effect when already closed', () => {
            const initialState = useAddDataTableColumnDialogStore.getState();

            expect(initialState.open).toBe(false);

            useAddDataTableColumnDialogStore.getState().clearDialog();

            const state = useAddDataTableColumnDialogStore.getState();

            expect(state.open).toBe(false);
        });
    });
});
