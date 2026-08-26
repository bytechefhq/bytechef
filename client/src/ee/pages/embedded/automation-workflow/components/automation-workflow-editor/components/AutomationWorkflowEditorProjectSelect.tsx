import FilterableSelect from '@/components/FilterableSelect/FilterableSelect';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const ALL_PROJECTS_VALUE = '0';

interface AutomationWorkflowEditorProjectSelectProps {
    projectId: string;
    projects: AutomationWorkflowProjectsQuery['automationWorkflowProjects'];
    selectedProjectId: string;
    setSelectedProjectId: (projectId: string) => void;
}

const AutomationWorkflowEditorProjectSelect = ({
    projectId,
    projects,
    selectedProjectId,
    setSelectedProjectId,
}: AutomationWorkflowEditorProjectSelectProps) => {
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

export default AutomationWorkflowEditorProjectSelect;
