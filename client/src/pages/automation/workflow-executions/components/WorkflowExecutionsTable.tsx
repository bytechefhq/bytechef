import TablePagination from '@/components/TablePagination';
import {Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useWorkflowExecutionsTable} from '@/pages/automation/workflow-executions/hooks/useWorkflowExecutionsTable';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {flexRender} from '@tanstack/react-table';

interface WorkflowExecutionsTableProps {
    data: WorkflowExecution[];
    onPaginationClick: (pageNumber: number) => void;
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
}

const WorkflowExecutionsTable = ({
    data,
    onPaginationClick,
    pageNumber,
    pageSize,
    totalElements,
    totalPages,
}: WorkflowExecutionsTableProps) => {
    const {handleRowClick, headerGroups, rows} = useWorkflowExecutionsTable(data);

    const columnCount = headerGroups[0]?.headers.length ?? 0;

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            <Table>
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header, index) => (
                                <TableHead key={`${headerGroup.id}_${header.id}_${index}`}>
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
                            className="cursor-pointer border-b-border/50"
                            key={row.id}
                            onClick={() => handleRowClick(row.index)}
                        >
                            {row.getVisibleCells().map((cell, index) => (
                                <TableCell className="py-4 whitespace-nowrap" key={`${row.id}_${cell.id}_${index}`}>
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>

                <TableFooter className="bg-surface-neutral-primary">
                    <TableRow>
                        <TableCell colSpan={columnCount}>
                            <TablePagination
                                onClick={onPaginationClick}
                                pageNumber={pageNumber}
                                pageSize={pageSize}
                                totalElements={totalElements}
                                totalPages={totalPages}
                            />
                        </TableCell>
                    </TableRow>
                </TableFooter>
            </Table>
        </div>
    );
};

export default WorkflowExecutionsTable;
