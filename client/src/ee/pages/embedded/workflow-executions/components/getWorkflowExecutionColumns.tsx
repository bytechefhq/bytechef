import EmbeddedWorkflowExecutionsDropdownMenu from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsDropdownMenu';
import {getJobDuration, getJobExecutionDate} from '@/ee/pages/embedded/workflow-executions/utils/jobTiming';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {coreTableFeatures} from '@/shared/util/table-features';
import {ColumnDef, RowData, createColumnHelper} from '@tanstack/react-table';

interface TimedExecutionI {
    endDate?: Date;
    startDate?: Date;
    status?: string;
}

interface ExecutionRowI {
    job?: TimedExecutionI & {
        id?: string;
    };
    triggerExecution?: TimedExecutionI;
    workflow?: {
        label?: string;
    };
}

interface GetWorkflowExecutionColumnsProps<TData extends RowData & ExecutionRowI> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    middleColumns: Array<ColumnDef<typeof coreTableFeatures, TData, any>>;
    onView: (index: number) => void;
}

export function getWorkflowExecutionColumns<TData extends RowData & ExecutionRowI>({
    middleColumns,
    onView,
}: GetWorkflowExecutionColumnsProps<TData>) {
    const columnHelper = createColumnHelper<typeof coreTableFeatures, TData>();

    const getTimedExecution = (row: TData): TimedExecutionI | undefined => row.job ?? row.triggerExecution;

    return [
        columnHelper.accessor((row) => getTimedExecution(row), {
            cell: (info) => <WorkflowExecutionBadge status={info.getValue()?.status || ''} />,
            header: 'Status',
            id: 'status',
        }),
        columnHelper.accessor((row) => row.workflow, {
            cell: (info) => info.getValue()?.label,
            header: 'Workflow',
            id: 'workflow',
        }),
        ...middleColumns,
        columnHelper.accessor((row) => getTimedExecution(row), {
            cell: (info) => getJobDuration(info.getValue()),
            header: 'Duration',
            id: 'duration',
        }),
        columnHelper.accessor((row) => getTimedExecution(row), {
            cell: (info) => getJobExecutionDate(info.getValue()),
            header: 'Execution date',
            id: 'executionDate',
        }),
        columnHelper.display({
            cell: ({row}) => (
                <EmbeddedWorkflowExecutionsDropdownMenu
                    jobId={row.original.job?.id}
                    jobStatus={row.original.job?.status}
                    onView={() => onView(row.index)}
                />
            ),
            header: 'Actions',
            id: 'actions',
        }),
    ];
}
