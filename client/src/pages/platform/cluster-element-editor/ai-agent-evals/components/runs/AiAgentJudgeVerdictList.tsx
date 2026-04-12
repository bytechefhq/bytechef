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
    [AiAgentJudgeType.ContainsText]: 'border-amber-200 bg-amber-50 text-amber-700',
    [AiAgentJudgeType.JsonSchema]: 'border-indigo-200 bg-indigo-50 text-indigo-700',
    [AiAgentJudgeType.LlmRule]: 'border-blue-200 bg-blue-50 text-blue-700',
    [AiAgentJudgeType.RegexMatch]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AiAgentJudgeType.ResponseLength]: 'border-green-200 bg-green-50 text-green-700',
    [AiAgentJudgeType.Similarity]: 'border-teal-200 bg-teal-50 text-teal-700',
    [AiAgentJudgeType.StringEquals]: 'border-cyan-200 bg-cyan-50 text-cyan-700',
    [AiAgentJudgeType.ToolUsage]: 'border-orange-200 bg-orange-50 text-orange-700',
};

const SCOPE_LABELS: Record<AiAgentJudgeScope, string> = {
    [AiAgentJudgeScope.Agent]: 'Agent',
    [AiAgentJudgeScope.Scenario]: 'Scenario',
};

const SCOPE_COLORS: Record<AiAgentJudgeScope, string> = {
    [AiAgentJudgeScope.Agent]: 'border-gray-200 bg-gray-50 text-gray-600',
    [AiAgentJudgeScope.Scenario]: 'border-sky-200 bg-sky-50 text-sky-700',
};

interface AiAgentJudgeVerdictListProps {
    verdicts: VerdictType[];
}

const AiAgentJudgeVerdictList = ({verdicts}: AiAgentJudgeVerdictListProps) => {
    if (verdicts.length === 0) {
        return <div className="px-3 py-2 text-xs text-gray-400">No verdicts</div>;
    }

    return (
        <div className="space-y-2 px-3 py-2">
            {verdicts.map((verdict) => (
                <div className="flex items-start gap-2 rounded-md bg-gray-50 px-3 py-2" key={verdict.id}>
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

                        {verdict.explanation && <div className="mt-1 text-xs text-gray-500">{verdict.explanation}</div>}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default AiAgentJudgeVerdictList;
