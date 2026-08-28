import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useRenameDataTableDialogStore} from '../useRenameDataTableDialogStore';

describe('useRenameDataTableDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useRenameDataTableDialogStore.getState().clearTableToRename();
        });
    });

    describe('initial state', () => {
        it('has tableIdToRename as null', () => {
            const state = useRenameDataTableDialogStore.getState();

            expect(state.tableIdToRename).toBeNull();
        });

        it('has empty baseName', () => {
            const state = useRenameDataTableDialogStore.getState();

            expect(state.baseName).toBe('');
        });

        it('has empty renameValue', () => {
            const state = useRenameDataTableDialogStore.getState();

            expect(state.renameValue).toBe('');
        });
    });

    describe('setTableToRename', () => {
        it('sets tableIdToRename to the provided id', () => {
            act(() => {
                useRenameDataTableDialogStore.getState().setTableToRename('table-123', 'MyTable');
            });

            const state = useRenameDataTableDialogStore.getState();

            expect(state.tableIdToRename).toBe('table-123');
        });

        it('sets baseName to the provided name', () => {
            act(() => {
                useRenameDataTableDialogStore.getState().setTableToRename('table-123', 'MyTable');
            });

            const state = useRenameDataTableDialogStore.getState();

            expect(state.baseName).toBe('MyTable');
        });

        it('sets renameValue to the provided baseName', () => {
            act(() => {
                useRenameDataTableDialogStore.getState().setTableToRename('table-123', 'MyTable');
            });

            const state = useRenameDataTableDialogStore.getState();

            expect(state.renameValue).toBe('MyTable');
        });
    });

    describe('setRenameValue', () => {
        it('updates renameValue', () => {
            act(() => {
                useRenameDataTableDialogStore.getState().setTableToRename('table-123', 'MyTable');
            });

            act(() => {
                useRenameDataTableDialogStore.getState().setRenameValue('NewTableName');
            });

            const state = useRenameDataTableDialogStore.getState();

            expect(state.renameValue).toBe('NewTableName');
        });
    });

    describe('clearTableToRename', () => {
        it('resets all state to initial values', () => {
            act(() => {
                useRenameDataTableDialogStore.getState().setTableToRename('table-123', 'MyTable');
            });

            act(() => {
                useRenameDataTableDialogStore.getState().setRenameValue('ChangedName');
            });

            act(() => {
                useRenameDataTableDialogStore.getState().clearTableToRename();
            });

            const state = useRenameDataTableDialogStore.getState();

            expect(state.tableIdToRename).toBeNull();
            expect(state.baseName).toBe('');
            expect(state.renameValue).toBe('');
        });
    });
});
