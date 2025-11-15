import Badge from '@/components/Badge/Badge';
import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ProjectDeploymentFilterTitle = ({
    filterData,
    projects,
    tags,
}: {
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
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'project'}:
            </span>

            <Badge styleType="secondary-filled" weight="semibold">
                <span className="text-sm">{pageTitle ?? 'All Projects'}</span>
            </Badge>
        </div>
    );
};

export default ProjectDeploymentFilterTitle;
