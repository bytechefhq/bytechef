import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import WorkflowExecutionBadge from '@/pages/platform/workflow-executions/components/WorkflowExecutionBadge';
import {JobBasic, WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {CellContext, createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

const getDuration = (info: CellContext<WorkflowExecution, JobBasic | undefined>) => {
    const infoValue = info.getValue();

    const startDate = infoValue?.startDate?.getTime();
    const endDate = infoValue?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round(endDate - startDate)}ms`;
    }
};

const columnHelper = createColumnHelper<WorkflowExecution>();

const columns = [
    columnHelper.accessor((row) => row.job, {
        cell: (info) => <WorkflowExecutionBadge status={info?.getValue()?.status || ''} />,
        header: 'Status',
    }),
    columnHelper.accessor('workflow', {
        cell: (info) => info.getValue()?.label,
        header: 'Workflow',
    }),
    columnHelper.accessor('project', {
        cell: (info) => info.getValue()?.name,
        header: 'Project',
    }),
    columnHelper.accessor('projectInstance', {
        cell: (info) => `${info.getValue()?.name}`,
        header: 'Instance',
    }),
    columnHelper.accessor('projectInstance', {
        cell: (info) => `V${info.getValue()?.projectVersion}`,
        header: 'Version',
    }),
    columnHelper.accessor('projectInstance', {
        cell: (info) => info.getValue()?.environment,
        header: 'Environment',
    }),
    columnHelper.accessor((row) => row.job, {
        cell: (info) => getDuration(info),
        header: 'Duration',
    }),
    columnHelper.accessor((row) => row.job, {
        cell: (info) => (
            <>
                {info.getValue()?.startDate &&
                    `${info.getValue()?.startDate?.toLocaleDateString()} ${info
                        .getValue()
                        ?.startDate?.toLocaleTimeString()}`}
            </>
        ),
        header: 'Execution date',
    }),
];

const WorkflowExecutionsTable = ({data}: {data: WorkflowExecution[]}) => {
    const reactTable = useReactTable<WorkflowExecution>({
        columns,
        data,
        getCoreRowModel: getCoreRowModel(),
    });

    const {setWorkflowExecutionDetailsSheetOpen, setWorkflowExecutionId} = useWorkflowExecutionSheetStore();

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionDetailsSheetOpen(true);
        }
    };

    return (
        <div className="w-full px-4 2xl:mx-auto 3xl:w-4/5">
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
                                <TableCell className="whitespace-nowrap py-4" key={`${row.id}_${cell.id}_${index}`}>
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

export default WorkflowExecutionsTable;
