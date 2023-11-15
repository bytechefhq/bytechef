import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import EmptyList from '@/components/EmptyList/EmptyList';
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
import {ProjectModel} from '@/middleware/helios/configuration';
import {useGetProjectInstancesQuery} from '@/queries/projectInstances.queries';
import {useGetWorkflowExecutionsQuery} from '@/queries/workflowExecutions.queries';
import {useGetWorkflowsQuery} from '@/queries/workflows.queries';
import {format} from 'date-fns';
import {ActivityIcon, CalendarIcon} from 'lucide-react';
import {
    GetWorkflowExecutionsJobStatusEnum,
    WorkflowExecutionModel,
    WorkflowExecutionModelFromJSON,
} from 'middleware/helios/execution';
import {useGetProjectsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowExecutionDetailsSheet from './components/WorkflowExecutionDetailsSheet';
import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';

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
                        className={cn(
                            'w-full justify-start text-left font-normal',
                            !date && 'text-muted-foreground'
                        )}
                        variant={'outline'}
                    >
                        <CalendarIcon className="mr-2 h-4 w-4" />

                        {date ? format(date, 'PPP') : <span>Pick a date</span>}
                    </Button>
                </PopoverTrigger>

                <PopoverContent align="start" className="w-auto p-0">
                    <Calendar
                        initialFocus
                        mode="single"
                        onSelect={(date) => {
                            setDate(date);
                            onChange(date);
                        }}
                        selected={date}
                    />
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

const ProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">
            {project?.tags?.map((tag) => tag.name).join(', ')}
        </span>
    </div>
);

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
                                onClick={setFilterPageNumber}
                                pageNumber={WorkflowExecutionsPage.number!}
                                pageSize={WorkflowExecutionsPage.size!}
                                totalElements={
                                    WorkflowExecutionsPage.totalElements!
                                }
                                totalPages={WorkflowExecutionsPage.totalPages!}
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
                            <ComboBox
                                items={jobStatusOptions}
                                label="Status"
                                name="jobStatus"
                                onChange={(item?: ComboBoxItemType) => {
                                    if (item) {
                                        setFilterStatus(
                                            item.value as GetWorkflowExecutionsJobStatusEnum
                                        );
                                    } else {
                                        setFilterStatus(undefined);
                                    }
                                }}
                                value={filterStatus}
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
                                <ComboBox
                                    items={projects?.map((project) => ({
                                        label: (
                                            <ProjectLabel project={project} />
                                        ),
                                        value: project.id,
                                    }))}
                                    label="Project"
                                    name="project"
                                    onChange={(value) => {
                                        if (value) {
                                            setFilterProjectId(
                                                Number(value.value)
                                            );
                                        } else {
                                            setFilterProjectId(undefined);
                                        }
                                    }}
                                    value={filterProjectId}
                                />
                            )}

                            {projectInstances &&
                                projectInstances?.length > 0 && (
                                    <ComboBox
                                        items={projectInstances?.map(
                                            (projectInstance) => ({
                                                label: (
                                                    <span className="flex items-center">
                                                        <span className="mr-1 ">
                                                            {
                                                                projectInstance.name
                                                            }
                                                        </span>

                                                        <span className="text-xs text-gray-500">
                                                            {projectInstance?.tags
                                                                ?.map(
                                                                    (tag) =>
                                                                        tag.name
                                                                )
                                                                .join(', ')}
                                                        </span>
                                                    </span>
                                                ),
                                                value: projectInstance.id,
                                            })
                                        )}
                                        label="Instance"
                                        name="instance"
                                        onChange={(value) =>
                                            value
                                                ? setFilterInstanceId(
                                                      Number(value.value)
                                                  )
                                                : setFilterInstanceId(undefined)
                                        }
                                        value={filterInstanceId}
                                    />
                                )}

                            {workflows?.length && (
                                <ComboBox
                                    items={workflows?.map((workflow) => ({
                                        label:
                                            workflow.label || 'undefined label',
                                        value: workflow.id,
                                    }))}
                                    label="Workflow"
                                    maxHeight
                                    name="workflow"
                                    onChange={(value) => {
                                        if (value) {
                                            setFilterWorkflowId(value.value);
                                        } else {
                                            setFilterWorkflowId(undefined);
                                        }
                                    }}
                                    value={filterWorkflowId}
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

                <WorkflowExecutionDetailsSheet />
            </LayoutContainer>
        </PageLoader>
    );
};

export default WorkflowExecutions;
