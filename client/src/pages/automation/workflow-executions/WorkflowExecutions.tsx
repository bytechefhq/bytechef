import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkflowExecutions} from '@/pages/automation/workflow-executions/hooks/useWorkflowExecutions';
import {
    getProjectDeploymentLabel,
    getWorkflowExecutionsFilters,
} from '@/pages/automation/workflow-executions/utils/workflowExecutionsFilters';
import FilterTitle from '@/shared/components/filters/FilterTitle';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Project} from '@/shared/middleware/automation/configuration';
import {GetWorkflowExecutionsPageJobStatusEnum} from '@/shared/middleware/automation/workflow/execution';
import {ActivityIcon, RefreshCwIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowExecutionsEmptyState from './components/WorkflowExecutionsEmptyState';
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
        filterEndDate,
        filterPageNumber,
        filterProjectDeploymentId,
        filterProjectId,
        filterStartDate,
        filterStatus,
        filterWorkflowId,
        handleClearFilters,
        handleEndDateChange,
        handlePaginationClick,
        handleProjectChange,
        handleProjectDeploymentChange,
        handleStartDateChange,
        handleStatusChange,
        handleWorkflowChange,
        hasActiveFilters,
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

    const activeFilters = useMemo(
        () =>
            getWorkflowExecutionsFilters({
                endDate: filterEndDate,
                projectDeploymentId: filterProjectDeploymentId,
                projectDeployments,
                projectId: filterProjectId,
                projects,
                startDate: filterStartDate,
                status: filterStatus,
                workflowId: filterWorkflowId,
                workflows,
            }),
        [
            filterEndDate,
            filterProjectDeploymentId,
            filterProjectId,
            filterStartDate,
            filterStatus,
            filterWorkflowId,
            projectDeployments,
            projects,
            workflows,
        ]
    );

    // An empty page of a non-empty result, e.g. a bookmarked page number after older executions were purged.
    const isPastLastPage = !!workflowExecutionPage?.totalElements;

    const isFirstRun = !hasActiveFilters && !isPastLastPage && workflowExecutions?.length === 0;

    let emptyState = <WorkflowExecutionsEmptyState onRefresh={() => refetchWorkflowExecutions()} />;

    if (isPastLastPage) {
        emptyState = (
            <EmptyList
                button={<Button label="Go to first page" onClick={() => handlePaginationClick(0)} variant="outline" />}
                icon={<ActivityIcon className="size-24 text-stroke-neutral-tertiary" />}
                message="This page is past the last execution."
                title="No Executions on This Page"
            />
        );
    } else if (hasActiveFilters) {
        emptyState = (
            <EmptyFilterResult entityName="executions" entityTitle="Executions" onClear={handleClearFilters} />
        );
    }

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
                        </div>
                    }
                    title={
                        workflowExecutionPage?.content && workflowExecutionPage.content.length > 0 ? (
                            <FilterTitle filters={activeFilters} />
                        ) : (
                            isFirstRun && 'Workflow Executions'
                        )
                    }
                />
            }
            leftSidebarBody={
                <div className="space-y-4 px-2">
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
                                            <span className="mr-1">{getProjectDeploymentLabel(projectDeployment)}</span>

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
            leftSidebarHidden={isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                {workflowExecutions && workflowExecutions.length > 0 ? (
                    <WorkflowExecutionsTable workflowExecutions={workflowExecutions} />
                ) : (
                    emptyState
                )}
            </PageLoader>

            <WorkflowExecutionSheet />
        </LayoutContainer>
    );
};
