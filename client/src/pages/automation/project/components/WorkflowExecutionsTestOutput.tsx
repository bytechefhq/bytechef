import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {JobModel, TaskExecutionModel, TriggerExecutionModel} from '@/middleware/platform/workflow/execution';
import {WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import WorkflowExecutionBadge from '@/pages/platform/workflow-executions/components/WorkflowExecutionBadge';
import WorkflowExecutionContent from '@/pages/platform/workflow-executions/components/WorkflowExecutionContent';
import WorkflowTaskExecutionItem from '@/pages/platform/workflow-executions/components/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/pages/platform/workflow-executions/components/WorkflowTriggerExecutionItem';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import {RefreshCwIcon, RefreshCwOffIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsTestOutputHeader = ({
    job,
    triggerExecution,
}: {
    job: JobModel;
    triggerExecution?: TriggerExecutionModel;
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
                <WorkflowExecutionBadge success={!!(taskExecutionsCompleted && triggerExecutionCompleted)} />

                <span
                    className={twMerge(
                        (!taskExecutionsCompleted || !triggerExecutionCompleted) && 'text-destructive',
                        'font-semibold uppercase text-sm'
                    )}
                >
                    {taskExecutionsCompleted && triggerExecutionCompleted
                        ? 'Workflow executed successfully'
                        : 'Workflow failed'}
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
    workflowIsRunning,
    workflowTestExecution,
}: {
    workflowIsRunning: boolean;
    workflowTestExecution?: WorkflowTestExecutionModel;
    onCloseClick?: () => void;
}) => {
    const [content, setContent] = useState<TaskExecutionModel | TriggerExecutionModel | undefined>(
        workflowTestExecution?.triggerExecution
            ? (workflowTestExecution.triggerExecution as TriggerExecutionModel)
            : workflowTestExecution?.job?.taskExecutions
              ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecutionModel)
              : undefined
    );

    const job = workflowTestExecution?.job as JobModel;
    const triggerExecution = workflowTestExecution?.triggerExecution as TriggerExecutionModel;

    useEffect(() => {
        setContent(
            workflowTestExecution?.triggerExecution
                ? (workflowTestExecution.triggerExecution as TriggerExecutionModel)
                : workflowTestExecution?.job?.taskExecutions
                  ? (workflowTestExecution.job?.taskExecutions[0] as TaskExecutionModel)
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

                    {!workflowIsRunning && workflowTestExecution?.job ? (
                        <ResizablePanelGroup direction="horizontal">
                            <ResizablePanel className="overflow-y-auto py-4" defaultSize={30}>
                                <ul className="divide-y divide-gray-100">
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

                            <ResizableHandle />

                            <ResizablePanel className="space-y-4 overflow-y-auto p-4">
                                <WorkflowExecutionContent {...content} />
                            </ResizablePanel>
                        </ResizablePanelGroup>
                    ) : (
                        <div className="flex size-full items-center justify-center gap-x-1 p-3 text-muted-foreground">
                            <RefreshCwOffIcon className="size-5" />

                            <span>The workflow has not yet been executed.</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default WorkflowExecutionsTestOutput;
