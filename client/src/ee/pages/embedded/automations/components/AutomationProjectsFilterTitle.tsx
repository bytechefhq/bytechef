import Badge from '@/components/Badge/Badge';
import {AutomationProjectFilterType} from '@/ee/pages/embedded/automations/components/automation-projects-filter-sidebar/AutomationProjectsFilterSidebar';
import {AutomationProjectCategoriesQuery, AutomationProjectTagsQuery} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

type EmbeddedCategoryType = AutomationProjectCategoriesQuery['automationProjectCategories'][number];
type EmbeddedTagType = AutomationProjectTagsQuery['automationProjectTags'][number];

interface AutomationProjectsFilterTitleProps {
    categories: EmbeddedCategoryType[] | undefined;
    filterData: {id?: string; type: AutomationProjectFilterType};
    tags: EmbeddedTagType[] | undefined;
}

const AutomationProjectsFilterTitle = ({categories, filterData, tags}: AutomationProjectsFilterTitleProps) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === AutomationProjectFilterType.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            <Badge
                className="border-transparent"
                label={`${searchParams.get('tagId') ? 'Tags' : 'Categories'}: ${pageTitle ?? 'All Categories'}`}
                styleType="primary-outline"
                weight="semibold"
            />
        </div>
    );
};

export default AutomationProjectsFilterTitle;
