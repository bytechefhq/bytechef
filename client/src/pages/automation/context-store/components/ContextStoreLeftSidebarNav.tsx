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

const ContextStoreLeftSidebarNav = ({isAdmin = false}: ContextStoreLeftSidebarNavPropsType) => {
    const [search, setSearch] = useState('');

    const {id, storeId} = useParams<{id?: string; storeId?: string}>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useContextStoresQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

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
            <div className="pt-0.5 pb-3">
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
                                <span className="px-2 text-xs">
                                    {search ? 'No context stores found.' : 'No context stores.'}
                                </span>
                            )}
                        </>
                    )
                }
                loading={isLoading}
            />
        </div>
    );
};

export default ContextStoreLeftSidebarNav;
