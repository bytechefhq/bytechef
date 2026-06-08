import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {JobBasic, WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {CellContext, createColumnHelper, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {useShallow} from 'zustand/react/shallow';

import WorkflowExecutionsDropdownMenu from '../components/WorkflowExecutionsDropdownMenu';
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

export const useWorkflowExecutionsTable = (data: WorkflowExecution[]) => {
    const reactTable = useReactTable<WorkflowExecution>({
        columns: [
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
            columnHelper.accessor('projectDeployment', {
                cell: (info) => `${info.getValue()?.name}`,
                header: 'Deployment',
            }),
            columnHelper.accessor((row) => row.job, {
                cell: (info) => {
                    /* eslint-disable @typescript-eslint/no-explicit-any */
                    const metadata = info.getValue()?.metadata as {[key: string]: any} | undefined;
                    const pv = metadata?.projectVersion;

                    return pv != null ? `V${pv}` : '';
                },
                header: 'Version',
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
                header: 'Start date',
            }),
            columnHelper.accessor((row) => row.job, {
                cell: (info) => {
                    const {endDate} = info.getValue();

                    if (!endDate) {
                        return <></>;
                    }

                    return <>{`${endDate.toLocaleDateString()} ${endDate.toLocaleTimeString()}`}</>;
                },
                header: 'End date',
            }),
            columnHelper.accessor((row) => row.job, {
                cell: (info) => <WorkflowExecutionsDropdownMenu data={info} />,
                header: 'Action',
            }),
        ],
        data,
        getCoreRowModel: getCoreRowModel(),
    });

    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore(
        useShallow((state) => ({
            setWorkflowExecutionId: state.setWorkflowExecutionId,
            setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
        }))
    );

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setWorkflowExecutionId(data[index].id!);

            setWorkflowExecutionSheetOpen(true);
        }
    };

    return {
        handleRowClick,
        headerGroups,
        rows,
    };
};
