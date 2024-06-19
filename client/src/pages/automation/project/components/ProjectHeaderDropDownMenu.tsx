import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectModel, ProjectStatusModel} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {CaretDownIcon} from '@radix-ui/react-icons';
import * as React from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const ProjectHeaderDropdownMenu = ({project}: {project: ProjectModel}) => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        id: currentWorkspaceId!,
    });

    const navigate = useNavigate();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button className="flex cursor-pointer items-center space-x-2 hover:bg-gray-200" variant="ghost">
                    <h1>{project?.name}</h1>

                    {project && (
                        <Badge
                            className="flex space-x-1"
                            variant={project.status === ProjectStatusModel.Published ? 'success' : 'outline'}
                        >
                            <span>V{project.projectVersion}</span>

                            <span>{project.status === ProjectStatusModel.Published ? `Published` : 'Draft'}</span>
                        </Badge>
                    )}

                    <CaretDownIcon />
                </Button>
            </DropdownMenuTrigger>

            {projects && (
                <DropdownMenuContent align="start" className="max-h-96 w-96 space-y-2 overflow-y-scroll">
                    {projects.map((curProject) => (
                        <DropdownMenuItem
                            className="flex cursor-pointer items-center justify-between"
                            key={curProject.id}
                            onClick={() =>
                                navigate(
                                    `/automation/projects/${curProject?.id}/project-workflows/${curProject?.projectWorkflowIds![0]}`
                                )
                            }
                        >
                            <div
                                className={twMerge(
                                    'flex flex-col gap-1',
                                    curProject.id === project.id && 'font-semibold'
                                )}
                            >
                                <div className="flex items-center gap-2">
                                    <span>{curProject.name}</span>

                                    {curProject.category && (
                                        <span className="text-xs uppercase text-gray-700">
                                            {curProject.category.name}
                                        </span>
                                    )}
                                </div>

                                <div className="mr-1 text-xs">
                                    {curProject.projectWorkflowIds?.length === 1
                                        ? `${curProject.projectWorkflowIds?.length} workflow`
                                        : `${curProject.projectWorkflowIds?.length} workflows`}
                                </div>
                            </div>

                            <Badge className="flex space-x-1" variant="secondary">
                                <span>V{curProject.projectVersion}</span>

                                <span>Draft</span>
                            </Badge>
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuContent>
            )}
        </DropdownMenu>
    );
};

export default ProjectHeaderDropdownMenu;
