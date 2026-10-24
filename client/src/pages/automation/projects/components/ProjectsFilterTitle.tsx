import Badge from '@/components/Badge/Badge';
import {AgentsFilterType} from '@/pages/automation/agents/components/AgentsFilterLeftSidebarNav';
import {DataSyncsFilterType} from '@/pages/automation/data-syncs/components/DataSyncsFilterLeftSidebarNav';
import {Type} from '@/pages/automation/projects/Projects';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ProjectsFilterTitle = ({
    agentsFilter,
    categories,
    dataSyncsFilter,
    filterData,
    tags,
}: {
    agentsFilter?: AgentsFilterType;
    categories: Category[] | undefined;
    dataSyncsFilter?: DataSyncsFilterType;
    filterData: {id?: number; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Category) {
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

            {agentsFilter && (
                <Badge
                    label={`Agents: ${agentsFilter === 'scheduled' ? 'Scheduled' : 'All Agents'}`}
                    styleType="primary-outline"
                    weight="semibold"
                />
            )}

            {dataSyncsFilter && (
                <Badge
                    label={`Data Syncs: ${dataSyncsFilter === 'scheduled' ? 'Scheduled' : 'All Data Syncs'}`}
                    styleType="primary-outline"
                    weight="semibold"
                />
            )}
        </div>
    );
};

export default ProjectsFilterTitle;
