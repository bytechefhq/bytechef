import AgentJudgeVerdictList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentJudgeVerdictList';
import TranscriptDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/TranscriptDialog';
import {AgentEvalResultStatus, type AgentEvalRunQuery, AgentScenarioType} from '@/shared/middleware/graphql';
import {
    CheckCircle2Icon,
    ChevronDownIcon,
    ChevronRightIcon,
    FileTextIcon,
    Loader2Icon,
    XCircleIcon,
} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

type ResultType = NonNullable<AgentEvalRunQuery['agentEvalRun']>['results'][number];

const STATUS_ICONS: Record<AgentEvalResultStatus, React.ReactNode> = {
    [AgentEvalResultStatus.Completed]: <CheckCircle2Icon className="size-4 text-green-500" />,
    [AgentEvalResultStatus.Failed]: <XCircleIcon className="size-4 text-red-500" />,
    [AgentEvalResultStatus.Pending]: <div className="size-4 rounded-full border-2 border-gray-300" />,
    [AgentEvalResultStatus.Running]: <Loader2Icon className="size-4 animate-spin text-blue-500" />,
};

interface ScenarioResultsTableProps {
    results: ResultType[];
}

const ScenarioResultsTable = ({results}: ScenarioResultsTableProps) => {
    const [expandedResultId, setExpandedResultId] = useState<string | null>(null);
    const [transcriptResult, setTranscriptResult] = useState<ResultType | null>(null);

    if (results.length === 0) {
        return <div className="py-6 text-center text-sm text-gray-500">No results yet</div>;
    }

    return (
        <>
            <div className="rounded-lg border border-border/50">
                <div className="border-b border-border/50 px-3 py-2">
                    <div className="grid grid-cols-[24px_1fr_100px_80px_80px_60px_32px] items-center gap-2 text-xs font-medium text-gray-500">
                        <div />

                        <div>Scenario</div>

                        <div>Type</div>

                        <div>Score</div>

                        <div>Tokens</div>

                        <div>Judges</div>

                        <div />
                    </div>
                </div>

                {results.map((result) => {
                    const isExpanded = expandedResultId === result.id;
                    const isSingleTurn = result.scenario.type === AgentScenarioType.SingleTurn;
                    const scorePercent = result.score != null ? Math.round(result.score * 100) : null;
                    const inputTokens = result.inputTokens ?? 0;
                    const outputTokens = result.outputTokens ?? 0;
                    const hasTokens = inputTokens > 0 || outputTokens > 0;
                    const runIndexSuffix =
                        result.runIndex != null && result.runIndex > 0 ? ` (Run ${result.runIndex + 1})` : '';

                    return (
                        <div className="border-b border-border/30 last:border-b-0" key={result.id}>
                            <div
                                className="grid cursor-pointer grid-cols-[24px_1fr_100px_80px_80px_60px_32px] items-center gap-2 px-3 py-2.5 hover:bg-gray-50"
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
                                                ? 'border-blue-200 bg-blue-50 text-blue-700'
                                                : 'border-purple-200 bg-purple-50 text-purple-700'
                                        )}
                                    >
                                        {isSingleTurn ? 'Single-turn' : 'Multi-turn'}
                                    </span>
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

                                <div className="text-xs text-gray-500">
                                    {hasTokens ? `${inputTokens + outputTokens}` : '\u2014'}
                                </div>

                                <div className="text-xs text-gray-500">{result.verdicts.length}</div>

                                <div className="flex items-center gap-1">
                                    {result.transcriptFile && (
                                        <button
                                            className="text-gray-400 hover:text-gray-600"
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                setTranscriptResult(result);
                                            }}
                                            title="View transcript"
                                        >
                                            <FileTextIcon className="size-3.5" />
                                        </button>
                                    )}

                                    {isExpanded ? (
                                        <ChevronDownIcon className="size-4 text-gray-400" />
                                    ) : (
                                        <ChevronRightIcon className="size-4 text-gray-400" />
                                    )}
                                </div>
                            </div>

                            {isExpanded && (
                                <div className="border-t border-border/30 bg-gray-50/50">
                                    {result.errorMessage && (
                                        <div className="px-3 py-2 text-xs text-red-600">
                                            Error: {result.errorMessage}
                                        </div>
                                    )}

                                    <AgentJudgeVerdictList verdicts={result.verdicts} />
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
