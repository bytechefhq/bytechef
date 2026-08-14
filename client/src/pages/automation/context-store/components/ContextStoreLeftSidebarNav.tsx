import {Input} from '@/components/Input/Input';
import ContextStoreRowActionsMenu from '@/pages/automation/context-store/components/ContextStoreRowActionsMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ContextStore, useContextStoresQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';

type ContextStoreLeftSidebarNavPropsType = {
    isAdmin?: boolean;
};

/**
 * Sidebar nav for the Context Store detail pages — both the store-sources list and the single-source detail. Mirrors
 * {@code KnowledgeBaseLeftSidebarNav}: lists every Context Store in the active workspace+env, highlights the one the
 * user is currently inside, and links each entry to its store-detail page. Lets users hop between stores without
 * bouncing back to the top-level list.
 *
 * <p>
 * Picks up the active store id from either {@code :id} (on the store-detail route) or {@code :storeId} (on the
 * nested source-detail route), so the same component highlights the right item on both pages.
 *
 * <p>
 * The search box replaces the group title: the list is a navigation list, and once a workspace has more than a
 * handful of stores the label is worth less than a filter.
 */
const ContextStoreLeftSidebarNav = ({isAdmin = false}: ContextStoreLeftSidebarNavPropsType) => {
    const [search, setSearch] = useState('');

    const {id, storeId} = useParams<{id?: string; storeId?: string}>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useContextStoresQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    // Filtered here rather than in the markup so the empty message can tell a search that matched
    // nothing apart from a workspace with no stores.
    const contextStores = useMemo(() => {
        const query = search.trim().toLowerCase();

        const allContextStores = (data?.contextStores ?? []).filter(
            (contextStore): contextStore is NonNullable<typeof contextStore> => contextStore !== null
        );

        if (!query) {
            return allContextStores;
        }

        return allContextStores.filter((contextStore) => contextStore.name.toLowerCase().includes(query));
    }, [data?.contextStores, search]);

    const currentContextStoreId = storeId ?? id;

    return (
        <div className="flex h-full flex-col">
            <div className="space-y-2 px-3 pt-0.5 pb-3">
                <Input
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search context stores..."
                    value={search}
                />
            </div>

            <LeftSidebarNav
                body={
                    !isLoading && (
                        <>
                            {contextStores.length ? (
                                contextStores.map((contextStore) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: String(contextStore.id) === currentContextStoreId,
                                            id: contextStore.id,
                                            name: contextStore.name,
                                        }}
                                        key={contextStore.id}
                                        toLink={`/automation/context-stores/${contextStore.id}`}
                                        trailing={
                                            <ContextStoreRowActionsMenu
                                                className="opacity-0 transition-opacity group-hover:opacity-100 data-[state=open]:opacity-100"
                                                contextStore={contextStore as ContextStore}
                                                isAdmin={isAdmin}
                                            />
                                        }
                                    />
                                ))
                            ) : (
                                <span className="px-3 text-xs">
                                    {search ? 'No context stores found.' : 'No context stores.'}
                                </span>
                            )}
                        </>
                    )
                }
            />
        </div>
    );
};

export default ContextStoreLeftSidebarNav;
