/**
 * Formatters shared between the AiExperiments list, comparison view, and runs drill-in. Extracted so the
 * untrusted-input branches (null / negative / extreme values) can be tested in isolation — the components
 * themselves render React and require a heavier test harness to exercise.
 *
 * The em-dash placeholder is used uniformly: missing data renders the same way regardless of whether the
 * value is null, undefined, or absent from the GraphQL response. Unifying the placeholder simplifies the
 * grid layout (every cell is a single token, never an empty string that collapses table column widths).
 */

const PLACEHOLDER = '—';

export const formatCost = (cost: number | null | undefined): string => {
    if (cost == null) {
        return PLACEHOLDER;
    }

    return `$${cost.toFixed(4)}`;
};

export const formatLatency = (latencyMs: number | null | undefined): string => {
    if (latencyMs == null) {
        return PLACEHOLDER;
    }

    if (latencyMs < 1000) {
        return `${latencyMs} ms`;
    }

    return `${(latencyMs / 1000).toFixed(2)} s`;
};

interface ScoreLikeI {
    dataType: string;
    stringValue?: string | null;
    value?: number | null;
}

export const formatScore = (score: ScoreLikeI): string => {
    if (score.dataType === 'NUMERIC' || score.dataType === 'BOOLEAN') {
        return score.value != null ? score.value.toString() : PLACEHOLDER;
    }

    return score.stringValue ?? PLACEHOLDER;
};

export const formatTimestamp = (epochMs: number | null | undefined): string => {
    if (epochMs == null) {
        return PLACEHOLDER;
    }

    return new Date(epochMs).toLocaleString();
};

const STATUS_BADGE_CLASSES: Record<string, string> = {
    COMPLETED: 'bg-emerald-100 text-emerald-900 dark:bg-emerald-900/30 dark:text-emerald-300',
    FAILED: 'bg-destructive/10 text-destructive',
    PENDING: 'bg-muted text-muted-foreground',
    RUNNING: 'bg-surface-brand-secondary text-content-brand-primary',
};

export const statusBadgeClass = (status: string): string =>
    STATUS_BADGE_CLASSES[status] ?? 'bg-secondary text-secondary-foreground';
