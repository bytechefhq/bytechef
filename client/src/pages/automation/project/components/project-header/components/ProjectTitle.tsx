import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Project, ProjectStatus} from '@/shared/middleware/automation/configuration';

const ProjectTitle = ({project}: {project: Project}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className="flex max-w-96 items-center space-x-2">
                    <h1 className="truncate">{project?.name}</h1>

                    {project.name.length > 43 && <TooltipContent>{project.name}</TooltipContent>}

                    {project && (
                        <Badge
                            className="bg-surface-neutral-primary flex space-x-1"
                            styleType={
                                project.lastStatus === ProjectStatus.Published ? 'success-outline' : 'outline-outline'
                            }
                            weight="semibold"
                        >
                            <span>V{project.lastProjectVersion}</span>

                            <span>{project.lastStatus}</span>
                        </Badge>
                    )}
                </div>
            </TooltipTrigger>
        </Tooltip>
    );
};

export default ProjectTitle;
