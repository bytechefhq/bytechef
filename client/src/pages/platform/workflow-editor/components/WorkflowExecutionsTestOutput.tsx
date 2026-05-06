import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {ChevronDownIcon, RefreshCwIcon, RefreshCwOffIcon} from 'lucide-react';

import useWorkflowExecutions from './properties/hooks/useWorkflowExecutions';

interface WorkflowExecutionsTestOutputProps {
    onCloseClick?: () => void;
    resizablePanelSize?: number;
    workflowIsRunning: boolean;
    workflowTestExecution: WorkflowTestExecution;
}

const WorkflowExecutionsTestOutput = ({
    onCloseClick,
    resizablePanelSize = 300,
    workflowIsRunning,
    workflowTestExecution,
}: WorkflowExecutionsTestOutputProps) => {
    const {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleExecutionClick,
        isTriggerExecution,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        selectedExecution,
        setActiveTab,
        setDialogOpen,
        taskExecutions,
        triggerExecution,
    } = useWorkflowExecutions({workflowTestExecution});

    return (
        <div className="flex size-full flex-col">
            <div className="flex items-center justify-between border-b border-stroke-neutral-secondary py-1">
                {job ? (
                    <WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />
                ) : (
                    <span className="flex w-full items-center gap-x-3 px-3 py-4 text-sm uppercase">Test Output</span>
                )}

                {onCloseClick && (
                    <button className="p-2" onClick={() => onCloseClick()}>
                        <ChevronDownIcon className="h-5" />
                    </button>
                )}
            </div>

            <div className="relative size-full">
                <div className="absolute inset-0 overflow-y-auto">
                    {workflowIsRunning && (
                        <div className="flex size-full items-center justify-center gap-x-1 p-3">
                            <span className="flex animate-spin text-gray-400">
                                <RefreshCwIcon className="size-5" />
                            </span>

                            <span className="text-muted-foreground">Workflow is running...</span>
                        </div>
                    )}

                    {!workflowIsRunning && (
                        <>
                            {workflowTestExecution?.job && jobFailedWithNoExecutions && (
                                <div className="flex-1 p-4">
                                    <WorkflowExecutionContent error={jobFailureError} />
                                </div>
                            )}

                            {workflowTestExecution?.job && !jobFailedWithNoExecutions && (
                                <ResizablePanelGroup orientation="horizontal">
                                    <ResizablePanel className="overflow-y-auto py-4" defaultSize={resizablePanelSize}>
                                        <ScrollArea className="h-full pl-1 pr-4">
                                            <Accordion
                                                className="ml-2 space-y-2"
                                                defaultValue={
                                                    deepestFailedExecution?.path ||
                                                    (isTriggerExecution
                                                        ? [triggerExecution?.id || '']
                                                        : [selectedExecution?.id || ''])
                                                }
                                                type="multiple"
                                            >
                                                {triggerExecution && (
                                                    <WorkflowExecutionsAccordionItem
                                                        defaultValue={deepestFailedExecution?.path}
                                                        execution={triggerExecution}
                                                        onExecutionClick={handleExecutionClick}
                                                        selectedExecutionId={selectedExecution?.id || ''}
                                                    >
                                                        <WorkflowTriggerExecutionItem
                                                            triggerExecution={triggerExecution}
                                                        />
                                                    </WorkflowExecutionsAccordionItem>
                                                )}

                                                {taskExecutions.map((taskExecution) => (
                                                    <WorkflowExecutionsAccordionItem
                                                        defaultValue={deepestFailedExecution?.path}
                                                        execution={taskExecution}
                                                        key={taskExecution.id}
                                                        onExecutionClick={handleExecutionClick}
                                                        selectedExecutionId={selectedExecution?.id || ''}
                                                    >
                                                        <WorkflowTaskExecutionItem taskExecution={taskExecution} />
                                                    </WorkflowExecutionsAccordionItem>
                                                ))}
                                            </Accordion>
                                        </ScrollArea>
                                    </ResizablePanel>

                                    <ResizableHandle className="bg-muted" />

                                    <ResizablePanel className="flex min-h-0 flex-col space-y-4 overflow-hidden p-4">
                                        {job && (
                                            <WorkflowExecutionsTabsPanel
                                                activeTab={activeTab}
                                                dialogOpen={dialogOpen}
                                                isEditorEnvironment={true}
                                                job={job}
                                                selectedItem={selectedExecution}
                                                setActiveTab={setActiveTab}
                                                setDialogOpen={setDialogOpen}
                                                triggerExecution={triggerExecution}
                                            />
                                        )}
                                    </ResizablePanel>
                                </ResizablePanelGroup>
                            )}

                            {!workflowTestExecution?.job && (
                                <div className="flex size-full items-center justify-center gap-x-1 p-3 text-muted-foreground">
                                    <RefreshCwOffIcon className="size-5" />

                                    <span>The workflow has not yet been executed.</span>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default WorkflowExecutionsTestOutput;
