import Button from '@/components/Button/Button';
import AiAgentJudgeVerdictList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AiAgentJudgeVerdictList';
import TranscriptDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/TranscriptDialog';
import {
    AiAgentEvalResultConclusion,
    AiAgentEvalResultStatus,
    type AiAgentEvalRunQuery,
    AiAgentJudgeVerdictStatus,
    AiAgentScenarioType,
} from '@/shared/middleware/graphql';
import {
    CheckCircle2Icon,
    ChevronDownIcon,
    ChevronRightIcon,
    FileTextIcon,
    Loader2Icon,
    TriangleAlertIcon,
    XCircleIcon,
} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

type ResultType = NonNullable<AiAgentEvalRunQuery['aiAgentEvalRun']>['results'][number];

const getJudgeSummary = (verdicts: ResultType['verdicts']) => {
    const passedCount = verdicts.filter((verdict) => verdict.status === AiAgentJudgeVerdictStatus.Pass).length;
    const failedCount = verdicts.filter((verdict) => verdict.status === AiAgentJudgeVerdictStatus.Fail).length;
    const erroredCount = verdicts.filter((verdict) => verdict.status === AiAgentJudgeVerdictStatus.Error).length;
    const undecidedCount = verdicts.length - passedCount - failedCount;

    let undecidedTitle = '';

    if (undecidedCount > 0) {
        undecidedTitle =
            erroredCount > 0
                ? `${undecidedCount} judge(s) did not decide, ${erroredCount} errored`
                : `${undecidedCount} judge(s) did not decide`;
    }

    return {decidedCount: passedCount + failedCount, passedCount, undecidedCount, undecidedTitle};
};

const STATUS_ICONS: Record<AiAgentEvalResultStatus, React.ReactNode> = {
    [AiAgentEvalResultStatus.Completed]: <CheckCircle2Icon className="size-4 text-green-500" />,
    [AiAgentEvalResultStatus.Failed]: <XCircleIcon className="size-4 text-red-500" />,
    [AiAgentEvalResultStatus.Pending]: <div className="size-4 rounded-full border-2 border-stroke-neutral-tertiary" />,
    [AiAgentEvalResultStatus.Running]: <Loader2Icon className="size-4 animate-spin text-blue-500" />,
};

interface ScenarioResultsTableProps {
    results: ResultType[];
}

