import {
    ClusterElementCallRecord,
    TaskExecution,
    WorkflowTestExecution,
} from '@/shared/middleware/platform/workflow/test';

function mergeRecords(
    stored: ClusterElementCallRecord[],
    live: ClusterElementCallRecord[]
): ClusterElementCallRecord[] {
    const storedIds = new Set(stored.map((record) => record.id));

    return [...stored, ...live.filter((record) => !storedIds.has(record.id))];
}

function mergeTaskExecution(
    taskExecution: TaskExecution,
    recordsByTaskExecutionId: Record<string, ClusterElementCallRecord[]>
): TaskExecution {
    let merged = taskExecution;

    const liveRecords = taskExecution.id == null ? undefined : recordsByTaskExecutionId[String(taskExecution.id)];

    if (liveRecords?.length) {
        const clusterElementCallRecords = mergeRecords(taskExecution.clusterElementCallRecords ?? [], liveRecords);

        merged = {...merged, clusterElementCallRecords};
    }

    if (taskExecution.children) {
        const children = mergeTaskExecutions(taskExecution.children, recordsByTaskExecutionId);

        if (children !== taskExecution.children) {
            merged = {...merged, children};
        }
    }

    if (taskExecution.iterations) {
        let iterationsChanged = false;

        const iterations = taskExecution.iterations.map((iteration) => {
            const mergedIteration = mergeTaskExecutions(iteration, recordsByTaskExecutionId);

            if (mergedIteration !== iteration) {
                iterationsChanged = true;
            }

            return mergedIteration;
        });

        if (iterationsChanged) {
            merged = {...merged, iterations};
        }
    }

    if (taskExecution.childJob?.taskExecutions) {
        const taskExecutions = mergeTaskExecutions(taskExecution.childJob.taskExecutions, recordsByTaskExecutionId);

        if (taskExecutions !== taskExecution.childJob.taskExecutions) {
            merged = {...merged, childJob: {...taskExecution.childJob, taskExecutions}};
        }
    }

    return merged;
}

function mergeTaskExecutions(
    taskExecutions: TaskExecution[],
    recordsByTaskExecutionId: Record<string, ClusterElementCallRecord[]>
): TaskExecution[] {
    let changed = false;

    const mergedTaskExecutions = taskExecutions.map((taskExecution) => {
        const merged = mergeTaskExecution(taskExecution, recordsByTaskExecutionId);

        if (merged !== taskExecution) {
            changed = true;
        }

        return merged;
    });

    return changed ? mergedTaskExecutions : taskExecutions;
}

export default function withLiveClusterElementCallRecords(
    workflowTestExecution: WorkflowTestExecution | undefined,
    recordsByTaskExecutionId: Record<string, ClusterElementCallRecord[]>
): WorkflowTestExecution | undefined {
    const taskExecutions = workflowTestExecution?.job?.taskExecutions;

    if (!workflowTestExecution?.job || !taskExecutions || Object.keys(recordsByTaskExecutionId).length === 0) {
        return workflowTestExecution;
    }

    const mergedTaskExecutions = mergeTaskExecutions(taskExecutions, recordsByTaskExecutionId);

    if (mergedTaskExecutions === taskExecutions) {
        return workflowTestExecution;
    }

    return {
        ...workflowTestExecution,
        job: {...workflowTestExecution.job, taskExecutions: mergedTaskExecutions},
    };
}
