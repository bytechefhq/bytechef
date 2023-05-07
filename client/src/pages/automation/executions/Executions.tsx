import {QueueListIcon} from '@heroicons/react/24/outline';
import DatePicker from 'components/DatePicker/DatePicker';
import EmptyList from 'components/EmptyList/EmptyList';
import FilterableSelect, {
    ISelectOption,
} from 'components/FilterableSelect/FilterableSelect';
import PageFooter from 'components/PageFooter/PageFooter';
import PageHeader from 'components/PageHeader/PageHeader';
import PageLoader from 'components/PageLoader/PageLoader';
import Pagination from 'components/Pagination/Pagination';
import LayoutContainer from 'layouts/LayoutContainer/LayoutContainer';
import {
    WorkflowExecutionModel,
    WorkflowExecutionModelFromJSON,
} from 'middleware/automation/project';
import {GetWorkflowExecutionsJobStatusEnum} from 'middleware/automation/project/apis/WorkflowExecutionsApi';
import {
    useGetProjectInstancesQuery,
    useGetProjectsQuery,
    useGetWorkflowExecutionsQuery,
} from 'queries/projects.queries';
import {useGetWorkflowsQuery} from 'queries/workflows.queries';
import {useState} from 'react';
import {OnChangeValue} from 'react-select';
import {twMerge} from 'tailwind-merge';

import useWorkflowExecutionDetailsDialogStore from '../project/stores/useWorkflowExecutionDetailsDialogStore';
import ExecutionDetailsDialog from './components/ExecutionDetailsDialog';
import ExecutionsTable from './components/ExecutionsTable';

const jobStatusOptions = [
    {
        label: GetWorkflowExecutionsJobStatusEnum.Started,
        value: GetWorkflowExecutionsJobStatusEnum.Started,
    },
    {
        label: GetWorkflowExecutionsJobStatusEnum.Completed,
        value: GetWorkflowExecutionsJobStatusEnum.Completed,
    },
    {
        label: GetWorkflowExecutionsJobStatusEnum.Created,
        value: GetWorkflowExecutionsJobStatusEnum.Created,
    },
    {
        label: GetWorkflowExecutionsJobStatusEnum.Stopped,
        value: GetWorkflowExecutionsJobStatusEnum.Stopped,
    },
    {
        label: GetWorkflowExecutionsJobStatusEnum.Failed,
        value: GetWorkflowExecutionsJobStatusEnum.Failed,
    },
];

export const Executions = () => {
    const [filterStatus, setFilterStatus] =
        useState<GetWorkflowExecutionsJobStatusEnum>();
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

    const {data: projectInstances} = useGetProjectInstancesQuery({});

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsLoading,
    } = useGetProjectsQuery({});

    const {
        data: WorkflowExecutionsPage,
        error: WorkflowExecutionsError,
        isLoading: WorkflowExecutionsLoading,
    } = useGetWorkflowExecutionsQuery({
        jobStatus: filterStatus,
        projectId: filterProjectId,
        workflowId: filterWorkflowId,
        jobStartDate: filterStartDate,
        jobEndDate: filterEndDate,
        projectInstanceId: filterInstanceId,
        pageNumber: filterPageNumber,
    });

    const {data: workflows, error: workflowsError} = useGetWorkflowsQuery();

    const {workflowExecutionDetailsDialogOpen} =
        useWorkflowExecutionDetailsDialogStore();

    const emptyListMessage =
        !filterStatus &&
        !filterProjectId &&
        !filterWorkflowId &&
        !filterStartDate &&
        !filterEndDate &&
        !filterInstanceId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const tableData = WorkflowExecutionsPage?.content?.map(
        (WorkflowExecution: WorkflowExecutionModel) =>
            WorkflowExecutionModelFromJSON(WorkflowExecution)
    );

    return (
        <PageLoader
            errors={[projectsError, workflowsError]}
            loading={projectsLoading}
        >
            <LayoutContainer
                bodyClassName="bg-white"
                footer={
                    WorkflowExecutionsPage?.content &&
                    WorkflowExecutionsPage.content.length > 0 && (
                        <PageFooter position="main">
                            <Pagination
                                pageNumber={WorkflowExecutionsPage.number!}
                                pageSize={WorkflowExecutionsPage.size!}
                                totalElements={
                                    WorkflowExecutionsPage.totalElements!
                                }
                                totalPages={WorkflowExecutionsPage.totalPages!}
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
                                            value.value as GetWorkflowExecutionsJobStatusEnum
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

                            {projects?.length && (
                                <FilterableSelect
                                    isClearable
                                    label="Projects"
                                    name="project"
                                    options={projects?.map((project) => ({
                                        label: project.name,
                                        value: (project.id || 0).toString(),
                                    }))}
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

                            {workflows?.length && (
                                <FilterableSelect
                                    isClearable
                                    label="Workflows"
                                    name="workflows"
                                    options={workflows?.map((workflow) => ({
                                        label:
                                            workflow.label || 'undefined label',
                                        value: (workflow.id || 0).toString(),
                                    }))}
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

                            {projectInstances &&
                                projectInstances?.length > 0 && (
                                    <FilterableSelect
                                        isClearable
                                        label="Instances"
                                        name="instances"
                                        options={projectInstances?.map(
                                            (instance) => ({
                                                label: instance.name,
                                                value: (
                                                    instance.id || 0
                                                ).toString(),
                                            })
                                        )}
                                        onChange={(
                                            value: OnChangeValue<
                                                ISelectOption,
                                                false
                                            >
                                        ) =>
                                            value
                                                ? setFilterInstanceId(
                                                      Number(value.value)
                                                  )
                                                : setFilterInstanceId(undefined)
                                        }
                                    />
                                )}
                        </div>
                    </>
                }
            >
                {!WorkflowExecutionsLoading &&
                    !WorkflowExecutionsError &&
                    WorkflowExecutionsPage?.content && (
                        <div
                            className={twMerge(
                                'w-full px-4 2xl:mx-auto 2xl:w-4/5',
                                !WorkflowExecutionsPage.content.length &&
                                    'place-self-center'
                            )}
                        >
                            {tableData ? (
                                <ExecutionsTable data={tableData} />
                            ) : (
                                <EmptyList
                                    icon={
                                        <QueueListIcon className="h-12 w-12 text-gray-400" />
                                    }
                                    message={emptyListMessage}
                                    title="No executed workflows"
                                />
                            )}
                        </div>
                    )}

                {workflowExecutionDetailsDialogOpen && (
                    <ExecutionDetailsDialog />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Executions;
