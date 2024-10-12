import {Type} from '@/pages/automation/project-instances/ProjectInstances';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ProjectInstancesFilterTitle = ({
    filterData,
    projects,
    tags,
}: {
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
            <span className="text-sm uppercase text-muted-foreground">{`Filter by ${searchParams.get('tagId') ? 'tag' : 'project'}:`}</span>

            <span className="text-base">{pageTitle ?? 'All Projects'}</span>
        </div>
    );
};

export default ProjectInstancesFilterTitle;
