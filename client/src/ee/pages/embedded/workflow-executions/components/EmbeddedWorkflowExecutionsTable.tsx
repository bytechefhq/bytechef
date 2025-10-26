import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {JobBasic, WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {CellContext, createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {useShallow} from 'zustand/react/shallow';

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

const EmbeddedWorkflowExecutionsTable = ({data}: {data: WorkflowExecution[]}) => {
    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore(
        useShallow((state) => ({
            setWorkflowExecutionId: state.setWorkflowExecutionId,
            setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
        }))
    );

    const {data: environmentsQuery} = useEnvironmentsQuery();

    const reactTable = useReactTable<WorkflowExecution>({
        columns: [
            columnHelper.accessor((row) => row.job, {
                cell: (info) => <WorkflowExecutionBadge status={info.getValue()?.status || ''} />,
                header: 'Status',
            }),
            columnHelper.accessor('workflow', {
                cell: (info) => info.getValue()?.label,
                header: 'Workflow',
            }),
            columnHelper.accessor('integration', {
                cell: (info) => info.getValue()?.componentName,
                header: 'Integration',
            }),
            columnHelper.accessor('integrationInstanceConfiguration', {
                cell: (info) => info.getValue()?.name,
                header: 'Instance',
            }),
            columnHelper.accessor('integrationInstanceConfiguration', {
                cell: (info) => `V${info.getValue()?.integrationVersion}`,
                header: 'Version',
            }),
            columnHelper.accessor('integrationInstance', {
                cell: (info) =>
                    environmentsQuery?.environments?.find(
                        (environment) => +environment!.id! === info.getValue()?.environmentId
                    )?.name,
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
        ],
        data,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionSheetOpen(true);
        }
    };

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            <Table className="table-auto">
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header, index) => (
                                <TableHead
                                    className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
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

export default EmbeddedWorkflowExecutionsTable;
