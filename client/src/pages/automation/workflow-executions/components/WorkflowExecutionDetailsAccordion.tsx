import {Accordion} from '@/components/ui/accordion';
import {JobModel, TriggerExecutionModel} from '@/middleware/automation/workflow/execution';
import WorkflowExecutionDetailsAccordionBadge from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordionBadge';
import WorkflowExecutionDetailsTaskAccordionItem from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsTaskAccordionItem';
import WorkflowExecutionDetailsTriggerAccordionItem from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsTriggerAccordionItem';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionDetailsAccordion = ({
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

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    return (
        <>
            <div className="px-3 py-4">
                <div className="mb-3 flex items-center gap-x-2">
                    <WorkflowExecutionDetailsAccordionBadge
                        success={!!(taskExecutionsCompleted && triggerExecutionCompleted)}
                    />

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

                <div className="flex justify-between text-xs">
                    <span>
                        {job?.startDate &&
                            `${job?.startDate?.toLocaleDateString()} ${job?.startDate?.toLocaleTimeString()}`}
                    </span>

                    <span>Duration: {duration}ms</span>

                    <span>{`${taskExecutionsCount} task${taskExecutionsCount > 1 ? 's' : ''} executed`}</span>
                </div>
            </div>

            <div>
                <Accordion collapsible defaultValue={triggerExecution?.id || ''} type="single">
                    {triggerExecution && (
                        <WorkflowExecutionDetailsTriggerAccordionItem triggerExecution={triggerExecution} />
                    )}

                    {job?.taskExecutions &&
                        job?.taskExecutions.map((taskExecution) => (
                            <WorkflowExecutionDetailsTaskAccordionItem
                                key={taskExecution.id}
                                taskExecution={taskExecution}
                            />
                        ))}
                </Accordion>
            </div>
        </>
    );
};

export default WorkflowExecutionDetailsAccordion;
