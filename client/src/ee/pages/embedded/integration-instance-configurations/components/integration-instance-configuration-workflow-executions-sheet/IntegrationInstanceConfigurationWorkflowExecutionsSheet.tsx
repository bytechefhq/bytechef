import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import EmbeddedWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTable';
import EmbeddedWorkflowExecutionSheet from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowExecutionFromJSON} from '@/ee/shared/middleware/embedded/workflow/execution';
import {useGetIntegrationWorkflowExecutionsQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import ProjectDeploymentWorkflowPanel from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel';
import {Workflow as AutomationWorkflow} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ActivityIcon, RefreshCwIcon, WorkflowIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

type IntegrationInstanceConfigurationWorkflowSheetTabType = 'executions' | 'workflow';

interface IntegrationInstanceConfigurationWorkflowExecutionsContentProps {
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationName?: string;
    integrationVersion?: number;
    workflow: Workflow;
}

const IntegrationInstanceConfigurationWorkflowExecutionsContent = ({
    integrationInstanceConfigurationId,
    integrationInstanceConfigurationName,
    integrationVersion,
    workflow,
}: IntegrationInstanceConfigurationWorkflowExecutionsContentProps) => {
    const [activeTab, setActiveTab] = useState<IntegrationInstanceConfigurationWorkflowSheetTabType>('executions');
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch: refetchWorkflowExecutions,
    } = useGetIntegrationWorkflowExecutionsQuery({
        environmentId: currentEnvironmentId,
        integrationInstanceConfigurationId,
        pageNumber,
        workflowId: workflow.id,
    });

    const workflowExecutions = useMemo(
        () =>
            workflowExecutionPage?.content?.map((workflowExecution: object) =>
                WorkflowExecutionFromJSON(workflowExecution)
            ) ?? [],
        [workflowExecutionPage]
    );

    const executionsTabActive = activeTab === 'executions';

    return (
        <>
            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                <div className="flex items-center gap-x-2">
                    <WorkflowIcon />

                    <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                        {integrationInstanceConfigurationName && (
                            <span>{`${integrationInstanceConfigurationName} /`}</span>
                        )}

                        <strong className="text-content-neutral-primary">{workflow.label}</strong>

                        {integrationVersion != null && <span>{`/ V${integrationVersion}`}</span>}
                    </span>

                    <Tabs
                        className="ml-4"
                        onValueChange={(value) =>
                            setActiveTab(value as IntegrationInstanceConfigurationWorkflowSheetTabType)
                        }
                        value={activeTab}
                    >
                        <TabsList>
                            <TabsTrigger value="executions">Executions</TabsTrigger>

                            <TabsTrigger value="workflow">Workflow</TabsTrigger>
                        </TabsList>
                    </Tabs>
                </div>

                <div className="flex items-center gap-1">
                    {executionsTabActive && (
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

            <div className={twMerge('flex min-h-0 flex-1 flex-col', !executionsTabActive && 'hidden')}>
                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg bg-surface-neutral-primary">
                    <div className="flex min-h-0 flex-1 overflow-y-auto">
                        <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                            {workflowExecutions.length > 0 ? (
                                <EmbeddedWorkflowExecutionsTable data={workflowExecutions} showConnectedUser />
                            ) : (
                                <EmptyList
                                    icon={<ActivityIcon className="size-24 text-stroke-neutral-tertiary" />}
                                    message="This workflow has not been executed in this instance configuration yet."
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

            {!executionsTabActive && <ProjectDeploymentWorkflowPanel workflow={workflow as AutomationWorkflow} />}

            <EmbeddedWorkflowExecutionSheet />
        </>
    );
};

const IntegrationInstanceConfigurationWorkflowExecutionsSheet = () => {
    const {
        integrationInstanceConfigurationId,
        integrationInstanceConfigurationName,
        integrationInstanceConfigurationWorkflowSheetOpen,
        integrationVersion,
        setIntegrationInstanceConfigurationWorkflowSheetOpen,
        workflow,
    } = useIntegrationInstanceConfigurationWorkflowSheetStore(
        useShallow((state) => ({
            integrationInstanceConfigurationId: state.integrationInstanceConfigurationId,
            integrationInstanceConfigurationName: state.integrationInstanceConfigurationName,
            integrationInstanceConfigurationWorkflowSheetOpen: state.integrationInstanceConfigurationWorkflowSheetOpen,
            integrationVersion: state.integrationVersion,
            setIntegrationInstanceConfigurationWorkflowSheetOpen:
                state.setIntegrationInstanceConfigurationWorkflowSheetOpen,
            workflow: state.workflow,
        }))
    );

    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleOpenChange = (open: boolean) => {
        setIntegrationInstanceConfigurationWorkflowSheetOpen(open);

        if (!open) {
            setWorkflowExecutionSheetOpen(false);
        }
    };

    return (
        <Sheet onOpenChange={handleOpenChange} open={integrationInstanceConfigurationWorkflowSheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <VisuallyHidden.Root>
                    <SheetTitle>{workflow?.label}</SheetTitle>
                </VisuallyHidden.Root>

                {integrationInstanceConfigurationId != null && workflow?.id && (
                    <IntegrationInstanceConfigurationWorkflowExecutionsContent
                        integrationInstanceConfigurationId={integrationInstanceConfigurationId}
                        integrationInstanceConfigurationName={integrationInstanceConfigurationName}
                        integrationVersion={integrationVersion}
                        key={`${integrationInstanceConfigurationId}_${workflow.id}`}
                        workflow={workflow}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationInstanceConfigurationWorkflowExecutionsSheet;
