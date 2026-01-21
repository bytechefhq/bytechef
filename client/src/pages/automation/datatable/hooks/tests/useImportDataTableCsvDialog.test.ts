import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockClearDialog: vi.fn(),
        mockMutate: vi.fn(),
        mockSetOpen: vi.fn(),
        mutationState: {
            isPending: false,
        },
        storeState: {
            open: false,
        },
    };
});

vi.mock('../../stores/useImportDataTableCsvDialogStore', () => ({
    useImportDataTableCsvDialogStore: () => ({
        clearDialog: hoisted.mockClearDialog,
        open: hoisted.storeState.open,
        setOpen: hoisted.mockSetOpen,
    }),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: {id: 'table-456'},
    }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 2,
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
        useImportDataTableCsvMutation: () => ({
            isPending: hoisted.mutationState.isPending,
            mutate: hoisted.mockMutate,
        }),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        rowsPage: (environmentId: number, tableId: string, pageSize: number) => [
            'datatables',
            'rows',
            environmentId,
            tableId,
            pageSize,
        ],
    },
}));

import useImportDataTableCsvDialog from '../useImportDataTableCsvDialog';

describe('useImportDataTableCsvDialog', () => {
    beforeEach(() => {
        hoisted.storeState.open = false;
        hoisted.mutationState.isPending = false;
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('handleOpen', () => {
        it('should call setOpen with true', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());

            act(() => {
                result.current.handleOpen();
            });

            expect(hoisted.mockSetOpen).toHaveBeenCalledWith(true);
        });
    });

    describe('handleClose', () => {
        it('should call clearDialog', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('should call clearDialog when openValue is false', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });

        it('should call setOpen with true when openValue is true', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.mockSetOpen).toHaveBeenCalledWith(true);
        });
    });

    describe('handleImport', () => {
        it('should call mutation with correct parameters', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());
            const csvContent = 'name,age\nJohn,30\nJane,25';

            act(() => {
                result.current.handleImport(csvContent);
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    csv: csvContent,
                    environmentId: '2',
                    tableId: 'table-456',
                },
            });
        });

        it('should handle empty CSV content', () => {
            const {result} = renderHook(() => useImportDataTableCsvDialog());

            act(() => {
                result.current.handleImport('');
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    csv: '',
                    environmentId: '2',
                    tableId: 'table-456',
                },
            });
        });
    });

    describe('open state', () => {
        it('should return the current open state from store', () => {
            hoisted.storeState.open = true;

            const {result} = renderHook(() => useImportDataTableCsvDialog());

            expect(result.current.open).toBe(true);
        });

        it('should return false when dialog is closed', () => {
            hoisted.storeState.open = false;

            const {result} = renderHook(() => useImportDataTableCsvDialog());

            expect(result.current.open).toBe(false);
        });
    });

    describe('isPending state', () => {
        it('should return false when mutation is not pending', () => {
            hoisted.mutationState.isPending = false;

            const {result} = renderHook(() => useImportDataTableCsvDialog());

            expect(result.current.isPending).toBe(false);
        });

        it('should return true when mutation is pending', () => {
            hoisted.mutationState.isPending = true;

            const {result} = renderHook(() => useImportDataTableCsvDialog());

            expect(result.current.isPending).toBe(true);
        });
    });
});
