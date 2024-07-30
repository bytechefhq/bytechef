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
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {ProjectModel, WorkflowModel} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import {
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
    useUpdateWorkflowMutation,
} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectWorkflowListItem = ({
    filteredComponentNames,
    project,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    project: ProjectModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            setShowDeleteDialog(false);
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(project.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditDialog(false);
        },
    });

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-gray-50" key={workflow.id}>
            <Link
                className="flex flex-1 items-center"
                to={`/automation/projects/${project.id}/project-workflows/${workflow.projectWorkflowId}`}
            >
                <div className="w-80 text-sm font-semibold">{workflow.label}</div>

                <div className="flex">
                    {filteredComponentNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : (taskDispatcherDefinition?.icon ?? '')
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="top">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </Link>

            <div className="flex justify-end gap-x-6">
                <Tooltip>
                    <TooltipTrigger className="flex items-center text-sm text-gray-500">
                        <span>
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            onClick={() => {
                                setShowEditDialog(true);
                            }}
                        >
                            Edit
                        </DropdownMenuItem>

                        {project && workflow && (
                            <DropdownMenuItem
                                onClick={() =>
                                    duplicateWorkflowMutation.mutate({
                                        id: project.id!,
                                        workflowId: workflow.id!,
                                    })
                                }
                            >
                                Duplicate
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuItem
                            onClick={() =>
                                (window.location.href = `/api/automation/internal/workflows/${workflow.id}/export`)
                            }
                        >
                            Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="text-destructive"
                            onClick={() => {
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
                            className="bg-destructive"
                            onClick={() => {
                                if (workflow?.id) {
                                    deleteWorkflowMutation.mutate({
                                        id: workflow.id,
                                    });
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && workflow && (
                <WorkflowDialog
                    onClose={() => setShowEditDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </li>
    );
};

export default ProjectWorkflowListItem;
