import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {InfoIcon} from 'lucide-react';

export interface RunSummaryI {
    agentVersion?: string | null;
    averageScore?: number | null;
    errorCount: number;
    failedCount: number;
    inconclusiveCount: number;
    passedCount: number;
    totalInputTokens?: number | null;
    totalOutputTokens?: number | null;
    totalScenarios: number;
}

interface RunSummaryCardsProps {
    summary: RunSummaryI;
}

const RunSummaryCards = ({summary}: RunSummaryCardsProps) => {
    const scorePercent = summary.averageScore != null ? Math.round(summary.averageScore * 100) : null;

    const totalInputTokens = summary.totalInputTokens ?? 0;
    const totalOutputTokens = summary.totalOutputTokens ?? 0;
    const hasTokenData = totalInputTokens > 0 || totalOutputTokens > 0;

    return (
        <div className="grid grid-cols-7 gap-3">
            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-content-neutral-secondary">Avg Score</div>

                <div className="mt-1 text-xl font-semibold">{scorePercent != null ? `${scorePercent}%` : 'N/A'}</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-content-neutral-secondary">Scenarios</div>

                <div className="mt-1 text-xl font-semibold">{summary.totalScenarios}</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-content-neutral-secondary">Passed</div>

                <div className="mt-1 text-xl font-semibold text-content-success">{summary.passedCount}</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-content-neutral-secondary">Failed</div>

                <div className="mt-1 text-xl font-semibold text-content-destructive">
                    {summary.failedCount + summary.errorCount}
                </div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="flex items-center gap-1 text-xs text-content-neutral-secondary">
                    Inconclusive
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoIcon className="size-3 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-64" side="right">
                            Scenarios where a judge errored or could not decide, so they are counted as neither passed
                            nor failed.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <div className="mt-1 text-xl font-semibold text-amber-600 dark:text-amber-400">
                    {summary.inconclusiveCount}
                </div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="flex items-center gap-1 text-xs text-content-neutral-secondary">
                    Tokens
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoIcon className="size-3 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-64" side="right">
                            Total tokens consumed by this evaluation run across all scenarios.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <div className="mt-1 text-xl font-semibold">
                    {hasTokenData ? `${totalInputTokens} in / ${totalOutputTokens} out` : 'N/A'}
                </div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="flex items-center gap-1 text-xs text-content-neutral-secondary">
                    Agent Version
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoIcon className="size-3 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-64" side="right">
                            The version of the Agent that was tested in this evaluation run.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <div className="mt-1 truncate text-xl font-semibold">{summary.agentVersion || 'N/A'}</div>
            </div>
        </div>
    );
};

export default RunSummaryCards;
