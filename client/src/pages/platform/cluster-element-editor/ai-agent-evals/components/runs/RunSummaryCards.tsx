import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {InfoIcon} from 'lucide-react';

interface RunSummaryI {
    agentVersion?: string | null;
    averageScore?: number | null;
    errorCount: number;
    failedCount: number;
    passedCount: number;
    totalInputTokens?: number | null;
    totalOutputTokens?: number | null;
    totalScenarios: number;
}

interface RunSummaryCardsProps {
    summary: RunSummaryI;
}

const RunSummaryCards = ({summary}: RunSummaryCardsProps) => {
    const scorePercent = summary.averageScore != null ? Math.round(summary.averageScore * 100) : 0;

    const totalInputTokens = summary.totalInputTokens ?? 0;
    const totalOutputTokens = summary.totalOutputTokens ?? 0;
    const hasTokenData = totalInputTokens > 0 || totalOutputTokens > 0;

    return (
        <div className="grid grid-cols-6 gap-3">
            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-gray-500">Avg Score</div>

                <div className="mt-1 text-xl font-semibold">{scorePercent}%</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-gray-500">Scenarios</div>

                <div className="mt-1 text-xl font-semibold">{summary.totalScenarios}</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-gray-500">Passed</div>

                <div className="mt-1 text-xl font-semibold text-green-600">{summary.passedCount}</div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="text-xs text-gray-500">Failed</div>

                <div className="mt-1 text-xl font-semibold text-red-600">
                    {summary.failedCount + summary.errorCount}
                </div>
            </div>

            <div className="rounded-lg border border-border/50 px-3 py-3">
                <div className="flex items-center gap-1 text-xs text-gray-500">
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
                <div className="flex items-center gap-1 text-xs text-gray-500">
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
