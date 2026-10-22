import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {PanelRightCloseIcon, PanelRightOpenIcon} from 'lucide-react';
import {useState} from 'react';

import WorkflowExecutionSheetContent from './WorkflowExecutionSheetContent';
import WorkflowExecutionSheetWorkflowPanel from './WorkflowExecutionSheetWorkflowPanel';
import useWorkflowExecutionDetail from './hooks/useWorkflowExecutionDetail';

interface WorkflowExecutionDetailProps {
    enabled?: boolean;
    workflowExecutionId: number;
}

const WorkflowExecutionDetail = ({enabled = true, workflowExecutionId}: WorkflowExecutionDetailProps) => {
    const [workflowPanelVisible, setWorkflowPanelVisible] = useState(false);

    const {
        activeTab,
        dialogOpen,
        focusedExecution,
        focusedTaskExecutionError,
        handleBreadcrumbNavigate,
        handleFocusedTaskExecutionRetry,
        handleSeeExecutions,
        handleTaskClick,
        handleWorkflowExecutionRetry,
        isTriggerExecution,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        lazy,
        rootJob,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutionAttempts,
        taskExecutionCount,
        triggerExecution,
        workflowExecution,
        workflowExecutionError,
        workflowExecutionLoading,
        workflowExecutionRefetchError,
    } = useWorkflowExecutionDetail(workflowExecutionId, enabled);

    if (workflowExecutionLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="workflow-execution-detail-loading">
                <LoadingIcon className="size-6" />
            </div>
        );
    }

    if (workflowExecutionError) {
        return (
            <div
                className="flex size-full items-center justify-center gap-x-2 text-sm text-content-neutral-secondary"
                data-testid="workflow-execution-detail-error"
            >
                <span>Failed to load the workflow execution.</span>

                <Button label="Retry" onClick={handleWorkflowExecutionRetry} size="xs" variant="outline" />
            </div>
        );
    }

    return (
        <div className="flex min-h-0 flex-1 p-3">
            <ResizablePanelGroup className="h-full" orientation="horizontal">
                <ResizablePanel
                    className="flex min-h-0 flex-col overflow-hidden rounded-lg bg-surface-neutral-primary"
                    defaultSize={65}
                    minSize={35}
                >
                    {(job || triggerExecution) && (
                        <WorkflowExecutionSheetContent
                            activeTab={activeTab}
                            dialogOpen={dialogOpen}
                            focusedExecution={focusedExecution}
                            focusedTaskExecutionError={focusedTaskExecutionError}
                            handleBreadcrumbNavigate={handleBreadcrumbNavigate}
                            handleFocusedTaskExecutionRetry={() => void handleFocusedTaskExecutionRetry()}
                            handleLiveUpdatesRetry={handleWorkflowExecutionRetry}
                            handleSeeExecutions={handleSeeExecutions}
                            handleTaskClick={handleTaskClick}
                            headerActions={
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label={workflowPanelVisible ? 'Hide workflow' : 'Show workflow'}
                                            icon={
                                                workflowPanelVisible ? <PanelRightCloseIcon /> : <PanelRightOpenIcon />
                                            }
                                            onClick={() => setWorkflowPanelVisible(!workflowPanelVisible)}
                                            size="iconSm"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        {workflowPanelVisible ? 'Hide workflow' : 'Show workflow'}
                                    </TooltipContent>
                                </Tooltip>
                            }
                            isTriggerExecution={isTriggerExecution}
                            job={job}
                            jobFailedWithNoExecutions={jobFailedWithNoExecutions}
                            jobFailureError={jobFailureError}
                            lazy={lazy}
                            liveUpdatesError={workflowExecutionRefetchError}
                            rootJob={rootJob}
                            selectedItem={selectedItem}
                            setActiveTab={setActiveTab}
                            setDialogOpen={setDialogOpen}
                            subflowStack={subflowStack}
                            taskExecutionAttempts={taskExecutionAttempts}
                            taskExecutionCount={taskExecutionCount}
                            triggerExecution={triggerExecution}
                        />
                    )}
                </ResizablePanel>

                {workflowPanelVisible && (
                    <>
                        <ResizableHandle className="w-1.5 bg-transparent" />

                        <ResizablePanel
                            className="flex min-h-0 flex-col overflow-hidden rounded-lg"
                            defaultSize={35}
                            minSize={15}
                        >
                            {workflowExecution && (
                                <WorkflowReadOnlyProvider
                                    value={{useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery}}
                                >
                                    <WorkflowExecutionSheetWorkflowPanel workflowExecution={workflowExecution} />
                                </WorkflowReadOnlyProvider>
                            )}
                        </ResizablePanel>
                    </>
                )}
            </ResizablePanelGroup>
        </div>
    );
};

export default WorkflowExecutionDetail;
