import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ContextStoreFormDialog from '@/pages/automation/context-store/components/ContextStoreFormDialog';
import ContextStoresFilterLeftSidebarNav, {
    ContextStoresFilterType,
} from '@/pages/automation/context-store/components/ContextStoresFilterLeftSidebarNav';
import ContextStoresFilterTitle from '@/pages/automation/context-store/components/ContextStoresFilterTitle';
import ContextStoreList from '@/pages/automation/context-store/components/context-store-list/ContextStoreList';
import useContextStoreSources from '@/pages/automation/context-store/components/hooks/useContextStoreSources';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {AUTHORITIES} from '@/shared/constants';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useContextStoreTagsQuery, useContextStoresQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {LayersIcon} from 'lucide-react';
import {useEffect, useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

/**
 * Top-level Context Stores management page. Lists the parent {@link ContextStore} entities scoped to the active
 * workspace + environment as a divider-style list (matching the Knowledge Bases layout). Clicking a row drills into
 * the store-detail page where its sources live.
 */
const ContextStores = () => {
    const account = useAuthenticationStore((state) => state.account);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, error, isLoading} = useContextStoresQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const {data: tagsData} = useContextStoreTagsQuery({
        workspaceId: String(currentWorkspaceId),
    });

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const isAdmin = account?.authorities?.includes(AUTHORITIES.ADMIN) ?? false;

    const contextStores = useMemo(() => data?.contextStores ?? [], [data?.contextStores]);
    const workspaceTags = useMemo(() => tagsData?.contextStoreTags ?? [], [tagsData?.contextStoreTags]);

    const {sources} = useContextStoreSources();

    const [searchParams] = useSearchParams();

    const sourceComponentNameFilter = searchParams.get('sourceComponentName') ?? undefined;
    const tagIdFilter = searchParams.get('tagId');

    const filterData = useMemo(
        () => ({
            sourceComponentName: sourceComponentNameFilter,
            tagId: tagIdFilter ? Number(tagIdFilter) : undefined,
            type: tagIdFilter ? ContextStoresFilterType.Tag : ContextStoresFilterType.SourceComponent,
        }),
        [sourceComponentNameFilter, tagIdFilter]
    );

    // storeIdToSourceComponentNames: pre-aggregated lookup so the source-component filter predicate stays
    // O(1) per store. A store qualifies for the filter when at least one of its sources uses that component.
    const storeIdToSourceComponentNames = useMemo(() => {
        const map = new Map<string, Set<string>>();

        for (const source of sources) {
            const existing = map.get(source.contextStoreId);

            if (existing) {
                existing.add(source.sourceComponentName);
            } else {
                map.set(source.contextStoreId, new Set([source.sourceComponentName]));
            }
        }

        return map;
    }, [sources]);

    // Component-definitions catalog lets us render Title-cased names ("Airtable") instead of the raw lowercase id
    // ("airtable") in the sidebar filter. Mirrors the MCP Servers sidebar pattern.
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({actionDefinitions: true});

    const sourceComponents = useMemo(() => {
        const names = new Set<string>();

        for (const source of sources) {
            names.add(source.sourceComponentName);
        }

        return Array.from(names)
            .map((name) => {
                const definition = componentDefinitions?.find((component) => component.name === name);

                return {name, title: definition?.title};
            })
            .sort((firstComponent, secondComponent) =>
                (firstComponent.title ?? firstComponent.name).localeCompare(
                    secondComponent.title ?? secondComponent.name
                )
            );
    }, [sources, componentDefinitions]);

    const sidebarTags = useMemo(
        () => workspaceTags.map((tag) => ({id: Number(tag.id), name: tag.name})),
        [workspaceTags]
    );

    const filteredContextStores = useMemo(() => {
        return contextStores.filter((store) => {
            if (filterData.type === ContextStoresFilterType.SourceComponent && filterData.sourceComponentName) {
                const componentNames = storeIdToSourceComponentNames.get(String(store.id));

                if (!componentNames || !componentNames.has(filterData.sourceComponentName)) {
                    return false;
                }
            }

            if (filterData.type === ContextStoresFilterType.Tag && filterData.tagId !== undefined) {
                const storeTagIds = new Set(store.tagIds.map((id) => Number(id)));

                if (!storeTagIds.has(filterData.tagId)) {
                    return false;
                }
            }

            return true;
        });
    }, [contextStores, filterData, storeIdToSourceComponentNames]);

    // Refresh the store list and its sources after a BUILD-mode copilot turn creates or updates either, so the page
    // reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.CONTEXT_STORE, () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
            queryClient.invalidateQueries({queryKey: ['contextStoreTags']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        (contextStores.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-1">
                                <CopilotButton source={Source.CONTEXT_STORE} />

                                {contextStores.length > 0 && isAdmin && (
                                    <ContextStoreFormDialog trigger={<Button>New Context Store</Button>} />
                                )}
                            </div>
                        )
                    }
                    title={
                        contextStores.length > 0 ? (
                            <ContextStoresFilterTitle
                                filterData={filterData}
                                sourceComponents={sourceComponents}
                                tags={sidebarTags}
                            />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={
                contextStores.length > 0 ? (
                    <ContextStoresFilterLeftSidebarNav
                        filterData={filterData}
                        sourceComponents={sourceComponents}
                        tags={sidebarTags}
                    />
                ) : undefined
            }
            leftSidebarHeader={<Header position="sidebar" title="Context Store" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {contextStores.length > 0 ? (
                    filteredContextStores.length > 0 ? (
                        <ContextStoreList
                            allTags={sidebarTags}
                            contextStores={filteredContextStores}
                            isAdmin={isAdmin}
                        />
                    ) : (
                        <div className="px-4 py-6 text-sm text-muted-foreground 3xl:mx-auto 3xl:w-4/5">
                            No Context Stores match the selected filter.
                        </div>
                    )
                ) : (
                    <EmptyList
                        button={
                            isAdmin ? (
                                <ContextStoreFormDialog trigger={<Button>New Context Store</Button>} />
                            ) : undefined
                        }
                        icon={<LayersIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Create a Context Store to group sources that share an environment."
                        title="No Context Stores"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ContextStores;
