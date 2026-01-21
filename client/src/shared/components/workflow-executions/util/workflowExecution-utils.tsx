import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {AlertTriangleIcon, CheckIcon, LoaderCircleIcon} from 'lucide-react';

export type WorkflowStatusType = 'completed' | 'running' | 'failed';

export type ExecutionStatusType = TaskExecution['status'] | TriggerExecution['status'];

export function getExecutionStatusIcon(status: ExecutionStatusType) {
    switch (status) {
        case 'COMPLETED':
            return <CheckIcon className="size-4 text-success" />;
        case 'STARTED':
        case 'CREATED':
            return <LoaderCircleIcon className="size-4 animate-spin text-primary" />;
        case 'FAILED':
        case 'CANCELLED':
        default:
            return <AlertTriangleIcon className="size-4 text-destructive" />;
    }
}

export function getWorkflowStatusType(job: Job, triggerExecution?: TriggerExecution): WorkflowStatusType {
    const jobStatus = job?.status;
    const triggerStatus = triggerExecution?.status;

    const isJobCompleted = jobStatus === 'COMPLETED';
    const isTriggerCompleted = !triggerExecution || triggerStatus === 'COMPLETED';

    if (isJobCompleted && isTriggerCompleted) {
        return 'completed';
    }

    const isJobRunning = jobStatus === 'STARTED' || jobStatus === 'CREATED';
    const isTriggerRunning = !triggerExecution || triggerStatus === 'STARTED' || triggerStatus === 'CREATED';

    if (isJobRunning && isTriggerRunning) {
        return 'running';
    }

    return 'failed';
}
