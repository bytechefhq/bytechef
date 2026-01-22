import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    DataTable,
    useDataTablesQuery,
    useInsertDataTableRowMutation,
    useUpdateDataTableRowMutation,
} from '@/shared/middleware/graphql';
import {useDataTableRowsInfiniteQuery} from '@/shared/queries/automation/datatables.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Plus} from 'lucide-react';
import {Dispatch, SetStateAction, type UIEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {type ColSpanArgs, Column, RowsChangeData} from 'react-data-grid';

import BottomLoader from '../components/BottomLoader';
import {
    type GridRowType,
    type SummaryRowType,
    createBooleanCellRenderer,
    createDateCellRenderer,
    createDateEditCellRenderer,
    createDateTimeCellRenderer,
    createDateTimeEditCellRenderer,
    createNumberEditCellRenderer,
    createStringEditCellRenderer,
} from '../components/cell-renderers';
import ColumnHeaderCell from '../components/cells/ColumnHeaderCell';
import RowIdCell from '../components/cells/RowIdCell';
import SelectAllHeaderCell from '../components/cells/SelectAllHeaderCell';
import {useAddDataTableColumnDialogStore} from '../stores/useAddDataTableColumnDialogStore';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import {useDeleteDataTableColumnDialogStore} from '../stores/useDeleteDataTableColumnDialogStore';
import {useRenameDataTableColumnDialogStore} from '../stores/useRenameDataTableColumnDialogStore';
import {useSelectedRowsStore} from '../stores/useSelectedRowsStore';

const PAGE_SIZE = 500;
const DEFAULT_COLUMN_WIDTH = 180;

interface UseDataTableParamsI {
    tableId: string | undefined;
}

interface ColumnDefinitionI {
    id: string;
    name: string;
    type: string;
}

interface BuildUserColumnParamsI {
    column: ColumnDefinitionI;
    isFetchingNextPage: boolean;
    isFirstNonFrozenColumn: boolean;
    onBooleanToggle: (rowId: string, columnName: string, value: boolean) => void;
    onDeleteColumn: (columnId: string, columnName: string) => void;
    onRenameColumn: (columnId: string, columnName: string) => void;
    rowCount: number;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
    totalColumns: number;
}

interface BuildTrailingColumnParamsI {
    hasColumns: boolean;
    isFetchingNextPage: boolean;
    rowCount: number;
    setAddColumnDialogOpen: (open: boolean) => void;
}

interface BuildGridColumnsParamsI {
    dataTable: DataTable | undefined;
    handleAddRow: () => void;
    handleSelectedRowsChange: (next: ReadonlySet<string>) => void;
    handleToggleSelectAll: (nextChecked: boolean) => void;
    hoveredRowId: string | null;
    isFetchingNextPage: boolean;
    localRowCount: number;
    onBooleanToggle: (rowId: string, columnName: string, value: boolean) => void;
    selectedRows: ReadonlySet<string>;
    setAddColumnDialogOpen: (open: boolean) => void;
    setColumnToDelete: (columnId: string, columnName: string) => void;
    setColumnToRename: (columnId: string, columnName: string) => void;
    setHoveredRowId: Dispatch<SetStateAction<string | null>>;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
}

function buildUserColumn({
    column,
    isFetchingNextPage,
    isFirstNonFrozenColumn,
    onBooleanToggle,
    onDeleteColumn,
    onRenameColumn,
    rowCount,
    setLocalRows,
    totalColumns,
}: BuildUserColumnParamsI): Column<GridRowType, SummaryRowType> {
    const baseColumnConfig = {
        cellClass: 'datatable-cell',
        editable: (row: GridRowType) => row.id !== '-1',
        headerCellClass: 'datatable-cell',
        key: column.name,
        name: column.name,
        renderHeaderCell: () => (
            <ColumnHeaderCell
                columnId={column.id}
                columnName={column.name}
                onDelete={onDeleteColumn}
                onRename={onRenameColumn}
            />
        ),
        resizable: true,
        width: DEFAULT_COLUMN_WIDTH,
    } satisfies Omit<Column<GridRowType, SummaryRowType>, 'colSpan' | 'renderSummaryCell'>;

    let columnDefinition: Column<GridRowType, SummaryRowType> = isFirstNonFrozenColumn
        ? {
              ...baseColumnConfig,
              colSpan: (args: ColSpanArgs<GridRowType, SummaryRowType>) => {
                  if (args.type === 'SUMMARY') {
                      return (totalColumns - 2) as number;
                  }

                  return undefined;
              },
              renderSummaryCell: () => <BottomLoader isFetchingNextPage={isFetchingNextPage} rowCount={rowCount} />,
              summaryCellClass: 'datatable-summary-row',
          }
        : {
              ...baseColumnConfig,
          };

    if (column.type === 'BOOLEAN') {
        const BooleanRenderer = createBooleanCellRenderer({
            columnName: column.name,
            onToggle: onBooleanToggle,
            setLocalRows,
        });

        columnDefinition = {
            ...columnDefinition,
            editable: false,
            renderCell: ({row}) => <BooleanRenderer row={row} />,
        } satisfies Column<GridRowType, SummaryRowType>;
    }

    if (column.type === 'STRING') {
        columnDefinition = {
            ...columnDefinition,
            renderEditCell: createStringEditCellRenderer(column.name),
        };
    }

    if (column.type === 'NUMBER' || column.type === 'INTEGER') {
        columnDefinition = {
            ...columnDefinition,
            renderEditCell: createNumberEditCellRenderer(column.name),
        };
    }

    if (column.type === 'DATE') {
        columnDefinition = {
            ...columnDefinition,
            renderCell: createDateCellRenderer(column.name),
            renderEditCell: createDateEditCellRenderer(column.name),
        };
    }

    if (column.type === 'DATE_TIME') {
        columnDefinition = {
            ...columnDefinition,
            renderCell: createDateTimeCellRenderer(column.name),
            renderEditCell: createDateTimeEditCellRenderer(column.name),
        };
    }

    return columnDefinition;
}

function buildTrailingColumn({
    hasColumns,
    isFetchingNextPage,
    rowCount,
    setAddColumnDialogOpen,
}: BuildTrailingColumnParamsI): Column<GridRowType, SummaryRowType> {
    return {
        key: '__add__',
        name: '',
        renderHeaderCell: () => (
            <div className="flex items-center justify-center">
                <Button
                    icon={<Plus className="h-4 w-4" />}
                    onClick={() => setAddColumnDialogOpen(true)}
                    size="icon"
                    title="Add column"
                    variant="ghost"
                ></Button>
            </div>
        ),
        renderSummaryCell: !hasColumns
            ? () => <BottomLoader isFetchingNextPage={isFetchingNextPage} rowCount={rowCount} />
            : undefined,
        resizable: false,
        width: 40,
    };
}

function buildGridColumns({
    dataTable,
    handleAddRow,
    handleSelectedRowsChange,
    handleToggleSelectAll,
    hoveredRowId,
    isFetchingNextPage,
    localRowCount,
    onBooleanToggle,
    selectedRows,
    setAddColumnDialogOpen,
    setColumnToDelete,
    setColumnToRename,
    setHoveredRowId,
    setLocalRows,
}: BuildGridColumnsParamsI): Column<GridRowType, SummaryRowType>[] {
    const columns: Column<GridRowType, SummaryRowType>[] = [];
    const totalColumnCount = 1 + (dataTable?.columns?.length ?? 0) + 1;
    const selectedCount = selectedRows.size;
    const isAllSelected = localRowCount > 0 && selectedCount === localRowCount;
    const isSomeSelected = selectedCount > 0 && selectedCount < localRowCount;

    columns.push({
        frozen: true,
        key: 'id',
        name: 'id',
        renderCell: ({row, rowIdx}) => (
            <RowIdCell
                hoveredRowId={hoveredRowId}
                onAddRow={handleAddRow}
                onSelectedRowsChange={handleSelectedRowsChange}
                row={row as GridRowType}
                rowIdx={rowIdx}
                selectedRows={selectedRows}
                setHoveredRowId={setHoveredRowId}
            />
        ),
        renderHeaderCell: () => (
            <SelectAllHeaderCell
                allSelected={isAllSelected}
                onToggleSelectAll={handleToggleSelectAll}
                someSelected={isSomeSelected}
            />
        ),
        width: 80,
    });

    let isSummaryRowAssigned = false;

    (dataTable?.columns ?? []).forEach((column) => {
        if (!dataTable?.id) return;

        const columnDefinition = buildUserColumn({
            column,
            isFetchingNextPage,
            isFirstNonFrozenColumn: !isSummaryRowAssigned,
            onBooleanToggle,
            onDeleteColumn: setColumnToDelete,
            onRenameColumn: setColumnToRename,
            rowCount: localRowCount,
            setLocalRows,
            totalColumns: totalColumnCount,
        });

        if (!isSummaryRowAssigned) {
            isSummaryRowAssigned = true;
        }

        columns.push(columnDefinition);
    });

    // Trailing column with + in the header to add a column
    columns.push(
        buildTrailingColumn({
            hasColumns: (dataTable?.columns?.length ?? 0) > 0,
            isFetchingNextPage,
            rowCount: localRowCount,
            setAddColumnDialogOpen,
        })
    );

    return columns;
}

export const useDataTable = ({tableId}: UseDataTableParamsI) => {
    const [localRows, setLocalRows] = useState<GridRowType[]>([]);
    const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);

    const {setOpen: setAddColumnDialogOpen} = useAddDataTableColumnDialogStore();
    const {dataTable, setDataTable} = useCurrentDataTableStore();
    const {setColumnToDelete} = useDeleteDataTableColumnDialogStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setColumnToRename} = useRenameDataTableColumnDialogStore();
    const {selectedRows, setSelectedRows} = useSelectedRowsStore();
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data: pagesData,
        error: rowsError,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        isLoading: rowsLoading,
    } = useDataTableRowsInfiniteQuery(
        {environmentId: String(environmentId), limit: PAGE_SIZE, tableId: dataTable?.id ?? ''},
        {enabled: Boolean(dataTable && dataTable.id != null)}
    );

    const fetchNextPageRef = useRef(fetchNextPage);
    const hasNextPageRef = useRef(hasNextPage);
    const isFetchingNextPageRef = useRef(isFetchingNextPage);

    const gridRows: GridRowType[] = useMemo(() => {
        const fetchedRows = pagesData?.pages.flatMap((page) => page.dataTableRowsPage.items) ?? [];

        // Spread row.values first, then assign id to prevent user column named "id" from overwriting the row identifier
        return fetchedRows.map((row) => ({...row.values, id: row.id}));
    }, [pagesData]);

    const gridRowsWithAdd: GridRowType[] = useMemo(() => {
        return [...localRows, {id: '-1'} as GridRowType];
    }, [localRows]);

    const queryClient = useQueryClient();

    const {
        data: tablesData,
        error: tablesError,
        isLoading: tablesLoading,
    } = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const invalidateRows = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});
    }, [queryClient]);

    const insertRowMutation = useInsertDataTableRowMutation({
        onSuccess: invalidateRows,
    });

    const updateRowMutation = useUpdateDataTableRowMutation({
        onSuccess: invalidateRows,
    });

    const handleGridScroll = useCallback((event: UIEvent<HTMLDivElement>) => {
        const scrollContainer = event.currentTarget;
        const isNearBottom =
            scrollContainer.scrollTop + scrollContainer.clientHeight >= scrollContainer.scrollHeight - 60;

        if (isNearBottom && hasNextPageRef.current && !isFetchingNextPageRef.current) {
            fetchNextPageRef.current();
        }
    }, []);

    const handleRowsChange = useCallback(
        (updatedRows: GridRowType[], changeData: RowsChangeData<GridRowType, SummaryRowType>) => {
            if (changeData.indexes.length === 1) {
                const rowIndex = changeData.indexes[0];
                const updatedRow = updatedRows[rowIndex];
                const originalRow = localRows.find((row) => row.id === updatedRow.id);

                if (!originalRow) {
                    return;
                }

                const changedValues: Record<string, unknown> = {};

                Object.keys(updatedRow).forEach((columnKey) => {
                    if (columnKey === 'id') return;

                    if (updatedRow[columnKey] !== originalRow[columnKey]) {
                        changedValues[columnKey] = updatedRow[columnKey];
                    }
                });

                if (Object.keys(changedValues).length > 0 && dataTable?.id) {
                    updateRowMutation.mutate({
                        input: {
                            environmentId: String(environmentId),
                            id: updatedRow.id,
                            tableId: dataTable.id,
                            values: changedValues,
                        },
                    });
                }

                setLocalRows(updatedRows.filter((row) => row.id !== '-1'));
            }
        },
        [localRows, dataTable?.id, environmentId, updateRowMutation]
    );

    const handleSelectedRowsChange = useCallback(
        (nextSelectedRows: ReadonlySet<string>) => {
            if (nextSelectedRows.has('-1')) {
                const filteredSelection = new Set(nextSelectedRows);

                filteredSelection.delete('-1');
                setSelectedRows(filteredSelection);
            } else {
                setSelectedRows(nextSelectedRows);
            }
        },
        [setSelectedRows]
    );

    const handleToggleSelectAll = useCallback(
        (isChecked: boolean) => {
            if (isChecked) {
                const allRowIds = new Set<string>(localRows.map((row) => String(row.id)));

                setSelectedRows(allRowIds);
            } else {
                setSelectedRows(new Set<string>());
            }
        },
        [localRows, setSelectedRows]
    );

    const handleBooleanToggle = useCallback(
        (rowId: string, columnName: string, value: boolean) => {
            if (!dataTable?.id) return;

            updateRowMutation.mutate({
                input: {
                    environmentId: String(environmentId),
                    id: rowId,
                    tableId: dataTable.id,
                    values: {[columnName]: value},
                },
            });
        },
        [dataTable?.id, environmentId, updateRowMutation]
    );

    const handleAddRow = useCallback(() => {
        if (!dataTable?.id) return;

        insertRowMutation.mutate({
            input: {environmentId: String(environmentId), tableId: dataTable.id, values: {}},
        });
    }, [dataTable?.id, environmentId, insertRowMutation]);

    const localRowCount = localRows.length;

    const gridColumns: Column<GridRowType, SummaryRowType>[] = useMemo(
        () =>
            buildGridColumns({
                dataTable,
                handleAddRow,
                handleSelectedRowsChange,
                handleToggleSelectAll,
                hoveredRowId,
                isFetchingNextPage,
                localRowCount,
                onBooleanToggle: handleBooleanToggle,
                selectedRows,
                setAddColumnDialogOpen,
                setColumnToDelete,
                setColumnToRename,
                setHoveredRowId,
                setLocalRows,
            }),
        [
            dataTable,
            handleAddRow,
            handleBooleanToggle,
            handleSelectedRowsChange,
            handleToggleSelectAll,
            hoveredRowId,
            isFetchingNextPage,
            localRowCount,
            selectedRows,
            setAddColumnDialogOpen,
            setColumnToDelete,
            setColumnToRename,
        ]
    );

    useEffect(() => {
        const selectedTable = tablesData?.dataTables?.find((tableItem) => tableId != null && tableItem.id === tableId);

        setDataTable(selectedTable);
    }, [tablesData, tableId, setDataTable]);

    useEffect(() => {
        handleSelectedRowsChange(new Set());
    }, [tableId, handleSelectedRowsChange]);

    useEffect(() => {
        setLocalRows(gridRows);
    }, [gridRows, tableId]);

    useEffect(() => {
        fetchNextPageRef.current = fetchNextPage;
    }, [fetchNextPage]);

    useEffect(() => {
        hasNextPageRef.current = hasNextPage;
    }, [hasNextPage]);

    useEffect(() => {
        isFetchingNextPageRef.current = isFetchingNextPage;
    }, [isFetchingNextPage]);

    return {
        gridColumns,
        gridRowsWithAdd,
        handleGridScroll,
        handleRowsChange,
        handleSelectedRowsChange,
        rowsError,
        rowsLoading,
        selectedRows,
        tablesError,
        tablesLoading,
    };
};
