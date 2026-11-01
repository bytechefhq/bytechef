import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export interface ExecutedClusterElementCallI {
    endDate?: Date | string;
    error?: string;
    name?: string;
    startDate?: Date | string;
    status?: string;
}

export interface ExecutedTaskI {
    children?: ExecutedTaskI[];
    clusterElementCalls?: ExecutedClusterElementCallI[];
    endDate?: Date | string;
    error?: {message?: string};
    iterations?: ExecutedTaskI[][];
    startDate?: Date | string;
    status?: string;
    workflowTask?: {name?: string};
}

type NodeStatusType = WorkflowTestNodeStateI['status'];

const STATUS_PRIORITY: Record<NodeStatusType, number> = {
    BLOCKED: 3,
    COMPLETED: 0,
    DENIED: 5,
    FAILED: 6,
    REDACTED: 1,
    RUNNING: 2,
    WITHHELD: 4,
};

function toNodeStatus(status: string | undefined, running: boolean): NodeStatusType | undefined {
    switch (status) {
        case 'COMPLETED':
            return 'COMPLETED';
        case 'BLOCKED':
            return 'BLOCKED';
        case 'DENIED':
            return 'DENIED';
        case 'REDACTED':
            return 'REDACTED';
        case 'WITHHELD':
            return 'WITHHELD';
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
        ...((existingNodeState.clusterElement || nodeState.clusterElement) && {clusterElement: true}),
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

            for (const clusterElementCall of taskExecution.clusterElementCalls ?? []) {
                const clusterElementStatus = toNodeStatus(clusterElementCall.status, running);

                if (clusterElementCall.name && clusterElementStatus) {
                    nodeStates[clusterElementCall.name] = mergeNodeState(nodeStates[clusterElementCall.name], {
                        clusterElement: true,
                        durationMillis:
                            clusterElementStatus === 'RUNNING'
                                ? undefined
                                : toDurationMillis(clusterElementCall.startDate, clusterElementCall.endDate),
                        error: clusterElementStatus === 'FAILED' ? clusterElementCall.error : undefined,
                        status: clusterElementStatus,
                    });
                }
            }

            visit([...(taskExecution.children ?? []), ...(taskExecution.iterations ?? []).flat()]);
        }
    };

    visit(taskExecutions ?? []);

    return nodeStates;
}
