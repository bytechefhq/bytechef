import {AiAgentEvalRunStatus} from '@/shared/middleware/graphql';

export const RUN_STATUS_LABELS: Record<AiAgentEvalRunStatus, string> = {
    [AiAgentEvalRunStatus.Completed]: 'Completed',
    [AiAgentEvalRunStatus.Failed]: 'Failed',
    [AiAgentEvalRunStatus.Pending]: 'Pending',
    [AiAgentEvalRunStatus.Running]: 'Running',
};

export const RUN_STATUS_COLORS: Record<AiAgentEvalRunStatus, string> = {
    [AiAgentEvalRunStatus.Completed]:
        'border-stroke-success-secondary bg-surface-success-secondary text-content-success',
    [AiAgentEvalRunStatus.Failed]:
        'border-stroke-destructive-secondary bg-surface-destructive-secondary text-content-destructive',
    [AiAgentEvalRunStatus.Pending]:
        'border-stroke-neutral-secondary bg-surface-neutral-secondary text-content-neutral-secondary',
    [AiAgentEvalRunStatus.Running]:
        'border-stroke-warning-secondary bg-surface-warning-secondary text-content-warning-primary',
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
