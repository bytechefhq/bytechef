import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDeleteDataTableAlertDialog from '../useDeleteDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        dropMutate: vi.fn(),
        invalidateQueries: vi.fn(),
        storeState: {
            clearTableIdToDelete: vi.fn(),
            setTableIdToDelete: vi.fn(),
            tableIdToDelete: null as string | null,
        },
    };
});

vi.mock('@/pages/automation/datatables/stores/useDeleteDataTableDialogStore', () => ({
    useDeleteDataTableDialogStore: vi.fn(() => {
        return {
            clearTableIdToDelete: () => {
                hoisted.storeState.tableIdToDelete = null;
                hoisted.storeState.clearTableIdToDelete();
            },
            setTableIdToDelete: (tableId: string) => {
                hoisted.storeState.tableIdToDelete = tableId;
                hoisted.storeState.setTableIdToDelete(tableId);
            },
            tableIdToDelete: hoisted.storeState.tableIdToDelete,
        };
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDropDataTableMutation: vi.fn((options: {onSuccess: () => void}) => ({
            mutate: (vars: unknown) => {
                hoisted.dropMutate(vars);
                options.onSuccess();
            },
        })),
    };
});

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 2),
}));

describe('useDeleteDataTableAlertDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.tableIdToDelete = null;
    });

    describe('initial state', () => {
        it('returns open as false', () => {
            const {result} = renderHook(() => useDeleteDataTableAlertDialog());

            expect(result.current.open).toBe(false);
        });
    });

    describe('open dialog', () => {
        it('opens dialog when handleOpen is called with tableId', () => {
            const {rerender, result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123');
            });

            rerender();

            expect(result.current.open).toBe(true);
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            hoisted.storeState.tableIdToDelete = 'table-123';
            const {rerender, result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleClose();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('handle delete', () => {
        it('calls drop mutation with correct data', () => {
            hoisted.storeState.tableIdToDelete = 'table-123';
            const {result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.dropMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    tableId: 'table-123',
                },
            });
        });

        it('does not call mutation when no table is selected', () => {
            const {result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.dropMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful delete', () => {
            hoisted.storeState.tableIdToDelete = 'table-123';
            const {rerender, result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });

        it('invalidates queries after successful delete', () => {
            hoisted.storeState.tableIdToDelete = 'table-123';
            const {result} = renderHook(() => useDeleteDataTableAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTables'],
            });
            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTableTagsByTable'],
            });
        });
    });
});
