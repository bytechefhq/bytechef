import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {
    AutomationWorkflowProjectCategoriesQuery,
    AutomationWorkflowProjectTagsQuery,
} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';

export enum AutomationWorkflowProjectFilterType {
    Category,
    Tag,
}

type EmbeddedCategoryType = AutomationWorkflowProjectCategoriesQuery['automationWorkflowProjectCategories'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

interface AutomationWorkflowProjectsFilterSidebarProps {
    categories: EmbeddedCategoryType[] | undefined;
    filterData: {id?: string; type: AutomationWorkflowProjectFilterType};
    tags: EmbeddedTagType[] | undefined;
}

const AutomationWorkflowProjectsFilterSidebar = ({
    categories,
    filterData,
    tags,
}: AutomationWorkflowProjectsFilterSidebarProps) => {
    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current:
                                    !filterData?.id && filterData.type === AutomationWorkflowProjectFilterType.Category,
                                name: 'All Categories',
                            }}
                        />

                        {categories &&
                            categories.map((category) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            filterData?.id === category.id &&
                                            filterData.type === AutomationWorkflowProjectFilterType.Category,
                                        id: category.id,
                                        name: category.name,
                                    }}
                                    key={category.name}
                                    toLink={`?categoryId=${category.id}`}
                                />
                            ))}
                    </>
                }
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
                                                filterData.type === AutomationWorkflowProjectFilterType.Tag,
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
                title="Tags"
            />
        </>
    );
};

export default AutomationWorkflowProjectsFilterSidebar;
