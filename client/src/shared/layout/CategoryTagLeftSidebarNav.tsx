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
    middleGroups?: ReactNode;
    otherFilterActive?: boolean;
    preservedSearchParams?: string;
    tags: CategoryTagLeftSidebarNavTagI[] | undefined;
    tagsClassName?: string;
    tagsEmptyMessage: string;
    tagsIsLoading?: boolean;
}

const CategoryTagLeftSidebarNav = ({
    categories,
    categoriesIsLoading = false,
    currentCategoryId,
    currentTagId,
    extraGroups,
    middleGroups,
    otherFilterActive = false,
    preservedSearchParams,
    tags,
    tagsClassName,
    tagsEmptyMessage,
    tagsIsLoading = false,
}: CategoryTagLeftSidebarNavProps) => {
    const preservedSuffix = preservedSearchParams ? `&${preservedSearchParams}` : '';

    return (
        <>
            <LeftSidebarFilterNav
                items={(categories ?? []).map((category) => ({
                    current: currentCategoryId === category.id,
                    id: category.id!,
                    name: category.name,
                    toLink: `?categoryId=${category.id}${preservedSuffix}`,
                }))}
                leadItem={{
                    current: currentCategoryId === undefined && currentTagId === undefined && !otherFilterActive,
                    name: 'All Categories',
                    toLink: preservedSearchParams ? `?${preservedSearchParams}` : '',
                }}
                loading={categoriesIsLoading}
                title="Categories"
            />

            {middleGroups}

            <LeftSidebarFilterNav
                className={tagsClassName}
                emptyMessage={tagsEmptyMessage}
                icon={<TagIcon className="mr-1 size-4" />}
                items={(tags ?? []).map((tag) => ({
                    current: currentTagId === tag.id,
                    id: tag.id!,
                    name: tag.name,
                    toLink: `?tagId=${tag.id}${preservedSuffix}`,
                }))}
                loading={tagsIsLoading}
                title="Tags"
            />

            {extraGroups}
        </>
    );
};

export default CategoryTagLeftSidebarNav;
