import LoadingIcon from '@/components/LoadingIcon';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';

import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';
import useWorkflowExecutionDetail from './hooks/useWorkflowExecutionDetail';

interface WorkflowExecutionDetailProps {
    enabled?: boolean;
    workflowExecutionId: number;
}

const WorkflowExecutionDetail = ({enabled = true, workflowExecutionId}: WorkflowExecutionDetailProps) => {
    const {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleBreadcrumbNavigate,
        handleSeeExecutions,
        handleTaskClick,
        isTriggerExecution,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        rootJob,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutions,
        triggerExecution,
        workflowExecution,
        workflowExecutionLoading,
    } = useWorkflowExecutionDetail(workflowExecutionId, enabled);

    if (workflowExecutionLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="workflow-execution-detail-loading">
                <LoadingIcon className="size-6" />
            </div>
        );
    }

    return (
        <div className="flex min-h-0 flex-1 p-3">
            <ResizablePanelGroup className="h-full" orientation="horizontal">
                <ResizablePanel
                    className="flex min-h-0 w-1/2 flex-col overflow-hidden rounded-md bg-surface-neutral-primary"
                    defaultSize={50}
                >
                    {job && (
                        <WorkflowExecutionSheetContent
                            activeTab={activeTab}
                            deepestFailedExecution={deepestFailedExecution}
                            dialogOpen={dialogOpen}
                            handleBreadcrumbNavigate={handleBreadcrumbNavigate}
                            handleSeeExecutions={handleSeeExecutions}
                            handleTaskClick={handleTaskClick}
                            isTriggerExecution={isTriggerExecution}
                            job={job}
                            jobFailedWithNoExecutions={jobFailedWithNoExecutions}
                            jobFailureError={jobFailureError}
                            rootJob={rootJob}
                            selectedItem={selectedItem}
                            setActiveTab={setActiveTab}
                            setDialogOpen={setDialogOpen}
                            subflowStack={subflowStack}
                            taskExecutions={taskExecutions}
                            triggerExecution={triggerExecution}
                            workflowExecutionId={workflowExecutionId}
                        />
                    )}
                </ResizablePanel>

                <ResizableHandle className="mx-2.5" withHandle />

                <ResizablePanel className="flex min-h-0 w-1/2 flex-col overflow-hidden" defaultSize={50}>
                    {workflowExecution && (
                        <WorkflowReadOnlyProvider
                            value={{useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery}}
                        >
                            <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />
                        </WorkflowReadOnlyProvider>
                    )}
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    );
};

export default WorkflowExecutionDetail;
