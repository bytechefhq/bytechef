import {Accordion} from '@/components/ui/accordion';
import {WorkflowExecutionModel} from '@/middleware/helios/execution';
import WorkflowExecutionsTasksAccordionItem from '@/pages/automation/workflow-executions/components/WorkflowExecutionsTasksAccordionItem';
import WorkflowExecutionsTriggerAccordionItem from '@/pages/automation/workflow-executions/components/WorkflowExecutionsTriggerAccordionItem';
import {CheckCircledIcon} from '@radix-ui/react-icons';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsDetailsAccordion = ({
    workflowExecution,
}: {
    workflowExecution: WorkflowExecutionModel;
}) => {
    const startTime = workflowExecution?.job?.startDate?.getTime();
    const endTime = workflowExecution?.job?.endDate?.getTime();

    const taskExecutionsCompleted =
        workflowExecution?.job?.taskExecutions?.every(
            (taskExecution) => taskExecution.status === 'COMPLETED'
        );
    const triggerExecutionCompleted =
        workflowExecution?.triggerExecution?.status === 'COMPLETED';

    let duration;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount =
        workflowExecution.job?.taskExecutions?.length || 0;

    return (
        <>
            <div className="px-3 py-4">
                <div className="mb-3 flex items-center justify-between">
                    <span className="text-lg">
                        {taskExecutionsCompleted && triggerExecutionCompleted
                            ? 'Workflow executed successfully'
                            : 'Workflow failed'}
                    </span>

                    <CheckCircledIcon
                        className={twMerge(
                            'h-5 w-5',
                            taskExecutionsCompleted && triggerExecutionCompleted
                                ? 'text-green-500'
                                : 'text-red-500'
                        )}
                    />
                </div>

                <div className="flex justify-between text-xs">
                    <span>
                        {workflowExecution.job?.startDate &&
                            `${workflowExecution.job?.startDate?.toLocaleDateString()} ${workflowExecution?.job?.startDate?.toLocaleTimeString()}`}
                    </span>

                    <span>Duration: {duration}ms</span>

                    <span>
                        {`${taskExecutionsCount} task${
                            taskExecutionsCount > 1 ? 's' : ''
                        } executed`}
                    </span>
                </div>
            </div>

            <div className="overflow-y-auto">
                <Accordion
                    collapsible
                    defaultValue={workflowExecution.triggerExecution?.id || ''}
                    type="single"
                >
                    {workflowExecution.triggerExecution && (
                        <WorkflowExecutionsTriggerAccordionItem
                            triggerExecution={
                                workflowExecution.triggerExecution
                            }
                            triggerExecutionCompleted={
                                triggerExecutionCompleted
                            }
                        />
                    )}

                    {workflowExecution.job?.taskExecutions && (
                        <WorkflowExecutionsTasksAccordionItem
                            taskExecutions={
                                workflowExecution.job.taskExecutions
                            }
                            taskExecutionsCompleted={!!taskExecutionsCompleted}
                        />
                    )}
                </Accordion>
            </div>
        </>
    );
};

export default WorkflowExecutionsDetailsAccordion;
