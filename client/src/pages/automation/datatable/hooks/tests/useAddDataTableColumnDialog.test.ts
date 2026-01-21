import {ColumnType} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockClearDialog: vi.fn(),
        mockMutate: vi.fn(),
        mockSetOpen: vi.fn(),
        storeState: {
            open: false,
        },
    };
});

vi.mock('../../stores/useAddDataTableColumnDialogStore', () => ({
    useAddDataTableColumnDialogStore: () => ({
        clearDialog: hoisted.mockClearDialog,
        open: hoisted.storeState.open,
        setOpen: hoisted.mockSetOpen,
    }),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: {id: 'table-123'},
    }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 2,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: () => 1049,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({
        invalidateQueries: vi.fn(),
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useAddDataTableColumnMutation: () => ({
            mutate: hoisted.mockMutate,
        }),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number) => ['datatables', environmentId, workspaceId],
    },
}));

import useAddDataTableColumnDialog from '../useAddDataTableColumnDialog';

describe('useAddDataTableColumnDialog', () => {
    beforeEach(() => {
        hoisted.storeState.open = false;
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('handleOpen', () => {
        it('should call setOpen with true', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleOpen();
            });

            expect(hoisted.mockSetOpen).toHaveBeenCalledWith(true);
        });
    });

    describe('handleClose', () => {
        it('should call clearDialog', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('should call clearDialog when openValue is false', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });

        it('should call setOpen with true when openValue is true', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.mockSetOpen).toHaveBeenCalledWith(true);
        });
    });

    describe('handleAdd', () => {
        it('should call mutation with correct parameters', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleAdd('columnName', ColumnType.String);
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    column: {name: 'columnName', type: ColumnType.String},
                    environmentId: '2',
                    tableId: 'table-123',
                },
            });
        });

        it('should call mutation with different column types', () => {
            const {result} = renderHook(() => useAddDataTableColumnDialog());

            act(() => {
                result.current.handleAdd('numColumn', ColumnType.Number);
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    column: {name: 'numColumn', type: ColumnType.Number},
                    environmentId: '2',
                    tableId: 'table-123',
                },
            });
        });
    });

    describe('open state', () => {
        it('should return the current open state from store', () => {
            hoisted.storeState.open = true;

            const {result} = renderHook(() => useAddDataTableColumnDialog());

            expect(result.current.open).toBe(true);
        });

        it('should return false when dialog is closed', () => {
            hoisted.storeState.open = false;

            const {result} = renderHook(() => useAddDataTableColumnDialog());

            expect(result.current.open).toBe(false);
        });
    });
});
