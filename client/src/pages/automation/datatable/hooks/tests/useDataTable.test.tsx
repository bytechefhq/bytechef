import {render, renderHook, resetAll, screen} from '@/shared/util/test-utils';
import {
    type CellKeyDownArgs,
    type CellKeyboardEvent,
    type ColSpanArgs,
    type Column,
    type RenderSummaryCellProps,
} from 'react-data-grid';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {type GridRowType, type SummaryRowType} from '../../components/cell-renderers';
import {useCurrentDataTableStore} from '../../stores/useCurrentDataTableStore';
import {useDataTable} from '../useDataTable';

const hoisted = vi.hoisted(() => {
    const dataTable = {
        columns: [
            {id: 'column-1', name: 'title', type: 'STRING'},
            {id: 'column-2', name: 'count', type: 'INTEGER'},
        ],
        id: 'table-1',
    };

    return {
        dataTable,
        defaultColumns: dataTable.columns,
        insertRowMutate: vi.fn(),
        rowsQueryResult: {
            data: {
                pages: [
                    {
                        dataTableRowsPage: {
                            items: [
                                {id: 'row-1', values: {title: 'first'}},
                                {id: 'row-2', values: {title: 'second'}},
                            ],
                        },
                    },
                ],
            },
            error: null,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
            isLoading: false,
        },
        tablesQueryResult: {
            data: {dataTables: [dataTable]},
            error: null,
            isLoading: false,
        },
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: 1049}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTablesQuery: () => hoisted.tablesQueryResult,
        useInsertDataTableRowMutation: () => ({mutate: hoisted.insertRowMutate}),
        useUpdateDataTableRowMutation: () => ({mutate: vi.fn()}),
    };
});

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    useDataTableRowsInfiniteQuery: () => hoisted.rowsQueryResult,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => number) =>
        selector({currentEnvironmentId: 0}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

const summaryCellProps = {} as RenderSummaryCellProps<SummaryRowType, GridRowType>;

const renderSummaryCell = (column: Column<GridRowType, SummaryRowType>) =>
    render(<>{column.renderSummaryCell?.(summaryCellProps)}</>);

const createKeyDownArgs = (rowId: string) =>
    ({mode: 'ACTIVE', row: {id: rowId}, rowIdx: 0}) as CellKeyDownArgs<GridRowType, SummaryRowType>;

const createKeyboardEvent = (key: string) => ({key, preventGridDefault: vi.fn()}) as unknown as CellKeyboardEvent;

const renderUseDataTable = () => renderHook(() => useDataTable({tableId: 'table-1'}));

beforeEach(() => {
    hoisted.dataTable.columns = hoisted.defaultColumns;
    hoisted.rowsQueryResult.isFetchingNextPage = false;

    useCurrentDataTableStore.setState({dataTable: undefined});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('useDataTable', () => {
    describe('summary row', () => {
        it('should render the total rows in the frozen first column', () => {
            const {result} = renderUseDataTable();

            const [idColumn] = result.current.gridColumns;

            expect(idColumn.frozen).toBe(true);
            expect(idColumn.summaryCellClass).toBe('datatable-summary-total');

            renderSummaryCell(idColumn);

            expect(screen.getByText('Total rows: 2')).toBeInTheDocument();
        });

        it('should not render the total in any other column', () => {
            const {result} = renderUseDataTable();

            const otherColumns = result.current.gridColumns.slice(1);

            expect(otherColumns).toHaveLength(3);
            expect(otherColumns.every((column) => column.renderSummaryCell == null)).toBe(true);
        });

        it('should keep the first user column spanning the remaining summary cells', () => {
            const {result} = renderUseDataTable();

            const firstUserColumn = result.current.gridColumns[1];

            expect(firstUserColumn.summaryCellClass).toBe('datatable-summary-row');
            expect(
                firstUserColumn.colSpan?.({row: {}, type: 'SUMMARY'} as ColSpanArgs<GridRowType, SummaryRowType>)
            ).toBe(2);
        });

        it('should show the loading indicator while the next page is fetching', () => {
            hoisted.rowsQueryResult.isFetchingNextPage = true;

            const {result} = renderUseDataTable();

            renderSummaryCell(result.current.gridColumns[0]);

            expect(screen.getByText('Loading…')).toBeInTheDocument();
        });

        it('should render the total in the first column when the table has no columns', () => {
            hoisted.dataTable.columns = [];

            const {result} = renderUseDataTable();

            const [idColumn, trailingColumn] = result.current.gridColumns;

            expect(result.current.gridColumns).toHaveLength(2);
            expect(trailingColumn.renderSummaryCell).toBeUndefined();

            renderSummaryCell(idColumn);

            expect(screen.getByText('Total rows: 2')).toBeInTheDocument();
        });
    });

    describe('add row cell keyboard', () => {
        it('should insert a row when Enter is pressed on the add row cell', () => {
            const {result} = renderUseDataTable();

            const keyboardEvent = createKeyboardEvent('Enter');

            result.current.handleCellKeyDown(createKeyDownArgs('-1'), keyboardEvent);

            expect(keyboardEvent.preventGridDefault).toHaveBeenCalled();
            expect(hoisted.insertRowMutate).toHaveBeenCalledWith({
                input: {environmentId: '0', tableId: 'table-1', values: {}},
            });
        });

        it('should not insert a row when Enter is pressed on a data row', () => {
            const {result} = renderUseDataTable();

            const keyboardEvent = createKeyboardEvent('Enter');

            result.current.handleCellKeyDown(createKeyDownArgs('row-1'), keyboardEvent);

            expect(keyboardEvent.preventGridDefault).not.toHaveBeenCalled();
            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
        });

        it('should not insert a row when another key is pressed on the add row cell', () => {
            const {result} = renderUseDataTable();

            const keyboardEvent = createKeyboardEvent('ArrowDown');

            result.current.handleCellKeyDown(createKeyDownArgs('-1'), keyboardEvent);

            expect(keyboardEvent.preventGridDefault).not.toHaveBeenCalled();
            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
        });
    });
});
