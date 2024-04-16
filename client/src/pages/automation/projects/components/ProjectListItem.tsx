import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectModel, ProjectStatusModel, TagModel} from '@/middleware/automation/configuration';
import {useUpdateProjectTagsMutation} from '@/mutations/automation/projectTags.mutations';
import {useDeleteProjectMutation, useDuplicateProjectMutation} from '@/mutations/automation/projects.mutations';
import {useCreateProjectWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {ProjectCategoryKeys} from '@/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';

import TagList from '../../../../components/TagList';
import ProjectDialog from './ProjectDialog';

interface ProjectItemProps {
    project: ProjectModel;
    remainingTags?: TagModel[];
}

const ProjectListItem = ({project, remainingTags}: ProjectItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (workflow) => {
            navigate(`/automation/projects/${project.id}/workflows/${workflow?.id}`);

            setShowWorkflowDialog(false);
        },
    });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const updateProjectTagsMutation = useUpdateProjectTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center border-b border-muted py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="relative flex items-center gap-2">
                                <Link
                                    className="flex gap-2"
                                    to={`/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`}
                                >
                                    {project.description ? (
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <span className="text-base font-semibold">{project.name}</span>
                                            </TooltipTrigger>

                                            <TooltipContent>{project.description}</TooltipContent>
                                        </Tooltip>
                                    ) : (
                                        <span className="text-base font-semibold">{project.name}</span>
                                    )}
                                </Link>

                                {project.category && (
                                    <span className="text-xs uppercase text-gray-700">{project.category.name}</span>
                                )}
                            </div>
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex items-center text-xs font-semibold text-gray-700">
                                    <div className="mr-1">
                                        {project.workflowIds?.length === 1
                                            ? `${project.workflowIds?.length} workflow`
                                            : `${project.workflowIds?.length} workflows`}
                                    </div>

                                    <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    {project.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequestModel: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={project.id!}
                                            remainingTags={remainingTags}
                                            tags={project.tags}
                                            updateTagsMutation={updateProjectTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex flex-col items-end gap-y-4">
                            <Badge
                                className="flex space-x-1"
                                variant={project.status === ProjectStatusModel.Published ? 'success' : 'secondary'}
                            >
                                <span>V{project.projectVersion}</span>

                                <span>{project.status === ProjectStatusModel.Published ? `Published` : 'Draft'}</span>
                            </Badge>

                            <Tooltip>
                                <TooltipTrigger>
                                    <div className="flex items-center text-sm text-gray-500 sm:mt-0">
                                        {project.status === ProjectStatusModel.Published ? (
                                            <span>
                                                {`Published at ${project.publishedDate?.toLocaleDateString()} ${project.publishedDate?.toLocaleTimeString()}`}
                                            </span>
                                        ) : (
                                            '-'
                                        )}
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent>Last Published Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost">
                                    <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                                </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                                <DropdownMenuItem
                                    onClick={() =>
                                        navigate(
                                            `/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`
                                        )
                                    }
                                >
                                    View Workflows
                                </DropdownMenuItem>

                                <DropdownMenuItem onClick={() => duplicateProjectMutation.mutate(project.id!)}>
                                    Duplicate
                                </DropdownMenuItem>

                                <DropdownMenuItem onClick={() => setShowWorkflowDialog(true)}>
                                    New Workflow
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the project and workflows it
                            contains..
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={() => {
                                if (project.id) {
                                    deleteProjectMutation.mutate(project.id);
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && <ProjectDialog onClose={() => setShowEditDialog(false)} project={project} />}

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={project.id}
                />
            )}
        </>
    );
};

export default ProjectListItem;
