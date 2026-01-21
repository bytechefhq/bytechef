import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDuplicateDataTableAlertDialog from '../useDuplicateDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        duplicateMutate: vi.fn(),
        invalidateQueries: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDuplicateDataTableMutation: vi.fn((options: {onSuccess: () => void}) => ({
            mutate: (vars: unknown) => {
                hoisted.duplicateMutate(vars);
                options.onSuccess();
            },
        })),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number) => ['dataTables', environmentId, workspaceId],
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

describe('useDuplicateDataTableAlertDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns open as false initially', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            expect(result.current.open).toBe(false);
        });

        it('returns empty duplicateValue initially', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            expect(result.current.duplicateValue).toBe('');
        });
    });

    describe('dialog operations', () => {
        it('opens dialog with table id and base name', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            expect(result.current.open).toBe(true);
            expect(result.current.duplicateValue).toBe('TestTable_copy');
        });

        it('closes dialog', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleClose();
            });

            expect(result.current.open).toBe(false);
            expect(result.current.duplicateValue).toBe('');
        });

        it('updates duplicate value', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateValueChange('CopyName');
            });

            expect(result.current.duplicateValue).toBe('CopyName');
        });
    });

    describe('canDuplicate', () => {
        it('returns true when name is not empty', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            expect(result.current.canDuplicate).toBe(true);
        });

        it('returns false when name is empty', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateValueChange('');
            });

            expect(result.current.canDuplicate).toBe(false);
        });

        it('returns false when name is whitespace only', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateValueChange('   ');
            });

            expect(result.current.canDuplicate).toBe(false);
        });
    });

    describe('submit', () => {
        it('submits duplicate with correct parameters', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateSubmit();
            });

            expect(hoisted.duplicateMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    newBaseName: 'TestTable_copy',
                    tableId: 'table-123',
                },
            });
        });

        it('trims the duplicate value before submitting', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateValueChange('  CopyName  ');
            });

            act(() => {
                result.current.handleDuplicateSubmit();
            });

            expect(hoisted.duplicateMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: '2',
                    newBaseName: 'CopyName',
                    tableId: 'table-123',
                },
            });
        });

        it('closes dialog after successful submit', () => {
            const {result} = renderHook(() => useDuplicateDataTableAlertDialog());

            act(() => {
                result.current.handleOpen('table-123', 'TestTable');
            });

            act(() => {
                result.current.handleDuplicateSubmit();
            });

            expect(result.current.open).toBe(false);
        });
    });
});
