import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTableActionsMenu from '../useDataTableActionsMenu';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleImport: vi.fn(),
        mockHandleImportOpen: vi.fn(),
        mockHandleImportOpenChange: vi.fn(),
        mockOpenDeleteDialog: vi.fn(),
        mockOpenRenameDialog: vi.fn(),
        mockRefetchExportCsv: vi.fn().mockResolvedValue({data: {exportDataTableCsv: 'csv,data'}}),
        storeState: {
            currentDataTable: {baseName: 'TestTable', id: 'table-123'} as {baseName: string; id: string} | null,
            deleteDialogTableId: null as string | null,
            deleteDialogTableName: '',
            environmentId: 2,
            importDialogOpen: false,
            renameDialogOpen: false,
            renameTableId: null as string | null,
            renameValue: '',
        },
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useExportDataTableCsvQuery: () => ({
            refetch: hoisted.mockRefetchExportCsv,
        }),
    };
});

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => number) =>
        selector({currentEnvironmentId: hoisted.storeState.environmentId}),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: hoisted.storeState.currentDataTable,
    }),
}));

vi.mock('../useDeleteDataTableDialog', () => ({
    default: () => ({
        handleDelete: vi.fn(),
        handleOpen: hoisted.mockOpenDeleteDialog,
        handleOpenChange: vi.fn(),
        open: hoisted.storeState.deleteDialogTableId !== null,
        tableName: hoisted.storeState.deleteDialogTableName,
    }),
}));

vi.mock('../useRenameDataTableDialog', () => ({
    default: () => ({
        canRename: hoisted.storeState.renameValue.length > 0,
        handleOpen: hoisted.mockOpenRenameDialog,
        handleOpenChange: vi.fn(),
        handleRename: vi.fn(),
        handleRenameValueChange: vi.fn(),
        open: hoisted.storeState.renameDialogOpen,
        renameValue: hoisted.storeState.renameValue,
    }),
}));

vi.mock('../useImportDataTableCsvDialog', () => ({
    default: () => ({
        handleImport: hoisted.mockHandleImport,
        handleOpen: hoisted.mockHandleImportOpen,
        handleOpenChange: hoisted.mockHandleImportOpenChange,
        isPending: false,
        open: hoisted.storeState.importDialogOpen,
    }),
}));

beforeEach(() => {
    hoisted.storeState.currentDataTable = {baseName: 'TestTable', id: 'table-123'};
    hoisted.storeState.deleteDialogTableId = null;
    hoisted.storeState.deleteDialogTableName = '';
    hoisted.storeState.renameDialogOpen = false;
    hoisted.storeState.renameValue = '';
    hoisted.storeState.importDialogOpen = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('useDataTableActionsMenu', () => {
    describe('initial state', () => {
        it('should return initial state values', () => {
            const {result} = renderHook(() => useDataTableActionsMenu());

            expect(result.current.deleteDialogOpen).toBe(false);
            expect(result.current.renameDialogOpen).toBe(false);
            expect(result.current.importCsvDialogOpen).toBe(false);
        });
    });

    describe('handleOpenDeleteDialog', () => {
        it('should call openDeleteDialog with current table id and name', () => {
            const {result} = renderHook(() => useDataTableActionsMenu());

            act(() => {
                result.current.handleOpenDeleteDialog();
            });

            expect(hoisted.mockOpenDeleteDialog).toHaveBeenCalledWith('table-123', 'TestTable');
        });

        it('should call openDeleteDialog with empty strings when no table is selected', () => {
            hoisted.storeState.currentDataTable = null;

            const {result} = renderHook(() => useDataTableActionsMenu());

            act(() => {
                result.current.handleOpenDeleteDialog();
            });

            expect(hoisted.mockOpenDeleteDialog).toHaveBeenCalledWith('', '');
        });
    });

    describe('handleOpenRenameDialog', () => {
        it('should call openRenameDialog with current table id and name', () => {
            const {result} = renderHook(() => useDataTableActionsMenu());

            act(() => {
                result.current.handleOpenRenameDialog();
            });

            expect(hoisted.mockOpenRenameDialog).toHaveBeenCalledWith('table-123', 'TestTable');
        });
    });

    describe('handleOpenImportCsvDialog', () => {
        it('should call openImportCsvDialog', () => {
            const {result} = renderHook(() => useDataTableActionsMenu());

            act(() => {
                result.current.handleOpenImportCsvDialog();
            });

            expect(hoisted.mockHandleImportOpen).toHaveBeenCalled();
        });
    });

    describe('handleExportCsv', () => {
        it('should not export when no table is selected', async () => {
            hoisted.storeState.currentDataTable = null;

            const {result} = renderHook(() => useDataTableActionsMenu());

            await act(async () => {
                await result.current.handleExportCsv();
            });

            expect(hoisted.mockRefetchExportCsv).not.toHaveBeenCalled();
        });
    });

    describe('handleImportCsv', () => {
        it('should call the import handler', () => {
            const {result} = renderHook(() => useDataTableActionsMenu());

            act(() => {
                result.current.handleImportCsv('csv,content');
            });

            expect(hoisted.mockHandleImport).toHaveBeenCalledWith('csv,content');
        });
    });
});
