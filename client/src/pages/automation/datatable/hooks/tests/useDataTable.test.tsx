import {act, fireEvent, render, renderHook, resetAll, screen} from '@/shared/util/test-utils';
import {
    type CellKeyDownArgs,
    type CellKeyboardEvent,
    type ColSpanArgs,
    type Column,
    type PositionChangeArgs,
    type RenderCellProps,
    type RenderSummaryCellProps,
    type RowsChangeData,
} from 'react-data-grid';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {type GridRowType, type SummaryRowType} from '../../components/cell-renderers';
import {useCurrentDataTableStore} from '../../stores/useCurrentDataTableStore';
import {ADD_ROW_ID, DRAFT_ROW_ID, useDataTable} from '../useDataTable';

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
        invalidateQueries: vi.fn(() => Promise.resolve()),
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
        updateRowMutate: vi.fn(),
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
        useUpdateDataTableRowMutation: () => ({mutate: hoisted.updateRowMutate}),
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
        useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
    };
});

const summaryCellProps = {} as RenderSummaryCellProps<SummaryRowType, GridRowType>;

const renderSummaryCell = (column: Column<GridRowType, SummaryRowType>) =>
    render(<>{column.renderSummaryCell?.(summaryCellProps)}</>);

const createKeyDownArgs = (rowId: string) =>
    ({mode: 'ACTIVE', row: {id: rowId}, rowIdx: 0}) as CellKeyDownArgs<GridRowType, SummaryRowType>;

const createKeyboardEvent = (key: string) => ({key, preventGridDefault: vi.fn()}) as unknown as CellKeyboardEvent;

const renderUseDataTable = () => renderHook(() => useDataTable({tableId: 'table-1'}));

type InsertRowOptionsType = {
    onError: () => void;
    onSuccess: (response: {insertDataTableRow: {id: string} | null}) => Promise<void>;
};

const getDraftRow = (rows: GridRowType[]) => rows.find((row) => row.id === DRAFT_ROW_ID);

const changeDraftRow = (result: {current: ReturnType<typeof useDataTable>}, values: Record<string, unknown>) => {
    const rows = result.current.gridRowsWithAdd;
    const draftRowIndex = rows.findIndex((row) => row.id === DRAFT_ROW_ID);
    const updatedRows = rows.map((row, index) => (index === draftRowIndex ? {...row, ...values} : row));

    act(() => {
        result.current.handleRowsChange(updatedRows, {
            column: result.current.gridColumns[1],
            indexes: [draftRowIndex],
        } as RowsChangeData<GridRowType, SummaryRowType>);
    });
};

const createPositionChangeArgs = (rowId: string) =>
    ({row: {id: rowId}, rowIdx: 0}) as unknown as PositionChangeArgs<GridRowType, SummaryRowType>;

const moveActivePosition = (result: {current: ReturnType<typeof useDataTable>}, rowId: string) => {
    act(() => {
        result.current.handleActivePositionChange(createPositionChangeArgs(rowId));

        vi.runAllTimers();
    });
};

const getInsertRowOptions = () => hoisted.insertRowMutate.mock.calls[0][1] as InsertRowOptionsType;

