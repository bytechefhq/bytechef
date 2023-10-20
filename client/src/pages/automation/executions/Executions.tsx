import {QueueListIcon} from '@heroicons/react/24/outline';
import {
    CellContext,
    createColumnHelper,
    flexRender,
    getCoreRowModel,
    useReactTable,
} from '@tanstack/react-table';
import FilterableSelect, {
    ISelectOption,
} from 'components/FilterableSelect/FilterableSelect';
import PageHeader from 'components/PageHeader/PageHeader';
import LayoutContainer from 'layouts/LayoutContainer/LayoutContainer';
import {
    JobBasicModel,
    ProjectExecutionModel,
    ProjectExecutionModelFromJSON,
} from 'middleware/project';
import {GetProjectExecutionsJobStatusEnum} from 'middleware/project/apis/ProjectExecutionsApi';
import {
    useGetProjectExecutionsQuery,
    useGetProjectInstancesQuery,
    useGetProjectsQuery,
} from 'queries/projects.queries';
import {useGetWorkflowsQuery} from 'queries/workflows.queries';
import {useState} from 'react';
import {OnChangeValue} from 'react-select';
import {twMerge} from 'tailwind-merge';

import Badge from '../../../components/Badge/Badge';
import DatePicker from '../../../components/DatePicker/DatePicker';
import EmptyList from '../../../components/EmptyList/EmptyList';
import PageFooter from '../../../components/PageFooter/PageFooter';
import PageLoader from '../../../components/PageLoader/PageLoader';
import Pagination from '../../../components/Pagination/Pagination';
import useExecutionDetailsDialogStore from '../project/stores/useExecutionDetailsDialogStore';
import ExecutionDetailsDialog from './ExecutionDetailsDialog';

const columnHelper = createColumnHelper<ProjectExecutionModel>();

const columns = [
    columnHelper.accessor((row) => row.job, {
        header: 'Status',
        cell: (info) => (
            <Badge
                color={
                    info.getValue()?.status === 'COMPLETED'
                        ? 'green'
                        : info.getValue()?.status === 'FAILED'
                        ? 'red'
                        : 'default'
                }
                text={info.getValue()?.status ?? ''}
            />
        ),
    }),
    columnHelper.accessor('project', {
        header: 'Project',
        cell: (info) => info.getValue()?.name,
    }),
    columnHelper.accessor('workflow', {
        header: 'Workflow',
        cell: (info) => info.getValue()?.label,
    }),
    columnHelper.accessor('instance', {
        header: 'Instance',
        cell: (info) => info.getValue()?.name,
    }),
    columnHelper.accessor((row) => row.job, {
        header: 'Duration',
        cell: (info) => getDuration(info),
    }),
    columnHelper.accessor((row) => row.job, {
        header: 'Completed date',
        cell: (info) => (
            <>
                {info.getValue()?.endDate &&
                    `${info.getValue()?.endDate?.toLocaleDateString()} ${info
                        .getValue()
                        ?.endDate?.toLocaleTimeString()}`}
            </>
        ),
    }),
];

const jobStatusOptions = [
    {
        label: GetProjectExecutionsJobStatusEnum.Started,
        value: GetProjectExecutionsJobStatusEnum.Started,
    },

    {
        label: GetProjectExecutionsJobStatusEnum.Completed,
        value: GetProjectExecutionsJobStatusEnum.Completed,
    },

    {
        label: GetProjectExecutionsJobStatusEnum.Created,
        value: GetProjectExecutionsJobStatusEnum.Created,
    },

    {
        label: GetProjectExecutionsJobStatusEnum.Stopped,
        value: GetProjectExecutionsJobStatusEnum.Stopped,
    },

    {
        label: GetProjectExecutionsJobStatusEnum.Failed,
        value: GetProjectExecutionsJobStatusEnum.Failed,
    },
];

