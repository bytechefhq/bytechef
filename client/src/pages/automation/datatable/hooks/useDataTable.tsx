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
import {
    type CellKeyDownArgs,
    type CellKeyboardEvent,
    type CellMouseArgs,
    type CellMouseEvent,
    type ColSpanArgs,
    Column,
    type PositionChangeArgs,
    RowsChangeData,
} from 'react-data-grid';

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

export const ADD_ROW_ID = '-1';
export const DRAFT_ROW_ID = 'draft';

const POPOVER_CONTENT_SELECTOR = '[data-radix-popper-content-wrapper]';

function getDraftRowValues(draftRow: GridRowType): Record<string, unknown> {
    return Object.fromEntries(
        Object.entries(draftRow).filter(
            ([columnKey, value]) => columnKey !== 'id' && value !== undefined && value !== null && value !== ''
        )
    );
}

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
    isFirstNonFrozenColumn: boolean;
    onBooleanToggle: (rowId: string, columnName: string, value: boolean) => void;
    onDeleteColumn: (columnId: string, columnName: string) => void;
    onRenameColumn: (columnId: string, columnName: string) => void;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
    totalColumns: number;
}

interface BuildTrailingColumnParamsI {
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
    isFirstNonFrozenColumn,
    onBooleanToggle,
    onDeleteColumn,
    onRenameColumn,
    setLocalRows,
    totalColumns,
}: BuildUserColumnParamsI): Column<GridRowType, SummaryRowType> {
    const baseColumnConfig = {
        cellClass: 'datatable-cell',
        editable: (row: GridRowType) => row.id !== ADD_ROW_ID,
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
        renderSummaryCell: () => <BottomLoader isFetchingNextPage={isFetchingNextPage} rowCount={localRowCount} />,
        summaryCellClass: 'datatable-summary-total',
        width: 80,
    });

    let isSummaryRowAssigned = false;

    (dataTable?.columns ?? []).forEach((column) => {
        if (!dataTable?.id) return;

        const columnDefinition = buildUserColumn({
            column,
            isFirstNonFrozenColumn: !isSummaryRowAssigned,
            onBooleanToggle,
            onDeleteColumn: setColumnToDelete,
            onRenameColumn: setColumnToRename,
            setLocalRows,
            totalColumns: totalColumnCount,
        });

        if (!isSummaryRowAssigned) {
            isSummaryRowAssigned = true;
        }

        columns.push(columnDefinition);
    });

    // Trailing column with + in the header to add a column
    columns.push(buildTrailingColumn({setAddColumnDialogOpen}));

    return columns;
}

