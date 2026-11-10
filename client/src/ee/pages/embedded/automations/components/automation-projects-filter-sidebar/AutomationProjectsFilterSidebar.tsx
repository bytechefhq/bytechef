import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {AutomationProjectCategoriesQuery, AutomationProjectTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';

export enum AutomationProjectFilterType {
    Category,
    Tag,
}

type EmbeddedCategoryType = AutomationProjectCategoriesQuery['automationProjectCategories'][number];
type EmbeddedTagType = AutomationProjectTagsQuery['automationProjectTags'][number];

interface AutomationProjectsFilterSidebarProps {
    categories: EmbeddedCategoryType[] | undefined;
    categoriesIsLoading?: boolean;
    filterData: {id?: string; type: AutomationProjectFilterType};
    tags: EmbeddedTagType[] | undefined;
    tagsIsLoading?: boolean;
}

const AutomationProjectsFilterSidebar = ({
    categories,
    categoriesIsLoading = false,
    filterData,
    tags,
    tagsIsLoading = false,
}: AutomationProjectsFilterSidebarProps) => {
    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: !filterData?.id && filterData.type === AutomationProjectFilterType.Category,
                                name: 'All Categories',
                            }}
                        />

                        {categories &&
                            categories.map((category) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            filterData?.id === category.id &&
                                            filterData.type === AutomationProjectFilterType.Category,
                                        id: category.id,
                                        name: category.name,
                                    }}
                                    key={category.name}
                                    toLink={`?categoryId=${category.id}`}
                                />
                            ))}
                    </>
                }
                loading={categoriesIsLoading}
                title="Categories"
            />

            <LeftSidebarNav
                body={
                    <>
                        {tags &&
                            (tags.length ? (
                                tags.map((tag) => (
                                    <LeftSidebarNavItem
                                        icon={<TagIcon className="mr-1 size-4" />}
                                        item={{
                                            current:
                                                filterData?.id === tag.id &&
                                                filterData.type === AutomationProjectFilterType.Tag,
                                            id: tag.id,
                                            name: tag.name,
                                        }}
                                        key={tag.id}
                                        toLink={`?tagId=${tag.id}`}
                                    />
                                ))
                            ) : (
                                <span className="px-3 text-xs">No defined tags.</span>
                            ))}
                    </>
                }
                className="mb-0"
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default AutomationProjectsFilterSidebar;
