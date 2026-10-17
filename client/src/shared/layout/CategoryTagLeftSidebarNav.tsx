import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {TagIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface CategoryTagLeftSidebarNavCategoryI {
    id?: number;
    name: string;
}

interface CategoryTagLeftSidebarNavTagI {
    id?: number;
    name: string;
}

interface CategoryTagLeftSidebarNavProps {
    categories: CategoryTagLeftSidebarNavCategoryI[] | undefined;
    categoriesIsLoading?: boolean;
    currentCategoryId?: number;
    currentTagId?: number;
    /** Groups appended after Tags, such as the embedded Unified API filters. */
    extraGroups?: ReactNode;
    /**
     * True when a filter outside these two groups is active, which stops "All Categories" from claiming to be
     * the current one.
     */
    otherFilterActive?: boolean;
    tags: CategoryTagLeftSidebarNavTagI[] | undefined;
    tagsClassName?: string;
    tagsEmptyMessage: string;
    tagsIsLoading?: boolean;
}

/**
 * The category-and-tag rail shared by the pages that filter a catalog that way — automation Projects and
 * embedded Integrations. They ask the same two questions of their data, so they ask them through one component
 * rather than each keeping a copy of the markup.
 */
const CategoryTagLeftSidebarNav = ({
    categories,
    categoriesIsLoading = false,
    currentCategoryId,
    currentTagId,
    extraGroups,
    otherFilterActive = false,
    tags,
    tagsClassName,
    tagsEmptyMessage,
    tagsIsLoading = false,
}: CategoryTagLeftSidebarNavProps) => (
    <>
        <LeftSidebarFilterNav
            items={(categories ?? []).map((category) => ({
                current: currentCategoryId === category.id,
                id: category.id!,
                name: category.name,
                toLink: `?categoryId=${category.id}`,
            }))}
            leadItem={{
                current: currentCategoryId === undefined && currentTagId === undefined && !otherFilterActive,
                name: 'All Categories',
            }}
            loading={categoriesIsLoading}
            title="Categories"
        />

        <LeftSidebarFilterNav
            className={tagsClassName}
            emptyMessage={tagsEmptyMessage}
            icon={<TagIcon className="mr-1 size-4" />}
            items={(tags ?? []).map((tag) => ({
                current: currentTagId === tag.id,
                id: tag.id!,
                name: tag.name,
                toLink: `?tagId=${tag.id}`,
            }))}
            loading={tagsIsLoading}
            title="Tags"
        />

        {extraGroups}
    </>
);

export default CategoryTagLeftSidebarNav;
