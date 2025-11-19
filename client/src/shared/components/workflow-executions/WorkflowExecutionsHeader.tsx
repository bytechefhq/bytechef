import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsHeader = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
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
        <header className="flex w-full flex-col gap-y-3 px-3 py-4">
            <div className="flex items-center gap-x-2">
                <span
                    className={twMerge(
                        (!taskExecutionsCompleted || !triggerExecutionCompleted) && 'text-destructive',
                        'text-base font-bold uppercase'
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
        </header>
    );
};

export default WorkflowExecutionsHeader;
