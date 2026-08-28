import {ColumnType, DataTable} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {MouseEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTableListItem from '../useDataTableListItem';

const hoisted = vi.hoisted(() => {
    return {
        navigate: vi.fn(),
    };
});

vi.mock('react-router-dom', () => ({
    useNavigate: vi.fn(() => hoisted.navigate),
}));

const createMockTable = (columnCount: number): DataTable => ({
    baseName: 'test-table',
    columns: Array.from({length: columnCount}, (_, index) => ({
        __typename: 'DataTableColumn' as const,
        id: `col-${index}`,
        name: `column-${index}`,
        type: ColumnType.String,
    })),
    id: 'table-123',
});

describe('useDataTableListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('columnCountLabel', () => {
        it('returns "0 columns" when table has no columns', () => {
            const table = createMockTable(0);
            const {result} = renderHook(() => useDataTableListItem({table}));

            expect(result.current.columnCountLabel).toBe('0 columns');
        });

        it('returns "1 column" (singular) when table has exactly one column', () => {
            const table = createMockTable(1);
            const {result} = renderHook(() => useDataTableListItem({table}));

            expect(result.current.columnCountLabel).toBe('1 column');
        });

        it('returns "2 columns" (plural) when table has multiple columns', () => {
            const table = createMockTable(2);
            const {result} = renderHook(() => useDataTableListItem({table}));

            expect(result.current.columnCountLabel).toBe('2 columns');
        });

        it('returns "5 columns" for five columns', () => {
            const table = createMockTable(5);
            const {result} = renderHook(() => useDataTableListItem({table}));

            expect(result.current.columnCountLabel).toBe('5 columns');
        });

        it('returns "0 columns" when columns is empty', () => {
            const table: DataTable = {
                baseName: 'test-table',
                columns: [],
                id: 'table-123',
            };
            const {result} = renderHook(() => useDataTableListItem({table}));

            expect(result.current.columnCountLabel).toBe('0 columns');
        });
    });

    describe('handleDataTableListItemTagListClick', () => {
        it('calls preventDefault on the event', () => {
            const table = createMockTable(1);
            const {result} = renderHook(() => useDataTableListItem({table}));

            const mockEvent = {
                preventDefault: vi.fn(),
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleDataTableListItemTagListClick(mockEvent);
            });

            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('calls stopPropagation on the event', () => {
            const table = createMockTable(1);
            const {result} = renderHook(() => useDataTableListItem({table}));

            const mockEvent = {
                preventDefault: vi.fn(),
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleDataTableListItemTagListClick(mockEvent);
            });

            expect(mockEvent.stopPropagation).toHaveBeenCalled();
        });
    });

    describe('handleRowClick', () => {
        it('navigates to the correct datatable URL', () => {
            const table = createMockTable(1);
            const {result} = renderHook(() => useDataTableListItem({table}));

            act(() => {
                result.current.handleRowClick();
            });

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/datatables/table-123');
        });

        it('navigates to different table IDs correctly', () => {
            const table: DataTable = {
                ...createMockTable(1),
                id: 'different-table-id',
            };
            const {result} = renderHook(() => useDataTableListItem({table}));

            act(() => {
                result.current.handleRowClick();
            });

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/datatables/different-table-id');
        });
    });
});
