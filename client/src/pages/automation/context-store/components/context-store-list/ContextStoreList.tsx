import {ContextStore} from '@/shared/middleware/graphql';

import ContextStoreListItem from './ContextStoreListItem';

interface ContextStoreListProps {
    /**
     * Workspace tag list — used to compute each row's `remainingTags` (workspace tags not yet attached to that
     * specific store). The TagList autocomplete reads from this to offer additions without re-fetching per row.
     */
    allTags: {id: number; name: string}[];
    contextStores: ContextStore[];
    isAdmin: boolean;
}

/**
 * Bordered-row list of {@link ContextStore} rows. Mirrors {@code KnowledgeBaseList}: clicking a row drills into the
 * single-store sources page. The previous {@code <Table>} layout is gone — same content, less visual chrome.
 */
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
