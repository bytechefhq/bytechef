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

import TagList from '../../../components/TagList/TagList';
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
            <div className="flex items-center justify-between">
                <div className="w-10/12">
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

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                className={twMerge(
                                    project.status ===
                                        ProjectModelStatusEnum.Published &&
                                        'border-transparent bg-success text-success-foreground hover:bg-success'
                                )}
                                variant="secondary"
                            >
                                {project.status ===
                                ProjectModelStatusEnum.Published
                                    ? `Published V${project.projectVersion}`
                                    : 'Not Published'}
                            </Badge>
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

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            <Tooltip>
                                <TooltipTrigger>
                                    {project.status ===
                                    ProjectModelStatusEnum.Published ? (
                                        <div className="flex items-center text-sm text-gray-500">
                                            <CalendarIcon
                                                aria-hidden="true"
                                                className="mr-0.5 h-4 w-4 shrink-0 text-gray-400"
                                            />

                                            <span>
                                                {`${project.publishedDate?.toLocaleDateString()} ${project.publishedDate?.toLocaleTimeString()}`}
                                            </span>
                                        </div>
                                    ) : (
                                        '-'
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Published Date</TooltipContent>
                            </Tooltip>
                        </div>
                    </div>
                </div>

                <div className="flex w-2/12 justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                className="text-xs"
                                onClick={() => setShowEditDialog(true)}
                            >
                                Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                className="text-xs"
                                onClick={() =>
                                    duplicateProjectMutation.mutate(project.id!)
                                }
                            >
                                Duplicate
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                className="text-xs"
                                onClick={() => setShowWorkflowDialog(true)}
                            >
                                New Workflow
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                className="text-xs text-red-600"
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
                    showTrigger={false}
                    visible
                />
            )}

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    createWorkflowRequestMutation={
                        createProjectWorkflowRequestMutation
                    }
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={project.id}
                    showTrigger={false}
                    visible
                />
            )}
        </>
    );
};

export default ProjectListItem;
