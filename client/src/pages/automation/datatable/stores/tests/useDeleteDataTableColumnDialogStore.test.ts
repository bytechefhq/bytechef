import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useDeleteDataTableColumnDialogStore} from '../useDeleteDataTableColumnDialogStore';

describe('useDeleteDataTableColumnDialogStore', () => {
    beforeEach(() => {
        useDeleteDataTableColumnDialogStore.getState().clearDialog();
    });

    afterEach(() => {
        useDeleteDataTableColumnDialogStore.getState().clearDialog();
    });

    describe('initial state', () => {
        it('should have null columnId initially', () => {
            const {columnId} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBeNull();
        });

        it('should have null columnName initially', () => {
            const {columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnName).toBeNull();
        });
    });

    describe('setColumnToDelete', () => {
        it('should set columnId and columnName', () => {
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-123', 'Test Column');

            const {columnId, columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBe('col-123');
            expect(columnName).toBe('Test Column');
        });

        it('should replace existing values when called again', () => {
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-1', 'First Column');
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-2', 'Second Column');

            const {columnId, columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBe('col-2');
            expect(columnName).toBe('Second Column');
        });

        it('should handle empty column name', () => {
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-123', '');

            const {columnId, columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBe('col-123');
            expect(columnName).toBe('');
        });

        it('should handle special characters in column name', () => {
            useDeleteDataTableColumnDialogStore
                .getState()
                .setColumnToDelete('col-special', 'Column with "quotes" & symbols!');

            const {columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnName).toBe('Column with "quotes" & symbols!');
        });
    });

    describe('clearDialog', () => {
        it('should reset columnId to null', () => {
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-123', 'Test Column');
            useDeleteDataTableColumnDialogStore.getState().clearDialog();

            const {columnId} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBeNull();
        });

        it('should reset columnName to null', () => {
            useDeleteDataTableColumnDialogStore.getState().setColumnToDelete('col-123', 'Test Column');
            useDeleteDataTableColumnDialogStore.getState().clearDialog();

            const {columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnName).toBeNull();
        });

        it('should be idempotent when called multiple times', () => {
            useDeleteDataTableColumnDialogStore.getState().clearDialog();
            useDeleteDataTableColumnDialogStore.getState().clearDialog();

            const {columnId, columnName} = useDeleteDataTableColumnDialogStore.getState();

            expect(columnId).toBeNull();
            expect(columnName).toBeNull();
        });
    });
});
