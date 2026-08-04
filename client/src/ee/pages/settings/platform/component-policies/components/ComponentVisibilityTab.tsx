import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import PageLoader from '@/components/PageLoader';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Input} from '@/components/ui/input';
import {
    type ComponentPoliciesQuery,
    useComponentPoliciesQuery,
    useUpdateComponentPolicyMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, ChevronRightIcon, SearchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import ComponentOperationPolicyList from './ComponentOperationPolicyList';

type ComponentPolicyItemType = ComponentPoliciesQuery['componentPolicies'][number];

const ComponentVisibilityTab = () => {
    const [expandedComponentNames, setExpandedComponentNames] = useState<Set<string>>(new Set());
    const [search, setSearch] = useState('');

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useComponentPoliciesQuery();

    const updateComponentPolicyMutation = useUpdateComponentPolicyMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ['ComponentPolicies']});
        },
        onMutate: async ({enabled, name}: {enabled: boolean; name: string}) => {
            await queryClient.cancelQueries({queryKey: ['ComponentPolicies']});

            const previous = queryClient.getQueryData<ComponentPoliciesQuery>(['ComponentPolicies']);

            queryClient.setQueryData<ComponentPoliciesQuery>(['ComponentPolicies'], (current) =>
                current
                    ? {
                          componentPolicies: current.componentPolicies.map((componentPolicy) =>
                              componentPolicy.name === name ? {...componentPolicy, enabled} : componentPolicy
                          ),
                      }
                    : current
            );

            return {previous};
        },
    });

    const componentPolicies = useMemo(
        () =>
            (data?.componentPolicies ?? []).filter((componentPolicy) => {
                const haystack = `${componentPolicy.title ?? ''} ${componentPolicy.name}`.toLowerCase();

                return haystack.includes(search.toLowerCase());
            }),
        [data?.componentPolicies, search]
    );

    const toggleComponentExpanded = (componentName: string, expanded: boolean) =>
        setExpandedComponentNames((currentExpandedComponentNames) => {
            const nextExpandedComponentNames = new Set(currentExpandedComponentNames);

            if (expanded) {
                nextExpandedComponentNames.add(componentName);
            } else {
                nextExpandedComponentNames.delete(componentName);
            }

            return nextExpandedComponentNames;
        });

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="mt-4 flex flex-col gap-4">
                <div className="relative max-w-sm">
                    <SearchIcon className="absolute top-2.5 left-2 size-4 text-muted-foreground" />

                    <Input
                        className="pl-8"
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search components"
                        value={search}
                    />
                </div>

                <ul className="divide-y rounded-md border">
                    {componentPolicies.map((componentPolicy: ComponentPolicyItemType) => {
                        const expanded = expandedComponentNames.has(componentPolicy.name);

                        return (
                            <li key={componentPolicy.name}>
                                <Collapsible
                                    onOpenChange={(open) => toggleComponentExpanded(componentPolicy.name, open)}
                                    open={expanded}
                                >
                                    <div className="flex items-center justify-between gap-3 px-4 py-3">
                                        <div className="flex items-center gap-3">
                                            <CollapsibleTrigger
                                                aria-label={`${expanded ? 'Collapse' : 'Expand'} ${componentPolicy.name} operations`}
                                                className="flex-none text-muted-foreground hover:text-foreground"
                                            >
                                                {expanded ? (
                                                    <ChevronDownIcon className="size-4" />
                                                ) : (
                                                    <ChevronRightIcon className="size-4" />
                                                )}
                                            </CollapsibleTrigger>

                                            {componentPolicy.icon ? (
                                                <LazyLoadSVG className="size-6 flex-none" src={componentPolicy.icon} />
                                            ) : (
                                                <span className="size-6 flex-none rounded bg-muted" />
                                            )}

                                            <div className="flex flex-col">
                                                <span className="text-sm font-semibold">
                                                    {componentPolicy.title ?? componentPolicy.name}
                                                </span>

                                                <span className="text-xs text-muted-foreground">
                                                    {componentPolicy.description ?? componentPolicy.name}
                                                </span>
                                            </div>
                                        </div>

                                        <Switch
                                            aria-label={componentPolicy.title ?? componentPolicy.name}
                                            checked={componentPolicy.enabled}
                                            onCheckedChange={(checked) =>
                                                updateComponentPolicyMutation.mutate({
                                                    enabled: checked,
                                                    name: componentPolicy.name,
                                                })
                                            }
                                        />
                                    </div>

                                    <CollapsibleContent>
                                        {expanded && (
                                            <ComponentOperationPolicyList
                                                componentEnabled={componentPolicy.enabled}
                                                componentName={componentPolicy.name}
                                                componentVersion={componentPolicy.version}
                                            />
                                        )}
                                    </CollapsibleContent>
                                </Collapsible>
                            </li>
                        );
                    })}
                </ul>
            </div>
        </PageLoader>
    );
};

export default ComponentVisibilityTab;
