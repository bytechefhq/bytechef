import {ContextStore} from '@/shared/middleware/graphql';

import ContextStoreListItem from './ContextStoreListItem';

interface ContextStoreListProps {
    allTags: {id: number; name: string}[];
    contextStores: ContextStore[];
    isAdmin: boolean;
}

const ContextStoreList = ({allTags, contextStores, isAdmin}: ContextStoreListProps) => {
    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            {contextStores.map((contextStore) => {
                const currentTagIds = new Set(contextStore.tagIds.map((id) => Number(id)));

                const remainingTags = allTags.filter((tag) => !currentTagIds.has(tag.id));

                const currentTags = (contextStore.tags ?? []).map((tag) => ({
                    id: Number(tag.id),
                    name: tag.name,
                }));

                return (
                    <ContextStoreListItem
                        contextStore={contextStore}
                        isAdmin={isAdmin}
                        key={contextStore.id}
                        remainingTags={remainingTags}
                        tags={currentTags}
                    />
                );
            })}
        </div>
    );
};

export default ContextStoreList;