function getDuration(
    info: CellContext<ProjectExecutionModel, JobBasicModel | undefined>
): string | undefined {
    const startDate = info.getValue()?.startDate?.getTime();
    const endDate = info.getValue()?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round((endDate - startDate) / 1000)}s`;
    } else {
        return undefined;
    }
}

export const Executions = () => {
    const [filterStatus, setFilterStatus] =
        useState<GetProjectExecutionsJobStatusEnum>();

    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        undefined
    );
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        undefined
    );
    const [filterProjectId, setFilterProjectId] = useState<number>();
    const [filterWorkflowId, setFilterWorkflowId] = useState<string>();
    const [filterInstanceId, setFilterInstanceId] = useState<number>();
    const [filterPageNumber, setFilterPageNumber] = useState<number>();

    const {
        data: projectInstances,
        error: projectInstancesError,
        isLoading: projectInstancesLoading,
    } = useGetProjectInstancesQuery({});

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsLoading,
    } = useGetProjectsQuery({});

    const {
        data: projectExecutionsPage,
        error: projectExecutionsError,
        isLoading: projectExecutionsLoading,
    } = useGetProjectExecutionsQuery({
        jobStatus: filterStatus,
        projectId: filterProjectId,
        workflowId: filterWorkflowId,
        jobStartDate: filterStartDate,
        jobEndDate: filterEndDate,
        projectInstanceId: filterInstanceId,
        pageNumber: filterPageNumber,
    });

    const {
        data: workflows,
        error: workflowsError,
        isLoading: workflowsLoading,
    } = useGetWorkflowsQuery();

    return (
        <PageLoader
            errors={[projectsError, workflowsError]}
            loading={projectsLoading}
        >
            <LayoutContainer
                bodyClassName="bg-white"
                footer={
                    projectExecutionsPage?.content &&
                    projectExecutionsPage.content.length > 0 && (
                        <PageFooter position="main">
                            <Pagination
                                pageNumber={projectExecutionsPage.number!}
                                pageSize={projectExecutionsPage.size!}
                                totalElements={
                                    projectExecutionsPage.totalElements!
                                }
                                totalPages={projectExecutionsPage.totalPages!}
                                onClick={setFilterPageNumber}
                            />
                        </PageFooter>
                    )
                }
                header={<PageHeader position="main" title="Executions" />}
                leftSidebarHeader={
                    <>
                        <PageHeader leftSidebar title="Execution History" />

                        <div className="px-4">
                            <FilterableSelect
                                isClearable
                                label="Status"
                                name="jobStatus"
                                options={jobStatusOptions}
                                onChange={(
                                    value: OnChangeValue<ISelectOption, false>
                                ) => {
                                    if (value) {
                                        setFilterStatus(
                                            value.value as GetProjectExecutionsJobStatusEnum
                                        );
                                    } else {
                                        setFilterStatus(undefined);
                                    }
                                }}
                            />

                            <DatePicker
                                label="Start date"
                                name="jobStartDate"
                                placeholder="Select..."
                                onChange={setFilterStartDate}
                            />

                            <DatePicker
                                label="End date"
                                name="jobEndDate"
                                placeholder="Select..."
                                onChange={setFilterEndDate}
                            />

                            {!projectsLoading && !projectsError && (
                                <FilterableSelect
                                    isClearable
                                    label="Projects"
                                    name="project"
                                    options={
                                        projects?.map((project) => ({
                                            label: project.name,
                                            value: (project.id || 0).toString(),
                                        })) || []
                                    }
                                    onChange={(
                                        value: OnChangeValue<
                                            ISelectOption,
                                            false
                                        >
                                    ) => {
                                        if (value) {
                                            setFilterProjectId(
                                                Number(value.value)
                                            );
                                        } else {
                                            setFilterProjectId(undefined);
                                        }
                                    }}
                                />
                            )}

                            {!workflowsLoading && !workflowsError && (
                                <FilterableSelect
                                    isClearable
                                    label="Workflows"
                                    name="workflows"
                                    options={
                                        workflows?.map((workflow) => ({
                                            label:
                                                workflow.label ||
                                                'undefined label',
                                            value: (
                                                workflow.id || 0
                                            ).toString(),
                                        })) || []
                                    }
                                    onChange={(
                                        value: OnChangeValue<
                                            ISelectOption,
                                            false
                                        >
                                    ) => {
                                        if (value) {
                                            setFilterWorkflowId(value.value);
                                        } else {
                                            setFilterWorkflowId(undefined);
                                        }
                                    }}
                                />
                            )}

                            {!projectInstancesLoading &&
                                !projectInstancesError && (
                                    <FilterableSelect
                                        isClearable
                                        label="Instances"
                                        name="instances"
                                        options={
                                            projectInstances?.map(
                                                (instance) => ({
                                                    label: instance.name,
                                                    value: (
                                                        instance.id || 0
                                                    ).toString(),
                                                })
                                            ) || []
                                        }
                                        onChange={(
                                            value: OnChangeValue<
                                                ISelectOption,
                                                false
                                            >
                                        ) => {
                                            if (value) {
                                                setFilterInstanceId(
                                                    Number(value.value)
                                                );
                                            } else {
                                                setFilterInstanceId(undefined);
                                            }
                                        }}
                                    />
                                )}
                        </div>
                    </>
                }
            >
                {!projectExecutionsLoading &&
                    !projectExecutionsError &&
                    projectExecutionsPage?.content && (
                        <div
                            className={twMerge(
                                'w-full px-4 2xl:mx-auto 2xl:w-4/5',
                                projectExecutionsPage.content.length === 0
                                    ? 'place-self-center'
                                    : ''
                            )}
                        >
                            {projectExecutionsPage.content.length === 0 ? (
                                <EmptyList
                                    icon={
                                        <QueueListIcon className="h-12 w-12 text-gray-400" />
                                    }
                                    message={
                                        !filterStatus &&
                                        !filterProjectId &&
                                        !filterWorkflowId &&
                                        !filterStartDate &&
                                        !filterEndDate &&
                                        !filterInstanceId &&
                                        !filterPageNumber
                                            ? "You don't have any executed workflows yet."
                                            : 'There is no executed workflows for the current criteria.'
                                    }
                                    title="No executed workflows"
                                />
                            ) : (
                                <Table
                                    data={projectExecutionsPage.content.map(
                                        (
                                            projectExecution: ProjectExecutionModel
                                        ) =>
                                            ProjectExecutionModelFromJSON(
                                                projectExecution
                                            )
                                    )}
                                />
                            )}
                        </div>
                    )}
            </LayoutContainer>
        </PageLoader>
    );
};

const Table = ({data}: {data: ProjectExecutionModel[]}): JSX.Element => {
    const table = useReactTable<ProjectExecutionModel>({
        data,
        columns,
        getCoreRowModel: getCoreRowModel(),
    });

    const [selectedItemId, setSelectedItemId] = useState<number | undefined>();

    const {setExecutionDetailsOpen} = useExecutionDetailsDialogStore();

    const handleRowClick = (index: number) => {
        if (data[index]?.id) {
            setSelectedItemId(data[index]?.id);
            setExecutionDetailsOpen(true);
        }
    };

    return (
        <>
            <table className="w-full divide-y divide-gray-300 bg-white text-sm">
                <thead>
                    {table.getHeaderGroups().map((headerGroup) => (
                        <tr key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <th
                                    key={header.id}
                                    className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
                                >
                                    {header.isPlaceholder
                                        ? null
                                        : flexRender(
                                              header.column.columnDef.header,
                                              header.getContext()
                                          )}
                                </th>
                            ))}
                        </tr>
                    ))}
                </thead>

                <tbody className="divide-y divide-gray-200 bg-white">
                    {table.getRowModel().rows.map((row) => (
                        <tr
                            key={row.id}
                            onClick={() => handleRowClick(row.index)}
                        >
                            {row.getVisibleCells().map((cell) => (
                                <td
                                    key={cell.id}
                                    className="whitespace-nowrap px-3 py-4 text-sm text-gray-900"
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

            {selectedItemId && (
                <ExecutionDetailsDialog selectedItemId={selectedItemId} />
            )}
        </>
    );
};

export default Executions;
