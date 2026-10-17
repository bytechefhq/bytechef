import {type AiAgentEvalRunQuery, AiAgentJudgeScope, AiAgentJudgeType} from '@/shared/middleware/graphql';
import {CheckCircle2Icon, XCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type VerdictType = NonNullable<AiAgentEvalRunQuery['aiAgentEvalRun']>['results'][number]['verdicts'][number];

const JUDGE_TYPE_LABELS: Record<AiAgentJudgeType, string> = {
    [AiAgentJudgeType.ContainsText]: 'Contains Text',
    [AiAgentJudgeType.JsonSchema]: 'JSON Schema',
    [AiAgentJudgeType.LlmRule]: 'LLM Rule',
    [AiAgentJudgeType.RegexMatch]: 'Regex Match',
    [AiAgentJudgeType.ResponseLength]: 'Response Length',
    [AiAgentJudgeType.Similarity]: 'Similarity',
    [AiAgentJudgeType.StringEquals]: 'String Equals',
    [AiAgentJudgeType.ToolUsage]: 'Tool Usage',
};

const JUDGE_TYPE_COLORS: Record<AiAgentJudgeType, string> = {
    [AiAgentJudgeType.ContainsText]:
        'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300',
    [AiAgentJudgeType.JsonSchema]:
        'border-indigo-200 bg-indigo-50 text-indigo-700 dark:border-indigo-800 dark:bg-indigo-950 dark:text-indigo-300',
    [AiAgentJudgeType.LlmRule]:
        'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-800 dark:bg-blue-950 dark:text-blue-300',
    [AiAgentJudgeType.RegexMatch]:
        'border-purple-200 bg-purple-50 text-purple-700 dark:border-purple-800 dark:bg-purple-950 dark:text-purple-300',
    [AiAgentJudgeType.ResponseLength]:
        'border-green-200 bg-green-50 text-green-700 dark:border-green-800 dark:bg-green-950 dark:text-green-300',
    [AiAgentJudgeType.Similarity]:
        'border-teal-200 bg-teal-50 text-teal-700 dark:border-teal-800 dark:bg-teal-950 dark:text-teal-300',
    [AiAgentJudgeType.StringEquals]:
        'border-cyan-200 bg-cyan-50 text-cyan-700 dark:border-cyan-800 dark:bg-cyan-950 dark:text-cyan-300',
    [AiAgentJudgeType.ToolUsage]:
        'border-orange-200 bg-orange-50 text-orange-700 dark:border-orange-800 dark:bg-orange-950 dark:text-orange-300',
};

const SCOPE_LABELS: Record<AiAgentJudgeScope, string> = {
    [AiAgentJudgeScope.Agent]: 'Agent',
    [AiAgentJudgeScope.Scenario]: 'Scenario',
};

const SCOPE_COLORS: Record<AiAgentJudgeScope, string> = {
    [AiAgentJudgeScope.Agent]:
        'border-stroke-neutral-secondary bg-surface-neutral-secondary text-content-neutral-secondary',
    [AiAgentJudgeScope.Scenario]:
        'border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-800 dark:bg-sky-950 dark:text-sky-300',
};

interface AiAgentJudgeVerdictListProps {
    verdicts: VerdictType[];
}

const AiAgentJudgeVerdictList = ({verdicts}: AiAgentJudgeVerdictListProps) => {
    if (verdicts.length === 0) {
        return <div className="px-3 py-2 text-xs text-content-neutral-tertiary">No verdicts</div>;
    }

    return (
        <div className="space-y-2 px-3 py-2">
            {verdicts.map((verdict) => (
                <div
                    className="flex items-start gap-2 rounded-md bg-surface-neutral-secondary/50 px-3 py-2"
                    key={verdict.id}
                >
                    {verdict.passed ? (
                        <CheckCircle2Icon className="mt-0.5 size-4 shrink-0 text-green-500" />
                    ) : (
                        <XCircleIcon className="mt-0.5 size-4 shrink-0 text-red-500" />
                    )}

                    <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                            <span className="text-sm font-medium">{verdict.judgeName}</span>

                            <span
                                className={twMerge(
                                    'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                    JUDGE_TYPE_COLORS[verdict.judgeType]
                                )}
                            >
                                {JUDGE_TYPE_LABELS[verdict.judgeType]}
                            </span>

                            <span
                                className={twMerge(
                                    'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                    SCOPE_COLORS[verdict.judgeScope]
                                )}
                            >
                                {SCOPE_LABELS[verdict.judgeScope]}
                            </span>
                        </div>

                        {verdict.explanation && (
                            <div className="mt-1 text-xs text-content-neutral-secondary">{verdict.explanation}</div>
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default AiAgentJudgeVerdictList;
