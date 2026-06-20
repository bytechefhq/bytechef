import PageLoader from '@/components/PageLoader';
import Switch from '@/components/Switch/Switch';
import {Input} from '@/components/ui/input';
import {
    type ComponentPoliciesQuery,
    useComponentPoliciesQuery,
    useUpdateComponentPolicyMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {SearchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

type ComponentPolicyItemType = ComponentPoliciesQuery['componentPolicies'][number];

const ComponentVisibilityTab = () => {
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
                    {componentPolicies.map((componentPolicy: ComponentPolicyItemType) => (
                        <li className="flex items-center justify-between gap-3 px-4 py-3" key={componentPolicy.name}>
                            <div className="flex items-center gap-3">
                                {componentPolicy.icon ? (
                                    <InlineSVG className="size-6 flex-none" src={componentPolicy.icon} />
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
                        </li>
                    ))}
                </ul>
            </div>
        </PageLoader>
    );
};

export default ComponentVisibilityTab;
