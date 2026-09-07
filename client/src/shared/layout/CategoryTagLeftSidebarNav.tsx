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
    extraGroups?: ReactNode;
    otherFilterActive?: boolean;
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
