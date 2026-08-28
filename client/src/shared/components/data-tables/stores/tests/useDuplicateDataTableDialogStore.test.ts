import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useDuplicateDataTableDialogStore} from '../useDuplicateDataTableDialogStore';

describe('useDuplicateDataTableDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useDuplicateDataTableDialogStore.getState().clearTableToDuplicate();
        });
    });

    describe('initial state', () => {
        it('has tableIdToDuplicate as null', () => {
            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.tableIdToDuplicate).toBeNull();
        });

        it('has empty baseName', () => {
            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.baseName).toBe('');
        });

        it('has empty duplicateValue', () => {
            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.duplicateValue).toBe('');
        });
    });

    describe('setTableToDuplicate', () => {
        it('sets tableIdToDuplicate to the provided id', () => {
            act(() => {
                useDuplicateDataTableDialogStore.getState().setTableToDuplicate('table-123', 'MyTable');
            });

            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.tableIdToDuplicate).toBe('table-123');
        });

        it('sets baseName to the provided name', () => {
            act(() => {
                useDuplicateDataTableDialogStore.getState().setTableToDuplicate('table-123', 'MyTable');
            });

            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.baseName).toBe('MyTable');
        });

        it('sets duplicateValue to baseName with _copy suffix', () => {
            act(() => {
                useDuplicateDataTableDialogStore.getState().setTableToDuplicate('table-123', 'MyTable');
            });

            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.duplicateValue).toBe('MyTable_copy');
        });
    });

    describe('setDuplicateValue', () => {
        it('updates duplicateValue', () => {
            act(() => {
                useDuplicateDataTableDialogStore.getState().setTableToDuplicate('table-123', 'MyTable');
            });

            act(() => {
                useDuplicateDataTableDialogStore.getState().setDuplicateValue('NewName');
            });

            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.duplicateValue).toBe('NewName');
        });
    });

    describe('clearTableToDuplicate', () => {
        it('resets all state to initial values', () => {
            act(() => {
                useDuplicateDataTableDialogStore.getState().setTableToDuplicate('table-123', 'MyTable');
            });

            act(() => {
                useDuplicateDataTableDialogStore.getState().setDuplicateValue('ChangedName');
            });

            act(() => {
                useDuplicateDataTableDialogStore.getState().clearTableToDuplicate();
            });

            const state = useDuplicateDataTableDialogStore.getState();

            expect(state.tableIdToDuplicate).toBeNull();
            expect(state.baseName).toBe('');
            expect(state.duplicateValue).toBe('');
        });
    });
});
