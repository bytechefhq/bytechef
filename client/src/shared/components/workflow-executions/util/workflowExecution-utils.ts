import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';

export type WorkflowStatusType = 'completed' | 'running' | 'failed';

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
