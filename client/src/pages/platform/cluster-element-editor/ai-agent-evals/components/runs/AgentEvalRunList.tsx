import RunProgressIndicator from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/RunProgressIndicator';
import {AgentEvalRunStatus, type AgentEvalRunsQuery} from '@/shared/middleware/graphql';
import {HistoryIcon, Loader2Icon, SquareIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type RunListItemType = AgentEvalRunsQuery['agentEvalRuns'][number];

const STATUS_LABELS: Record<AgentEvalRunStatus, string> = {
    [AgentEvalRunStatus.Completed]: 'Completed',
    [AgentEvalRunStatus.Failed]: 'Failed',
    [AgentEvalRunStatus.Pending]: 'Pending',
    [AgentEvalRunStatus.Running]: 'Running',
};

const STATUS_COLORS: Record<AgentEvalRunStatus, string> = {
    [AgentEvalRunStatus.Completed]: 'border-green-200 bg-green-50 text-green-700',
    [AgentEvalRunStatus.Failed]: 'border-red-200 bg-red-50 text-red-700',
    [AgentEvalRunStatus.Pending]: 'border-gray-200 bg-gray-50 text-gray-600',
    [AgentEvalRunStatus.Running]: 'border-yellow-200 bg-yellow-50 text-yellow-700',
};

function formatDate(epochMillis: number | null | undefined): string {
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

interface AgentEvalRunListProps {
    onCancelRun: (id: string) => void;
    onSelectRun: (id: string) => void;
    runs: RunListItemType[];
}

const AgentEvalRunList = ({onCancelRun, onSelectRun, runs}: AgentEvalRunListProps) => {
    if (runs.length === 0) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-full bg-gray-100">
                    <HistoryIcon className="size-6 text-gray-400" />
                </div>

                <h3 className="text-sm font-semibold">No runs yet</h3>

                <p className="max-w-xs text-center text-xs text-gray-500">
                    Click &quot;Run Test&quot; to execute the selected test and see results here.
                </p>
            </div>
        );
    }

    return (
        <div className="rounded-lg border border-border/50">
            <div className="border-b border-border/50 px-3 py-2">
                <div className="grid grid-cols-[1fr_100px_180px_100px_160px] items-center gap-2 text-xs font-medium text-gray-500">
                    <div>Name</div>

                    <div>Status</div>

                    <div>Progress</div>

                    <div>Score</div>

                    <div>Date</div>
                </div>
            </div>

            {runs.map((run) => {
                const scorePercent = run.averageScore != null ? Math.round(run.averageScore * 100) : null;
                const isRunning =
                    run.status === AgentEvalRunStatus.Running || run.status === AgentEvalRunStatus.Pending;

                return (
                    <div
                        className="cursor-pointer border-b border-border/30 px-3 py-2.5 last:border-b-0 hover:bg-gray-50"
                        key={run.id}
                        onClick={() => onSelectRun(run.id)}
                    >
                        <div className="grid grid-cols-[1fr_100px_180px_100px_160px] items-center gap-2">
                            <div className="truncate text-sm font-medium">{run.name}</div>

                            <div>
                                <span
                                    className={twMerge(
                                        'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
                                        STATUS_COLORS[run.status]
                                    )}
                                >
                                    {run.status === AgentEvalRunStatus.Running && (
                                        <Loader2Icon className="size-3 animate-spin" />
                                    )}

                                    {STATUS_LABELS[run.status]}
                                </span>
                            </div>

                            <div>
                                <RunProgressIndicator
                                    completedScenarios={run.completedScenarios}
                                    totalScenarios={run.totalScenarios}
                                />
                            </div>

                            <div>
                                {scorePercent != null ? (
                                    <div className="flex items-center gap-1.5">
                                        <div className="h-1.5 w-10 overflow-hidden rounded-full bg-gray-200">
                                            <div
                                                className={twMerge(
                                                    'h-full rounded-full',
                                                    scorePercent >= 100
                                                        ? 'bg-green-500'
                                                        : scorePercent >= 50
                                                          ? 'bg-yellow-500'
                                                          : 'bg-red-500'
                                                )}
                                                style={{width: `${scorePercent}%`}}
                                            />
                                        </div>

                                        <span className="text-xs text-gray-600">{scorePercent}%</span>
                                    </div>
                                ) : (
                                    <span className="text-xs text-gray-400">--</span>
                                )}
                            </div>

                            <div className="flex items-center gap-2">
                                <span className="text-xs text-gray-500">{formatDate(run.createdDate)}</span>

                                {isRunning && (
                                    <button
                                        className="text-gray-400 hover:text-red-500"
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            onCancelRun(run.id);
                                        }}
                                        title="Cancel run"
                                    >
                                        <SquareIcon className="size-3.5" />
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default AgentEvalRunList;
