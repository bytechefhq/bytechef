import Badge from '@/components/Badge/Badge';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {CheckIcon, LoaderCircleIcon} from 'lucide-react';

const WorkflowExecutionsHeader = ({job, triggerExecution}: {job: Job; triggerExecution?: TriggerExecution}) => {
    const startTime = job?.startDate?.getTime();
    const endTime = job?.endDate?.getTime();

    const workflowStatus = getWorkflowStatusType(job, triggerExecution);

    let duration = 0;

    if (startTime && endTime) {
        duration = Math.round(endTime - startTime);
    }

    const taskExecutionsCount = job?.taskExecutions?.length || 0;

    return (
        <header className="flex w-full items-center gap-x-3 px-3 py-4">
            <div className="flex items-center gap-x-2">
                <span className="text-base font-bold uppercase">
                    {workflowStatus === 'completed' && (
                        <Badge
                            icon={<CheckIcon className="size-5 text-success" />}
                            label="DONE"
                            styleType="success-outline"
                            weight="semibold"
                        />
                    )}

                    {workflowStatus === 'running' && (
                        <Badge
                            icon={<LoaderCircleIcon className="size-5 animate-spin" />}
                            label="Running"
                            styleType="primary-outline"
                            weight="semibold"
                        />
                    )}

                    {workflowStatus === 'failed' && (
                        <Badge label="Workflow failed" styleType="destructive-filled" weight="semibold" />
                    )}
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