export const useDataTable = ({tableId}: UseDataTableParamsI) => {
    const [localRows, setLocalRows] = useState<GridRowType[]>([]);
    const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);
    const [draftRow, setDraftRow] = useState<GridRowType | null>(null);

    const commitDraftRowRef = useRef<() => void>(() => {});
    const draftGenerationRef = useRef(0);
    const draftRowRef = useRef<GridRowType | null>(null);
    const draftTableIdRef = useRef<string | undefined>(undefined);
    const gridContainerRef = useRef<HTMLDivElement>(null);
    const isAddRowRequestedRef = useRef(false);
    const isDraftInsertPendingRef = useRef(false);

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
        return [...localRows, ...(draftRow ? [draftRow] : []), {id: ADD_ROW_ID} as GridRowType];
    }, [draftRow, localRows]);

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

    const insertRowMutation = useInsertDataTableRowMutation();

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

    const clearDraftRow = useCallback(() => {
        draftGenerationRef.current += 1;
        draftRowRef.current = null;
        draftTableIdRef.current = undefined;
        isAddRowRequestedRef.current = false;
        isDraftInsertPendingRef.current = false;

        setDraftRow(null);
    }, []);

    const createDraftRow = useCallback((tableId: string) => {
        const newDraftRow: GridRowType = {id: DRAFT_ROW_ID};

        draftGenerationRef.current += 1;
        draftRowRef.current = newDraftRow;
        draftTableIdRef.current = tableId;

        setDraftRow(newDraftRow);
    }, []);

    const commitDraftRow = useCallback(() => {
        const currentDraftRow = draftRowRef.current;
        const tableId = draftTableIdRef.current;

        if (!currentDraftRow || !tableId || isDraftInsertPendingRef.current) {
            return;
        }

        const insertedValues = getDraftRowValues(currentDraftRow);
        const isAddRowRequested = isAddRowRequestedRef.current;

        if (Object.keys(insertedValues).length === 0) {
            clearDraftRow();

            if (isAddRowRequested) {
                createDraftRow(tableId);
            }

            return;
        }

        const draftGeneration = draftGenerationRef.current;

        isDraftInsertPendingRef.current = true;

        insertRowMutation.mutate(
            {input: {environmentId: String(environmentId), tableId, values: insertedValues}},
            {
                onError: () => {
                    if (draftGenerationRef.current === draftGeneration) {
                        isAddRowRequestedRef.current = false;
                        isDraftInsertPendingRef.current = false;
                    }
                },
                onSuccess: async (response) => {
                    await queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});

                    if (draftGenerationRef.current !== draftGeneration) {
                        return;
                    }

                    const insertedRowId = response.insertDataTableRow?.id;
                    const latestValues = draftRowRef.current ? getDraftRowValues(draftRowRef.current) : {};
                    const shouldAddRow = isAddRowRequestedRef.current;

                    const valuesEditedDuringInsert = Object.fromEntries(
                        Object.entries(latestValues).filter(([columnKey, value]) => insertedValues[columnKey] !== value)
                    );

                    clearDraftRow();

                    if (insertedRowId && Object.keys(valuesEditedDuringInsert).length > 0) {
                        updateRowMutation.mutate({
                            input: {
                                environmentId: String(environmentId),
                                id: insertedRowId,
                                tableId,
                                values: valuesEditedDuringInsert,
                            },
                        });
                    }

                    if (shouldAddRow) {
                        createDraftRow(tableId);
                    }
                },
            }
        );
    }, [clearDraftRow, createDraftRow, environmentId, insertRowMutation, queryClient, updateRowMutation]);

    const scheduleDraftRowCommit = useCallback(() => {
        const draftGeneration = draftGenerationRef.current;

        setTimeout(() => {
            if (draftGenerationRef.current === draftGeneration) {
                commitDraftRowRef.current();
            }
        }, 0);
    }, []);

    const handleDraftRowChange = useCallback((updatedDraftRow: GridRowType) => {
        draftRowRef.current = updatedDraftRow;

        setDraftRow(updatedDraftRow);
    }, []);

    const handleActivePositionChange = useCallback(
        ({row}: PositionChangeArgs<GridRowType, SummaryRowType>) => {
            if (!draftRowRef.current || row?.id === DRAFT_ROW_ID) {
                return;
            }

            scheduleDraftRowCommit();
        },
        [scheduleDraftRowCommit]
    );

    const handleRowsChange = useCallback(
        (updatedRows: GridRowType[], changeData: RowsChangeData<GridRowType, SummaryRowType>) => {
            if (changeData.indexes.length === 1) {
                const rowIndex = changeData.indexes[0];
                const updatedRow = updatedRows[rowIndex];

                if (updatedRow.id === DRAFT_ROW_ID) {
                    handleDraftRowChange(updatedRow);

                    return;
                }

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

                setLocalRows(updatedRows.filter((row) => row.id !== ADD_ROW_ID && row.id !== DRAFT_ROW_ID));
            }
        },
        [localRows, dataTable?.id, environmentId, handleDraftRowChange, updateRowMutation]
    );

    const handleSelectedRowsChange = useCallback(
        (nextSelectedRows: ReadonlySet<string>) => {
            if (nextSelectedRows.has(ADD_ROW_ID) || nextSelectedRows.has(DRAFT_ROW_ID)) {
                const filteredSelection = new Set(nextSelectedRows);

                filteredSelection.delete(ADD_ROW_ID);
                filteredSelection.delete(DRAFT_ROW_ID);
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

            if (rowId === DRAFT_ROW_ID) {
                handleDraftRowChange({...(draftRowRef.current ?? {id: DRAFT_ROW_ID}), [columnName]: value});

                return;
            }

            updateRowMutation.mutate({
                input: {
                    environmentId: String(environmentId),
                    id: rowId,
                    tableId: dataTable.id,
                    values: {[columnName]: value},
                },
            });
        },
        [dataTable?.id, environmentId, handleDraftRowChange, updateRowMutation]
    );

    const handleAddRow = useCallback(() => {
        if (!dataTable?.id) return;

        if (draftRowRef.current) {
            isAddRowRequestedRef.current = true;

            commitDraftRow();

            return;
        }

        createDraftRow(dataTable.id);
    }, [commitDraftRow, createDraftRow, dataTable?.id]);

    const handleCellClick = useCallback(
        ({row}: CellMouseArgs<GridRowType, SummaryRowType>, event: CellMouseEvent) => {
            if (row.id !== ADD_ROW_ID) {
                return;
            }

            event.preventGridDefault();

            handleAddRow();
        },
        [handleAddRow]
    );

    const handleCellKeyDown = useCallback(
        ({row}: CellKeyDownArgs<GridRowType, SummaryRowType>, event: CellKeyboardEvent) => {
            if (row?.id !== ADD_ROW_ID || event.key !== 'Enter') {
                return;
            }

            event.preventGridDefault();

            handleAddRow();
        },
        [handleAddRow]
    );

    const localRowCount = localRows.length;
    const hasDraftRow = draftRow !== null;

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
        commitDraftRowRef.current = commitDraftRow;
    }, [commitDraftRow]);

    useEffect(() => {
        return () => {
            commitDraftRowRef.current();

            clearDraftRow();
        };
    }, [tableId, clearDraftRow]);

    useEffect(() => {
        if (!hasDraftRow) {
            return;
        }

        const handleWindowMouseDown = (event: MouseEvent) => {
            const target = event.target;

            if (
                target instanceof Element &&
                (gridContainerRef.current?.contains(target) || target.closest(POPOVER_CONTENT_SELECTOR))
            ) {
                return;
            }

            scheduleDraftRowCommit();
        };

        window.addEventListener('mousedown', handleWindowMouseDown);

        return () => window.removeEventListener('mousedown', handleWindowMouseDown);
    }, [hasDraftRow, scheduleDraftRowCommit]);

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
        gridContainerRef,
        gridRowsWithAdd,
        handleActivePositionChange,
        handleCellClick,
        handleCellKeyDown,
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
