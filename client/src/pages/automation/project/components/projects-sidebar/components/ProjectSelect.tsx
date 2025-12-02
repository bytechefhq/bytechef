import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectSeparator,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {useCallback} from 'react';

interface ProjectSelectProps {
    projectId: number;
    setSelectedProjectId: (projectId: number) => void;
    selectedProjectId: number;
    projects: Project[];
}

const ProjectSelect = ({projectId, projects, selectedProjectId, setSelectedProjectId}: ProjectSelectProps) => {
    const getProjectName = useCallback(
        (projectId: number) => {
            const project = projects.find((project) => project.id === projectId);

            return project ? project.name : '';
        },
        [projects]
    );

    const currentProjectName = getProjectName(selectedProjectId);

    return (
        <Select
            defaultValue={projectId.toString()}
            onValueChange={(value) => setSelectedProjectId(+value)}
            value={selectedProjectId.toString()}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    <SelectTrigger
                        aria-label="Select project"
                        className="[&>span]:line-clamp-0 w-full border-stroke-neutral-secondary bg-background px-3 py-2 shadow-none hover:bg-surface-neutral-primary-hover [&>span]:truncate [&>svg]:min-w-4"
                    >
                        <SelectValue placeholder="Select a project">
                            {selectedProjectId === projectId ? 'Current project' : currentProjectName || 'All projects'}
                        </SelectValue>
                    </SelectTrigger>
                </TooltipTrigger>

                {selectedProjectId !== projectId && currentProjectName.length > 42 && (
                    <TooltipContent>{currentProjectName}</TooltipContent>
                )}
            </Tooltip>

            <SelectContent className="w-full">
                <SelectItem
                    className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                    value={projectId.toString()}
                >
                    Current project
                </SelectItem>

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
                                key={project.id!}
                                title={project.name!.length > 40 ? project.name! : undefined}
                                value={project.id!.toString()}
                            >
                                {project.name!}
                            </SelectItem>
                        ))}
                    </SelectGroup>
                )}
            </SelectContent>
        </Select>
    );
};

export default ProjectSelect;
