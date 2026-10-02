import FilterableSelect from '@/components/FilterableSelect/FilterableSelect';
import {Project} from '@/shared/middleware/automation/configuration';
import {useMemo} from 'react';

const ALL_PROJECTS_VALUE = '0';

interface ProjectSelectProps {
    projectId: number;
    setSelectedProjectId: (projectId: number) => void;
    selectedProjectId: number;
    projects: Project[];
}

const ProjectSelect = ({projectId, projects, selectedProjectId, setSelectedProjectId}: ProjectSelectProps) => {
    const items = useMemo(
        () => projects.map((project) => ({label: project.name!, value: project.id!.toString()})),
        [projects]
    );

    const pinnedItems = useMemo(
        () => [
            {label: 'Current project', value: projectId.toString()},
            {label: 'All projects', value: ALL_PROJECTS_VALUE},
        ],
        [projectId]
    );

    const selectedProject = projects.find((project) => project.id === selectedProjectId);
    const currentProjectName = selectedProject ? selectedProject.name! : '';
    const showsCurrentProject = selectedProjectId === projectId;

    return (
        <FilterableSelect
            ariaLabel="Select project"
            emptyMessage="No projects found."
            items={items}
            onValueChange={(value) => setSelectedProjectId(+value)}
            pinnedItems={pinnedItems}
            searchPlaceholder="Search projects..."
            tooltip={!showsCurrentProject && currentProjectName.length > 42 ? currentProjectName : undefined}
            triggerLabel={showsCurrentProject ? 'Current project' : currentProjectName || 'All projects'}
            value={selectedProjectId.toString()}
        />
    );
};

export default ProjectSelect;
