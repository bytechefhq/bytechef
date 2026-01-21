import {beforeEach, describe, expect, it} from 'vitest';

import {useRenameDataTableColumnDialogStore} from '../useRenameDataTableColumnDialogStore';

describe('useRenameDataTableColumnDialogStore', () => {
    beforeEach(() => {
        useRenameDataTableColumnDialogStore.getState().clearDialog();
    });

    describe('initial state', () => {
        it('should have columnId as null initially', () => {
            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.columnId).toBeNull();
        });

        it('should have currentName as empty string initially', () => {
            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.currentName).toBe('');
        });

        it('should have renameValue as empty string initially', () => {
            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.renameValue).toBe('');
        });
    });

    describe('setColumnToRename', () => {
        it('should set columnId to the provided value', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.columnId).toBe('col-123');
        });

        it('should set currentName to the provided value', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.currentName).toBe('originalName');
        });

        it('should set renameValue to the same as currentName', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.renameValue).toBe('originalName');
        });

        it('should replace existing values when called again', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'firstName');
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-456', 'secondName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.columnId).toBe('col-456');
            expect(state.currentName).toBe('secondName');
            expect(state.renameValue).toBe('secondName');
        });
    });

    describe('setRenameValue', () => {
        it('should update renameValue to the provided value', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().setRenameValue('newName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.renameValue).toBe('newName');
        });

        it('should not affect currentName when changing renameValue', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().setRenameValue('newName');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.currentName).toBe('originalName');
        });

        it('should allow setting empty string as renameValue', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().setRenameValue('');

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.renameValue).toBe('');
        });
    });

    describe('clearDialog', () => {
        it('should reset columnId to null', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().clearDialog();

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.columnId).toBeNull();
        });

        it('should reset currentName to empty string', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().clearDialog();

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.currentName).toBe('');
        });

        it('should reset renameValue to empty string', () => {
            useRenameDataTableColumnDialogStore.getState().setColumnToRename('col-123', 'originalName');
            useRenameDataTableColumnDialogStore.getState().setRenameValue('newName');
            useRenameDataTableColumnDialogStore.getState().clearDialog();

            const state = useRenameDataTableColumnDialogStore.getState();

            expect(state.renameValue).toBe('');
        });
    });
});
