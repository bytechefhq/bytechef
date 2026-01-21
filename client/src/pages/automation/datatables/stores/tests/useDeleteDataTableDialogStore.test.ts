import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useDeleteDataTableDialogStore} from '../useDeleteDataTableDialogStore';

describe('useDeleteDataTableDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useDeleteDataTableDialogStore.getState().clearTableIdToDelete();
        });
    });

    describe('initial state', () => {
        it('has tableIdToDelete as null', () => {
            const state = useDeleteDataTableDialogStore.getState();

            expect(state.tableIdToDelete).toBeNull();
        });
    });

    describe('setTableIdToDelete', () => {
        it('sets tableIdToDelete to the provided id', () => {
            act(() => {
                useDeleteDataTableDialogStore.getState().setTableIdToDelete('table-123');
            });

            const state = useDeleteDataTableDialogStore.getState();

            expect(state.tableIdToDelete).toBe('table-123');
        });

        it('updates tableIdToDelete when called multiple times', () => {
            act(() => {
                useDeleteDataTableDialogStore.getState().setTableIdToDelete('table-123');
            });

            act(() => {
                useDeleteDataTableDialogStore.getState().setTableIdToDelete('table-456');
            });

            const state = useDeleteDataTableDialogStore.getState();

            expect(state.tableIdToDelete).toBe('table-456');
        });
    });

    describe('clearTableIdToDelete', () => {
        it('resets tableIdToDelete to null', () => {
            act(() => {
                useDeleteDataTableDialogStore.getState().setTableIdToDelete('table-123');
            });

            act(() => {
                useDeleteDataTableDialogStore.getState().clearTableIdToDelete();
            });

            const state = useDeleteDataTableDialogStore.getState();

            expect(state.tableIdToDelete).toBeNull();
        });
    });
});
