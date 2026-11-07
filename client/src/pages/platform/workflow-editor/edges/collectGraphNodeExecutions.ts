import {TaskExecution} from '@/shared/middleware/platform/workflow/test';

import {GraphNodeExecutionI} from './getExecutedEdgeStatus';

function findTaskExecutions(taskExecutions: TaskExecution[], workflowNodeName: string): TaskExecution[] {
    return taskExecutions.flatMap((taskExecution) => {
        if (taskExecution.workflowTask?.name === workflowNodeName) {
            return [taskExecution];
        }

        const iterationTaskExecutions = (taskExecution.iterations ?? []).flat();

        const nestedTaskExecutions = [...(taskExecution.children ?? []), ...iterationTaskExecutions];

        return findTaskExecutions(nestedTaskExecutions, workflowNodeName);
    });
}

export default function collectGraphNodeExecutions(
    taskExecutions: TaskExecution[] | undefined,
    graphId: string
): GraphNodeExecutionI[][] {
    const graphTaskExecutions = findTaskExecutions(taskExecutions ?? [], graphId);

    return graphTaskExecutions.map((graphTaskExecution) =>
        (graphTaskExecution.children ?? []).map((child) => ({
            nodeName: child.workflowTask?.name ?? '',
            startDate: child.startDate ? new Date(child.startDate) : undefined,
            status: child.status,
        }))
    );
}
