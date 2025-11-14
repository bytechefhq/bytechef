import {Accordion} from '@/components/ui/accordion';
import WorkflowExecutionContent from '@/shared/components/workflow-executions/WorkflowExecutionContent';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {Job, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {AccordionContent, AccordionItem, AccordionTrigger} from '@radix-ui/react-accordion';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionSheetAccordion = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const startTime = job?.startDate?.getTime();
    const endTime = job?.endDate?.getTime();

    const taskExecutionsCompleted = job?.status === 'COMPLETED';
    const triggerExecutionCompleted = !triggerExecution || triggerExecution?.status === 'COMPLETED';

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    return (
        <>
            <div className="px-3 py-4">
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
            </div>

            <div className="overflow-y-auto pl-1 pr-1.5">
                <Accordion collapsible defaultValue={triggerExecution?.id || ''} type="single">
                    {triggerExecution && (
                        <AccordionItem key={triggerExecution.id} value={triggerExecution.id || ''}>
                            <AccordionTrigger className="flex w-full items-center justify-between border-border/50 bg-background data-[state=closed]:border-b">
                                <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
                            </AccordionTrigger>

                            <AccordionContent className="space-y-4 border-b border-b-border/50 p-3">
                                <WorkflowExecutionContent
                                    jobInputs={job.inputs}
                                    workflowTriggerName={triggerExecution?.workflowTrigger?.name}
                                    {...triggerExecution}
                                />
                            </AccordionContent>
                        </AccordionItem>
                    )}

                    {job?.taskExecutions &&
                        job?.taskExecutions.map((taskExecution) => (
                            <AccordionItem key={taskExecution.id} value={taskExecution.id || ''}>
                                <AccordionTrigger className="flex w-full items-center justify-between border-border/50 bg-background data-[state=closed]:border-b">
                                    <WorkflowTaskExecutionItem taskExecution={taskExecution} />
                                </AccordionTrigger>

                                <AccordionContent className="space-y-4 border-b border-b-border/50 p-3">
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
