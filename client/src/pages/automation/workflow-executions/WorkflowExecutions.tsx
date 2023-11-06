import EmptyList from '@/components/EmptyList/EmptyList';
import FilterableSelect, {
    ISelectOption,
} from '@/components/FilterableSelect/FilterableSelect';
import PageLoader from '@/components/PageLoader/PageLoader';
import Pagination from '@/components/Pagination/Pagination';
import {Button} from '@/components/ui/button';
import {Calendar} from '@/components/ui/calendar';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageFooter from '@/layouts/PageFooter';
import PageHeader from '@/layouts/PageHeader';
import {cn} from '@/lib/utils';
import {
    useGetWorkflowExecutionsQuery,
    useGetWorkflowsQuery,
} from '@/queries/executions';
import {useGetProjectInstancesQuery} from '@/queries/projectInstances.queries';
import {format} from 'date-fns';
import {ActivityIcon, CalendarIcon} from 'lucide-react';
import {
    GetWorkflowExecutionsJobStatusEnum,
    WorkflowExecutionModel,
    WorkflowExecutionModelFromJSON,
} from 'middleware/helios/execution';
import {useGetProjectsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import {OnChangeValue} from 'react-select';
import {twMerge} from 'tailwind-merge';

import WorkflowExecutionDetailsDialog from './components/WorkflowExecutionDetailsDialog';
import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';
import useWorkflowExecutionDetailsDialogStore from './stores/useWorkflowExecutionDetailsDialogStore';

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
const DatePicker = ({
    label,
    onChange,
}: {
    label: string;
    onChange: (date: Date | undefined) => void;
}) => {
    const [date, setDate] = useState<Date>();

    return (
        <fieldset className="mb-3">
            <Label>{label}</Label>

            <Popover>
                <PopoverTrigger asChild className="mt-1">
                    <Button
                        variant={'outline'}
                        className={cn(
                            'w-full justify-start text-left font-normal',
                            !date && 'text-muted-foreground'
                        )}
                    >
                        <CalendarIcon className="mr-2 h-4 w-4" />

                        {date ? format(date, 'PPP') : <span>Pick a date</span>}
                    </Button>
                </PopoverTrigger>

                <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                        mode="single"
                        selected={date}
                        onSelect={(date) => {
                            setDate(date);
                            onChange(date);
                        }}
                        initialFocus
                    />
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

const WorkflowExecutions = () => {
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
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        projectId: filterProjectId,
        projectInstanceId: filterInstanceId,
        workflowId: filterWorkflowId,
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
        (workflowExecutionModel: WorkflowExecutionModel) =>
            WorkflowExecutionModelFromJSON(workflowExecutionModel)
    );

    return (
        <PageLoader
            errors={[projectsError, workflowsError]}
            loading={projectsLoading}
        >
            <LayoutContainer
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
                header={
                    <PageHeader
                        centerTitle={true}
                        position="main"
                        title="All Workflow Executions"
                    />
                }
                leftSidebarHeader={
                    <>
                        <PageHeader position="sidebar" title="Executions" />

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
                                onChange={setFilterStartDate}
                            />

                            <DatePicker
                                label="Start date"
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
                            {tableData && tableData.length > 0 ? (
                                <WorkflowExecutionsTable data={tableData} />
                            ) : (
                                <EmptyList
                                    icon={
                                        <ActivityIcon className="h-12 w-12 text-gray-400" />
                                    }
                                    message={emptyListMessage}
                                    title="No executed workflows"
                                />
                            )}
                        </div>
                    )}

                {workflowExecutionDetailsDialogOpen && (
                    <WorkflowExecutionDetailsDialog />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default WorkflowExecutions;
