import {TaskExecution} from '@/shared/middleware/platform/workflow/test';
import {useMemo} from 'react';

import {type WorkflowExecutionOverlayI} from '../providers/workflowExecutionOverlayContext';
import buildNodeStatesFromTaskExecutions from '../utils/buildNodeStatesFromTaskExecutions';

interface OverlaidJobI {
    status?: string;
    taskExecutions?: TaskExecution[];
}

/**
 * The overlay a read-only canvas draws for one job. Recomputed only when the job's task executions change,
 * which a polled, still-running job does on every refresh — so its nodes turn green as they finish.
 */
export default function useJobExecutionOverlay(job: OverlaidJobI | undefined): WorkflowExecutionOverlayI {
    const isRunning = job?.status === 'CREATED' || job?.status === 'STARTED';
    const taskExecutions = job?.taskExecutions;

    return useMemo(
        () => ({
            isRunning,
            nodeStates: buildNodeStatesFromTaskExecutions(taskExecutions, isRunning),
            taskExecutions: taskExecutions ?? [],
        }),
        [isRunning, taskExecutions]
    );
}
