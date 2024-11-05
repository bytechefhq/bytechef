import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {Project, ProjectStatus} from '@/shared/middleware/automation/configuration';
import {PanelLeftIcon} from 'lucide-react';
import * as React from 'react';

const ProjectHeaderTitle = ({project}: {project: Project}) => {
    const {leftSidebarOpen, setLeftSidebarOpen} = useProjectsLeftSidebarStore();

    return (
        <div className="flex items-center space-x-2">
            {!leftSidebarOpen && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="hover:bg-background/70"
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            size="icon"
                            variant="ghost"
                        >
                            <PanelLeftIcon className="size-5" />
                        </Button>
                    </TooltipTrigger>

                    <TooltipContent>See projects</TooltipContent>
                </Tooltip>
            )}

            <h1>{project?.name}</h1>

            {project && (
                <Badge
                    className="flex space-x-1"
                    variant={project.lastStatus === ProjectStatus.Published ? 'success' : 'outline'}
                >
                    <span>V{project.lastProjectVersion}</span>

                    <span>{project.lastStatus}</span>
                </Badge>
            )}
        </div>
    );
};

export default ProjectHeaderTitle;