const ScenarioResultsTable = ({results}: ScenarioResultsTableProps) => {
    const [expandedResultId, setExpandedResultId] = useState<string | null>(null);
    const [transcriptResult, setTranscriptResult] = useState<ResultType | null>(null);

    if (results.length === 0) {
        return <div className="py-6 text-center text-sm text-content-neutral-secondary">No results yet</div>;
    }

    return (
        <>
            <div className="rounded-lg border border-border/50">
                <div className="border-b border-border/50 px-3 py-2">
                    <div className="grid grid-cols-[24px_1fr_100px_80px_80px_90px_56px] items-center gap-2 text-xs font-medium text-content-neutral-secondary">
                        <div />

                        <div>Scenario</div>

                        <div>Type</div>

                        <div>Score</div>

                        <div>Tokens</div>

                        <div>Judges</div>

                        <div>Transcript</div>
                    </div>
                </div>

                {results.map((result) => {
                    const isExpanded = expandedResultId === result.id;
                    const isSingleTurn = result.scenario.type === AiAgentScenarioType.SingleTurn;
                    const scorePercent = result.score != null ? Math.round(result.score * 100) : null;
                    const isInconclusive = result.conclusion === AiAgentEvalResultConclusion.Inconclusive;
                    const isNotApplicable = result.conclusion === AiAgentEvalResultConclusion.NotApplicable;
                    const judgeSummary = getJudgeSummary(result.verdicts);
                    const inputTokens = result.inputTokens ?? 0;
                    const outputTokens = result.outputTokens ?? 0;
                    const hasTokens = inputTokens > 0 || outputTokens > 0;
                    const runIndexSuffix =
                        result.runIndex != null && result.runIndex > 0 ? ` (Run ${result.runIndex})` : '';

                    return (
                        <div className="border-b border-border/30 last:border-b-0" key={result.id}>
                            <div
                                className="grid cursor-pointer grid-cols-[24px_1fr_100px_80px_80px_90px_56px] items-center gap-2 px-3 py-2.5 hover:bg-surface-neutral-primary-hover"
                                onClick={() => setExpandedResultId(isExpanded ? null : result.id)}
                            >
                                <div>{STATUS_ICONS[result.status]}</div>

                                <div className="truncate text-sm font-medium">
                                    {result.scenario.name}

                                    {runIndexSuffix}
                                </div>

                                <div>
                                    <span
                                        className={twMerge(
                                            'rounded-full border px-2 py-0.5 text-xs font-medium',
                                            isSingleTurn
                                                ? 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-800 dark:bg-blue-950 dark:text-blue-300'
                                                : 'border-purple-200 bg-purple-50 text-purple-700 dark:border-purple-800 dark:bg-purple-950 dark:text-purple-300'
                                        )}
                                    >
                                        {isSingleTurn ? 'Single-turn' : 'Multi-turn'}
                                    </span>
                                </div>

                                <div>
                                    {isInconclusive && (
                                        <span
                                            className="text-xs font-medium text-amber-600 dark:text-amber-400"
                                            title="A judge could not reach a decision, so this scenario is neither passed nor failed"
                                        >
                                            Inconclusive
                                        </span>
                                    )}

                                    {isNotApplicable && (
                                        <span
                                            className="text-xs text-content-neutral-tertiary"
                                            title="No judges applied"
                                        >
                                            N/A
                                        </span>
                                    )}

                                    {!isInconclusive && !isNotApplicable && scorePercent != null && (
                                        <div className="flex items-center gap-1.5">
                                            <div className="h-1.5 w-10 overflow-hidden rounded-full bg-surface-neutral-tertiary">
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

                                            <span className="text-xs text-content-neutral-secondary">
                                                {scorePercent}%
                                            </span>
                                        </div>
                                    )}

                                    {!isInconclusive && !isNotApplicable && scorePercent == null && (
                                        <span className="text-xs text-content-neutral-tertiary">--</span>
                                    )}
                                </div>

                                <div className="text-xs text-content-neutral-secondary">
                                    {hasTokens ? `${inputTokens + outputTokens}` : '\u2014'}
                                </div>

                                <div className="flex items-center gap-1 text-xs text-content-neutral-secondary">
                                    {result.verdicts.length > 0
                                        ? `${judgeSummary.passedCount}/${judgeSummary.decidedCount} passed`
                                        : '\u2014'}

                                    {judgeSummary.undecidedCount > 0 && (
                                        <TriangleAlertIcon
                                            aria-label={judgeSummary.undecidedTitle}
                                            className="size-3 shrink-0 text-amber-500"
                                        >
                                            <title>{judgeSummary.undecidedTitle}</title>
                                        </TriangleAlertIcon>
                                    )}
                                </div>

                                <div className="flex items-center gap-1">
                                    {result.transcriptFile && (
                                        <Button
                                            icon={<FileTextIcon />}
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                setTranscriptResult(result);
                                            }}
                                            size="iconXs"
                                            title="View transcript"
                                            variant="ghost"
                                        />
                                    )}

                                    {isExpanded ? (
                                        <ChevronDownIcon className="size-4 text-content-neutral-tertiary" />
                                    ) : (
                                        <ChevronRightIcon className="size-4 text-content-neutral-tertiary" />
                                    )}
                                </div>
                            </div>

                            {isExpanded && (
                                <div className="border-t border-border/30 bg-surface-neutral-secondary/50">
                                    {result.errorMessage && (
                                        <div className="px-3 py-2 text-xs text-content-destructive">
                                            Error: {result.errorMessage}
                                        </div>
                                    )}

                                    <AiAgentJudgeVerdictList verdicts={result.verdicts} />
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {transcriptResult?.transcriptFile && (
                <TranscriptDialog
                    onClose={() => setTranscriptResult(null)}
                    resultId={transcriptResult.id}
                    scenarioName={transcriptResult.scenario.name}
                />
            )}
        </>
    );
};

export default ScenarioResultsTable;
