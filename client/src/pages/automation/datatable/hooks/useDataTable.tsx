import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    DataTable,
    DataTableRow,
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
    environmentId: string;
    isFetchingNextPage: boolean;
    isFirstNonFrozenColumn: boolean;
    onDeleteColumn: (columnId: string, columnName: string) => void;
    onRenameColumn: (columnId: string, columnName: string) => void;
    rowCount: number;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
    tableId: string;
    totalColumns: number;
    updateRowMutation: {
        mutate: (params: {
            input: {environmentId: string; id: string; tableId: string; values: Record<string, unknown>};
        }) => void;
    };
}

interface BuildTrailingColumnParamsI {
    hasColumns: boolean;
    isFetchingNextPage: boolean;
    rowCount: number;
    setAddColumnDialogOpen: (open: boolean) => void;
}

interface BuildGridColumnsParamsI {
    dataTable: DataTable | undefined;
    environmentId: string;
    handleAddRow: () => void;
    handleSelectedRowsChange: (next: ReadonlySet<string>) => void;
    handleToggleSelectAll: (nextChecked: boolean) => void;
    hoveredRowId: string | null;
    isFetchingNextPage: boolean;
    localRowCount: number;
    selectedRows: ReadonlySet<string>;
    setAddColumnDialogOpen: (open: boolean) => void;
    setColumnToDelete: (columnId: string, columnName: string) => void;
    setColumnToRename: (columnId: string, columnName: string) => void;
    setHoveredRowId: React.Dispatch<React.SetStateAction<string | null>>;
    setLocalRows: React.Dispatch<React.SetStateAction<GridRowType[]>>;
    updateRowMutation: {
        mutate: (params: {
            input: {environmentId: string; id: string; tableId: string; values: Record<string, unknown>};
        }) => void;
    };
}

function buildUserColumn({
    column,
    environmentId,
    isFetchingNextPage,
    isFirstNonFrozenColumn,
    onDeleteColumn,
    onRenameColumn,
    rowCount,
    setLocalRows,
    tableId,
    totalColumns,
    updateRowMutation,
}: BuildUserColumnParamsI): Column<GridRowType, SummaryRowType> {
    const baseCol = {
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

    let userCol: Column<GridRowType, SummaryRowType> = isFirstNonFrozenColumn
        ? {
              ...baseCol,
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
              ...baseCol,
          };

    if (column.type === 'BOOLEAN') {
        const BooleanRenderer = createBooleanCellRenderer({
            columnName: column.name,
            environmentId,
            setLocalRows,
            tableId,
            updateRowMutation,
        });

        userCol = {
            ...userCol,
            editable: false,
            renderCell: ({row}) => <BooleanRenderer row={row} />,
        } satisfies Column<GridRowType, SummaryRowType>;
    }

    if (column.type === 'STRING') {
        userCol = {
            ...userCol,
            renderEditCell: createStringEditCellRenderer(column.name),
        };
    }

    if (column.type === 'NUMBER' || column.type === 'INTEGER') {
        userCol = {
            ...userCol,
            renderEditCell: createNumberEditCellRenderer(column.name),
        };
    }

    if (column.type === 'DATE') {
        userCol = {
            ...userCol,
            renderCell: createDateCellRenderer(column.name),
            renderEditCell: createDateEditCellRenderer(column.name),
        };
    }

    if (column.type === 'DATE_TIME') {
        userCol = {
            ...userCol,
            renderCell: createDateTimeCellRenderer(column.name),
            renderEditCell: createDateTimeEditCellRenderer(column.name),
        };
    }

    return userCol;
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
    environmentId,
    handleAddRow,
    handleSelectedRowsChange,
    handleToggleSelectAll,
    hoveredRowId,
    isFetchingNextPage,
    localRowCount,
    selectedRows,
    setAddColumnDialogOpen,
    setColumnToDelete,
    setColumnToRename,
    setHoveredRowId,
    setLocalRows,
    updateRowMutation,
}: BuildGridColumnsParamsI): Column<GridRowType, SummaryRowType>[] {
    const cols: Column<GridRowType, SummaryRowType>[] = [];
    const totalCols = 1 + (dataTable?.columns?.length ?? 0) + 1;
    const selectedCount = selectedRows.size;
    const allSelected = localRowCount > 0 && selectedCount === localRowCount;
    const someSelected = selectedCount > 0 && selectedCount < localRowCount;

    // ID column
    cols.push({
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
                allSelected={allSelected}
                onToggleSelectAll={handleToggleSelectAll}
                someSelected={someSelected}
            />
        ),
        width: 80,
    });

    let summaryAssigned = false;

    (dataTable?.columns ?? []).forEach((column) => {
        if (!dataTable?.id) return;

        const userCol = buildUserColumn({
            column,
            environmentId,
            isFetchingNextPage,
            isFirstNonFrozenColumn: !summaryAssigned,
            onDeleteColumn: setColumnToDelete,
            onRenameColumn: setColumnToRename,
            rowCount: localRowCount,
            setLocalRows,
            tableId: dataTable.id,
            totalColumns: totalCols,
            updateRowMutation,
        });

        if (!summaryAssigned) {
            summaryAssigned = true;
        }

        cols.push(userCol);
    });

    // Trailing column with + in the header to add a column
    cols.push(
        buildTrailingColumn({
            hasColumns: (dataTable?.columns?.length ?? 0) > 0,
            isFetchingNextPage,
            rowCount: localRowCount,
            setAddColumnDialogOpen,
        })
    );

    return cols;
}

