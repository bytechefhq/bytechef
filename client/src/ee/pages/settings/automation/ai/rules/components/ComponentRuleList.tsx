import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import Switch from '@/components/Switch/Switch';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import RiskLevelBadge from '@/ee/pages/settings/automation/ai/rules/components/RiskLevelBadge';
import {
    type ComponentRulesQuery,
    useDeleteComponentRuleMutation,
    useSaveComponentRuleMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, Trash2Icon} from 'lucide-react';

export type ComponentRuleItemType = ComponentRulesQuery['componentRules'][number];

interface ComponentRuleListProps {
    componentRules: ComponentRuleItemType[];
    onEdit: (componentRule: ComponentRuleItemType) => void;
    workspaceId?: string;
}

const ComponentRuleList = ({componentRules, onEdit, workspaceId}: ComponentRuleListProps) => {
    const queryClient = useQueryClient();

    const componentRulesQueryKey = ['ComponentRules', {componentName: undefined, workspaceId}];

    const deleteComponentRuleMutation = useDeleteComponentRuleMutation({
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['ComponentRules']});
        },
    });

    const saveComponentRuleMutation = useSaveComponentRuleMutation<unknown, {previous?: ComponentRulesQuery}>({
        onError: (_error, _variables, context) => {
            if (context?.previous) {
                queryClient.setQueryData(componentRulesQueryKey, context.previous);
            }
        },
        onMutate: async ({enabled, id}) => {
            await queryClient.cancelQueries({queryKey: componentRulesQueryKey});

            const previous = queryClient.getQueryData<ComponentRulesQuery>(componentRulesQueryKey);

            queryClient.setQueryData<ComponentRulesQuery>(componentRulesQueryKey, (current) =>
                current
                    ? {
                          componentRules: current.componentRules.map((componentRule) =>
                              componentRule.id === id ? {...componentRule, enabled} : componentRule
                          ),
                      }
                    : current
            );

            return {previous};
        },
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['ComponentRules']});
        },
    });

    return (
        <ul className="divide-y rounded-md border">
            {componentRules.map((componentRule) => {
                const componentLabel = componentRule.componentTitle ?? componentRule.componentName;
                const toolLabel = componentRule.toolName ?? 'all tools';
                const isTenantWide = componentRule.workspaceId == null;

                return (
                    <li className="flex items-center justify-between gap-3 px-4 py-3" key={componentRule.id}>
                        <div className="flex min-w-0 items-center gap-3">
                            {componentRule.componentIcon ? (
                                <LazyLoadSVG className="size-6 flex-none" src={componentRule.componentIcon} />
                            ) : (
                                <span className="size-6 flex-none rounded bg-muted" />
                            )}

                            <div className="flex min-w-0 flex-col">
                                <span className="text-sm font-semibold">
                                    {componentLabel}

                                    <span className="ml-2 font-normal text-muted-foreground">
                                        {componentRule.toolName ?? 'All tools'}
                                    </span>
                                </span>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="truncate font-mono text-xs text-muted-foreground">
                                            {componentRule.condition}
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent className="max-w-md font-mono break-all">
                                        {componentRule.condition}
                                    </TooltipContent>
                                </Tooltip>
                            </div>
                        </div>

                        <div className="flex flex-none items-center gap-2">
                            {isTenantWide && <Badge variant="outline">All workspaces</Badge>}

                            {componentRule.toolRiskLevel && <RiskLevelBadge riskLevel={componentRule.toolRiskLevel} />}

                            <Badge variant="secondary">{componentRule.phase === 'BEFORE' ? 'Before' : 'After'}</Badge>

                            <Badge
                                variant={
                                    componentRule.ruleAction === 'BLOCK'
                                        ? 'destructive'
                                        : componentRule.ruleAction === 'REQUIRE_APPROVAL'
                                          ? 'default'
                                          : 'outline'
                                }
                            >
                                {componentRule.ruleAction === 'BLOCK'
                                    ? 'Block'
                                    : componentRule.ruleAction === 'REQUIRE_APPROVAL'
                                      ? 'Require approval'
                                      : 'Tag'}
                            </Badge>

                            {componentRule.strict && <Badge variant="outline">Fail closed</Badge>}

                            <Switch
                                aria-label={`${componentLabel} ${toolLabel}`}
                                checked={componentRule.enabled}
                                onCheckedChange={(checked) =>
                                    saveComponentRuleMutation.mutate({
                                        componentName: componentRule.componentName,
                                        condition: componentRule.condition,
                                        description: componentRule.description,
                                        enabled: checked,
                                        id: componentRule.id,
                                        phase: componentRule.phase,
                                        ruleAction: componentRule.ruleAction,
                                        strict: componentRule.strict,
                                        toolName: componentRule.toolName,
                                        workspaceId: componentRule.workspaceId,
                                    })
                                }
                            />

                            <Button
                                aria-label={`Edit ${componentLabel} ${toolLabel} rule`}
                                onClick={() => onEdit(componentRule)}
                                size="icon"
                                variant="ghost"
                            >
                                <PencilIcon className="size-4" />
                            </Button>

                            <Button
                                aria-label={`Delete ${componentLabel} ${toolLabel} rule`}
                                onClick={() => deleteComponentRuleMutation.mutate({id: componentRule.id})}
                                size="icon"
                                variant="ghost"
                            >
                                <Trash2Icon className="size-4" />
                            </Button>
                        </div>
                    </li>
                );
            })}
        </ul>
    );
};

export default ComponentRuleList;
