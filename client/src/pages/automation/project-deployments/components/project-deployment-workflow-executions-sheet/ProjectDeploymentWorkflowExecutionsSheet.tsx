import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowExecutionsTable from '@/pages/automation/workflow-executions/components/WorkflowExecutionsTable';
import WorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {getProjectVersion} from '@/pages/automation/workflow-executions/utils/workflowExecutionsTable';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {WorkflowExecutionFromJSON} from '@/shared/middleware/automation/workflow/execution';
import {
    useGetProjectWorkflowExecutionQuery,
    useGetWorkspaceProjectWorkflowExecutionsQuery,
} from '@/shared/queries/automation/workflowExecutions.queries';
import {ActivityIcon, ArrowLeftIcon, RefreshCwIcon, WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import ProjectDeploymentWorkflowPanel from './ProjectDeploymentWorkflowPanel';

type ProjectDeploymentWorkflowSheetTabType = 'executions' | 'workflow';

interface ProjectDeploymentWorkflowExecutionsContentProps {
    enabled: boolean;
    projectDeploymentId: number;
    projectName?: string;
    projectVersion?: number;
    workflow: Workflow;
}

const ProjectDeploymentWorkflowExecutionsContent = ({
    enabled,
    projectDeploymentId,
    projectName,
    projectVersion,
    workflow,
}: ProjectDeploymentWorkflowExecutionsContentProps) => {
    const [activeTab, setActiveTab] = useState<ProjectDeploymentWorkflowSheetTabType>('executions');
    const [pageNumber, setPageNumber] = useState(0);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionKind, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionKind: state.workflowExecutionKind,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const {data: workflowExecution} = useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId},
        enabled && activeTab === 'executions' && workflowExecutionSheetOpen,
        undefined,
        workflowExecutionKind
    );

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch: refetchWorkflowExecutions,
    } = useGetWorkspaceProjectWorkflowExecutionsQuery(
        {
            id: currentWorkspaceId!,
            pageNumber,
            projectDeploymentId,
            workflowId: workflow.id,
        },
        enabled && !!currentWorkspaceId
    );

    const workflowExecutions = useMemo(
        () =>
            workflowExecutionPage?.content?.map((workflowExecution: object) =>
                WorkflowExecutionFromJSON(workflowExecution)
            ) ?? [],
        [workflowExecutionPage]
    );

    const executionsTabActive = activeTab === 'executions';
    const workflowExecutionDetailOpen = enabled && executionsTabActive && workflowExecutionSheetOpen;

    let headerProjectVersion = projectVersion;

    if (workflowExecutionDetailOpen) {
        headerProjectVersion = workflowExecution ? getProjectVersion(workflowExecution) : undefined;
    }

    return (
        <>
            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                <div className="flex items-center gap-x-2">
                    {workflowExecutionDetailOpen && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Back to executions"
                                    icon={<ArrowLeftIcon />}
                                    onClick={() => setWorkflowExecutionSheetOpen(false)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Back to executions</TooltipContent>
                        </Tooltip>
                    )}

                    <WorkflowIcon />

                    <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                        {projectName && `${projectName} /`}

                        <strong className="text-content-neutral-primary">{workflow.label}</strong>

                        {headerProjectVersion != null && <span>{`/ V${headerProjectVersion}`}</span>}
                    </span>

                    {!workflowExecutionDetailOpen && (
                        <Tabs
                            className="ml-4"
                            onValueChange={(value) => setActiveTab(value as ProjectDeploymentWorkflowSheetTabType)}
                            value={activeTab}
                        >
                            <TabsList>
                                <TabsTrigger value="executions">Executions</TabsTrigger>

                                <TabsTrigger value="workflow">Workflow</TabsTrigger>
                            </TabsList>
                        </Tabs>
                    )}
                </div>

                <div className="flex items-center gap-1">
                    {executionsTabActive && !workflowExecutionDetailOpen && (
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
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Refresh</TooltipContent>
                        </Tooltip>
                    )}

                    <SheetCloseButton />
                </div>
            </header>

            <div
                className={twMerge(
                    'flex min-h-0 flex-1 flex-col',
                    (!executionsTabActive || workflowExecutionDetailOpen) && 'hidden'
                )}
            >
                <div
                    className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-b-md bg-surface-neutral-primary"
                    data-testid="workflow-executions-island"
                >
                    <div className="flex min-h-0 flex-1 overflow-y-auto">
                        <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                            {workflowExecutions.length > 0 ? (
                                <WorkflowExecutionsTable className="p-0" workflowExecutions={workflowExecutions} />
                            ) : (
                                <EmptyList
                                    icon={<ActivityIcon className="size-24 text-gray-300" />}
                                    message="This workflow has not been executed in this deployment yet."
                                    title="No Executions"
                                />
                            )}
                        </PageLoader>
                    </div>

                    {workflowExecutionPage && workflowExecutions.length > 0 && (
                        <div className="shrink-0 border-t border-stroke-neutral-primary">
                            <TablePagination
                                onClick={setPageNumber}
                                pageNumber={pageNumber}
                                pageSize={workflowExecutionPage.size!}
                                totalElements={workflowExecutionPage.totalElements!}
                                totalPages={workflowExecutionPage.totalPages!}
                            />
                        </div>
                    )}
                </div>
            </div>

            {workflowExecutionDetailOpen && <WorkflowExecutionDetail workflowExecutionId={workflowExecutionId} />}

            {!executionsTabActive && <ProjectDeploymentWorkflowPanel workflow={workflow} />}
        </>
    );
};

const ProjectDeploymentWorkflowExecutionsSheet = () => {
    const {
        projectDeploymentId,
        projectDeploymentWorkflowSheetOpen,
        projectName,
        projectVersion,
        setProjectDeploymentWorkflowSheetOpen,
        workflow,
    } = useProjectDeploymentWorkflowSheetStore(
        useShallow((state) => ({
            projectDeploymentId: state.projectDeploymentId,
            projectDeploymentWorkflowSheetOpen: state.projectDeploymentWorkflowSheetOpen,
            projectName: state.projectName,
            projectVersion: state.projectVersion,
            setProjectDeploymentWorkflowSheetOpen: state.setProjectDeploymentWorkflowSheetOpen,
            workflow: state.workflow,
        }))
    );

    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleOpenChange = (open: boolean) => {
        setProjectDeploymentWorkflowSheetOpen(open);

        if (!open) {
            setWorkflowExecutionSheetOpen(false);
        }
    };

    return (
        <Sheet onOpenChange={handleOpenChange} open={projectDeploymentWorkflowSheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <VisuallyHidden.Root>
                    <SheetTitle>{workflow?.label}</SheetTitle>
                </VisuallyHidden.Root>

                {projectDeploymentId != null && workflow?.id && (
                    <ProjectDeploymentWorkflowExecutionsContent
                        enabled={projectDeploymentWorkflowSheetOpen}
                        key={`${projectDeploymentId}_${workflow.id}`}
                        projectDeploymentId={projectDeploymentId}
                        projectName={projectName}
                        projectVersion={projectVersion}
                        workflow={workflow}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default ProjectDeploymentWorkflowExecutionsSheet;
