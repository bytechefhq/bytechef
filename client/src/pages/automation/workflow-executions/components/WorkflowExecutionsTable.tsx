import Button from '@/components/Button/Button';
import TablePagination from '@/components/TablePagination';
import {Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {ChevronDownIcon, ChevronUpIcon} from 'lucide-react';
import {Fragment, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import {useWorkflowExecutionsTable} from '../hooks/useWorkflowExecutionsTable';
import {
    MAX_SUBFLOW_DEPTH,
    formatDateTime,
    getSubflowChildJobs,
    hasExpandedSubflow,
} from '../utils/workflowExecutionsTable';
import WorkflowExecutionsDropdownMenu from './WorkflowExecutionsDropdownMenu';

interface ExecutionColumnI {
    cell: (execution: WorkflowExecution) => ReactNode;
    cellClassName?: string;
    header: (depth: number) => string;
    id: string;
}

const columns: ExecutionColumnI[] = [
    {
        cell: (execution) => <WorkflowExecutionBadge status={execution.job?.status || ''} />,
        header: () => 'Status',
        id: 'status',
    },
    {
        cell: (execution) => execution.job?.label,
        header: (depth) => (depth > 0 ? 'Subflow' : 'Workflow'),
        id: 'workflow',
    },
    {
        cell: (execution) => execution.project?.name,
        cellClassName: 'max-w-40 truncate',
        header: () => 'Project',
        id: 'project',
    },
    {
        cell: (execution) => execution.projectDeployment?.name,
        cellClassName: 'max-w-40 truncate',
        header: () => 'Deployment',
        id: 'deployment',
    },
    {
        cell: (execution) => {
            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
            const projectVersion = (execution.job?.metadata as {[key: string]: any} | undefined)?.projectVersion;

            return projectVersion != null ? `V${projectVersion}` : '';
        },
        header: () => 'Version',
        id: 'version',
    },
    {
        cell: (execution) => {
            const startDate = execution.job?.startDate?.getTime();
            const endDate = execution.job?.endDate?.getTime();

            if (startDate && endDate) {
                return `${Math.round(endDate - startDate)}ms`;
            }
        },
        header: () => 'Duration',
        id: 'duration',
    },
    {cell: (execution) => formatDateTime(execution.job?.startDate), header: () => 'Start date', id: 'startDate'},
    {
        cell: (execution) => (execution.job?.endDate ? formatDateTime(execution.job?.endDate) : null),
        header: () => 'End date',
        id: 'endDate',
    },
    {
        cell: (execution) => <WorkflowExecutionsDropdownMenu execution={execution} />,
        header: () => 'Actions',
        id: 'actions',
    },
];

const ExecutionTableHeader = ({className, depth = 0}: {className?: string; depth?: number}) => (
    <TableHeader
        className={twMerge(
            'border border-stroke-neutral-secondary bg-surface-main text-content-neutral-secondary',
            className
        )}
    >
        <TableRow>
            <TableHead className="w-9" />

            {columns.map((column) => (
                <TableHead className="w-4 text-sm font-medium text-inherit" key={column.id}>
                    {column.header(depth)}
                </TableHead>
            ))}
        </TableRow>
    </TableHeader>
);

interface ExecutionRowsProps {
    depth: number;
    executions: WorkflowExecution[];
    expandedJobIds: Set<string>;
    onRowClick: (execution: WorkflowExecution) => void;
    onToggleExpand: (jobId: string) => void;
    seenJobIds: Set<string>;
}

const ExecutionRows = ({
    depth,
    executions,
    expandedJobIds,
    onRowClick,
    onToggleExpand,
    seenJobIds,
}: ExecutionRowsProps) => (
    <>
        {executions.map((execution, index) => {
            const jobId = execution.job?.id;

            const nextSeenJobIds = jobId != null ? new Set(seenJobIds).add(jobId) : seenJobIds;

            const childJobs = getSubflowChildJobs({job: execution.job, seenJobIds: nextSeenJobIds});

            const expandable = depth < MAX_SUBFLOW_DEPTH && childJobs.length > 0;
            const expanded = jobId != null && expandedJobIds.has(jobId);

            const isDeepestExpandedSubflow =
                expandable &&
                expanded &&
                !hasExpandedSubflow({childJobs, depth: depth + 1, expandedJobIds, seenJobIds: nextSeenJobIds});

            return (
                <Fragment key={`${depth}_${jobId ?? index}`}>
                    <TableRow
                        className={twMerge(
                            'cursor-pointer border-0 hover:bg-surface-brand-secondary',
                            !expandable && 'even:bg-surface-neutral-secondary',
                            depth > 0 && 'border-b border-stroke-neutral-secondary',
                            expandable && 'border-b-0'
                        )}
                        onClick={() => onRowClick(execution)}
                    >
                        <TableCell className="w-9 py-4">
                            {expandable && (
                                <Button
                                    className={twMerge(
                                        expanded
                                            ? 'border-stroke-brand-secondary bg-surface-brand-secondary text-content-brand-primary'
                                            : 'border-stroke-neutral-secondary bg-surface-neutral-primary text-content-neutral-primary'
                                    )}
                                    icon={expanded ? <ChevronUpIcon /> : <ChevronDownIcon />}
                                    onClick={(event) => {
                                        event.stopPropagation();

                                        if (jobId != null) {
                                            onToggleExpand(jobId);
                                        }
                                    }}
                                    size="iconXxs"
                                    variant="outline"
                                />
                            )}
                        </TableCell>

                        {columns.map((column) => (
                            <TableCell
                                className={twMerge('py-4 whitespace-nowrap', column.cellClassName)}
                                key={column.id}
                            >
                                {column.cell(execution)}
                            </TableCell>
                        ))}
                    </TableRow>

                    {expandable && expanded && (
                        <TableRow className="border-0 even:bg-surface-neutral-primary">
                            <TableCell className="p-0" colSpan={columns.length + 1}>
                                <Table
                                    className={twMerge(
                                        'ml-9 border border-stroke-brand-secondary',
                                        depth > 0 && 'border-r-0 border-b-0'
                                    )}
                                >
                                    <ExecutionTableHeader
                                        className={twMerge(
                                            'border-stroke-brand-secondary text-content-neutral-secondary',
                                            isDeepestExpandedSubflow && 'text-content-brand-primary',
                                            expanded && 'border-r-0'
                                        )}
                                        depth={depth + 1}
                                    />

                                    <TableBody>
                                        <ExecutionRows
                                            depth={depth + 1}
                                            executions={childJobs.map((childJob) => ({
                                                id: execution.id,
                                                job: childJob,
                                                project: execution.project,
                                                projectDeployment: execution.projectDeployment,
                                                workflow: execution.workflow,
                                            }))}
                                            expandedJobIds={expandedJobIds}
                                            onRowClick={onRowClick}
                                            onToggleExpand={onToggleExpand}
                                            seenJobIds={nextSeenJobIds}
                                        />
                                    </TableBody>
                                </Table>
                            </TableCell>
                        </TableRow>
                    )}
                </Fragment>
            );
        })}
    </>
);

interface WorkflowExecutionsTableProps {
    onPaginationClick: (pageNumber: number) => void;
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    workflowExecutions: WorkflowExecution[];
}

const WorkflowExecutionsTable = ({
    onPaginationClick,
    pageNumber,
    pageSize,
    totalElements,
    totalPages,
    workflowExecutions,
}: WorkflowExecutionsTableProps) => {
    const {expandedJobIds, handleRowClick, handleToggleExpand} = useWorkflowExecutionsTable();

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-11/12">
            <Table>
                <ExecutionTableHeader />

                <TableBody>
                    <ExecutionRows
                        depth={0}
                        executions={workflowExecutions}
                        expandedJobIds={expandedJobIds}
                        onRowClick={handleRowClick}
                        onToggleExpand={handleToggleExpand}
                        seenJobIds={new Set()}
                    />
                </TableBody>

                <TableFooter className="bg-surface-neutral-primary">
                    <TableRow className="hover:bg-surface-neutral-primary">
                        <TableCell colSpan={columns.length}>
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
