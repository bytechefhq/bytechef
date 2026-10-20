import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import EmbeddedWorkflowExecutionsDropdownMenu from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsDropdownMenu';
import {getConnectedUserExternalId} from '@/ee/pages/embedded/workflow-executions/utils/getConnectedUserExternalId';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
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
import {Job, WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {coreTableFeatures} from '@/shared/util/table-features';
import {CellContext, createColumnHelper, flexRender, useTable} from '@tanstack/react-table';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const getDuration = (info: CellContext<typeof coreTableFeatures, WorkflowExecution, Job | undefined>) => {
    const infoValue = info.getValue();

    const startDate = infoValue?.startDate?.getTime();
    const endDate = infoValue?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round(endDate - startDate)}ms`;
    }
};

const columnHelper = createColumnHelper<typeof coreTableFeatures, WorkflowExecution>();

const AutomationWorkflowExecutionsTable = ({data}: {data: WorkflowExecution[]}) => {
    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore(
        useShallow((state) => ({
            setWorkflowExecutionId: state.setWorkflowExecutionId,
            setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
        }))
    );

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionSheetOpen(true);
        }
    };

    const reactTable = useTable({
        columns: columnHelper.columns([
            columnHelper.accessor((row) => row.job, {
                cell: (info) => <WorkflowExecutionBadge status={info?.getValue()?.status || ''} />,
                header: 'Status',
            }),
            columnHelper.accessor('workflow', {
                cell: (info) => info.getValue()?.label,
                header: 'Workflow',
            }),
            columnHelper.accessor((row) => row.projectDeployment, {
                cell: (info) => getConnectedUserExternalId(info.getValue()?.name),
                header: 'Connected user',
                id: 'connectedUser',
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
            columnHelper.display({
                cell: ({row}) => (
                    <EmbeddedWorkflowExecutionsDropdownMenu
                        jobId={row.original.job?.id}
                        jobStatus={row.original.job?.status}
                        onView={() => handleRowClick(row.index)}
                    />
                ),
                header: 'Actions',
                id: 'actions',
            }),
        ]),
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
                            onClick={() => handleRowClick(row.index)}
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

export default AutomationWorkflowExecutionsTable;
