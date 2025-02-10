import {Badge} from '@/components/ui/badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import * as React from 'react';
import {twMerge} from 'tailwind-merge';

const ProjectsSidebar = ({
    onProjectClick,
    projectId,
    projects,
}: {
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    projectId: number;
    projects?: Project[];
}) => {
    return (
        <div className="space-y-0.5 overflow-y-scroll px-2">
            {projects &&
                projects.map((curProject) => (
                    <div
                        className={twMerge(
                            'flex cursor-pointer items-center justify-between rounded-lg px-2 py-3 text-sm hover:bg-muted',
                            curProject.id === projectId && 'bg-muted/50'
                        )}
                        key={curProject.id}
                        onClick={() => onProjectClick(curProject.id!, curProject?.projectWorkflowIds![0])}
                    >
                        <div className={twMerge('flex flex-col gap-1', curProject.id === projectId && 'font-semibold')}>
                            <div className="flex">
                                <Tooltip>
                                    <TooltipTrigger>
                                        <div className="max-w-56 overflow-hidden truncate">{curProject.name}</div>
                                    </TooltipTrigger>

                                    <TooltipContent>{curProject.name}</TooltipContent>
                                </Tooltip>
                            </div>

                            <div className="mr-1 text-xs">
                                {curProject.projectWorkflowIds?.length === 1
                                    ? `${curProject.projectWorkflowIds?.length} workflow`
                                    : `${curProject.projectWorkflowIds?.length} workflows`}
                            </div>
                        </div>

                        {curProject.lastPublishedDate && curProject.lastProjectVersion ? (
                            <Badge className="flex space-x-1" variant="success">
                                <span>V{curProject.lastProjectVersion - 1}</span>

                                <span>PUBLISHED</span>
                            </Badge>
                        ) : (
                            <Badge className="flex space-x-1" variant="outline">
                                <span>V{curProject.lastProjectVersion}</span>

                                <span>{curProject.lastStatus}</span>
                            </Badge>
                        )}
                    </div>
                ))}
        </div>
    );
};

export default ProjectsSidebar;
