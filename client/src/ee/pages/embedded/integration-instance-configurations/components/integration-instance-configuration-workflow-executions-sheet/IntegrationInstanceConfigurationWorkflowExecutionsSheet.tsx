import {Sheet, SheetContent, SheetTitle} from '@/components/ui/sheet';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import EmbeddedWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTable';
import WorkflowExecutionDetail from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';
import WorkflowExecutionsSheetLayout from '@/ee/pages/embedded/workflow-executions/components/workflow-executions-sheet/WorkflowExecutionsSheetLayout';
import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowExecutionFromJSON} from '@/ee/shared/middleware/embedded/workflow/execution';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationWorkflowExecutionsQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import ProjectDeploymentWorkflowPanel from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow as AutomationWorkflow} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationInstanceConfigurationWorkflowExecutionsContentProps {
    connectedUserId?: number;
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationName?: string;
    integrationVersion?: number;
    workflow: Workflow;
}

const IntegrationInstanceConfigurationWorkflowExecutionsContent = ({
    connectedUserId,
    integrationInstanceConfigurationId,
    integrationInstanceConfigurationName,
    integrationVersion,
    workflow,
}: IntegrationInstanceConfigurationWorkflowExecutionsContentProps) => {
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch: refetchWorkflowExecutions,
    } = useGetIntegrationWorkflowExecutionsQuery({
        connectedUserId,
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

    return (
        <WorkflowExecutionsSheetLayout
            emptyMessage={
                connectedUserId
                    ? 'This workflow has not been executed for this connected user yet.'
                    : 'This workflow has not been executed in this instance configuration yet.'
            }
            executionsTable={
                <EmbeddedWorkflowExecutionsTable data={workflowExecutions} showConnectedUser={!connectedUserId} />
            }
            hasExecutions={workflowExecutions.length > 0}
            onBackToExecutions={() => setWorkflowExecutionSheetOpen(false)}
            onPageNumberChange={setPageNumber}
            onRefresh={() => refetchWorkflowExecutions()}
            pageNumber={pageNumber}
            parentLabel={integrationInstanceConfigurationName}
            version={integrationVersion}
            workflowExecutionDetail={
                <WorkflowExecutionDetail
                    enabled={workflowExecutionSheetOpen}
                    workflowExecutionId={workflowExecutionId}
                />
            }
            workflowExecutionDetailOpen={workflowExecutionSheetOpen}
            workflowExecutionPage={workflowExecutionPage}
            workflowExecutionsError={workflowExecutionsError}
            workflowExecutionsIsFetching={workflowExecutionsIsFetching}
            workflowExecutionsIsLoading={workflowExecutionsIsLoading}
            workflowLabel={workflow.label}
            workflowPanel={
                <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                    <ProjectDeploymentWorkflowPanel workflow={workflow as AutomationWorkflow} />
                </WorkflowReadOnlyProvider>
            }
        />
    );
};

const IntegrationInstanceConfigurationWorkflowExecutionsSheet = () => {
    const {
        connectedUserId,
        integrationInstanceConfigurationId,
        integrationInstanceConfigurationName,
        integrationInstanceConfigurationWorkflowSheetOpen,
        integrationVersion,
        setIntegrationInstanceConfigurationWorkflowSheetOpen,
        workflow,
    } = useIntegrationInstanceConfigurationWorkflowSheetStore(
        useShallow((state) => ({
            connectedUserId: state.connectedUserId,
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
                        connectedUserId={connectedUserId}
                        integrationInstanceConfigurationId={integrationInstanceConfigurationId}
                        integrationInstanceConfigurationName={integrationInstanceConfigurationName}
                        integrationVersion={integrationVersion}
                        key={`${integrationInstanceConfigurationId}_${workflow.id}_${connectedUserId ?? ''}`}
                        workflow={workflow}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationInstanceConfigurationWorkflowExecutionsSheet;
