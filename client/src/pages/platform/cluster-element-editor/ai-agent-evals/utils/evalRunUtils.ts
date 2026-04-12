import {AiAgentEvalRunStatus} from '@/shared/middleware/graphql';

export const RUN_STATUS_LABELS: Record<AiAgentEvalRunStatus, string> = {
    [AiAgentEvalRunStatus.Completed]: 'Completed',
    [AiAgentEvalRunStatus.Failed]: 'Failed',
    [AiAgentEvalRunStatus.Pending]: 'Pending',
    [AiAgentEvalRunStatus.Running]: 'Running',
};

export const RUN_STATUS_COLORS: Record<AiAgentEvalRunStatus, string> = {
    [AiAgentEvalRunStatus.Completed]: 'border-green-200 bg-green-50 text-green-700',
    [AiAgentEvalRunStatus.Failed]: 'border-red-200 bg-red-50 text-red-700',
    [AiAgentEvalRunStatus.Pending]: 'border-gray-200 bg-gray-50 text-gray-600',
    [AiAgentEvalRunStatus.Running]: 'border-yellow-200 bg-yellow-50 text-yellow-700',
};

export function formatRunDate(epochMillis: number | null | undefined): string {
    if (epochMillis == null) {
        return '--';
    }

    return new Date(epochMillis).toLocaleString(undefined, {
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        month: 'short',
    });
}
