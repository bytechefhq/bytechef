import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTableHeader from '../useDataTableHeader';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleDelete: vi.fn(),
        mockHandleOpen: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        storeState: {
            deleteRowsDialogOpen: false,
            rowCount: 0,
            selectedRows: new Set<string>(),
        },
    };
});

vi.mock('../../stores/useSelectedRowsStore', () => ({
    useSelectedRowsStore: () => ({
        selectedRows: hoisted.storeState.selectedRows,
    }),
}));

vi.mock('../useDeleteDataTableRowsDialog', () => ({
    default: () => ({
        handleDelete: hoisted.mockHandleDelete,
        handleOpen: hoisted.mockHandleOpen,
        handleOpenChange: hoisted.mockHandleOpenChange,
        open: hoisted.storeState.deleteRowsDialogOpen,
        rowCount: hoisted.storeState.rowCount,
    }),
}));

beforeEach(() => {
    hoisted.storeState.selectedRows = new Set<string>();
    hoisted.storeState.deleteRowsDialogOpen = false;
    hoisted.storeState.rowCount = 0;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('useDataTableHeader', () => {
    describe('initial state', () => {
        it('should return 0 selectedRowsCount when no rows are selected', () => {
            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.selectedRowsCount).toBe(0);
        });

        it('should return deleteRowsDialogOpen as false initially', () => {
            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.deleteRowsDialogOpen).toBe(false);
        });

        it('should return deleteRowsCount as 0 initially', () => {
            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.deleteRowsCount).toBe(0);
        });
    });

    describe('selectedRowsCount', () => {
        it('should return correct count when rows are selected', () => {
            hoisted.storeState.selectedRows = new Set(['row-1', 'row-2', 'row-3']);

            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.selectedRowsCount).toBe(3);
        });

        it('should return correct count when single row is selected', () => {
            hoisted.storeState.selectedRows = new Set(['row-1']);

            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.selectedRowsCount).toBe(1);
        });
    });

    describe('handleOpenDeleteRowsDialog', () => {
        it('should call handleOpen from useDeleteDataTableRowsDialog', () => {
            const {result} = renderHook(() => useDataTableHeader());

            act(() => {
                result.current.handleOpenDeleteRowsDialog();
            });

            expect(hoisted.mockHandleOpen).toHaveBeenCalled();
        });
    });

    describe('handleDeleteRows', () => {
        it('should call handleDelete from useDeleteDataTableRowsDialog', () => {
            const {result} = renderHook(() => useDataTableHeader());

            act(() => {
                result.current.handleDeleteRows();
            });

            expect(hoisted.mockHandleDelete).toHaveBeenCalled();
        });
    });

    describe('handleDeleteRowsDialogOpenChange', () => {
        it('should call handleOpenChange from useDeleteDataTableRowsDialog', () => {
            const {result} = renderHook(() => useDataTableHeader());

            act(() => {
                result.current.handleDeleteRowsDialogOpenChange(false);
            });

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });
    });

    describe('deleteRowsDialogOpen', () => {
        it('should reflect the dialog open state', () => {
            hoisted.storeState.deleteRowsDialogOpen = true;

            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.deleteRowsDialogOpen).toBe(true);
        });
    });

    describe('deleteRowsCount', () => {
        it('should reflect the row count from dialog', () => {
            hoisted.storeState.rowCount = 5;

            const {result} = renderHook(() => useDataTableHeader());

            expect(result.current.deleteRowsCount).toBe(5);
        });
    });
});