const addDraftRow = (result: {current: ReturnType<typeof useDataTable>}) => {
    act(() => {
        result.current.handleCellKeyDown(createKeyDownArgs(ADD_ROW_ID), createKeyboardEvent('Enter'));
    });
};

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
        it('should add a draft row without inserting when Enter is pressed on the add row cell', () => {
            const {result} = renderUseDataTable();

            const keyboardEvent = createKeyboardEvent('Enter');

            act(() => {
                result.current.handleCellKeyDown(createKeyDownArgs(ADD_ROW_ID), keyboardEvent);
            });

            expect(keyboardEvent.preventGridDefault).toHaveBeenCalled();
            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
            expect(result.current.gridRowsWithAdd.map((row) => row.id)).toEqual([
                'row-1',
                'row-2',
                DRAFT_ROW_ID,
                ADD_ROW_ID,
            ]);
        });

        it('should keep a single draft row when Enter is pressed twice on the add row cell', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            addDraftRow(result);

            expect(result.current.gridRowsWithAdd.filter((row) => row.id === DRAFT_ROW_ID)).toHaveLength(1);
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

            result.current.handleCellKeyDown(createKeyDownArgs(ADD_ROW_ID), keyboardEvent);

            expect(keyboardEvent.preventGridDefault).not.toHaveBeenCalled();
            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
        });
    });

    describe('draft row', () => {
        beforeEach(() => {
            vi.useFakeTimers({toFake: ['setTimeout']});
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it('should not insert the draft row while it is being edited', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            changeDraftRow(result, {count: 3});
            moveActivePosition(result, DRAFT_ROW_ID);

            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
            expect(getDraftRow(result.current.gridRowsWithAdd)).toEqual({count: 3, id: DRAFT_ROW_ID, title: 'new'});
        });

        it('should insert the draft row with all its values when the active cell moves to another row', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            changeDraftRow(result, {count: 3});
            moveActivePosition(result, 'row-1');

            expect(hoisted.insertRowMutate).toHaveBeenCalledTimes(1);
            expect(hoisted.insertRowMutate).toHaveBeenCalledWith(
                {input: {environmentId: '0', tableId: 'table-1', values: {count: 3, title: 'new'}}},
                expect.any(Object)
            );
        });

        it('should insert the draft row with the value committed by the editor in the same event', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);

            act(() => {
                result.current.handleActivePositionChange(createPositionChangeArgs('row-1'));
            });

            changeDraftRow(result, {title: 'committed on leave'});

            act(() => {
                vi.runAllTimers();
            });

            expect(hoisted.insertRowMutate).toHaveBeenCalledWith(
                {input: {environmentId: '0', tableId: 'table-1', values: {title: 'committed on leave'}}},
                expect.any(Object)
            );
        });

        it('should discard an empty draft row when the active cell moves to another row', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: ''});
            moveActivePosition(result, 'row-1');

            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
            expect(getDraftRow(result.current.gridRowsWithAdd)).toBeUndefined();
        });

        it('should insert the draft row on a mouse down outside the grid', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});

            act(() => {
                fireEvent.mouseDown(document.body);

                vi.runAllTimers();
            });

            expect(hoisted.insertRowMutate).toHaveBeenCalledTimes(1);
        });

        it('should not insert the draft row on a mouse down inside a cell editor popover', () => {
            const {result} = renderUseDataTable();

            const popoverContent = document.createElement('div');
            const popoverInput = document.createElement('input');

            popoverContent.setAttribute('data-radix-popper-content-wrapper', '');
            popoverContent.appendChild(popoverInput);
            document.body.appendChild(popoverContent);

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});

            act(() => {
                fireEvent.mouseDown(popoverInput);

                vi.runAllTimers();
            });

            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();

            popoverContent.remove();
        });

        it('should insert the draft row when the hook unmounts', () => {
            const {result, unmount} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});

            unmount();

            expect(hoisted.insertRowMutate).toHaveBeenCalledWith(
                {input: {environmentId: '0', tableId: 'table-1', values: {title: 'new'}}},
                expect.any(Object)
            );
        });

        it('should insert the draft row when a boolean cell of it is toggled and the row is left', () => {
            hoisted.dataTable.columns = [{id: 'column-3', name: 'active', type: 'BOOLEAN'}];

            const {result} = renderUseDataTable();

            addDraftRow(result);

            const booleanColumn = result.current.gridColumns[1];
            const draftRow = getDraftRow(result.current.gridRowsWithAdd)!;

            render(<>{booleanColumn.renderCell?.({row: draftRow} as RenderCellProps<GridRowType, SummaryRowType>)}</>);

            act(() => {
                fireEvent.click(screen.getByRole('checkbox', {name: 'Toggle active'}));
            });

            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();

            moveActivePosition(result, 'row-1');

            expect(hoisted.insertRowMutate).toHaveBeenCalledWith(
                {input: {environmentId: '0', tableId: 'table-1', values: {active: true}}},
                expect.any(Object)
            );
        });

        it('should insert the draft row and add a new one when the add row cell is used again', async () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            addDraftRow(result);

            expect(hoisted.insertRowMutate).toHaveBeenCalledTimes(1);

            await act(async () => {
                await getInsertRowOptions().onSuccess({insertDataTableRow: {id: 'row-3'}});
            });

            expect(getDraftRow(result.current.gridRowsWithAdd)).toEqual({id: DRAFT_ROW_ID});
        });

        it('should replace an empty draft row with a new one when the add row cell is used again', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            addDraftRow(result);

            expect(hoisted.insertRowMutate).not.toHaveBeenCalled();
            expect(result.current.gridRowsWithAdd.filter((row) => row.id === DRAFT_ROW_ID)).toHaveLength(1);
        });

        it('should not insert the draft row again while its insert is pending', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            moveActivePosition(result, 'row-1');
            moveActivePosition(result, 'row-2');

            expect(hoisted.insertRowMutate).toHaveBeenCalledTimes(1);
        });

        it('should remove the draft row and update the inserted row with edits made during the insert', async () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            moveActivePosition(result, 'row-1');
            changeDraftRow(result, {count: 3});

            await act(async () => {
                await getInsertRowOptions().onSuccess({insertDataTableRow: {id: 'row-3'}});
            });

            expect(getDraftRow(result.current.gridRowsWithAdd)).toBeUndefined();
            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['dataTableRowsPage']});
            expect(hoisted.updateRowMutate).toHaveBeenCalledWith({
                input: {environmentId: '0', id: 'row-3', tableId: 'table-1', values: {count: 3}},
            });
        });

        it('should keep the draft row and insert it again after a failed insert when the row is left', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);
            changeDraftRow(result, {title: 'new'});
            moveActivePosition(result, 'row-1');

            act(() => {
                getInsertRowOptions().onError();
            });

            expect(getDraftRow(result.current.gridRowsWithAdd)).toEqual({id: DRAFT_ROW_ID, title: 'new'});

            moveActivePosition(result, 'row-2');

            expect(hoisted.insertRowMutate).toHaveBeenCalledTimes(2);
        });

        it('should exclude the draft row from the selected rows', () => {
            const {result} = renderUseDataTable();

            addDraftRow(result);

            act(() => {
                result.current.handleSelectedRowsChange(new Set(['row-1', DRAFT_ROW_ID]));
            });

            expect(result.current.selectedRows).toEqual(new Set(['row-1']));
        });
    });
});
