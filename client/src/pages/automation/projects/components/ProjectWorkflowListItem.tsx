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
import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectModel, WorkflowModel} from '@/middleware/helios/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
    useUpdateWorkflowMutation,
} from '@/mutations/workflows.mutations';
import WorkflowDialog from '@/pages/automation/project/components/WorkflowDialog';
import {ProjectKeys} from '@/queries/projects.queries';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectWorkflowListItem = ({
    filteredDefinitionNames,
    project,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredDefinitionNames?: string[];
    project: ProjectModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
}) => {
    const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowModel | undefined>(undefined);

    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteWorkflowMutationMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            setShowDeleteDialog(false);
        },
    });

    const duplicateWorkflowMutationMutation = useDuplicateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const updateWorkflowMutationMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projectWorkflows(project.id!)});

            setShowEditDialog(false);
        },
    });

    return (
        <>
            <Link
                className="flex flex-1 items-center"
                to={`/automation/projects/${project.id}/workflows/${workflow.id}`}
            >
                <div className="w-6/12 text-sm font-semibold">{workflow.label}</div>

                <div className="flex">
                    {filteredDefinitionNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];

                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="h-5 w-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : taskDispatcherDefinition?.icon ?? ''
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="right">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </Link>

            <div className="flex justify-end gap-x-4">
                <Tooltip>
                    <TooltipTrigger className="flex items-center text-sm text-gray-500">
                        <CalendarIcon aria-hidden="true" className="mr-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" />

                        <span>
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            onClick={() => {
                                setSelectedWorkflow(workflow);
                                setShowEditDialog(true);
                            }}
                        >
                            Edit
                        </DropdownMenuItem>

                        {project && workflow && (
                            <DropdownMenuItem
                                onClick={() =>
                                    duplicateWorkflowMutationMutation.mutate({
                                        id: project.id!,
                                        workflowId: workflow.id!,
                                    })
                                }
                            >
                                Duplicate
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => {
                                setSelectedWorkflow(workflow);
                                setShowDeleteDialog(true);
                            }}
                        >
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the workflow.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={() => {
                                if (project?.id && selectedWorkflow?.id) {
                                    deleteWorkflowMutationMutation.mutate({
                                        id: project?.id,
                                        workflowId: selectedWorkflow?.id,
                                    });
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutationMutation}
                    workflow={selectedWorkflow!}
                />
            )}
        </>
    );
};

export default ProjectWorkflowListItem;
