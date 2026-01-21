import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useRenameDataTableDialog from '../useRenameDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
        renameMutate: vi.fn(),
        storeState: {
            baseName: '',
            clearTableToRename: vi.fn(),
            renameValue: '',
            setRenameValue: vi.fn(),
            setTableToRename: vi.fn(),
            tableIdToRename: null as string | null,
        },
    };
});

vi.mock('@/pages/automation/datatable/stores/useRenameDataTableDialogStore', () => ({
    useRenameDataTableDialogStore: vi.fn(() => {
        return {
            baseName: hoisted.storeState.baseName,
            clearTableToRename: () => {
                hoisted.storeState.baseName = '';
                hoisted.storeState.renameValue = '';
                hoisted.storeState.tableIdToRename = null;
                hoisted.storeState.clearTableToRename();
            },
            renameValue: hoisted.storeState.renameValue,
            setRenameValue: (value: string) => {
                hoisted.storeState.renameValue = value;
                hoisted.storeState.setRenameValue(value);
            },
            setTableToRename: (tableId: string, baseName: string) => {
                hoisted.storeState.tableIdToRename = tableId;
                hoisted.storeState.baseName = baseName;
                hoisted.storeState.renameValue = baseName;
                hoisted.storeState.setTableToRename(tableId, baseName);
            },
            tableIdToRename: hoisted.storeState.tableIdToRename,
        };
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useRenameDataTableMutation: vi.fn((options: {onSuccess: () => void}) => ({
            mutate: (vars: unknown) => {
                hoisted.renameMutate(vars);
                options.onSuccess();
            },
        })),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number | null) => ['dataTables', environmentId, workspaceId],
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1049),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 2),
}));

describe('useRenameDataTableDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.baseName = '';
        hoisted.storeState.renameValue = '';
        hoisted.storeState.tableIdToRename = null;
    });

    describe('initial state', () => {
        it('returns open as false', () => {
            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.open).toBe(false);
        });

        it('returns empty renameValue', () => {
            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.renameValue).toBe('');
        });

        it('returns canRename as false when no table is selected', () => {
            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.canRename).toBe(false);
        });
    });

    describe('open dialog', () => {
        it('opens dialog when handleOpen is called', () => {
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            rerender();

            expect(result.current.open).toBe(true);
        });

        it('sets renameValue to baseName when opened', () => {
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            rerender();

            expect(result.current.renameValue).toBe('TestTable');
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleClose();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('canRename', () => {
        it('returns false when renameValue equals baseName', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'TestTable';

            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('returns true when renameValue differs from baseName', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'NewName';

            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.canRename).toBe(true);
        });

        it('returns false when renameValue is empty', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = '';

            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.canRename).toBe(false);
        });

        it('returns false when renameValue is whitespace only', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = '   ';

            const {result} = renderHook(() => useRenameDataTableDialog());

            expect(result.current.canRename).toBe(false);
        });
    });

    describe('handleRenameValueChange', () => {
        it('updates rename value', () => {
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleRenameValueChange('NewName');
            });

            rerender();

            expect(result.current.renameValue).toBe('NewName');
        });
    });

    describe('handleRename', () => {
        it('calls rename mutation with correct data', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'NewName';

            const {result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.renameMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    newBaseName: 'NewName',
                    tableId: 'table-123',
                },
            });
        });

        it('does not call mutation when no table is selected', () => {
            const {result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.renameMutate).not.toHaveBeenCalled();
        });

        it('does not call mutation when canRename is false', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'TestTable';

            const {result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.renameMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful rename', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'NewName';

            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });

        it('invalidates queries after successful rename', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = 'NewName';

            const {result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['dataTables'],
            });
        });

        it('trims the rename value before submitting', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            hoisted.storeState.baseName = 'TestTable';
            hoisted.storeState.renameValue = '  NewName  ';

            const {result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleRename();
            });

            expect(hoisted.renameMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    newBaseName: 'NewName',
                    tableId: 'table-123',
                },
            });
        });
    });

    describe('handleOpenChange', () => {
        it('closes dialog when called with false', () => {
            hoisted.storeState.tableIdToRename = 'table-123';
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleOpenChange(false);
            });

            rerender();

            expect(result.current.open).toBe(false);
        });

        it('does nothing when called with true', () => {
            const {rerender, result} = renderHook(() => useRenameDataTableDialog());

            act(() => {
                result.current.handleOpenChange(true);
            });

            rerender();

            expect(hoisted.storeState.clearTableToRename).not.toHaveBeenCalled();
        });
    });
});
