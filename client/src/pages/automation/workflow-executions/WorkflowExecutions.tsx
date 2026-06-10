import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowExecutionsFilterTitle from '@/pages/automation/workflow-executions/components/WorkflowExecutionsFilterTitle';
import {useWorkflowExecutions} from '@/pages/automation/workflow-executions/hooks/useWorkflowExecutions';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Project} from '@/shared/middleware/automation/configuration';
import {GetWorkflowExecutionsPageJobStatusEnum} from '@/shared/middleware/automation/workflow/execution';
import {ActivityIcon, RefreshCwIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import WorkflowExecutionsTable from './components/WorkflowExecutionsTable';
import WorkflowExecutionSheet from './components/workflow-execution-sheet/WorkflowExecutionSheet';

const ANY_FILTER_OPTION = {label: 'Any', value: ''};

const jobStatusOptions = [
    ANY_FILTER_OPTION,
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

        <span className="text-xs text-content-neutral-secondary">
            {project?.tags?.map((tag) => tag.name).join(', ')}
        </span>
    </div>
);

export const WorkflowExecutions = () => {
    const {
        currentEnvironmentId,
        emptyListMessage,
        filterEndDate,
        filterPageNumber,
        filterProjectDeploymentId,
        filterProjectId,
        filterStartDate,
        filterStatus,
        filterWorkflowId,
        handleEndDateChange,
        handlePaginationClick,
        handleProjectChange,
        handleProjectDeploymentChange,
        handleStartDateChange,
        handleStatusChange,
        handleWorkflowChange,
        projectDeployments,
        projects,
        refetchWorkflowExecutions,
        setFiltersInteracted,
        workflowExecutionPage,
        workflowExecutions,
        workflowExecutionsError,
        workflowExecutionsIsFetching,
        workflowExecutionsIsLoading,
        workflows,
    } = useWorkflowExecutions();

    return (
        <LayoutContainer
            footer={
                workflowExecutionPage?.content &&
                workflowExecutionPage.content.length > 0 && (
                    <Footer centerTitle className="border-t border-stroke-neutral-primary 3xl:w-full" position="main">
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
                <Header
                    centerTitle
                    className="3xl:w-full"
                    position="main"
                    right={
                        <div className="flex items-center gap-2">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label="Refresh workflow executions"
                                        disabled={workflowExecutionsIsFetching}
                                        icon={
                                            <RefreshCwIcon
                                                className={twMerge(workflowExecutionsIsFetching && 'animate-spin')}
                                            />
                                        }
                                        onClick={() => refetchWorkflowExecutions()}
                                        size="icon"
                                        variant="outline"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Refresh</TooltipContent>
                            </Tooltip>

                            <EnvironmentSelect />
                        </div>
                    }
                    title={
                        workflowExecutionPage?.content && workflowExecutionPage.content.length > 0 ? (
                            <WorkflowExecutionsFilterTitle
                                filterData={{environment: currentEnvironmentId, status: filterStatus}}
                            />
                        ) : (
                            ''
                        )
                    }
                />
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
                            emptyMessage={!projects ? 'Loading...' : 'No item found.'}
                            items={[
                                ANY_FILTER_OPTION,
                                ...(projects?.map((project) => ({
                                    label: <ProjectLabel project={project} />,
                                    value: project.id,
                                })) ?? []),
                            ]}
                            onChange={handleProjectChange}
                            onOpen={() => setFiltersInteracted(true)}
                            value={filterProjectId}
                        />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Deployment</Label>

                        <ComboBox
                            emptyMessage={!projectDeployments ? 'Loading...' : 'No item found.'}
                            items={[
                                ANY_FILTER_OPTION,
                                ...(projectDeployments?.map((projectDeployment) => ({
                                    label: (
                                        <span className="flex items-center">
                                            <span className="mr-1">
                                                {projectDeployment.name} V{projectDeployment.projectVersion}
                                            </span>

                                            <span className="text-xs text-content-neutral-secondary">
                                                {projectDeployment?.tags?.map((tag) => tag.name).join(', ')}
                                            </span>
                                        </span>
                                    ),
                                    value: projectDeployment.id,
                                })) ?? []),
                            ]}
                            onChange={handleProjectDeploymentChange}
                            onOpen={() => setFiltersInteracted(true)}
                            value={filterProjectDeploymentId}
                        />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>Workflow</Label>

                        <ComboBox
                            items={[
                                ANY_FILTER_OPTION,
                                ...(workflows?.map((workflow) => ({
                                    label: workflow.label || 'undefined label',
                                    value: workflow.id,
                                })) ?? []),
                            ]}
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
                    <WorkflowExecutionsTable workflowExecutions={workflowExecutions} />
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
