import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useDeleteDataTableColumnDialog from '../useDeleteDataTableColumnDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockClearDialog: vi.fn(),
        mockInvalidateQueries: vi.fn(),
        mockMutate: vi.fn(),
        mockSetColumnToDelete: vi.fn(),
        storeState: {
            columnId: null as string | null,
            columnName: '',
            currentDataTable: {baseName: 'TestTable', id: 'table-123'},
            environmentId: 2,
            workspaceId: 1049,
        },
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: hoisted.storeState.workspaceId}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useRemoveDataTableColumnMutation: ({onSuccess}: {onSuccess: () => void}) => ({
            mutate: (params: unknown) => {
                hoisted.mockMutate(params);
                onSuccess();
            },
        }),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number) => ['dataTables', environmentId, workspaceId],
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => number) =>
        selector({currentEnvironmentId: hoisted.storeState.environmentId}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({
        invalidateQueries: hoisted.mockInvalidateQueries,
    }),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: hoisted.storeState.currentDataTable,
    }),
}));

vi.mock('../../stores/useDeleteDataTableColumnDialogStore', () => ({
    useDeleteDataTableColumnDialogStore: () => ({
        clearDialog: hoisted.mockClearDialog,
        columnId: hoisted.storeState.columnId,
        columnName: hoisted.storeState.columnName,
        setColumnToDelete: hoisted.mockSetColumnToDelete,
    }),
}));

beforeEach(() => {
    hoisted.storeState.columnId = null;
    hoisted.storeState.columnName = '';
    hoisted.storeState.currentDataTable = {baseName: 'TestTable', id: 'table-123'};
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('useDeleteDataTableColumnDialog', () => {
    describe('initial state', () => {
        it('should return open as false when columnId is null', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            expect(result.current.open).toBe(false);
        });

        it('should return empty columnName initially', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            expect(result.current.columnName).toBe('');
        });
    });

    describe('open state', () => {
        it('should return open as true when columnId is set', () => {
            hoisted.storeState.columnId = 'col-123';
            hoisted.storeState.columnName = 'Test Column';

            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            expect(result.current.open).toBe(true);
            expect(result.current.columnName).toBe('Test Column');
        });
    });

    describe('handleOpen', () => {
        it('should call setColumnToDelete with columnId and columnName', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleOpen('col-456', 'Column Name');
            });

            expect(hoisted.mockSetColumnToDelete).toHaveBeenCalledWith('col-456', 'Column Name');
        });
    });

    describe('handleClose', () => {
        it('should call clearDialog', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('should call clearDialog when open is false', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });

        it('should not call clearDialog when open is true', () => {
            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.mockClearDialog).not.toHaveBeenCalled();
        });
    });

    describe('handleDelete', () => {
        it('should not call mutate when dataTable is null', () => {
            hoisted.storeState.currentDataTable = null as unknown as {baseName: string; id: string};
            hoisted.storeState.columnId = 'col-123';

            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });

        it('should not call mutate when columnId is null', () => {
            hoisted.storeState.columnId = null;

            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });

        it('should call mutate with correct parameters when dataTable and columnId exist', () => {
            hoisted.storeState.columnId = 'col-123';

            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    columnId: 'col-123',
                    environmentId: '2',
                    tableId: 'table-123',
                },
            });
        });

        it('should invalidate queries and clear dialog on success', () => {
            hoisted.storeState.columnId = 'col-123';

            const {result} = renderHook(() => useDeleteDataTableColumnDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTables'],
            });
            expect(hoisted.mockClearDialog).toHaveBeenCalled();
        });
    });
});
