import {Badge} from '@/components/ui/badge';
import {
    CellContext,
    createColumnHelper,
    flexRender,
    getCoreRowModel,
    useReactTable,
} from '@tanstack/react-table';
import {
    JobBasicModel,
    WorkflowExecutionModel,
} from 'middleware/helios/execution';
import {twMerge} from 'tailwind-merge';

import useWorkflowExecutionDetailsDialogStore from '../stores/useWorkflowExecutionDetailsDialogStore';

const getDuration = (
    info: CellContext<WorkflowExecutionModel, JobBasicModel | undefined>
) => {
    const infoValue = info.getValue();

    const startDate = infoValue?.startDate?.getTime();
    const endDate = infoValue?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round(endDate - startDate)}ms`;
    }
};

const columnHelper = createColumnHelper<WorkflowExecutionModel>();

const columns = [
    columnHelper.accessor((row) => row.job, {
        cell: (info) => (
            <Badge
                className={twMerge(
                    info.getValue()?.status === 'COMPLETED' &&
                        'bg-success text-success-foreground hover:bg-success'
                )}
                variant={
                    info.getValue()?.status === 'COMPLETED'
                        ? 'destructive'
                        : 'secondary'
                }
            >
                {info.getValue()?.status ?? ''}
            </Badge>
        ),
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
        cell: (info) => info.getValue()?.name,
        header: 'Instance',
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

const WorkflowExecutionsTable = ({data}: {data: WorkflowExecutionModel[]}) => {
    const reactTable = useReactTable<WorkflowExecutionModel>({
        columns,
        data,
        getCoreRowModel: getCoreRowModel(),
    });

    const {setWorkflowExecutionDetailsDialogOpen, setWorkflowExecutionId} =
        useWorkflowExecutionDetailsDialogStore();

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionDetailsDialogOpen(true);
        }
    };

    return (
        <table className="w-full divide-y divide-gray-300 bg-white text-sm">
            <thead>
                {headerGroups.map((headerGroup) => (
                    <tr key={headerGroup.id}>
                        {headerGroup.headers.map((header) => (
                            <th
                                className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
                                key={header.id}
                            >
                                {!header.isPlaceholder &&
                                    flexRender(
                                        header.column.columnDef.header,
                                        header.getContext()
                                    )}
                            </th>
                        ))}
                    </tr>
                ))}
            </thead>

            <tbody className="divide-y divide-gray-200 bg-white">
                {rows.map((row) => (
                    <tr
                        className="cursor-pointer"
                        key={row.id}
                        onClick={() => handleRowClick(row.index)}
                    >
                        {row.getVisibleCells().map((cell) => (
                            <td
                                className="whitespace-nowrap px-3 py-4 text-sm"
                                key={cell.id}
                            >
                                {flexRender(
                                    cell.column.columnDef.cell,
                                    cell.getContext()
                                )}
                            </td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </table>
    );
};

export default WorkflowExecutionsTable;
