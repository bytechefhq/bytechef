import FilterableSelect from '@/components/FilterableSelect/FilterableSelect';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export const ALL_PROJECTS_VALUE = '0';

interface AutomationEditorProjectSelectProps {
    projectId: string;
    projects: AutomationProjectsQuery['automationProjects'];
    selectedProjectId: string;
    setSelectedProjectId: (projectId: string) => void;
}

const AutomationEditorProjectSelect = ({
    projectId,
    projects,
    selectedProjectId,
    setSelectedProjectId,
}: AutomationEditorProjectSelectProps) => {
    const items = useMemo(() => projects.map((project) => ({label: project.name, value: project.id})), [projects]);

    const pinnedItems = useMemo(
        () => [
            ...(projectId ? [{label: 'Current project', value: projectId}] : []),
            {label: 'All projects', value: ALL_PROJECTS_VALUE},
        ],
        [projectId]
    );

    const selectedProject = projects.find((project) => project.id === selectedProjectId);
    const currentProjectName = selectedProject ? selectedProject.name : '';
    const showsCurrentProject = selectedProjectId === projectId;

    return (
        <FilterableSelect
            ariaLabel="Select project"
            emptyMessage="No projects found."
            items={items}
            onValueChange={setSelectedProjectId}
            pinnedItems={pinnedItems}
            searchPlaceholder="Search projects..."
            tooltip={!showsCurrentProject && currentProjectName.length > 42 ? currentProjectName : undefined}
            triggerLabel={showsCurrentProject ? 'Current project' : currentProjectName || 'All projects'}
            value={selectedProjectId}
        />
    );
};

export default AutomationEditorProjectSelect;
