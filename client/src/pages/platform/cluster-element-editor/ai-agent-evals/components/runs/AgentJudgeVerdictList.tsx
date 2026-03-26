import {type AgentEvalRunQuery, AgentJudgeScope, AgentJudgeType} from '@/shared/middleware/graphql';
import {CheckCircle2Icon, XCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type VerdictType = NonNullable<AgentEvalRunQuery['agentEvalRun']>['results'][number]['verdicts'][number];

const JUDGE_TYPE_LABELS: Record<AgentJudgeType, string> = {
    [AgentJudgeType.ContainsText]: 'Contains Text',
    [AgentJudgeType.JsonSchema]: 'JSON Schema',
    [AgentJudgeType.LlmRule]: 'LLM Rule',
    [AgentJudgeType.RegexMatch]: 'Regex Match',
    [AgentJudgeType.ResponseLength]: 'Response Length',
    [AgentJudgeType.Similarity]: 'Similarity',
};

const JUDGE_TYPE_COLORS: Record<AgentJudgeType, string> = {
    [AgentJudgeType.ContainsText]: 'border-amber-200 bg-amber-50 text-amber-700',
    [AgentJudgeType.JsonSchema]: 'border-indigo-200 bg-indigo-50 text-indigo-700',
    [AgentJudgeType.LlmRule]: 'border-blue-200 bg-blue-50 text-blue-700',
    [AgentJudgeType.RegexMatch]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AgentJudgeType.ResponseLength]: 'border-green-200 bg-green-50 text-green-700',
    [AgentJudgeType.Similarity]: 'border-teal-200 bg-teal-50 text-teal-700',
};

const SCOPE_LABELS: Record<AgentJudgeScope, string> = {
    [AgentJudgeScope.Agent]: 'Agent',
    [AgentJudgeScope.Scenario]: 'Scenario',
};

const SCOPE_COLORS: Record<AgentJudgeScope, string> = {
    [AgentJudgeScope.Agent]: 'border-gray-200 bg-gray-50 text-gray-600',
    [AgentJudgeScope.Scenario]: 'border-sky-200 bg-sky-50 text-sky-700',
};

interface AgentJudgeVerdictListProps {
    verdicts: VerdictType[];
}

const AgentJudgeVerdictList = ({verdicts}: AgentJudgeVerdictListProps) => {
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

export default AgentJudgeVerdictList;
