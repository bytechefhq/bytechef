import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export interface ExecutedTaskI {
    children?: ExecutedTaskI[];
    endDate?: Date | string;
    error?: {message?: string};
    iterations?: ExecutedTaskI[][];
    startDate?: Date | string;
    status?: string;
    workflowTask?: {name?: string};
}

type NodeStatusType = WorkflowTestNodeStateI['status'];

const STATUS_PRIORITY: Record<NodeStatusType, number> = {
    COMPLETED: 0,
    FAILED: 2,
    RUNNING: 1,
};

function toNodeStatus(status: string | undefined, running: boolean): NodeStatusType | undefined {
    switch (status) {
        case 'COMPLETED':
            return 'COMPLETED';
        case 'FAILED':
        case 'CANCELLED':
            return 'FAILED';
        case 'CREATED':
        case 'STARTED':
            return running ? 'RUNNING' : undefined;
        default:
            return undefined;
    }
}

function toDurationMillis(startDate?: Date | string, endDate?: Date | string): number | undefined {
    if (!startDate || !endDate) {
        return undefined;
    }

    const durationMillis = new Date(endDate).getTime() - new Date(startDate).getTime();

    return Number.isFinite(durationMillis) && durationMillis >= 0 ? durationMillis : undefined;
}

function mergeNodeState(
    existingNodeState: WorkflowTestNodeStateI | undefined,
    nodeState: WorkflowTestNodeStateI
): WorkflowTestNodeStateI {
    if (!existingNodeState) {
        return nodeState;
    }

    const status =
        STATUS_PRIORITY[nodeState.status] > STATUS_PRIORITY[existingNodeState.status]
            ? nodeState.status
            : existingNodeState.status;

    const durationMillis =
        existingNodeState.durationMillis == null && nodeState.durationMillis == null
            ? undefined
            : (existingNodeState.durationMillis ?? 0) + (nodeState.durationMillis ?? 0);

    return {
        durationMillis,
        error: existingNodeState.error ?? nodeState.error,
        status,
    };
}

export default function buildNodeStatesFromTaskExecutions(
    taskExecutions: ExecutedTaskI[] | undefined,
    running: boolean
): Record<string, WorkflowTestNodeStateI> {
    const nodeStates: Record<string, WorkflowTestNodeStateI> = {};

    const visit = (currentTaskExecutions: ExecutedTaskI[]) => {
        for (const taskExecution of currentTaskExecutions) {
            const name = taskExecution.workflowTask?.name;
            const status = toNodeStatus(taskExecution.status, running);

            if (name && status) {
                nodeStates[name] = mergeNodeState(nodeStates[name], {
                    durationMillis:
                        status === 'RUNNING'
                            ? undefined
                            : toDurationMillis(taskExecution.startDate, taskExecution.endDate),
                    error: status === 'FAILED' ? taskExecution.error?.message : undefined,
                    status,
                });
            }

            visit([...(taskExecution.children ?? []), ...(taskExecution.iterations ?? []).flat()]);
        }
    };

    visit(taskExecutions ?? []);

    return nodeStates;
}
