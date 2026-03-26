interface RunSummaryI {
    averageScore?: number | null;
    errorCount: number;
    failedCount: number;
    passedCount: number;
    totalScenarios: number;
}

interface RunSummaryCardsProps {
    summary: RunSummaryI;
}

const RunSummaryCards = ({summary}: RunSummaryCardsProps) => {
    const scorePercent = summary.averageScore != null ? Math.round(summary.averageScore * 100) : 0;

    return (
        <div className="grid grid-cols-4 gap-3">
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
        </div>
    );
};

export default RunSummaryCards;
