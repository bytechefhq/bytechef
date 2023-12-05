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
import {
    ProjectModel,
    ProjectModelStatusEnum,
    TagModel,
} from '@/middleware/helios/configuration';
import {useUpdateProjectTagsMutation} from '@/mutations/projectTags.mutations';
import {useCreateProjectWorkflowMutation} from '@/mutations/projectWorkflows.mutations';
import {
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
} from '@/mutations/projects.mutations';
import WorkflowDialog from '@/pages/automation/project/components/WorkflowDialog';
import {ProjectCategoryKeys} from '@/queries/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/projectTags.quries';
import {ProjectKeys} from '@/queries/projects.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

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

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowMutation({
            onSuccess: (workflow) => {
                navigate(
                    `/automation/projects/${project.id}/workflows/${workflow?.id}`
                );

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
            <div className="flex w-full items-center justify-between rounded-md px-2 py-5 hover:bg-gray-50">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
                            <Link
                                to={`/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`}
                            >
                                {project.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="mr-2 text-base font-semibold">
                                                {project.name}
                                            </span>
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            {project.description}
                                        </TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <span className="mr-2 text-base font-semibold">
                                        {project.name}
                                    </span>
                                )}
                            </Link>

                            {project.category && (
                                <span className="text-xs uppercase text-gray-700">
                                    {project.category.name}
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
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
                                        updateTagsMutation={
                                            updateProjectTagsMutation
                                        }
                                    />
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <div className="flex flex-col items-end gap-y-4">
                        <Badge
                            className={twMerge(
                                project.status ===
                                    ProjectModelStatusEnum.Published &&
                                    'border-transparent bg-success text-success-foreground hover:bg-success'
                            )}
                            variant="secondary"
                        >
                            {project.status === ProjectModelStatusEnum.Published
                                ? `Published V${project.projectVersion}`
                                : 'Not Published'}
                        </Badge>

                        <Tooltip>
                            <TooltipTrigger>
                                <div className="flex items-center text-sm text-gray-500 sm:mt-0">
                                    {project.status ===
                                    ProjectModelStatusEnum.Published ? (
                                        <>
                                            <CalendarIcon
                                                aria-hidden="true"
                                                className="mr-0.5 h-3.5 w-3.5 shrink-0 text-gray-400"
                                            />

                                            <span>
                                                {`Published at ${project.publishedDate?.toLocaleDateString()} ${project.publishedDate?.toLocaleTimeString()}`}
                                            </span>
                                        </>
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
                                <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                onClick={() => setShowEditDialog(true)}
                            >
                                Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                onClick={() =>
                                    duplicateProjectMutation.mutate(project.id!)
                                }
                            >
                                Duplicate
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                onClick={() => setShowWorkflowDialog(true)}
                            >
                                New Workflow
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                className="text-red-600"
                                onClick={() => setShowDeleteDialog(true)}
                            >
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            Are you absolutely sure?
                        </AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently
                            delete the project and workflows it contains..
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={() => setShowDeleteDialog(false)}
                        >
                            Cancel
                        </AlertDialogCancel>

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

            {showEditDialog && (
                <ProjectDialog
                    onClose={() => setShowEditDialog(false)}
                    project={project}
                />
            )}

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    createWorkflowRequestMutation={
                        createProjectWorkflowRequestMutation
                    }
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={project.id}
                />
            )}
        </>
    );
};

export default ProjectListItem;
