import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    EXECUTIONS_TABLE_CELL_CLASS_NAME,
    EXECUTIONS_TABLE_HEADER_CLASS_NAME,
    EXECUTIONS_TABLE_HEAD_CLASS_NAME,
    EXECUTIONS_TABLE_ROW_CLASS_NAME,
    EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME,
    PINNED_CELL_CLASS_NAME,
    PINNED_HEAD_CLASS_NAME,
    PINNED_STRIPED_CELL_CLASS_NAME,
} from '@/shared/components/workflow-executions/workflowExecutionsTableClassNames';
import {coreTableFeatures} from '@/shared/util/table-features';
import {ColumnDef, RowData, flexRender, useTable} from '@tanstack/react-table';
import {twMerge} from 'tailwind-merge';

interface EmbeddedWorkflowExecutionsTableContentProps<TData extends RowData> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    columns: ReadonlyArray<ColumnDef<typeof coreTableFeatures, TData, any>>;
    data: TData[];
    onRowClick: (index: number) => void;
}

const EmbeddedWorkflowExecutionsTableContent = <TData extends RowData>({
    columns,
    data,
    onRowClick,
}: EmbeddedWorkflowExecutionsTableContentProps<TData>) => {
    const reactTable = useTable({
        columns,
        data,
        features: coreTableFeatures,
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-full">
            <Table>
                <TableHeader className={EXECUTIONS_TABLE_HEADER_CLASS_NAME}>
                    {headerGroups.map((headerGroup) => (
                        <TableRow key={headerGroup.id}>
                            {headerGroup.headers.map((header, index) => (
                                <TableHead
                                    className={twMerge(
                                        EXECUTIONS_TABLE_HEAD_CLASS_NAME,
                                        header.column.id === 'actions' && PINNED_HEAD_CLASS_NAME
                                    )}
                                    key={`${headerGroup.id}_${header.id}_${index}`}
                                >
                                    {!header.isPlaceholder &&
                                        flexRender(header.column.columnDef.header, header.getContext())}
                                </TableHead>
                            ))}
                        </TableRow>
                    ))}
                </TableHeader>

                <TableBody>
                    {rows.map((row) => (
                        <TableRow
                            className={twMerge(
                                EXECUTIONS_TABLE_ROW_CLASS_NAME,
                                EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME
                            )}
                            key={row.id}
                            onClick={() => onRowClick(row.index)}
                        >
                            {row.getAllCells().map((cell, index) => (
                                <TableCell
                                    className={twMerge(
                                        EXECUTIONS_TABLE_CELL_CLASS_NAME,
                                        cell.column.id === 'actions' && PINNED_CELL_CLASS_NAME,
                                        cell.column.id === 'actions' && PINNED_STRIPED_CELL_CLASS_NAME
                                    )}
                                    key={`${row.id}_${cell.id}_${index}`}
                                >
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
};

export default EmbeddedWorkflowExecutionsTableContent;
