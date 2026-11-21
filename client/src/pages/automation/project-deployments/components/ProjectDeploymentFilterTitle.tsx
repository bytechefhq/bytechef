import Badge from '@/components/Badge/Badge';
import {AgentsFilterType} from '@/pages/automation/agents/components/AgentsFilterLeftSidebarNav';
import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ProjectDeploymentFilterTitle = ({
    agentsFilter,
    filterData,
    projects,
    tags,
}: {
    agentsFilter?: AgentsFilterType;
    environment?: number;
    filterData: {id?: number; type: Type};
    projects: Project[] | undefined;
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Project) {
        pageTitle = projects?.find((project) => project.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            <Badge
                className="border-transparent"
                label={`${searchParams.get('tagId') ? 'Tags' : 'Projects'}: ${pageTitle ?? 'All Projects'}`}
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

export default ProjectDeploymentFilterTitle;
