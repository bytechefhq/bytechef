import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {AiAgentJudgeType, type AiAgentJudgesQuery} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon, PencilIcon, ScaleIcon, TrashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type AiAgentJudgeListItemType = AiAgentJudgesQuery['aiAgentJudges'][number];

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

function getConfigurationSummary(type: AiAgentJudgeType, configuration: Record<string, unknown>): string {
    switch (type) {
        case AiAgentJudgeType.ContainsText: {
            const text = String(configuration.text ?? '');
            const mustNotContain = configuration.mustNotContain === true;

            return mustNotContain ? `Must not contain: "${text}"` : `Must contain: "${text}"`;
        }

        case AiAgentJudgeType.RegexMatch: {
            const pattern = String(configuration.pattern ?? '');
            const mustNotMatch = configuration.mustNotMatch === true;

            return mustNotMatch ? `Must not match: ${pattern}` : `Must match: ${pattern}`;
        }

        case AiAgentJudgeType.LlmRule: {
            const rule = String(configuration.rule ?? '');

            return rule.length > 100 ? `${rule.slice(0, 100)}...` : rule;
        }

        case AiAgentJudgeType.ResponseLength: {
            const minLength = configuration.minLength as number | undefined;
            const maxLength = configuration.maxLength as number | undefined;

            if (minLength != null && maxLength != null) {
                return `Length: ${minLength} - ${maxLength}`;
            }

            if (minLength != null) {
                return `Min length: ${minLength}`;
            }

            if (maxLength != null) {
                return `Max length: ${maxLength}`;
            }

            return 'No length constraints';
        }

        case AiAgentJudgeType.JsonSchema:
            return 'Validates against JSON schema';

        case AiAgentJudgeType.Similarity: {
            const threshold = configuration.threshold ?? 0.8;
            const algorithm = String(configuration.algorithm ?? 'COSINE');

            return `${algorithm === 'COSINE' ? 'Cosine' : 'Edit Distance'} similarity >= ${threshold}`;
        }

        case AiAgentJudgeType.StringEquals: {
            const expectedValue = String(configuration.expectedValue ?? '');

            return `Must equal: '${expectedValue}'`;
        }

        case AiAgentJudgeType.ToolUsage: {
            const toolName = String(configuration.toolName ?? '');
            const position = String(configuration.position ?? 'ANYWHERE').toLowerCase();
            const comparison = String(configuration.comparison ?? 'AT_LEAST')
                .toLowerCase()
                .replace('_', ' ');
            const count = configuration.count ?? 1;

            return `Tool: ${toolName} (${position}, ${comparison} ${count})`;
        }

        default:
            return '';
    }
}

interface AiAgentJudgeCardProps {
    judge: AiAgentJudgeListItemType;
    onDelete: (id: string) => void;
    onEdit: (judge: AiAgentJudgeListItemType) => void;
}

const AiAgentJudgeCard = ({judge, onDelete, onEdit}: AiAgentJudgeCardProps) => {
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

export default AiAgentJudgeCard;
