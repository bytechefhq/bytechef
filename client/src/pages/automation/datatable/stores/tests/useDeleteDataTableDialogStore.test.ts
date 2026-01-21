import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useDeleteDataTableDialogStore} from '../useDeleteDataTableDialogStore';

describe('useDeleteDataTableDialogStore', () => {
    beforeEach(() => {
        useDeleteDataTableDialogStore.getState().clearTableToDelete();
    });

    afterEach(() => {
        useDeleteDataTableDialogStore.getState().clearTableToDelete();
    });

    describe('initial state', () => {
        it('should have null tableIdToDelete initially', () => {
            const {tableIdToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
        });

        it('should have null tableNameToDelete initially', () => {
            const {tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableNameToDelete).toBeNull();
        });
    });

    describe('setTableToDelete', () => {
        it('should set tableIdToDelete and tableNameToDelete', () => {
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-123', 'Test Table');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBe('table-123');
            expect(tableNameToDelete).toBe('Test Table');
        });

        it('should replace existing values when called again', () => {
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-1', 'First Table');
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-2', 'Second Table');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBe('table-2');
            expect(tableNameToDelete).toBe('Second Table');
        });

        it('should handle empty table name', () => {
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-123', '');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBe('table-123');
            expect(tableNameToDelete).toBe('');
        });

        it('should handle special characters in table name', () => {
            useDeleteDataTableDialogStore
                .getState()
                .setTableToDelete('table-special', 'Table with "quotes" & symbols!');

            const {tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableNameToDelete).toBe('Table with "quotes" & symbols!');
        });
    });

    describe('clearTableToDelete', () => {
        it('should reset tableIdToDelete to null', () => {
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-123', 'Test Table');
            useDeleteDataTableDialogStore.getState().clearTableToDelete();

            const {tableIdToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
        });

        it('should reset tableNameToDelete to null', () => {
            useDeleteDataTableDialogStore.getState().setTableToDelete('table-123', 'Test Table');
            useDeleteDataTableDialogStore.getState().clearTableToDelete();

            const {tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableNameToDelete).toBeNull();
        });

        it('should be idempotent when called multiple times', () => {
            useDeleteDataTableDialogStore.getState().clearTableToDelete();
            useDeleteDataTableDialogStore.getState().clearTableToDelete();

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
            expect(tableNameToDelete).toBeNull();
        });
    });
});
