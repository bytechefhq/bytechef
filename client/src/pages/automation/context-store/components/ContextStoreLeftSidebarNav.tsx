import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useContextStoresQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {LayersIcon} from 'lucide-react';
import {useParams} from 'react-router-dom';

/**
 * Sidebar nav for the Context Store detail pages — both the store-sources list and the single-source detail. Mirrors
 * {@code KnowledgeBaseLeftSidebarNav}: lists every Context Store in the active workspace+env, highlights the one the
 * user is currently inside, and links each entry to its store-detail page. Lets users hop between stores without
 * bouncing back to the top-level list.
 *
 * <p>
 * Picks up the active store id from either {@code :id} (on the store-detail route) or {@code :storeId} (on the
 * nested source-detail route), so the same component highlights the right item on both pages.
 */
const ContextStoreLeftSidebarNav = () => {
    const {id, storeId} = useParams<{id?: string; storeId?: string}>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useContextStoresQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const contextStores = (data?.contextStores ?? []).filter(
        (contextStore): contextStore is NonNullable<typeof contextStore> => contextStore !== null
    );

    const currentContextStoreId = storeId ?? id;

    return (
        <LeftSidebarNav
            body={
                !isLoading && (
                    <>
                        {contextStores.length ? (
                            contextStores.map((contextStore) => (
                                <LeftSidebarNavItem
                                    icon={<LayersIcon className="mr-1 size-4" />}
                                    item={{
                                        current: String(contextStore.id) === currentContextStoreId,
                                        id: contextStore.id,
                                        name: contextStore.name,
                                    }}
                                    key={contextStore.id}
                                    toLink={`/automation/context-stores/${contextStore.id}`}
                                />
                            ))
                        ) : (
                            <span className="px-3 text-xs">No context stores.</span>
                        )}
                    </>
                )
            }
            title="Context Stores"
        />
    );
};

export default ContextStoreLeftSidebarNav;
