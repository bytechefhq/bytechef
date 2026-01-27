import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useDeleteDataTableAlertDialogStore} from '../useDeleteDataTableAlertDialogStore';

describe('useDeleteDataTableAlertDialogStore', () => {
    beforeEach(() => {
        useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();
    });

    afterEach(() => {
        useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();
    });

    describe('initial state', () => {
        it('should have null tableIdToDelete initially', () => {
            const {tableIdToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
        });

        it('should have null tableNameToDelete initially', () => {
            const {tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableNameToDelete).toBeNull();
        });
    });

    describe('setTableToDelete', () => {
        it('should set tableIdToDelete and tableNameToDelete', () => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-123', 'Test Table');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBe('table-123');
            expect(tableNameToDelete).toBe('Test Table');
        });

        it('should replace existing values when called again', () => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-1', 'First Table');
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-2', 'Second Table');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBe('table-2');
            expect(tableNameToDelete).toBe('Second Table');
        });

        it('should handle empty table name', () => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-123', '');

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBe('table-123');
            expect(tableNameToDelete).toBe('');
        });

        it('should handle special characters in table name', () => {
            useDeleteDataTableAlertDialogStore
                .getState()
                .setTableToDelete('table-special', 'Table with "quotes" & symbols!');

            const {tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableNameToDelete).toBe('Table with "quotes" & symbols!');
        });
    });

    describe('clearTableToDelete', () => {
        it('should reset tableIdToDelete to null', () => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-123', 'Test Table');
            useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();

            const {tableIdToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
        });

        it('should reset tableNameToDelete to null', () => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-123', 'Test Table');
            useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();

            const {tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableNameToDelete).toBeNull();
        });

        it('should be idempotent when called multiple times', () => {
            useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();
            useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();

            const {tableIdToDelete, tableNameToDelete} = useDeleteDataTableAlertDialogStore.getState();

            expect(tableIdToDelete).toBeNull();
            expect(tableNameToDelete).toBeNull();
        });
    });
});
