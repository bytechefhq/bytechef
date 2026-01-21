import {beforeEach, describe, expect, it} from 'vitest';

import {useSelectedRowsStore} from '../useSelectedRowsStore';

describe('useSelectedRowsStore', () => {
    beforeEach(() => {
        useSelectedRowsStore.getState().clearSelectedRows();
    });

    describe('initial state', () => {
        it('should have selectedRows as empty set initially', () => {
            const state = useSelectedRowsStore.getState();

            expect(state.selectedRows.size).toBe(0);
        });
    });

    describe('setSelectedRows', () => {
        it('should set selectedRows to the provided set', () => {
            const rows = new Set<string>(['1', '2', '3']);

            useSelectedRowsStore.getState().setSelectedRows(rows);

            const state = useSelectedRowsStore.getState();

            expect(state.selectedRows.size).toBe(3);
            expect(state.selectedRows.has('1')).toBe(true);
            expect(state.selectedRows.has('2')).toBe(true);
            expect(state.selectedRows.has('3')).toBe(true);
        });

        it('should replace existing selectedRows', () => {
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1', '2']));
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['3', '4', '5']));

            const state = useSelectedRowsStore.getState();

            expect(state.selectedRows.size).toBe(3);
            expect(state.selectedRows.has('1')).toBe(false);
            expect(state.selectedRows.has('3')).toBe(true);
        });
    });

    describe('clearSelectedRows', () => {
        it('should clear all selected rows', () => {
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1', '2', '3']));

            expect(useSelectedRowsStore.getState().selectedRows.size).toBe(3);

            useSelectedRowsStore.getState().clearSelectedRows();

            const state = useSelectedRowsStore.getState();

            expect(state.selectedRows.size).toBe(0);
        });
    });
});
