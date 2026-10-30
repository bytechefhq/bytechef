import {TaskExecution} from '@/shared/middleware/platform/workflow/test';
import {useMemo} from 'react';

import {type WorkflowExecutionOverlayI} from '../providers/workflowExecutionOverlayContext';
import buildNodeStatesFromTaskExecutions from '../utils/buildNodeStatesFromTaskExecutions';

interface OverlaidJobI {
    status?: string;
    taskExecutions?: TaskExecution[];
}

export default function useJobExecutionOverlay(
    job: OverlaidJobI | undefined,
    triggerName?: string
): WorkflowExecutionOverlayI {
    const isRunning = job?.status === 'CREATED' || job?.status === 'STARTED';
    const taskExecutions = job?.taskExecutions;

    return useMemo(
        () => ({
            isRunning,
            nodeStates: {
                ...(triggerName && {[triggerName]: {status: 'COMPLETED' as const}}),
                ...buildNodeStatesFromTaskExecutions(taskExecutions, isRunning),
            },
            taskExecutions: taskExecutions ?? [],
        }),
        [isRunning, taskExecutions, triggerName]
    );
}
