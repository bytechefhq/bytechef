import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectSeparator,
    SelectTrigger,
    SelectValue,
} from '@/components/Select/Select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useCallback} from 'react';

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
    const getProjectName = useCallback(
        (targetProjectId: string) => {
            const project = projects.find((project) => project.id === targetProjectId);

            return project ? project.name : '';
        },
        [projects]
    );

    const currentProjectName = getProjectName(selectedProjectId);

    return (
        <Select
            defaultValue={projectId}
            onValueChange={(value) => setSelectedProjectId(value)}
            value={selectedProjectId}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    <SelectTrigger
                        aria-label="Select project"
                        className="w-full border-stroke-neutral-secondary bg-background px-3 py-2 shadow-none hover:bg-surface-neutral-primary-hover [&>span]:line-clamp-0 [&>span]:truncate [&>svg]:min-w-4"
                    >
                        <SelectValue placeholder="Select a project">
                            {selectedProjectId === projectId ? 'Current project' : currentProjectName || 'All projects'}
                        </SelectValue>
                    </SelectTrigger>
                </TooltipTrigger>

                {selectedProjectId !== projectId && currentProjectName && currentProjectName.length > 42 && (
                    <TooltipContent>{currentProjectName}</TooltipContent>
                )}
            </Tooltip>

            <SelectContent className="w-full">
                {projectId && (
                    <SelectItem
                        className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                        value={projectId}
                    >
                        Current project
                    </SelectItem>
                )}

                <SelectItem
                    className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover data-[state=checked]:bg-surface-brand-secondary [&>span]:truncate"
                    value="0"
                >
                    All projects
                </SelectItem>

                <SelectSeparator />

                {projects && (
                    <SelectGroup>
                        {projects.map((project) => (
                            <SelectItem
                                className="cursor-pointer overflow-hidden rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                                key={project.id}
                                title={project.name.length > 40 ? project.name : undefined}
                                value={project.id}
                            >
                                {project.name}
                            </SelectItem>
                        ))}
                    </SelectGroup>
                )}
            </SelectContent>
        </Select>
    );
};

export default AutomationWorkflowEditorProjectSelect;
