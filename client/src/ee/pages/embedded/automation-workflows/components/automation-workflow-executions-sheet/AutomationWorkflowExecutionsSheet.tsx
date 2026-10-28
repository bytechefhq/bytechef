import PageLoader from '@/components/PageLoader';
import {Sheet, SheetContent, SheetTitle} from '@/components/ui/sheet';
import useAutomationWorkflowExecutionsSheetStore, {
    AutomationWorkflowExecutionsSheetWorkflowI,
} from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import AutomationWorkflowExecutionsTable from '@/ee/pages/embedded/workflow-executions/components/AutomationWorkflowExecutionsTable';
import WorkflowExecutionsSheetLayout from '@/ee/pages/embedded/workflow-executions/components/workflow-executions-sheet/WorkflowExecutionsSheetLayout';
import ProjectDeploymentWorkflowPanel from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import AutomationWorkflowExecutionSheet from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {WorkflowExecutionFromJSON} from '@/shared/middleware/automation/workflow/execution';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetEmbeddedWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {VisuallyHidden} from 'radix-ui';
import {useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface AutomationWorkflowTemplatePanelProps {
    workflowId: string;
}

const AutomationWorkflowTemplatePanel = ({workflowId}: AutomationWorkflowTemplatePanelProps) => {
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

interface AutomationWorkflowExecutionsContentProps {
    projectName?: string;
    workflow: AutomationWorkflowExecutionsSheetWorkflowI;
}

const AutomationWorkflowExecutionsContent = ({projectName, workflow}: AutomationWorkflowExecutionsContentProps) => {
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch: refetchWorkflowExecutions,
    } = useGetEmbeddedWorkflowExecutionsQuery(
        {
            environmentId: currentEnvironmentId,
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
            emptyMessage="No connected user has executed this workflow in this environment yet."
            executionSheet={<AutomationWorkflowExecutionSheet />}
            executionsTable={<AutomationWorkflowExecutionsTable data={workflowExecutions} />}
            hasExecutions={workflowExecutions.length > 0}
            onPageNumberChange={setPageNumber}
            onRefresh={() => refetchWorkflowExecutions()}
            pageNumber={pageNumber}
            parentLabel={projectName}
            workflowExecutionPage={workflowExecutionPage}
            workflowExecutionsError={workflowExecutionsError}
            workflowExecutionsIsFetching={workflowExecutionsIsFetching}
            workflowExecutionsIsLoading={workflowExecutionsIsLoading}
            workflowLabel={workflow.label || workflow.workflowUuid}
            workflowPanel={<AutomationWorkflowTemplatePanel workflowId={workflow.workflowId} />}
        />
    );
};

const AutomationWorkflowExecutionsSheet = () => {
    const {automationWorkflowExecutionsSheetOpen, projectName, setAutomationWorkflowExecutionsSheetOpen, workflow} =
        useAutomationWorkflowExecutionsSheetStore(
            useShallow((state) => ({
                automationWorkflowExecutionsSheetOpen: state.automationWorkflowExecutionsSheetOpen,
                projectName: state.projectName,
                setAutomationWorkflowExecutionsSheetOpen: state.setAutomationWorkflowExecutionsSheetOpen,
                workflow: state.workflow,
            }))
        );

    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const handleOpenChange = (open: boolean) => {
        setAutomationWorkflowExecutionsSheetOpen(open);

        if (!open) {
            setWorkflowExecutionSheetOpen(false);
        }
    };

    return (
        <Sheet onOpenChange={handleOpenChange} open={automationWorkflowExecutionsSheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <VisuallyHidden.Root>
                    <SheetTitle>{workflow?.label || workflow?.workflowUuid}</SheetTitle>
                </VisuallyHidden.Root>

                {workflow && (
                    <AutomationWorkflowExecutionsContent
                        key={workflow.workflowUuid}
                        projectName={projectName}
                        workflow={workflow}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default AutomationWorkflowExecutionsSheet;
