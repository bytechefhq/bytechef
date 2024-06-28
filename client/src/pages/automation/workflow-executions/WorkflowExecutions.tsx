import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import DatePicker from '@/components/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {EnvironmentModel, ProjectModel} from '@/shared/middleware/automation/configuration';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionModelFromJSON,
} from '@/shared/middleware/automation/workflow/execution';
import {
    useGetProjectInstanceQuery,
    useGetWorkspaceProjectInstancesQuery,
} from '@/shared/queries/automation/projectInstances.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {ActivityIcon} from 'lucide-react';
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
    const [searchParams] = useSearchParams();

    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(
        searchParams.get('endDate') ? new Date(+searchParams.get('endDate')!) : undefined
    );
    const [filterEnvironment, setFilterEnvironment] = useState<string | undefined>(
        searchParams.get('environment') ? searchParams.get('environment')! : undefined
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
    const [filterStatus, setFilterStatus] = useState<GetWorkflowExecutionsPageJobStatusEnum | undefined>(
        searchParams.get('status') ? (searchParams.get('status')! as GetWorkflowExecutionsPageJobStatusEnum) : undefined
    );
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(
        searchParams.get('startDate') ? new Date(+searchParams.get('startDate')!) : undefined
    );
    const [filterWorkflowId, setFilterWorkflowId] = useState<string | undefined>();

    const {currentWorkspaceId} = useWorkspaceStore();

    const navigate = useNavigate();

    const {data: projectInstance} = useGetProjectInstanceQuery(filterProjectInstanceId!, !!filterProjectInstanceId);

    const {data: projectInstances} = useGetWorkspaceProjectInstancesQuery({
        id: currentWorkspaceId!,
        projectId: filterProjectId,
    });

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId!});

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isLoading: workflowExecutionsIsLoading,
    } = useGetWorkflowExecutionsQuery({
        environment: filterEnvironment as EnvironmentModel,
        jobEndDate: filterEndDate,
        jobStartDate: filterStartDate,
        jobStatus: filterStatus,
        pageNumber: filterPageNumber,
        projectId: filterProjectId,
        projectInstanceId: filterProjectInstanceId,
        workflowId: filterWorkflowId,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        filterProjectId!,
        projectInstance?.projectVersion!,
        !!projectInstance
    );

    const emptyListMessage =
        !filterStatus &&
        !filterStartDate &&
        !filterEndDate &&
        !filterProjectId &&
        !filterProjectInstanceId &&
        !filterWorkflowId &&
        !filterPageNumber
            ? "You don't have any executed workflows yet."
            : 'There is no executed workflows for the current criteria.';

    const workflowExecutions = workflowExecutionPage?.content?.map((workflowExecutionModel: object) =>
        WorkflowExecutionModelFromJSON(workflowExecutionModel)
    );

    function filter(
        environment?: string,
        status?: GetWorkflowExecutionsPageJobStatusEnum,
        startDate?: Date,
        endDate?: Date,
        projectId?: number,
        projectInstanceId?: number,
        workflowId?: string,
        pageNumber?: number
    ) {
        navigate(
            `/automation/executions?environment=${environment ?? ''}&status=${status ? status : ''}&startDate=${startDate ? startDate.getTime() : ''}&endDate=${endDate ? endDate.getTime() : ''}&projectId=${projectId ? projectId : ''}&projectInstanceId=${projectInstanceId ? projectInstanceId : ''}&workflowId=${workflowId ? workflowId : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleEndDateChange = (date?: Date) => {
        setFilterEndDate(date);

        filter(
            filterEnvironment,
            filterStatus,
            filterStartDate,
            date,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleEnvironmentChange = (environment: string) => {
        setFilterEnvironment(environment);

        filter(
            environment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handlePaginationClick = (pageNumber: number) => {
        setFilterPageNumber(pageNumber);

        filter(
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectInstanceId,
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
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            projectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleProjectInstanceChange = (item?: ComboBoxItemType) => {
        let projectInstanceId;

        if (item) {
            projectInstanceId = Number(item.value);
        }

        setFilterProjectInstanceId(projectInstanceId);

        filter(
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            projectInstanceId,
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
            filterEnvironment,
            status,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectInstanceId,
            filterWorkflowId,
            filterPageNumber
        );
    };

    const handleStartDateChange = (date?: Date) => {
        setFilterStartDate(date);

        filter(
            filterEnvironment,
            filterStatus,
            date,
            filterEndDate,
            filterProjectId,
            filterProjectInstanceId,
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
            filterEnvironment,
            filterStatus,
            filterStartDate,
            filterEndDate,
            filterProjectId,
            filterProjectInstanceId,
            workflowId,
            filterPageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Footer position="main">
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
                    <Header centerTitle={true} position="main" title="All Workflow Executions" />
                )
            }
            leftSidebarBody={
                <div className="space-y-4 px-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Environment</Label>

                        <Select onValueChange={handleEnvironmentChange} value={filterEnvironment}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select environment" />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="TEST">Test</SelectItem>

                                <SelectItem value="PRODUCTION">Production</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

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
                                                  <span className="mr-1 ">
                                                      {projectInstance.name} V{projectInstance.projectVersion}
                                                  </span>

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
            leftSidebarHeader={<Header position="sidebar" title="Execution History" />}
            leftSidebarWidth="72"
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
