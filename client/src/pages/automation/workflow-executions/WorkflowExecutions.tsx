import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowExecutionsFilterTitle from '@/pages/automation/workflow-executions/components/WorkflowExecutionsFilterTitle';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Project} from '@/shared/middleware/automation/configuration';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionFromJSON,
} from '@/shared/middleware/automation/workflow/execution';
import {
    useGetProjectDeploymentQuery,
    useGetWorkspaceProjectDeploymentsQuery,
} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkspaceProjectWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {ActivityIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';
import WorkflowExecutionSheet from './components/workflow-execution-sheet/WorkflowExecutionSheet';

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

const ProjectLabel = ({project}: {project: Project}) => (
    <div className="flex items-center">
        <span className="mr-1">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export const WorkflowExecutions = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterPageNumber, setFilterPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );
    const [filterProjectId, setFilterProjectId] = useState<number | undefined>(
        searchParams.get('projectId') ? +searchParams.get('projectId')! : undefined
    );
    const [filterProjectDeploymentId, setFilterProjectDeploymentId] = useState<number | undefined>(
        searchParams.get('projectDeploymentId') ? +searchParams.get('projectDeploymentId')! : undefined
    );
    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();

    const {data: projectDeployment} = useGetProjectDeploymentQuery(
        filterProjectDeploymentId!,
        !!filterProjectDeploymentId
    );

    const {data: projectDeployments} = useGetWorkspaceProjectDeploymentsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        includeAllFields: false,
        projectId: filterProjectId,
    });

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId!, includeAllFields: false});

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isLoading: workflowExecutionsIsLoading,
    } = useGetWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        projectDeploymentId: filterProjectDeploymentId,
        projectId: filterProjectId,
        workflowId: filterWorkflowId,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        filterProjectId!,
        projectDeployment?.projectVersion!,
        false,
        !!projectDeployment
    );

    const emptyListMessage =
        !filterStatus &&
        !filterStartDate &&
        !filterEndDate &&
        !filterProjectId &&
        !filterProjectDeploymentId &&
        !filterWorkflowId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecution: object) =>
        WorkflowExecutionFromJSON(workflowExecution)
    );

    function filter(
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        startDate?: Date,
        endDate?: Date,
        projectId?: number,
        projectDeploymentId?: number,
        workflowId?: string,
        pageNumber?: number
    ) {
        navigate(
            `/automation/executions?status=${status ? status : ''}&startDate=${startDate ? startDate.getTime() : ''}&endDate=${endDate ? endDate.getTime() : ''}&projectId=${projectId ? projectId : ''}&projectDeploymentId=${projectDeploymentId ? projectDeploymentId : ''}&workflowId=${workflowId ? workflowId : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        setFilterEndDate(date);

        filter(
            filterStatus,
            filterStartDate,
            date,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            pageNumber
        );
    };

    const handleProjectChange = (item?: ComboBoxItemType) => {
        let projectId;

        if (item) {
            projectId = Number(item.value);
        }

        setFilterProjectId(projectId);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            projectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleProjectDeploymentChange = (item?: ComboBoxItemType) => {
        let projectDeploymentId;

        if (item) {
            projectDeploymentId = Number(item.value);
        }

        setFilterProjectDeploymentId(projectDeploymentId);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            projectDeploymentId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleStatusChange = (item?: ComboBoxItemType) => {
        let status;

        if (item) {
            status = item.value as GetWorkflowExecutionsPageJobStatusEnum;
        }

        setFilterStatus(status);

        filter(
            status,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        setFilterStartDate(date);

        filter(
            filterStatus,
            date,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleWorkflowChange = (item?: ComboBoxItemType) => {
        let workflowId;

        if (item) {
            workflowId = item.value;
        }

        setFilterWorkflowId(workflowId);

        filter(
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectDeploymentId,
            workflowId,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Footer centerTitle position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber ? filterPageNumber : 0}
                            pageSize={workflowExecutionPage.size!}
                            totalElements={workflowExecutionPage.totalElements!}
                            totalPages={workflowExecutionPage.totalPages!}
                        />
                    </Footer>
                )
            }
            header={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Header
                        centerTitle
                        position="main"
                        title={
                            <WorkflowExecutionsFilterTitle
                                filterData={{environment: currentEnvironmentId, status: filterStatus}}
                            />
                        }
                    />
                )
            }
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
                        <Label>Deployment</Label>

                        <ComboBox
                            items={
                                projectDeployments?.length
                                    ? projectDeployments?.map((projectDeployment) => ({
                                          label: (
                                              <span className="flex items-center">
                                                  <span className="mr-1">
                                                      {projectDeployment.name} V{projectDeployment.projectVersion}
                                                  </span>

                                                  <span className="text-xs text-gray-500">
                                                      {projectDeployment?.tags?.map((tag) => tag.name).join(', ')}
                                                  </span>
                                              </span>
                                          ),
                                          value: projectDeployment.id,
                                      }))
                                    : []
                            }
                            onChange={handleProjectDeploymentChange}
                            value={filterProjectDeploymentId}
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
            leftSidebarHeader={<Header position="sidebar" title="Workflow Executions" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    <WorkflowExecutionsTable data={workflowExecutions} />
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-24 text-gray-300" />}
                        message={emptyListMessage}
                        title="No Executed Workflows"
                    />
                )}
            </PageLoader>

            <WorkflowExecutionSheet />
        </LayoutContainer>
    );
};
