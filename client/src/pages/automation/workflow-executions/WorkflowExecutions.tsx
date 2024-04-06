import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import DatePicker from '@/components/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageFooter from '@/layouts/PageFooter';
import PageHeader from '@/layouts/PageHeader';
import {ProjectModel} from '@/middleware/automation/configuration';
import {useGetProjectInstancesQuery} from '@/queries/automation/projectInstances.queries';
import {useGetWorkflowExecutionsQuery} from '@/queries/automation/workflowExecutions.queries';
import {useGetWorkflowsQuery} from '@/queries/automation/workflows.queries';
import {ActivityIcon} from 'lucide-react';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionModel,
    WorkflowExecutionModelFromJSON,
} from 'middleware/automation/workflow/execution';
import {useGetProjectsQuery} from 'queries/automation/projects.queries';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import WorkflowExecutionSheet from './components/WorkflowExecutionSheet';
import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';

const jobStatusOptions = [
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Started,
        value: GetWorkflowExecutionsPageJobStatusEnum.Started,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Completed,
        value: GetWorkflowExecutionsPageJobStatusEnum.Completed,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Created,
        value: GetWorkflowExecutionsPageJobStatusEnum.Created,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Stopped,
        value: GetWorkflowExecutionsPageJobStatusEnum.Stopped,
    },
    {
        label: GetWorkflowExecutionsPageJobStatusEnum.Failed,
        value: GetWorkflowExecutionsPageJobStatusEnum.Failed,
    },
];

const ProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export const WorkflowExecutions = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterPageNumber, setFilterPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );
    const [filterProjectId, setFilterProjectId] = useState<number | undefined>(
        searchParams.get('projectId') ? +searchParams.get('projectId')! : undefined
    );
    const [filterProjectInstanceId, setFilterProjectInstanceId] = useState<number | undefined>(
        searchParams.get('projectInstanceId') ? +searchParams.get('projectInstanceId')! : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();

    const {data: projectInstances} = useGetProjectInstancesQuery({});

    const {data: projects} = useGetProjectsQuery({});

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isLoading: workflowExecutionsIsLoading,
    } = useGetWorkflowExecutionsQuery({
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        projectId: filterProjectId,
        projectInstanceId: filterProjectInstanceId,
        workflowId: filterWorkflowId,
    });

    const {data: workflows} = useGetWorkflowsQuery();

    const emptyListMessage =
        !filterStatus &&
        !filterProjectId &&
        !filterWorkflowId &&
        !filterStartDate &&
        !filterEndDate &&
        !filterProjectInstanceId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecutionModel: WorkflowExecutionModel) =>
        WorkflowExecutionModelFromJSON(workflowExecutionModel)
    );

    function search(
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        projectId?: number,
        projectInstanceId?: number,
        workflowId?: string,
        startDate?: Date,
        endDate?: Date,
        pageNumber?: number
    ) {
        navigate(
            `/automation/executions?status=${status ? status : ''}&${
                'projectId=' + (projectId ? projectId : '') + '&'
            }projectInstanceId=${projectInstanceId ? projectInstanceId : ''}&workflowId=${
                workflowId ? workflowId : ''
            }&startDate=${startDate ? startDate.getTime() : ''}&endDate=${
                endDate ? endDate.getTime() : ''
            }&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        search(
            filterStatus,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterStartDate,
            date,
            filterPageNumber
        );
        setFilterEndDate(date);
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        search(
            filterStatus,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            pageNumber
        );
    };

    const handleProjectChange = (item?: ComboBoxItemType) => {
        let projectId;

        if (item) {
            projectId = Number(item.value);
        }

        setFilterProjectId(projectId);
        search(
            filterStatus,
            projectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handleProjectInstanceChange = (item?: ComboBoxItemType) => {
        let projectInstanceId;

        if (item) {
            projectInstanceId = Number(item.value);
        }

        setFilterProjectInstanceId(projectInstanceId);
        search(
            filterStatus,
            filterProjectId,
            projectInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handleStatusChange = (item?: ComboBoxItemType) => {
        let status;

        if (item) {
            status = item.value as GetWorkflowExecutionsPageJobStatusEnum;
        }

        setFilterStatus(status);
        search(
            status,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        search(
            filterStatus,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            date,
            filterEndDate,
            filterPageNumber
        );
        setFilterStartDate(date);
    };

    const handleWorkflowChange = (item?: ComboBoxItemType) => {
        let workflowId;

        if (item) {
            workflowId = item.value;
        }

        setFilterWorkflowId(workflowId);
        search(
            filterStatus,
            filterProjectId,
            filterProjectInstanceId,
            workflowId,
            filterStartDate,
            filterEndDate,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <PageFooter position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber ? filterPageNumber : 0}
                            pageSize={workflowExecutionPage.size!}
                            totalElements={workflowExecutionPage.totalElements!}
                            totalPages={workflowExecutionPage.totalPages!}
                        />
                    </PageFooter>
                )
            }
            header={<PageHeader centerTitle={true} position="main" title="All Workflow Executions" />}
            leftSidebarBody={
                <div className="space-y-4 px-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Status</Label>

                        <ComboBox items={jobStatusOptions} onChange={handleStatusChange} value={filterStatus} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Start date</Label>

                        <DatePicker onChange={handleStartDateChange} value={filterStartDate} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>End date</Label>

                        <DatePicker onChange={handleEndDateChange} value={filterEndDate} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Project</Label>

                        <ComboBox
                            items={
                                projects?.length
                                    ? projects?.map((project) => ({
                                          label: <ProjectLabel project={project} />,
                                          value: project.id,
                                      }))
                                    : []
                            }
                            onChange={handleProjectChange}
                            value={filterProjectId}
                        />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Instance</Label>

                        <ComboBox
                            items={
                                projectInstances?.length
                                    ? projectInstances?.map((projectInstance) => ({
                                          label: (
                                              <span className="flex items-center">
                                                  <span className="mr-1 ">{projectInstance.name}</span>

                                                  <span className="text-xs text-gray-500">
                                                      {projectInstance?.tags?.map((tag) => tag.name).join(', ')}
                                                  </span>
                                              </span>
                                          ),
                                          value: projectInstance.id,
                                      }))
                                    : []
                            }
                            onChange={handleProjectInstanceChange}
                            value={filterProjectInstanceId}
                        />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Workflow</Label>

                        <ComboBox
                            items={
                                workflows?.length
                                    ? workflows?.map((workflow) => ({
                                          label: workflow.label || 'undefined label',
                                          value: workflow.id,
                                      }))
                                    : []
                            }
                            onChange={handleWorkflowChange}
                            value={filterWorkflowId}
                        />
                    </div>
                </div>
            }
            leftSidebarHeader={<PageHeader position="sidebar" title="Execution History" />}
            leftSidebarWidth="72"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    <WorkflowExecutionsTable data={workflowExecutions} />
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-12 text-gray-400" />}
                        message={emptyListMessage}
                        title="No Executed Workflows"
                    />
                )}
            </PageLoader>

            <WorkflowExecutionSheet />
        </LayoutContainer>
    );
};
