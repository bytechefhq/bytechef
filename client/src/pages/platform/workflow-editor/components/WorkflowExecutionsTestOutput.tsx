import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {ChevronDownIcon, RefreshCwIcon, RefreshCwOffIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
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

    const taskExecutionsCompleted = job?.status === 'COMPLETED';
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
    const [content, setContent] = useState<TaskExecution | TriggerExecution | undefined>(
        workflowTestExecution?.triggerExecution
            ? (workflowTestExecution.triggerExecution as TriggerExecution)
            : workflowTestExecution?.job?.taskExecutions
              ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecution)
              : undefined
    );

    const job = workflowTestExecution?.job as Job;
    const triggerExecution = workflowTestExecution?.triggerExecution as TriggerExecution;

    useEffect(() => {
        setContent(
            workflowTestExecution?.triggerExecution
                ? (workflowTestExecution.triggerExecution as TriggerExecution)
                : workflowTestExecution?.job?.taskExecutions
                  ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecution)
                  : undefined
        );
    }, [workflowTestExecution]);

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
                                        <ul className="mx-2 space-y-0.5">
                                            {triggerExecution && (
                                                <WorkflowTriggerExecutionItem
                                                    key={triggerExecution.id}
                                                    onClick={() => setContent(triggerExecution)}
                                                    selected={content?.id === triggerExecution.id}
                                                    triggerExecution={triggerExecution}
                                                />
                                            )}

                                            {job?.taskExecutions &&
                                                job?.taskExecutions.map((taskExecution) => (
                                                    <WorkflowTaskExecutionItem
                                                        key={taskExecution.id}
                                                        onClick={() => setContent(taskExecution)}
                                                        selected={content?.id === taskExecution.id}
                                                        taskExecution={taskExecution}
                                                    />
                                                ))}
                                        </ul>
                                    </ResizablePanel>

                                    <ResizableHandle className="bg-muted" />

                                    <ResizablePanel className="space-y-4 overflow-y-auto p-4">
                                        <WorkflowExecutionContent {...content} />
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
