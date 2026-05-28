import Badge from '@/components/Badge/Badge';
import {AutomationWorkflowProjectFilterType} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-projects-filter-sidebar/AutomationWorkflowProjectsFilterSidebar';
import {
    AutomationWorkflowProjectCategoriesQuery,
    AutomationWorkflowProjectTagsQuery,
} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

type EmbeddedCategoryType = AutomationWorkflowProjectCategoriesQuery['automationWorkflowProjectCategories'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

interface AutomationWorkflowProjectsFilterTitleProps {
    categories: EmbeddedCategoryType[] | undefined;
    filterData: {id?: string; type: AutomationWorkflowProjectFilterType};
    tags: EmbeddedTagType[] | undefined;
}

const AutomationWorkflowProjectsFilterTitle = ({
    categories,
    filterData,
    tags,
}: AutomationWorkflowProjectsFilterTitleProps) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === AutomationWorkflowProjectFilterType.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm text-muted-foreground uppercase">{`Filter by ${searchParams.get('tagId') ? 'tag' : 'category'}:`}</span>

            <Badge label={`${pageTitle ?? 'All Categories'}`} styleType="secondary-filled" weight="semibold" />
        </div>
    );
};

export default AutomationWorkflowProjectsFilterTitle;
