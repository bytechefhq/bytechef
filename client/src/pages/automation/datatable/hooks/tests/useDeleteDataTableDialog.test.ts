import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useDeleteDataTableDialog from '../useDeleteDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockClearTableToDelete: vi.fn(),
        mockInvalidateQueries: vi.fn(),
        mockMutate: vi.fn(),
        mockNavigate: vi.fn(),
        mockSetTableToDelete: vi.fn(),
        storeState: {
            dataTables: [
                {baseName: 'Alpha', id: 'table-1'},
                {baseName: 'Beta', id: 'table-2'},
                {baseName: 'Gamma', id: 'table-3'},
            ],
            environmentId: 2,
            tableIdToDelete: null as string | null,
            tableNameToDelete: '',
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
        useDataTablesQuery: () => ({
            data: {dataTables: hoisted.storeState.dataTables},
        }),
        useDropDataTableMutation: ({onSuccess}: {onSuccess: () => void}) => ({
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

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({
            invalidateQueries: hoisted.mockInvalidateQueries,
        }),
    };
});

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.mockNavigate,
}));

vi.mock('../../stores/useDeleteDataTableDialogStore', () => ({
    useDeleteDataTableDialogStore: () => ({
        clearTableToDelete: hoisted.mockClearTableToDelete,
        setTableToDelete: hoisted.mockSetTableToDelete,
        tableIdToDelete: hoisted.storeState.tableIdToDelete,
        tableNameToDelete: hoisted.storeState.tableNameToDelete,
    }),
}));

beforeEach(() => {
    hoisted.storeState.tableIdToDelete = null;
    hoisted.storeState.tableNameToDelete = '';
    hoisted.storeState.dataTables = [
        {baseName: 'Alpha', id: 'table-1'},
        {baseName: 'Beta', id: 'table-2'},
        {baseName: 'Gamma', id: 'table-3'},
    ];
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('useDeleteDataTableDialog', () => {
    describe('initial state', () => {
        it('should return open as false when tableIdToDelete is null', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            expect(result.current.open).toBe(false);
        });

        it('should return empty tableName initially', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            expect(result.current.tableName).toBe('');
        });
    });

    describe('open state', () => {
        it('should return open as true when tableIdToDelete is set', () => {
            hoisted.storeState.tableIdToDelete = 'table-1';
            hoisted.storeState.tableNameToDelete = 'Alpha';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            expect(result.current.open).toBe(true);
            expect(result.current.tableName).toBe('Alpha');
        });
    });

    describe('handleOpen', () => {
        it('should call setTableToDelete with tableId and tableName', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleOpen('table-456', 'Test Table');
            });

            expect(hoisted.mockSetTableToDelete).toHaveBeenCalledWith('table-456', 'Test Table');
        });
    });

    describe('handleClose', () => {
        it('should call clearTableToDelete', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.mockClearTableToDelete).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('should call clearTableToDelete when open is false', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.mockClearTableToDelete).toHaveBeenCalled();
        });

        it('should not call clearTableToDelete when open is true', () => {
            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.mockClearTableToDelete).not.toHaveBeenCalled();
        });
    });

    describe('handleDelete', () => {
        it('should not call mutate when tableIdToDelete is null', () => {
            hoisted.storeState.tableIdToDelete = null;

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutate).not.toHaveBeenCalled();
        });

        it('should call mutate with correct parameters when tableIdToDelete exists', () => {
            hoisted.storeState.tableIdToDelete = 'table-2';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    tableId: 'table-2',
                },
            });
        });

        it('should navigate to next table after deletion', () => {
            hoisted.storeState.tableIdToDelete = 'table-1';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/automation/datatables/table-2');
        });

        it('should navigate to datatables list when deleting last table', () => {
            hoisted.storeState.tableIdToDelete = 'table-3';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/automation/datatables');
        });

        it('should navigate to datatables list when no tables remain', () => {
            hoisted.storeState.dataTables = [{baseName: 'OnlyTable', id: 'table-only'}];
            hoisted.storeState.tableIdToDelete = 'table-only';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/automation/datatables');
        });

        it('should invalidate queries and clear dialog on success', () => {
            hoisted.storeState.tableIdToDelete = 'table-1';

            const {result} = renderHook(() => useDeleteDataTableDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTables'],
            });
            expect(hoisted.mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTableTagsByTable'],
            });
            expect(hoisted.mockClearTableToDelete).toHaveBeenCalled();
        });
    });
});
