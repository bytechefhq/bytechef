import {Accordion} from '@/components/ui/accordion';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import WorkflowExecutionSheetAccordionItem from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetAccordionItem';
import WorkflowExecutionSheetAccordionTrigger from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetAccordionTrigger';
import WorkflowExecutionTabsContent from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionTabsContent';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {getTasksTree, handleTaskClick} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {ChevronDownIcon, RefreshCwIcon, RefreshCwOffIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsTestOutputHeader = ({
    job,
    triggerExecution,
}: {
    job: Job;
    triggerExecution?: TriggerExecution;
}) => {
    const startTime = job?.startDate?.getTime();
    const endTime = job?.endDate?.getTime();

    const taskExecutionsCompleted = job?.taskExecutions?.every((taskExecution) => taskExecution.status === 'COMPLETED');
    const triggerExecutionCompleted = !triggerExecution || triggerExecution?.status === 'COMPLETED';

    let duration = 0;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    return (
        <div className="flex items-center gap-x-3 py-2">
            <div className="flex items-center gap-x-2">
                <WorkflowExecutionBadge
                    status={taskExecutionsCompleted && triggerExecutionCompleted ? 'COMPLETED' : 'FAILED'}
                />

                <span
                    className={twMerge(
                        (!taskExecutionsCompleted || !triggerExecutionCompleted) && 'text-destructive',
                        'text-sm font-semibold uppercase'
                    )}
                >
                    {taskExecutionsCompleted && triggerExecutionCompleted ? 'Workflow executed' : 'Workflow failed'}
                </span>
            </div>

            <div className="flex justify-between gap-x-2 text-xs">
                <span>
                    {job?.startDate &&
                        `${job?.startDate?.toLocaleDateString()} ${job?.startDate?.toLocaleTimeString()}`}
                </span>

                <span>Duration: {duration}ms</span>

                <span>{`${taskExecutionsCount} task${taskExecutionsCount > 1 ? 's' : ''} executed`}</span>
            </div>
        </div>
    );
};

const WorkflowExecutionsTestOutput = ({
    onCloseClick,
    resizablePanelSize = 30,
    workflowIsRunning,
    workflowTestExecution,
}: {
    resizablePanelSize?: number;
    workflowIsRunning: boolean;
    workflowTestExecution?: WorkflowTestExecution;
    onCloseClick?: () => void;
}) => {
    const [activeTab, setActiveTab] = useState<'input' | 'output' | 'error'>('input');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        workflowTestExecution?.triggerExecution
            ? (workflowTestExecution.triggerExecution as TriggerExecution)
            : workflowTestExecution?.job?.taskExecutions
              ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecution)
              : undefined
    );

    const job = workflowTestExecution?.job as Job;
    const triggerExecution = workflowTestExecution?.triggerExecution as TriggerExecution;

    useEffect(() => {
        setSelectedItem(
            workflowTestExecution?.triggerExecution
                ? (workflowTestExecution.triggerExecution as TriggerExecution)
                : workflowTestExecution?.job?.taskExecutions
                  ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecution)
                  : undefined
        );

        setActiveTab('input');
    }, [workflowTestExecution]);

    const tasksTree = useMemo(() => getTasksTree(job), [job]);

    const onTaskClick = useCallback(
        (taskExecution: TaskExecution | TriggerExecution) => {
            handleTaskClick({setActiveTab, setSelectedItem, taskExecution});
        },
        [setActiveTab, setSelectedItem]
    );

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    return (
        <div className="flex size-full flex-col">
            <div className="flex items-center justify-between border-b border-b-muted px-3 py-1">
                {workflowTestExecution ? (
                    <WorkflowExecutionsTestOutputHeader job={job} triggerExecution={triggerExecution} />
                ) : (
                    <span className="text-sm uppercase">Test Output</span>
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
                                <ResizablePanelGroup direction="horizontal">
                                    <ResizablePanel className="overflow-y-auto py-4" defaultSize={resizablePanelSize}>
                                        <ScrollArea className="h-full pr-4">
                                            <Accordion
                                                className="space-y-2 [transform:translateZ(0)]"
                                                collapsible
                                                defaultValue={
                                                    isTriggerExecution
                                                        ? triggerExecution?.id || ''
                                                        : selectedItem?.id || ''
                                                }
                                                type="single"
                                            >
                                                {triggerExecution && (
                                                    <WorkflowExecutionSheetAccordionTrigger
                                                        onTaskClick={onTaskClick}
                                                        selectedItem={selectedItem}
                                                        triggerExecution={triggerExecution}
                                                    />
                                                )}

                                                {tasksTree.map((node) => (
                                                    <WorkflowExecutionSheetAccordionItem
                                                        key={node.task.id}
                                                        node={node}
                                                        onTaskClick={onTaskClick}
                                                        selectedTaskExecutionId={selectedItem?.id || ''}
                                                    />
                                                ))}
                                            </Accordion>
                                        </ScrollArea>
                                    </ResizablePanel>

                                    <ResizableHandle className="bg-muted" />

                                    <ResizablePanel className="flex min-h-0 flex-col space-y-4 overflow-hidden p-4">
                                        <WorkflowExecutionTabsContent
                                            activeTab={activeTab}
                                            dialogOpen={dialogOpen}
                                            job={job}
                                            selectedItem={selectedItem}
                                            setActiveTab={setActiveTab}
                                            setDialogOpen={setDialogOpen}
                                            triggerExecution={triggerExecution}
                                        />
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
