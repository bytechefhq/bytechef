import {Badge} from '@/components/ui/badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import * as React from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const ProjectsSidebar = ({projectId}: {projectId: number}) => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        id: currentWorkspaceId!,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    return (
        <div className="space-y-0.5 overflow-y-scroll px-2">
            {projects &&
                projects.map((curProject) => (
                    <div
                        className={twMerge(
                            'py-3 px-2 flex cursor-pointer items-center justify-between hover:bg-background/50 rounded-lg text-sm',
                            curProject.id === projectId && 'bg-background/50'
                        )}
                        key={curProject.id}
                        onClick={() =>
                            navigate(
                                `/automation/projects/${curProject?.id}/project-workflows/${curProject?.projectWorkflowIds![0]}?${searchParams}`
                            )
                        }
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
