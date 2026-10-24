import EmptyList from '@/components/EmptyList';
import useDataSyncs from '@/pages/automation/data-syncs/hooks/useDataSyncs';
import ProjectDataSyncCreationActions from '@/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncCreationActions';
import ProjectDataSyncListItem from '@/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {ArrowLeftRightIcon} from 'lucide-react';
import {useMemo} from 'react';

interface ProjectDataSyncListProps {
    project: Project;
}

const ProjectDataSyncList = ({project}: ProjectDataSyncListProps) => {
    const {dataSyncs} = useDataSyncs();

    const projectDataSyncs = useMemo(
        () => dataSyncs.filter((dataSync) => +dataSync.projectId === project.id),
        [dataSyncs, project.id]
    );

    return (
        <div className="pt-3">
            {projectDataSyncs.length > 0 ? (
                <ul className="divide-y divide-stroke-neutral-primary">
                    {projectDataSyncs.map((dataSync) => (
                        <ProjectDataSyncListItem dataSync={dataSync} key={dataSync.id} />
                    ))}
                </ul>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={<ProjectDataSyncCreationActions placement="emptyState" project={project} />}
                        icon={<ArrowLeftRightIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new data sync."
                        title="No data syncs in this project"
                    />
                </div>
            )}
        </div>
    );
};

export default ProjectDataSyncList;
