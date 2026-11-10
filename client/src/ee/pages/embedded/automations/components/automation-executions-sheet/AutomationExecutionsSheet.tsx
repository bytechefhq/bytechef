import PageLoader from '@/components/PageLoader';
import {Sheet, SheetContent, SheetTitle} from '@/components/ui/sheet';
import useAutomationExecutionsSheetStore, {
    AutomationExecutionsSheetWorkflowI,
} from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import AutomationExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/AutomationExecutionsTable';
import WorkflowExecutionsSheetLayout from '@/ee/pages/embedded/workflow-executions/components/workflow-executions-sheet/WorkflowExecutionsSheetLayout';
import ProjectDeploymentWorkflowPanel from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {WorkflowExecutionFromJSON} from '@/shared/middleware/automation/workflow/execution';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetEmbeddedWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface AutomationTemplatePanelProps {
    workflowId: string;
}

const AutomationTemplatePanel = ({workflowId}: AutomationTemplatePanelProps) => {
    const {data: workflow, error: workflowError, isLoading: workflowIsLoading} = useGetWorkflowQuery(workflowId);

    return (
        <PageLoader errors={[workflowError]} loading={workflowIsLoading}>
            {workflow && (
                <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                    <ProjectDeploymentWorkflowPanel workflow={workflow} />
                </WorkflowReadOnlyProvider>
            )}
        </PageLoader>
    );
};

interface AutomationExecutionsContentProps {
    connectedUserId?: number;
    environmentId: number;
    projectName?: string;
    workflow: AutomationExecutionsSheetWorkflowI;
}

const AutomationExecutionsContent = ({
    connectedUserId,
    environmentId,
    projectName,
    workflow,
}: AutomationExecutionsContentProps) => {
    const [pageNumber, setPageNumber] = useState(0);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

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
    } = useGetEmbeddedWorkflowExecutionsQuery(
        {
            connectedUserId,
            environmentId,
            id: currentWorkspaceId!,
            pageNumber,
            workflowId: workflow.workflowUuid,
        },
        currentWorkspaceId != null
    );

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
                    ? 'This connected user has not executed this workflow in this environment yet.'
                    : 'No connected user has executed this workflow in this environment yet.'
            }
            executionsTable={<AutomationExecutionsTable data={workflowExecutions} />}
            hasExecutions={workflowExecutions.length > 0}
            onBackToExecutions={() => setWorkflowExecutionSheetOpen(false)}
            onPageNumberChange={setPageNumber}
            onRefresh={() => refetchWorkflowExecutions()}
            pageNumber={pageNumber}
            parentLabel={projectName}
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
            workflowLabel={workflow.label || workflow.workflowUuid}
            workflowPanel={<AutomationTemplatePanel workflowId={workflow.workflowId} />}
        />
    );
};

const AutomationExecutionsSheet = () => {
    const {
        automationExecutionsSheetOpen,
        connectedUserId,
        environmentId,
        projectName,
        setAutomationExecutionsSheetOpen,
        workflow,
    } = useAutomationExecutionsSheetStore(
        useShallow((state) => ({
            automationExecutionsSheetOpen: state.automationExecutionsSheetOpen,
            connectedUserId: state.connectedUserId,
            environmentId: state.environmentId,
            projectName: state.projectName,
            setAutomationExecutionsSheetOpen: state.setAutomationExecutionsSheetOpen,
            workflow: state.workflow,
        }))
    );

    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleOpenChange = (open: boolean) => {
        setAutomationExecutionsSheetOpen(open);

        if (!open) {
            setWorkflowExecutionSheetOpen(false);
        }
    };

    return (
        <Sheet onOpenChange={handleOpenChange} open={automationExecutionsSheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <VisuallyHidden.Root>
                    <SheetTitle>{workflow?.label || workflow?.workflowUuid}</SheetTitle>
                </VisuallyHidden.Root>

                {workflow && environmentId != null && (
                    <AutomationExecutionsContent
                        connectedUserId={connectedUserId}
                        environmentId={environmentId}
                        key={`${environmentId}-${workflow.workflowUuid}-${connectedUserId ?? ''}`}
                        projectName={projectName}
                        workflow={workflow}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default AutomationExecutionsSheet;
