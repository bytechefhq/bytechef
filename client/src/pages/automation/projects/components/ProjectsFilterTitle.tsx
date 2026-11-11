import Badge from '@/components/Badge/Badge';
import {AgentsFilterType} from '@/pages/automation/agents/components/AgentsFilterLeftSidebarNav';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

interface ProjectsFilterTitleItemI {
    id?: number | string;
    name?: string;
}

interface ProjectsFilterTitleProps {
    agentsFilter?: AgentsFilterType;
    categories: ProjectsFilterTitleItemI[] | undefined;
    filterByTag: boolean;
    filterId?: number | string;
    tags: ProjectsFilterTitleItemI[] | undefined;
}

const ProjectsFilterTitle = ({agentsFilter, categories, filterByTag, filterId, tags}: ProjectsFilterTitleProps) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterByTag) {
        pageTitle = tags?.find((tag) => tag.id === filterId)?.name;
    } else {
        pageTitle = categories?.find((category) => category.id === filterId)?.name;
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

            {agentsFilter && (
                <Badge
                    label={`Agents: ${agentsFilter === 'scheduled' ? 'Scheduled' : 'All Agents'}`}
                    styleType="primary-outline"
                    weight="semibold"
                />
            )}
        </div>
    );
};

export default ProjectsFilterTitle;
