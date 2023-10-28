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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
    HoverCard,
    HoverCardContent,
    HoverCardTrigger,
} from '@/components/ui/hover-card';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ProjectModel,
    ProjectModelStatusEnum,
    TagModel,
} from '@/middleware/helios/configuration';
import {
    useCreateProjectWorkflowRequestMutation,
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    useUpdateProjectTagsMutation,
} from '@/mutations/projects.mutations';
import {ProjectKeys} from '@/queries/projects.queries';
import {AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';

import TagList from '../../../components/TagList/TagList';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
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
        useCreateProjectWorkflowRequestMutation({
            onSuccess: (workflow) => {
                navigate(
                    `/automation/projects/${project.id}/workflow/${workflow?.id}`
                );

                setShowWorkflowDialog(false);
            },
        });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
            queryClient.invalidateQueries(ProjectKeys.projectCategories);
            queryClient.invalidateQueries(ProjectKeys.projectTags);
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
        },
    });

    const updateProjectTagsMutation = useUpdateProjectTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
            queryClient.invalidateQueries(ProjectKeys.projectTags);
        },
    });

    return (
        <>
            <div className="flex items-center justify-between">
                <div className="w-10/12">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
                            <Link
                                to={`/automation/projects/${project?.id}/workflow/${project?.workflowIds![0]}`}
                            >
                                {project.description ? (
                                    <HoverCard>
                                        <HoverCardTrigger asChild>
                                            <span className="mr-2 text-base font-semibold text-gray-900">
                                                {project.name}
                                            </span>
                                        </HoverCardTrigger>

                                        <HoverCardContent className="w-80">
                                            {project.description}
                                        </HoverCardContent>
                                    </HoverCard>
                                ) : (
                                    <span className="mr-2 text-base font-semibold text-gray-900">
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
                                variant={
                                    project.status ===
                                    ProjectModelStatusEnum.Published
                                        ? 'success'
                                        : 'secondary'
                                }
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
                            <AccordionTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
                                <div className="mr-1">
                                    {project.workflowIds?.length === 1
                                        ? `${project.workflowIds?.length} workflow`
                                        : `${project.workflowIds?.length} workflows`}
                                </div>

                                <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
                            </AccordionTrigger>

                            <div onClick={(event) => event.preventDefault()}>
                                {project.tags && (
                                    <TagList
                                        id={project.id!}
                                        remainingTags={remainingTags}
                                        tags={project.tags}
                                        updateTagsMutation={
                                            updateProjectTagsMutation
                                        }
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            updateTagsRequestModel: {
                                                tags: tags || [],
                                            },
                                        })}
                                    />
                                )}
                            </div>
                        </div>

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            <Tooltip>
                                <TooltipTrigger>
                                    {project.status ===
                                    ProjectModelStatusEnum.Published ? (
                                        <div className="flex text-sm text-gray-500">
                                            <CalendarIcon
                                                className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                                aria-hidden="true"
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
                            <Button variant="ghost" size="icon">
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
                    project={project}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditDialog(false)}
                />
            )}

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    parentId={project.id}
                    showTrigger={false}
                    visible
                    createWorkflowRequestMutation={
                        createProjectWorkflowRequestMutation
                    }
                    onClose={() => setShowWorkflowDialog(false)}
                />
            )}
        </>
    );
};

export default ProjectListItem;
