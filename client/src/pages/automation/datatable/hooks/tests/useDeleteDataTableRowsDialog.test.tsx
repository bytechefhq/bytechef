import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useCurrentDataTableStore} from '../../stores/useCurrentDataTableStore';
import {useDeleteDataTableRowsDialogStore} from '../../stores/useDeleteDataTableRowsDialogStore';
import {useSelectedRowsStore} from '../../stores/useSelectedRowsStore';
import useDeleteDataTableRowsDialog from '../useDeleteDataTableRowsDialog';

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 2),
}));

const hoisted = vi.hoisted(() => {
    return {
        mockMutateAsync: vi.fn().mockResolvedValue({}),
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDeleteDataTableRowMutation: vi.fn(() => ({
            mutateAsync: hoisted.mockMutateAsync,
        })),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        rowsPage: vi.fn((environmentId: number, tableId: string, pageSize: number) => [
            'datatable-rows',
            environmentId,
            tableId,
            pageSize,
        ]),
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

    return ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
};

describe('useDeleteDataTableRowsDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        useCurrentDataTableStore.getState().clearDataTable();
        useDeleteDataTableRowsDialogStore.getState().clearDialog();
        useSelectedRowsStore.getState().clearSelectedRows();
    });

    describe('initial state', () => {
        it('should return open as false initially', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            expect(result.current.open).toBe(false);
        });

        it('should return rowCount as 0 when no rows are selected in store', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            expect(result.current.rowCount).toBe(0);
        });

        it('should return correct rowCount when rows are selected in store', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1', '2', '3']));

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            expect(result.current.rowCount).toBe(3);
        });
    });

    describe('handleOpen', () => {
        it('should set open to true when called', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.open).toBe(true);
        });
    });

    describe('handleClose', () => {
        it('should set open to false when called', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.open).toBe(true);

            act(() => {
                result.current.handleClose();
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('handleOpenChange', () => {
        it('should close dialog when called with false', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.open).toBe(true);

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(result.current.open).toBe(false);
        });

        it('should not change state when called with true', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('handleDelete', () => {
        it('should not call mutate when no rows are selected in store', () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutateAsync).not.toHaveBeenCalled();
        });

        it('should not call mutate when dataTable is undefined', () => {
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1']));

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutateAsync).not.toHaveBeenCalled();
        });

        it('should call mutate for each selected row in store', async () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1', '2', '3']));

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            await act(async () => {
                await result.current.handleDelete();
            });

            expect(hoisted.mockMutateAsync).toHaveBeenCalledTimes(3);
            expect(hoisted.mockMutateAsync).toHaveBeenCalledWith({
                input: {environmentId: '2', id: '1', tableId: 'test-table-id'},
            });
            expect(hoisted.mockMutateAsync).toHaveBeenCalledWith({
                input: {environmentId: '2', id: '2', tableId: 'test-table-id'},
            });
            expect(hoisted.mockMutateAsync).toHaveBeenCalledWith({
                input: {environmentId: '2', id: '3', tableId: 'test-table-id'},
            });
        });

        it('should close dialog after delete', async () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1']));

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            act(() => {
                result.current.handleOpen();
            });

            expect(result.current.open).toBe(true);

            await act(async () => {
                await result.current.handleDelete();
            });

            expect(result.current.open).toBe(false);
        });

        it('should clear selected rows from store after delete', async () => {
            useCurrentDataTableStore.getState().setDataTable({baseName: 'test-table', id: 'test-table-id'} as never);
            useSelectedRowsStore.getState().setSelectedRows(new Set<string>(['1', '2', '3']));

            expect(useSelectedRowsStore.getState().selectedRows.size).toBe(3);

            const {result} = renderHook(() => useDeleteDataTableRowsDialog(), {
                wrapper: createWrapper(),
            });

            await act(async () => {
                await result.current.handleDelete();
            });

            expect(useSelectedRowsStore.getState().selectedRows.size).toBe(0);
        });
    });
});
