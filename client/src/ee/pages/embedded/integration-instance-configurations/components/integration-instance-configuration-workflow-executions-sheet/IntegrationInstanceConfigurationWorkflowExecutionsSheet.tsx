import {Sheet, SheetContent, SheetTitle} from '@/components/ui/sheet';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import EmbeddedWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/EmbeddedWorkflowExecutionsTable';
import EmbeddedWorkflowExecutionSheet from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import WorkflowExecutionsSheetLayout from '@/ee/pages/embedded/workflow-executions/components/workflow-executions-sheet/WorkflowExecutionsSheetLayout';
import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowExecutionFromJSON} from '@/ee/shared/middleware/embedded/workflow/execution';
import {useGetIntegrationWorkflowExecutionsQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import ProjectDeploymentWorkflowPanel from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel';
import {Workflow as AutomationWorkflow} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

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

    return (
        <WorkflowExecutionsSheetLayout
            emptyMessage="This workflow has not been executed in this instance configuration yet."
            executionSheet={<EmbeddedWorkflowExecutionSheet />}
            executionsTable={<EmbeddedWorkflowExecutionsTable data={workflowExecutions} showConnectedUser />}
            hasExecutions={workflowExecutions.length > 0}
            onPageNumberChange={setPageNumber}
            onRefresh={() => refetchWorkflowExecutions()}
            pageNumber={pageNumber}
            parentLabel={integrationInstanceConfigurationName}
            version={integrationVersion}
            workflowExecutionPage={workflowExecutionPage}
            workflowExecutionsError={workflowExecutionsError}
            workflowExecutionsIsFetching={workflowExecutionsIsFetching}
            workflowExecutionsIsLoading={workflowExecutionsIsLoading}
            workflowLabel={workflow.label}
            workflowPanel={<ProjectDeploymentWorkflowPanel workflow={workflow as AutomationWorkflow} />}
        />
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
