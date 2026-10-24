import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataSyncActions from './useDataSyncActions';

const mockDeleteDataSyncMutate = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteDataSyncMutation: (options: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string}) => {
            mockDeleteDataSyncMutate(variables);
            options.onSuccess?.();
        },
    }),
}));

const wrapper = ({children}: {children: React.ReactNode}) =>
    createElement(QueryClientProvider, {client: new QueryClient()}, children);

describe('useDataSyncActions', () => {
    beforeEach(() => {
        mockDeleteDataSyncMutate.mockReset();
    });

    it('opens the delete confirmation without deleting', () => {
        const {result} = renderHook(() => useDataSyncActions({dataSync: {id: '1'}}), {wrapper});

        act(() => result.current.handleDeleteClick());

        expect(result.current.showDeleteConfirmDialog).toBe(true);
        expect(mockDeleteDataSyncMutate).not.toHaveBeenCalled();
    });

    it('deletes and closes the confirmation once confirmed', () => {
        const {result} = renderHook(() => useDataSyncActions({dataSync: {id: '1'}}), {wrapper});

        act(() => result.current.handleDeleteClick());
        act(() => result.current.deleteDataSync());

        expect(mockDeleteDataSyncMutate).toHaveBeenCalledWith({id: '1'});
        expect(result.current.showDeleteConfirmDialog).toBe(false);
    });

    it('calls onDeleted after a successful delete', () => {
        const onDeleted = vi.fn();

        const {result} = renderHook(() => useDataSyncActions({dataSync: {id: '1'}, onDeleted}), {wrapper});

        act(() => result.current.deleteDataSync());

        expect(onDeleted).toHaveBeenCalled();
    });

    it('opens the edit dialog', () => {
        const {result} = renderHook(() => useDataSyncActions({dataSync: {id: '1'}}), {wrapper});

        act(() => result.current.openEditDialog());

        expect(result.current.showEditDialog).toBe(true);
    });
});
