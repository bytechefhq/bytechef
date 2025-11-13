import {Accordion} from '@/components/ui/accordion';
import {ScrollArea} from '@/components/ui/scroll-area';
import {getTasksTree, handleTaskClick} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowExecutionSheetAccordionItem from './WorkflowExecutionSheetAccordionItem';
import WorkflowExecutionSheetAccordionTrigger from './WorkflowExecutionSheetAccordionTrigger';
import WorkflowExecutionTabsContent from './WorkflowExecutionTabsContent';

const WorkflowExecutionSheetAccordion = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const [activeTab, setActiveTab] = useState<'input' | 'output' | 'error'>('input');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(
        triggerExecution || job.taskExecutions?.[0] || undefined
    );

    const startTime = job?.startDate?.getTime();
    const endTime = job?.endDate?.getTime();

    const taskExecutionsCompleted = job?.taskExecutions?.every((taskExecution) => taskExecution.status === 'COMPLETED');
    const triggerExecutionCompleted = !triggerExecution || triggerExecution?.status === 'COMPLETED';

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    const tasksTree = useMemo(() => getTasksTree(job), [job]);

    const onTaskClick = useCallback(
        (taskExecution: TaskExecution | TriggerExecution) => {
            handleTaskClick({setActiveTab, setSelectedItem, taskExecution});
        },
        [setActiveTab, setSelectedItem]
    );

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    return (
        <div className="flex size-full flex-col px-2 py-4">
            <header>
                <div className="mb-3 flex items-center gap-x-2">
                    <span
                        className={twMerge(
                            (!taskExecutionsCompleted || !triggerExecutionCompleted) && 'text-destructive',
                            'text-base font-semibold uppercase'
                        )}
                    >
                        {taskExecutionsCompleted && triggerExecutionCompleted ? 'Workflow executed' : 'Workflow failed'}
                    </span>
                </div>

                <div className="flex justify-between text-xs">
                    <span>
                        {job?.startDate &&
                            `${job?.startDate?.toLocaleDateString()} ${job?.startDate?.toLocaleTimeString()}`}
                    </span>

                    <span>Duration: {duration}ms</span>

                    <span>{`${taskExecutionsCount} task${taskExecutionsCount > 1 ? 's' : ''} executed`}</span>
                </div>
            </header>

            <div className="grid min-h-0 w-full max-w-full flex-1 grid-cols-2 gap-1">
                <div className="mt-4 flex min-h-0 flex-col overflow-hidden [overflow-anchor:none]">
                    <ScrollArea className="h-full pr-4">
                        <Accordion
                            className="space-y-2 [transform:translateZ(0)]"
                            collapsible
                            defaultValue={isTriggerExecution ? triggerExecution?.id || '' : selectedItem?.id || ''}
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
                </div>

                <div className="mt-4 flex min-h-0 flex-col rounded-md border border-border/50 p-3">
                    <WorkflowExecutionTabsContent
                        activeTab={activeTab}
                        dialogOpen={dialogOpen}
                        job={job}
                        selectedItem={selectedItem}
                        setActiveTab={setActiveTab}
                        setDialogOpen={setDialogOpen}
                        triggerExecution={triggerExecution}
                    />
                </div>
            </div>
        </div>
    );
};

export default WorkflowExecutionSheetAccordion;
