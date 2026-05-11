import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {LayersIcon, PuzzleIcon, TagIcon} from 'lucide-react';

export type ContextSourcesFiltersType = {
    contextStoreId: string | undefined;
    sourceComponentName: string | undefined;
    tagId: string | undefined;
};

export type ContextStoreOptionType = {
    id: string;
    name: string;
};

export type TagOptionType = {
    id: string;
    name: string;
};

type ContextStoresLeftSidebarNavPropsType = {
    contextStores: ContextStoreOptionType[];
    /** All source-component names present in the current sources list, deduplicated and sorted. */
    sourceComponentNames: string[];
    /** Workspace tags used across context stores, deduplicated. */
    tags: TagOptionType[];
    filters: ContextSourcesFiltersType;
    onFiltersChange: (filters: ContextSourcesFiltersType) => void;
};

const EMPTY_FILTERS: ContextSourcesFiltersType = {
    contextStoreId: undefined,
    sourceComponentName: undefined,
    tagId: undefined,
};

/**
 * Left sidebar on the Context Sources page. Three filter groups — Context Stores, Components, Tags — each
 * single-pick within the group, ANDed across groups. The "All sources" entry resets every dimension at once.
 *
 * Filtering happens client-side in the parent page; this component just reflects state via {@link filters} and
 * raises clicks via {@link onFiltersChange}. Data (stores / tags / source-component names) is fetched at the page
 * level so the sidebar stays presentational and reusable.
 */
const ContextStoresLeftSidebarNav = ({
    contextStores,
    filters,
    onFiltersChange,
    sourceComponentNames,
    tags,
}: ContextStoresLeftSidebarNavPropsType) => {
    const filtersActive =
        filters.contextStoreId !== undefined ||
        filters.sourceComponentName !== undefined ||
        filters.tagId !== undefined;

    return (
        <LeftSidebarNav
            body={
                <>
                    <LeftSidebarNavItem
                        icon={<LayersIcon className="mr-1 size-4" />}
                        item={{
                            current: !filtersActive,
                            id: 'all',
                            name: 'All sources',
                            onItemClick: () => onFiltersChange(EMPTY_FILTERS),
                        }}
                    />

                    {contextStores.length > 0 && (
                        <>
                            <p className="px-3 pt-3 pb-1 text-xs font-medium text-muted-foreground uppercase">
                                Context Stores
                            </p>

                            {contextStores.map((store) => (
                                <LeftSidebarNavItem
                                    icon={<LayersIcon className="mr-1 size-4" />}
                                    item={{
                                        current: store.id === filters.contextStoreId,
                                        id: store.id,
                                        name: store.name,
                                        onItemClick: () =>
                                            onFiltersChange({
                                                ...filters,
                                                contextStoreId:
                                                    filters.contextStoreId === store.id ? undefined : store.id,
                                            }),
                                    }}
                                    key={store.id}
                                />
                            ))}
                        </>
                    )}

                    {sourceComponentNames.length > 0 && (
                        <>
                            <p className="px-3 pt-3 pb-1 text-xs font-medium text-muted-foreground uppercase">
                                Components
                            </p>

                            {sourceComponentNames.map((componentName) => (
                                <LeftSidebarNavItem
                                    icon={<PuzzleIcon className="mr-1 size-4" />}
                                    item={{
                                        current: componentName === filters.sourceComponentName,
                                        id: componentName,
                                        name: componentName,
                                        onItemClick: () =>
                                            onFiltersChange({
                                                ...filters,
                                                sourceComponentName:
                                                    filters.sourceComponentName === componentName
                                                        ? undefined
                                                        : componentName,
                                            }),
                                    }}
                                    key={componentName}
                                />
                            ))}
                        </>
                    )}

                    {tags.length > 0 && (
                        <>
                            <p className="px-3 pt-3 pb-1 text-xs font-medium text-muted-foreground uppercase">Tags</p>

                            {tags.map((tag) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: tag.id === filters.tagId,
                                        id: tag.id,
                                        name: tag.name,
                                        onItemClick: () =>
                                            onFiltersChange({
                                                ...filters,
                                                tagId: filters.tagId === tag.id ? undefined : tag.id,
                                            }),
                                    }}
                                    key={tag.id}
                                />
                            ))}
                        </>
                    )}
                </>
            }
            title="Filters"
        />
    );
};

export default ContextStoresLeftSidebarNav;
