import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {AgentJudgeType, type AgentJudgesQuery} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon, PencilIcon, ScaleIcon, TrashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type AgentJudgeListItemType = AgentJudgesQuery['agentJudges'][number];

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

function getConfigurationSummary(type: AgentJudgeType, configuration: Record<string, unknown>): string {
    switch (type) {
        case AgentJudgeType.ContainsText: {
            const text = String(configuration.text ?? '');
            const mustNotContain = configuration.mustNotContain === true;

            return mustNotContain ? `Must not contain: "${text}"` : `Must contain: "${text}"`;
        }

        case AgentJudgeType.RegexMatch: {
            const pattern = String(configuration.pattern ?? '');
            const mustNotMatch = configuration.mustNotMatch === true;

            return mustNotMatch ? `Must not match: ${pattern}` : `Must match: ${pattern}`;
        }

        case AgentJudgeType.LlmRule: {
            const rule = String(configuration.rule ?? '');

            return rule.length > 100 ? `${rule.slice(0, 100)}...` : rule;
        }

        case AgentJudgeType.ResponseLength: {
            const minLength = configuration.minLength as number | undefined;
            const maxLength = configuration.maxLength as number | undefined;

            if (minLength && maxLength) {
                return `Length: ${minLength} - ${maxLength}`;
            }

            if (minLength) {
                return `Min length: ${minLength}`;
            }

            if (maxLength) {
                return `Max length: ${maxLength}`;
            }

            return 'No length constraints';
        }

        case AgentJudgeType.JsonSchema:
            return 'Validates against JSON schema';

        case AgentJudgeType.Similarity: {
            const threshold = configuration.threshold ?? 0.8;
            const algorithm = String(configuration.algorithm ?? 'COSINE');

            return `${algorithm === 'COSINE' ? 'Cosine' : 'Edit Distance'} similarity >= ${threshold}`;
        }

        default:
            return '';
    }
}

interface AgentJudgeCardProps {
    judge: AgentJudgeListItemType;
    onDelete: (id: string) => void;
    onEdit: (judge: AgentJudgeListItemType) => void;
}

const AgentJudgeCard = ({judge, onDelete, onEdit}: AgentJudgeCardProps) => {
    const configSummary = getConfigurationSummary(judge.type, judge.configuration as Record<string, unknown>);

    return (
        <div className="rounded-lg border border-border/50">
            <div className="flex items-center justify-between px-3 py-3">
                <div className="flex flex-1 items-center gap-3">
                    <div className="flex size-8 items-center justify-center rounded bg-violet-500">
                        <ScaleIcon className="size-4 text-white" />
                    </div>

                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <span className="text-sm font-semibold">{judge.name}</span>

                            <span
                                className={twMerge(
                                    'rounded-full border px-2 py-0.5 text-xs font-medium',
                                    JUDGE_TYPE_COLORS[judge.type]
                                )}
                            >
                                {JUDGE_TYPE_LABELS[judge.type]}
                            </span>
                        </div>

                        {configSummary && (
                            <div className="mt-0.5 line-clamp-1 text-xs text-gray-500">{configSummary}</div>
                        )}
                    </div>
                </div>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => onEdit(judge)}>
                            <PencilIcon className="mr-2 size-4" />
                            Edit
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem className="text-red-600" onClick={() => onDelete(judge.id)}>
                            <TrashIcon className="mr-2 size-4" />
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </div>
    );
};

export default AgentJudgeCard;
