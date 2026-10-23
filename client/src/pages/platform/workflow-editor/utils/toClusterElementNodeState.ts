import {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export interface ClusterElementCallI {
    endDate?: string | Date;
    error?: string;
    startDate?: string | Date;
    status?: string;
}

function toMillis(date?: string | Date): number | undefined {
    if (date === undefined) {
        return undefined;
    }

    const millis = date instanceof Date ? date.getTime() : Date.parse(date);

    return Number.isNaN(millis) ? undefined : millis;
}

export default function toClusterElementNodeState(
    call: ClusterElementCallI,
    current?: WorkflowTestNodeStateI
): WorkflowTestNodeStateI {
    if (current?.status === 'FAILED') {
        return {...current, clusterElement: true};
    }

    if (call.status !== 'COMPLETED' && call.status !== 'FAILED') {
        return {clusterElement: true, status: 'RUNNING'};
    }

    const startMillis = toMillis(call.startDate);
    const endMillis = toMillis(call.endDate);

    const durationMillis =
        startMillis !== undefined && endMillis !== undefined && endMillis >= startMillis
            ? endMillis - startMillis
            : undefined;

    return {
        clusterElement: true,
        ...(durationMillis !== undefined && {durationMillis}),
        ...(call.status === 'FAILED' && call.error && {error: call.error}),
        status: call.status,
    };
}
