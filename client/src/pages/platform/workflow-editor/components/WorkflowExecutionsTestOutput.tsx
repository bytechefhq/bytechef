import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import WorkflowExecutionsHeader from '@/shared/components/workflow-executions/WorkflowExecutionsHeader';
import WorkflowExecutionsTabsPanel from '@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel';
import WorkflowExecutionsTaskAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsTaskAccordionItem';
import WorkflowExecutionsTriggerAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsTriggerAccordionItem';
import {
    getErrorItem,
    getInitialSelectedItem,
    getTasksTree,
} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {TaskExecution, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {TabValueType} from '@/shared/types';
import {ChevronDownIcon, RefreshCwIcon, RefreshCwOffIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';

const WorkflowExecutionsTestOutput = ({
    onCloseClick,
    resizablePanelSize = 350,
    workflowIsRunning,
    workflowTestExecution,
}: {
    resizablePanelSize?: number;
    workflowIsRunning: boolean;
    workflowTestExecution?: WorkflowTestExecution;
    onCloseClick?: () => void;
}) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('input');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        getInitialSelectedItem(workflowTestExecution)
    );

    const setWorkflowExecutionError = useCopilotStore((state) => state.setWorkflowExecutionError);

    const job = workflowTestExecution?.job;
    const triggerExecution = workflowTestExecution?.triggerExecution;
    const currentWorkflowId = job?.workflowId;

    useEffect(() => {
        setSelectedItem(getInitialSelectedItem(workflowTestExecution));
        setActiveTab('input');
    }, [workflowTestExecution]);

    useEffect(() => {
        const errorItem = getErrorItem(workflowTestExecution);

        if (errorItem?.error && currentWorkflowId) {
            setWorkflowExecutionError({
                errorMessage: errorItem.error.message,
                stackTrace: errorItem.error.stackTrace,
                title: errorItem.title,
                workflowId: currentWorkflowId,
            });
        } else if (!errorItem?.error) {
            setWorkflowExecutionError(undefined);
        }
    }, [workflowTestExecution, currentWorkflowId, setWorkflowExecutionError]);

    const tasksTree = useMemo(() => (job ? getTasksTree(job) : []), [job]);

    const onTaskClick = useCallback((taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'input');
        setSelectedItem(taskExecution);
    }, []);

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

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
                            {workflowTestExecution?.job && (
                                <ResizablePanelGroup orientation="horizontal">
                                    <ResizablePanel className="overflow-y-auto py-4" defaultSize={resizablePanelSize}>
                                        <ScrollArea className="h-full pl-1 pr-4">
                                            <Accordion
                                                className="space-y-2"
                                                collapsible
                                                defaultValue={
                                                    isTriggerExecution
                                                        ? triggerExecution?.id || ''
                                                        : selectedItem?.id || ''
                                                }
                                                type="single"
                                            >
                                                {triggerExecution && (
                                                    <WorkflowExecutionsTriggerAccordionItem
                                                        onTaskClick={onTaskClick}
                                                        selectedItem={selectedItem}
                                                        triggerExecution={triggerExecution}
                                                    />
                                                )}

                                                {tasksTree.map((taskTreeItem) => (
                                                    <WorkflowExecutionsTaskAccordionItem
                                                        key={taskTreeItem.task.id}
                                                        onTaskClick={onTaskClick}
                                                        selectedTaskExecutionId={selectedItem?.id || ''}
                                                        taskTreeItem={taskTreeItem}
                                                    />
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
                                                selectedItem={selectedItem}
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
