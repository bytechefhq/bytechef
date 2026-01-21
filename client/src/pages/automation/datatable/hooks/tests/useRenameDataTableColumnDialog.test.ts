import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockClearDialog: vi.fn(),
        mockMutate: vi.fn(),
        mockSetColumnToRename: vi.fn(),
        mockSetRenameValue: vi.fn(),
        storeState: {
            columnId: null as string | null,
            currentName: '',
            renameValue: '',
        },
    };
});

vi.mock('../../stores/useRenameDataTableColumnDialogStore', () => ({
    useRenameDataTableColumnDialogStore: () => ({
        clearDialog: hoisted.mockClearDialog,
        columnId: hoisted.storeState.columnId,
        currentName: hoisted.storeState.currentName,
        renameValue: hoisted.storeState.renameValue,
        setColumnToRename: hoisted.mockSetColumnToRename,
        setRenameValue: hoisted.mockSetRenameValue,
    }),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: {id: 'table-789'},
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
        useRenameDataTableColumnMutation: () => ({
            mutate: hoisted.mockMutate,
        }),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number) => ['datatables', environmentId, workspaceId],
        rowsPage: (environmentId: number, tableId: string, pageSize: number) => [
            'datatables',
            'rows',
            environmentId,
            tableId,
            pageSize,
        ],
    },
}));

import useRenameDataTableColumnDialog from '../useRenameDataTableColumnDialog';

describe('useRenameDataTableColumnDialog', () => {
    beforeEach(() => {
        hoisted.storeState.columnId = null;
        hoisted.storeState.currentName = '';
        hoisted.storeState.renameValue = '';
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('handleOpen', () => {
        it('should call setColumnToRename with correct parameters', () => {
            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleOpen('col-123', 'originalName');
            });

            expect(hoisted.mockSetColumnToRename).toHaveBeenCalledWith('col-123', 'originalName');
        });
    });

    describe('handleClose', () => {
        it('should call clearDialog', () => {
            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('should call clearDialog when open is false', () => {
            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });

        it('should not call clearDialog when open is true', () => {
            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.mockClearDialog).not.toHaveBeenCalled();
        });
    });

    describe('handleRenameValueChange', () => {
        it('should call setRenameValue with the new value', () => {
            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleRenameValueChange('newColumnName');
            });

            expect(hoisted.mockSetRenameValue).toHaveBeenCalledWith('newColumnName');
        });
    });

    describe('handleRename', () => {
        it('should call mutation when canRename is true', () => {
            hoisted.storeState.columnId = 'col-123';
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = 'newName';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    columnId: 'col-123',
                    environmentId: '2',
                    newName: 'newName',
                    tableId: 'table-789',
                },
            });
        });

        it('should not call mutation when columnId is null', () => {
            hoisted.storeState.columnId = null;
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = 'newName';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });
    });

    describe('canRename', () => {
        it('should return true when renameValue is different from currentName', () => {
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = 'newName';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(true);
        });

        it('should return false when renameValue equals currentName', () => {
            hoisted.storeState.currentName = 'sameName';
            hoisted.storeState.renameValue = 'sameName';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('should return false when renameValue is empty', () => {
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = '';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('should return false when renameValue is only whitespace', () => {
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = '   ';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('should return false when renameValue is "id" (case insensitive)', () => {
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = 'id';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('should return false when renameValue is "ID" (uppercase)', () => {
            hoisted.storeState.currentName = 'oldName';
            hoisted.storeState.renameValue = 'ID';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.canRename).toBe(false);
        });
    });

    describe('open state', () => {
        it('should return true when columnId is not null', () => {
            hoisted.storeState.columnId = 'col-123';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.open).toBe(true);
        });

        it('should return false when columnId is null', () => {
            hoisted.storeState.columnId = null;

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.open).toBe(false);
        });
    });

    describe('currentName and renameValue', () => {
        it('should return currentName from store', () => {
            hoisted.storeState.currentName = 'myColumn';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.currentName).toBe('myColumn');
        });

        it('should return renameValue from store', () => {
            hoisted.storeState.renameValue = 'renamedColumn';

            const {result} = renderHook(() => useRenameDataTableColumnDialog());

            expect(result.current.renameValue).toBe('renamedColumn');
        });
    });
});
