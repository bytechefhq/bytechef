import {Accordion} from '@/components/ui/accordion';
import WorkflowExecutionContent from '@/pages/platform/workflow-executions/components/WorkflowExecutionContent';
import WorkflowTaskExecutionItem from '@/pages/platform/workflow-executions/components/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/pages/platform/workflow-executions/components/WorkflowTriggerExecutionItem';
import {Job, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {AccordionContent, AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionSheetAccordion = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const startTime = job?.startDate?.getTime();
    const endTime = job?.endDate?.getTime();

    const taskExecutionsCompleted = job?.taskExecutions?.every((taskExecution) => taskExecution.status === 'COMPLETED');
    const triggerExecutionCompleted = !triggerExecution || triggerExecution?.status === 'COMPLETED';

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    return (
        <>
            <div className="px-3 py-4">
                <div className="mb-3 flex items-center justify-between">
                    <span
                        className={twMerge(
                            (!taskExecutionsCompleted || !triggerExecutionCompleted) && 'text-destructive',
                            'font-semibold uppercase text-base'
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
            </div>

            <div className="overflow-y-auto">
                <Accordion collapsible defaultValue={triggerExecution?.id || ''} type="single">
                    {triggerExecution && (
                        <AccordionItem key={triggerExecution.id} value={triggerExecution.id || ''}>
                            <AccordionTrigger className="flex w-full items-center justify-between border-muted bg-background data-[state=closed]:border-b">
                                <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                            </AccordionTrigger>

                            <AccordionContent className="space-y-4 border-b border-muted p-3">
                                <WorkflowExecutionContent {...triggerExecution} />
                            </AccordionContent>
                        </AccordionItem>
                    )}

                    {job?.taskExecutions &&
                        job?.taskExecutions.map((taskExecution) => (
                            <AccordionItem key={taskExecution.id} value={taskExecution.id || ''}>
                                <AccordionTrigger className="flex w-full items-center justify-between border-muted bg-background data-[state=closed]:border-b">
                                    <WorkflowTaskExecutionItem taskExecution={taskExecution} />
                                </AccordionTrigger>

                                <AccordionContent className="space-y-4 border-b border-muted p-3">
                                    <WorkflowExecutionContent {...taskExecution} />
                                </AccordionContent>
                            </AccordionItem>
                        ))}
                </Accordion>
            </div>
        </>
    );
};

export default WorkflowExecutionSheetAccordion;
