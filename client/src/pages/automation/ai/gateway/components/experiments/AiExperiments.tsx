import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiExperimentComparisonQuery,
    AiExperimentRunsQuery,
    AiExperimentsQuery,
    useAiExperimentComparisonQuery,
    useAiExperimentRunsQuery,
    useAiExperimentsQuery,
} from '@/shared/middleware/graphql';
import {ChartBarIcon, ChevronLeftIcon, FlaskConicalIcon, GitCompareIcon, ListIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import {formatCost, formatLatency, formatScore, formatTimestamp, statusBadgeClass} from './format';

type ExperimentType = AiExperimentsQuery['aiExperiments'][number];
type ExperimentComparisonType = NonNullable<AiExperimentComparisonQuery['experimentComparison']>;
type ExperimentSummaryType = ExperimentComparisonType['experiments'][number];
type AggregateScoreDeltaType = ExperimentComparisonType['aggregateScoreDeltas'][number];
type ExperimentComparisonRowType = ExperimentComparisonType['rows'][number];
type ExperimentRunPointType = ExperimentComparisonRowType['runsByExperiment'][number];
type AiExperimentRunType = AiExperimentRunsQuery['aiExperimentRuns'][number];

const AiExperiments = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [comparisonIds, setComparisonIds] = useState<string[]>([]);
    const [viewedRunsExperiment, setViewedRunsExperiment] = useState<ExperimentType | null>(null);

    const workspaceIdString = currentWorkspaceId != null ? String(currentWorkspaceId) : '';

    const {
        data: experimentsData,
        error: experimentsError,
        isLoading: experimentsIsLoading,
    } = useAiExperimentsQuery({workspaceId: workspaceIdString}, {enabled: workspaceIdString.length > 0});

    const experiments = useMemo<ExperimentType[]>(
        () =>
            (experimentsData?.aiExperiments ?? [])
                .slice()
                .sort((left, right) => (right.createdDate ?? 0) - (left.createdDate ?? 0)),
        [experimentsData]
    );

    const {
        data: comparisonData,
        error: comparisonError,
        isFetching: comparisonIsFetching,
    } = useAiExperimentComparisonQuery({experimentIds: comparisonIds}, {enabled: comparisonIds.length > 0});

    const comparison = comparisonData?.experimentComparison;

    const experimentColumnIds = useMemo(
        () => (comparison?.experiments ?? []).map((experiment) => experiment.id),
        [comparison]
    );

    const toggleSelected = (id: string) => {
        setSelectedIds((previous) => {
            const next = new Set(previous);

            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }

            return next;
        });
    };

    const handleCompare = () => {
        if (selectedIds.size === 0) {
            return;
        }

        setComparisonIds(Array.from(selectedIds));
    };

    const handleClearComparison = () => {
        setComparisonIds([]);
    };

    const canCompare = selectedIds.size >= 1;

    if (viewedRunsExperiment != null) {
        return <ExperimentRunsView experiment={viewedRunsExperiment} onBack={() => setViewedRunsExperiment(null)} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Experiments</h2>

                <div className="text-sm text-muted-foreground">
                    Read-only view; experiments are created via the SDK / public REST API.
                </div>
            </div>

            <PageLoader errors={[experimentsError]} loading={experimentsIsLoading}>
                {experiments.length === 0 ? (
                    <EmptyList
                        icon={<FlaskConicalIcon className="size-8 text-muted-foreground" />}
                        message="Use the public REST API or SDK to create one — POST /api/ai-gateway/v1/experiments."
                        title="No experiments yet"
                    />
                ) : (
                    <ExperimentList
                        canCompare={canCompare}
                        comparisonIds={comparisonIds}
                        experiments={experiments}
                        onClearComparison={handleClearComparison}
                        onCompare={handleCompare}
                        onToggleSelected={toggleSelected}
                        onViewRuns={setViewedRunsExperiment}
                        selectedIds={selectedIds}
                    />
                )}

                {comparisonIds.length > 0 && (
                    <div className="mt-8">
                        <h3 className="mb-3 flex items-center gap-2 text-base font-semibold">
                            <GitCompareIcon className="size-4" />
                            Comparison
                        </h3>

                        {comparisonError != null && (
                            <div className="mb-4 rounded-md border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
                                {`Failed to load comparison: ${
                                    comparisonError instanceof Error ? comparisonError.message : 'unknown error'
                                }`}
                            </div>
                        )}

                        <PageLoader loading={comparisonIsFetching}>
                            {comparison != null && (
                                <div className="space-y-6">
                                    <ExperimentSummaryCards experiments={comparison.experiments} />

                                    <AggregateScoreDeltas
                                        deltas={comparison.aggregateScoreDeltas}
                                        experimentIds={experimentColumnIds}
                                    />

                                    <PerItemRunsTable comparison={comparison} experimentIds={experimentColumnIds} />
                                </div>
                            )}
                        </PageLoader>
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

interface ExperimentListProps {
    canCompare: boolean;
    comparisonIds: string[];
    experiments: ExperimentType[];
    onClearComparison: () => void;
    onCompare: () => void;
    onToggleSelected: (id: string) => void;
    onViewRuns: (experiment: ExperimentType) => void;
    selectedIds: Set<string>;
}

const ExperimentList = ({
    canCompare,
    comparisonIds,
    experiments,
    onClearComparison,
    onCompare,
    onToggleSelected,
    onViewRuns,
    selectedIds,
}: ExperimentListProps) => (
    <section>
        <div className="mb-3 flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
                Select two or more experiments and click Compare to load the side-by-side view.
            </p>

            <div className="flex gap-2">
                {comparisonIds.length > 0 && (
                    <button
                        className="rounded-md border bg-background px-3 py-1.5 text-sm hover:bg-muted"
                        onClick={onClearComparison}
                        type="button"
                    >
                        Clear comparison
                    </button>
                )}

                <button
                    className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={!canCompare}
                    onClick={onCompare}
                    type="button"
                >
                    <GitCompareIcon className="size-4" />

                    <span>{`Compare (${selectedIds.size})`}</span>
                </button>
            </div>
        </div>

        <div className="overflow-x-auto rounded-md border">
            <table className="min-w-full text-sm">
                <thead className="bg-muted/50">
                    <tr>
                        <th className="w-12 px-3 py-2 text-left">
                            <span className="sr-only">Select</span>
                        </th>

                        <th className="px-3 py-2 text-left font-medium">ID</th>

                        <th className="px-3 py-2 text-left font-medium">Status</th>

                        <th className="px-3 py-2 text-left font-medium">Model</th>

                        <th className="px-3 py-2 text-right font-medium">Runs</th>

                        <th className="px-3 py-2 text-left font-medium">Created</th>

                        <th className="px-3 py-2 text-left font-medium">Completed</th>

                        <th className="px-3 py-2 text-right font-medium">
                            <span className="sr-only">Actions</span>
                        </th>
                    </tr>
                </thead>

                <tbody>
                    {experiments.map((experiment) => (
                        <tr className="border-t hover:bg-muted/30" key={experiment.id}>
                            <td className="px-3 py-2">
                                <input
                                    aria-label={`Select experiment ${experiment.id}`}
                                    checked={selectedIds.has(experiment.id)}
                                    className="size-4"
                                    onChange={() => onToggleSelected(experiment.id)}
                                    type="checkbox"
                                />
                            </td>

                            <td className="px-3 py-2 font-mono text-xs">#{experiment.id}</td>

                            <td className="px-3 py-2">
                                <span
                                    className={`rounded px-1.5 py-0.5 text-xs font-medium ${statusBadgeClass(experiment.status)}`}
                                >
                                    {experiment.status}
                                </span>

                                {experiment.stopRequested && (
                                    <span className="ml-2 text-xs text-muted-foreground">stop requested</span>
                                )}
                            </td>

                            <td className="px-3 py-2">{experiment.model ?? '—'}</td>

                            <td className="px-3 py-2 text-right font-mono">
                                <span>{`${experiment.completedRuns}/${experiment.totalRuns}`}</span>

                                {experiment.failedRuns > 0 && (
                                    <span className="ml-2 text-destructive">{`(${experiment.failedRuns} failed)`}</span>
                                )}
                            </td>

                            <td className="px-3 py-2 text-xs">{formatTimestamp(experiment.createdDate)}</td>

                            <td className="px-3 py-2 text-xs">{formatTimestamp(experiment.completedDate)}</td>

                            <td className="px-3 py-2 text-right">
                                <button
                                    aria-label={`View runs for experiment ${experiment.id}`}
                                    className="inline-flex items-center gap-1 rounded-md border bg-background px-2 py-1 text-xs hover:bg-muted"
                                    onClick={() => onViewRuns(experiment)}
                                    type="button"
                                >
                                    <ListIcon className="size-3" />
                                    View runs
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    </section>
);

interface ExperimentSummaryCardsProps {
    experiments: ExperimentSummaryType[];
}

const ExperimentSummaryCards = ({experiments}: ExperimentSummaryCardsProps) => {
    if (experiments.length === 0) {
        return null;
    }

    return (
        <section>
            <h4 className="mb-3 text-sm font-semibold text-muted-foreground uppercase">Summaries</h4>

            <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
                {experiments.map((experiment) => {
                    const successRate =
                        experiment.totalRuns > 0 ? (experiment.completedRuns / experiment.totalRuns) * 100 : 0;

                    return (
                        <div className="rounded-md border bg-card p-4 shadow-sm" key={experiment.id}>
                            <div className="mb-2 flex items-center justify-between">
                                <span className="font-mono text-sm text-muted-foreground">#{experiment.id}</span>

                                <span className="rounded bg-secondary px-2 py-0.5 text-xs">
                                    {experiment.model ?? 'no model'}
                                </span>
                            </div>

                            <dl className="grid grid-cols-2 gap-x-3 gap-y-1 text-sm">
                                <dt className="text-muted-foreground">Runs</dt>

                                <dd className="text-right font-medium">
                                    <span>{`${experiment.completedRuns}/${experiment.totalRuns}`}</span>

                                    {experiment.failedRuns > 0 && (
                                        <span className="ml-2 text-destructive">{`(${experiment.failedRuns} failed)`}</span>
                                    )}
                                </dd>

                                <dt className="text-muted-foreground">Success rate</dt>

                                <dd className="text-right font-medium">{successRate.toFixed(1)}%</dd>

                                <dt className="text-muted-foreground">Total cost</dt>

                                <dd className="text-right font-medium">{formatCost(experiment.totalCost)}</dd>

                                <dt className="text-muted-foreground">Avg latency</dt>

                                <dd className="text-right font-medium">{formatLatency(experiment.averageLatencyMs)}</dd>
                            </dl>
                        </div>
                    );
                })}
            </div>
        </section>
    );
};

interface AggregateScoreDeltasProps {
    deltas: AggregateScoreDeltaType[];
    experimentIds: string[];
}

const AggregateScoreDeltas = ({deltas, experimentIds}: AggregateScoreDeltasProps) => {
    if (deltas.length === 0) {
        return (
            <section>
                <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold text-muted-foreground uppercase">
                    <ChartBarIcon className="size-4" />
                    Aggregate score deltas
                </h4>

                <p className="text-sm text-muted-foreground">No scored runs to compare yet.</p>
            </section>
        );
    }

    return (
        <section>
            <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold text-muted-foreground uppercase">
                <ChartBarIcon className="size-4" />
                Aggregate score deltas
            </h4>

            <div className="overflow-x-auto rounded-md border">
                <table className="min-w-full text-sm">
                    <thead className="bg-muted/50">
                        <tr>
                            <th className="px-3 py-2 text-left font-medium">Score</th>

                            {experimentIds.map((experimentId) => (
                                <th className="px-3 py-2 text-right font-mono text-xs" key={experimentId}>
                                    #{experimentId}
                                </th>
                            ))}
                        </tr>
                    </thead>

                    <tbody>
                        {deltas.map((delta) => {
                            const averageByExperimentId = new Map<string, {average: number | null; count: number}>();

                            for (const point of delta.deltas) {
                                averageByExperimentId.set(point.experimentId, {
                                    average: point.average ?? null,
                                    count: point.count,
                                });
                            }

                            return (
                                <tr className="border-t" key={delta.scoreName}>
                                    <td className="px-3 py-2 font-medium">{delta.scoreName}</td>

                                    {experimentIds.map((experimentId) => {
                                        const point = averageByExperimentId.get(experimentId);

                                        return (
                                            <td className="px-3 py-2 text-right font-mono" key={experimentId}>
                                                {point?.average != null ? point.average.toFixed(3) : '—'}

                                                {point != null && (
                                                    <span className="ml-1 text-xs text-muted-foreground">
                                                        (n={point.count})
                                                    </span>
                                                )}
                                            </td>
                                        );
                                    })}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </section>
    );
};

interface PerItemRunsTableProps {
    comparison: ExperimentComparisonType;
    experimentIds: string[];
}

const PerItemRunsTable = ({comparison, experimentIds}: PerItemRunsTableProps) => {
    if (comparison.rows.length === 0) {
        return (
            <section>
                <h4 className="mb-3 text-sm font-semibold text-muted-foreground uppercase">Per-item runs</h4>

                <p className="text-sm text-muted-foreground">
                    No dataset items have been replayed across the selected experiments yet.
                </p>
            </section>
        );
    }

    return (
        <section>
            <h4 className="mb-3 text-sm font-semibold text-muted-foreground uppercase">Per-item runs</h4>

            <div className="overflow-x-auto rounded-md border">
                <table className="min-w-full text-sm">
                    <thead className="bg-muted/50">
                        <tr>
                            <th className="px-3 py-2 text-left font-medium">Dataset item</th>

                            {experimentIds.map((experimentId) => (
                                <th className="px-3 py-2 text-left font-mono text-xs" key={experimentId}>
                                    #{experimentId}
                                </th>
                            ))}
                        </tr>
                    </thead>

                    <tbody>
                        {comparison.rows.map((row) => {
                            const runByExperimentId = new Map<string, ExperimentRunPointType>(
                                row.runsByExperiment.map((run) => [run.experimentId, run])
                            );

                            return (
                                <tr className="border-t align-top" key={row.datasetItemId}>
                                    <td className="px-3 py-2 font-mono text-xs text-muted-foreground">
                                        #{row.datasetItemId}
                                    </td>

                                    {experimentIds.map((experimentId) => {
                                        const run = runByExperimentId.get(experimentId);

                                        if (run == null) {
                                            return (
                                                <td className="px-3 py-2 text-muted-foreground" key={experimentId}>
                                                    —
                                                </td>
                                            );
                                        }

                                        return <RunCell key={experimentId} run={run} />;
                                    })}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </section>
    );
};

interface RunCellProps {
    run: ExperimentRunPointType;
}

const RunCell = ({run}: RunCellProps) => (
    <td className="px-3 py-2">
        <div className="flex items-center gap-2">
            <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${statusBadgeClass(run.status)}`}>
                {run.status}
            </span>

            <span className="text-xs text-muted-foreground">{formatLatency(run.latencyMs)}</span>
        </div>

        <div className="mt-1 text-xs text-muted-foreground">cost {formatCost(run.cost)}</div>

        {run.scores.length > 0 && (
            <ul className="mt-2 space-y-0.5 text-xs">
                {run.scores.map((score) => (
                    <li className="flex justify-between gap-2" key={score.name}>
                        <span className="text-muted-foreground">{score.name}</span>

                        <span className="font-mono">{formatScore(score)}</span>
                    </li>
                ))}
            </ul>
        )}
    </td>
);

interface ExperimentRunsViewProps {
    experiment: ExperimentType;
    onBack: () => void;
}

const ExperimentRunsView = ({experiment, onBack}: ExperimentRunsViewProps) => {
    const {
        data: runsData,
        error: runsError,
        isLoading: runsIsLoading,
    } = useAiExperimentRunsQuery({experimentId: experiment.id});

    const runs: AiExperimentRunType[] = useMemo(
        () =>
            (runsData?.aiExperimentRuns ?? [])
                .slice()
                .sort((left, right) => (left.id != null && right.id != null ? Number(left.id) - Number(right.id) : 0)),
        [runsData]
    );

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4">
                <button
                    className="mb-1 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                    type="button"
                >
                    <ChevronLeftIcon className="size-4" />
                    Back to experiments
                </button>

                <h2 className="text-lg font-semibold">
                    {`Experiment `}

                    <span className="font-mono">{`#${experiment.id}`}</span>

                    {experiment.model != null && (
                        <span className="ml-2 rounded bg-secondary px-2 py-0.5 text-xs">{experiment.model}</span>
                    )}
                </h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    {`${experiment.completedRuns}/${experiment.totalRuns} completed`}

                    {experiment.failedRuns > 0 && (
                        <span className="ml-2 text-destructive">{`(${experiment.failedRuns} failed)`}</span>
                    )}
                </p>
            </div>

            <PageLoader errors={[runsError]} loading={runsIsLoading}>
                {runs.length === 0 ? (
                    <EmptyList
                        icon={<FlaskConicalIcon className="size-8 text-muted-foreground" />}
                        message="The replay loop has not produced any runs yet, or this experiment was created without a dataset version."
                        title="No runs"
                    />
                ) : (
                    <div className="overflow-x-auto rounded-md border">
                        <table className="min-w-full text-sm">
                            <thead className="bg-muted/50">
                                <tr>
                                    <th className="px-3 py-2 text-left font-medium">Run</th>

                                    <th className="px-3 py-2 text-left font-medium">Status</th>

                                    <th className="px-3 py-2 text-left font-medium">Dataset item</th>

                                    <th className="px-3 py-2 text-left font-medium">Trace</th>

                                    <th className="px-3 py-2 text-right font-medium">Latency</th>

                                    <th className="px-3 py-2 text-right font-medium">Cost</th>

                                    <th className="px-3 py-2 text-left font-medium">Created</th>

                                    <th className="px-3 py-2 text-left font-medium">Error</th>
                                </tr>
                            </thead>

                            <tbody>
                                {runs.map((run) => (
                                    <tr className="border-t hover:bg-muted/30" key={run.id}>
                                        <td className="px-3 py-2 font-mono text-xs">{`#${run.id}`}</td>

                                        <td className="px-3 py-2">
                                            <span
                                                className={`rounded px-1.5 py-0.5 text-xs font-medium ${statusBadgeClass(run.status)}`}
                                            >
                                                {run.status}
                                            </span>
                                        </td>

                                        <td className="px-3 py-2 font-mono text-xs">{`#${run.datasetItemId}`}</td>

                                        <td className="px-3 py-2 font-mono text-xs">
                                            {run.traceId != null ? `#${run.traceId}` : '—'}
                                        </td>

                                        <td className="px-3 py-2 text-right font-mono text-xs">
                                            {formatLatency(run.latencyMs)}
                                        </td>

                                        <td className="px-3 py-2 text-right font-mono text-xs">
                                            {formatCost(run.cost)}
                                        </td>

                                        <td className="px-3 py-2 text-xs">{formatTimestamp(run.createdDate)}</td>

                                        <td className="px-3 py-2 text-xs text-destructive">
                                            {run.errorMessage ?? '—'}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

export default AiExperiments;