export const useDataTable = ({tableId}: UseDataTableParamsI) => {
    const [localRows, setLocalRows] = useState<GridRowType[]>([]);

    const {selectedRows, setSelectedRows} = useSelectedRowsStore();
    const {setColumnToDelete} = useDeleteDataTableColumnDialogStore();
    const {setColumnToRename} = useRenameDataTableColumnDialogStore();
    const {setOpen: setAddColumnDialogOpen} = useAddDataTableColumnDialogStore();
    const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    // Tables query
    const {
        data: tablesData,
        error: tablesError,
        isLoading: tablesLoading,
    } = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    // Current data table from store
    const {dataTable, setDataTable} = useCurrentDataTableStore();

    // Update store when table data changes
    useEffect(() => {
        const foundTable = tablesData?.dataTables?.find((table) => (tableId != null ? table.id === tableId : false));
        setDataTable(foundTable);
    }, [tablesData, tableId, setDataTable]);

    // Rows query with infinite scrolling
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

    // Infinite scrolling refs
    const fetchNextPageRef = useRef(fetchNextPage);
    const hasNextPageRef = useRef(hasNextPage);
    const isFetchingNextPageRef = useRef(isFetchingNextPage);

    const rows: DataTableRow[] = useMemo(
        () => pagesData?.pages.flatMap((page) => page.dataTableRowsPage.items) ?? [],
        [pagesData]
    );

    // Invalidate helper
    const invalidateRows = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});
    }, [queryClient]);

    // Mutations
    const insertRowMutation = useInsertDataTableRowMutation({
        onSuccess: invalidateRows,
    });

    const updateRowMutation = useUpdateDataTableRowMutation({
        onSuccess: invalidateRows,
    });

    // Source of truth for grid rows + synthetic last "+" row for adding new records
    const gridRowsWithAdd: GridRowType[] = useMemo(() => {
        return [...localRows, {id: '-1'} as GridRowType];
    }, [localRows]);

    const handleGridScroll = useCallback((event: UIEvent<HTMLDivElement>) => {
        const element = event.currentTarget;
        const nearBottom = element.scrollTop + element.clientHeight >= element.scrollHeight - 60;

        if (nearBottom && hasNextPageRef.current && !isFetchingNextPageRef.current) {
            fetchNextPageRef.current();
        }
    }, []);

    // Row change handler
    const handleRowsChange = useCallback(
        (newRows: GridRowType[], data: RowsChangeData<GridRowType, SummaryRowType>) => {
            if (data.indexes.length === 1) {
                const idx = data.indexes[0];
                const updated = newRows[idx];
                const original = localRows.find((row) => row.id === updated.id);

                if (!original) return;

                const changed: Record<string, unknown> = {};

                Object.keys(updated).forEach((key) => {
                    if (key === 'id') return;

                    if (updated[key] !== original[key]) {
                        changed[key] = updated[key];
                    }
                });

                if (Object.keys(changed).length > 0 && dataTable?.id) {
                    updateRowMutation.mutate({
                        input: {
                            environmentId: String(environmentId),
                            id: updated.id,
                            tableId: dataTable.id,
                            values: changed,
                        },
                    });
                }

                setLocalRows(newRows.filter((row) => row.id !== '-1'));
            }
        },
        [localRows, dataTable?.id, environmentId, updateRowMutation]
    );

    // Selection handler
    const handleSelectedRowsChange = useCallback(
        (next: ReadonlySet<string>) => {
            if (next.has('-1')) {
                const copy = new Set(next);
                copy.delete('-1');
                setSelectedRows(copy);
            } else {
                setSelectedRows(next);
            }
        },
        [setSelectedRows]
    );

    const handleToggleSelectAll = useCallback(
        (nextChecked: boolean) => {
            if (nextChecked) {
                const all = new Set<string>(localRows.map((row) => String(row.id)));
                setSelectedRows(all);
            } else {
                setSelectedRows(new Set<string>());
            }
        },
        [localRows, setSelectedRows]
    );

    // Action handlers
    const handleAddRow = useCallback(() => {
        if (!dataTable?.id) return;

        insertRowMutation.mutate({
            input: {environmentId: String(environmentId), tableId: dataTable.id, values: {}},
        });
    }, [dataTable?.id, environmentId, insertRowMutation]);

    // Build grid columns: ID + user columns + trailing header + add column
    const stringEnvironmentId = String(environmentId);
    const localRowCount = localRows.length;
    const gridColumns: Column<GridRowType, SummaryRowType>[] = useMemo(
        () =>
            buildGridColumns({
                dataTable,
                environmentId: stringEnvironmentId,
                handleAddRow,
                handleSelectedRowsChange,
                handleToggleSelectAll,
                hoveredRowId,
                isFetchingNextPage,
                localRowCount,
                selectedRows,
                setAddColumnDialogOpen,
                setColumnToDelete,
                setColumnToRename,
                setHoveredRowId,
                setLocalRows,
                updateRowMutation,
            }),
        [
            dataTable,
            stringEnvironmentId,
            handleAddRow,
            handleSelectedRowsChange,
            handleToggleSelectAll,
            hoveredRowId,
            isFetchingNextPage,
            localRowCount,
            selectedRows,
            setAddColumnDialogOpen,
            setColumnToDelete,
            setColumnToRename,
            updateRowMutation,
        ]
    );

    // Grid rows transformation
    const gridRows: GridRowType[] = useMemo(() => {
        return rows.map((row) => ({id: row.id, ...row.values}));
    }, [rows]);

    // Clear selected rows when switching to a different table to avoid stale selections
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
