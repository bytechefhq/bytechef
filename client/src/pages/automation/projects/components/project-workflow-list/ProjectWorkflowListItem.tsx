import '@/shared/styles/dropdownMenu.css';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
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
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
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
import {CopyIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon, UploadIcon} from 'lucide-react';
import {useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';

const ProjectWorkflowListItem = ({
    filteredComponentNames,
    project,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    project: Project;
    workflow: Workflow;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const [searchParams] = useSearchParams();

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
        onSuccess: () => {
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
        <li
            className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-destructive-foreground"
            key={workflow.id}
        >
            <Link
                className="flex flex-1 items-center"
                to={`/automation/projects/${project.id}/project-workflows/${workflow.projectWorkflowId}?${searchParams}`}
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
                                        <LazyLoadSVG
                                            className="size-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : (taskDispatcherDefinition?.icon ?? '')
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="top">
                                        {componentDefinition?.title ?? taskDispatcherDefinition?.title}
                                    </TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </Link>

            <div className="flex justify-end gap-x-6">
                <Tooltip>
                    <TooltipTrigger className="flex items-center text-sm text-muted-foreground">
                        <span className="text-xs">
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <EllipsisVerticalIcon className="size-4 cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="p-0">
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setShowEditDialog(true);
                            }}
                        >
                            <EditIcon /> Edit
                        </DropdownMenuItem>

                        {project && workflow && (
                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() =>
                                    duplicateWorkflowMutation.mutate({
                                        id: project.id!,
                                        workflowId: workflow.id!,
                                    })
                                }
                            >
                                <CopyIcon /> Duplicate
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() =>
                                (window.location.href = `/api/automation/internal/workflows/${workflow.id}/export`)
                            }
                        >
                            <UploadIcon /> Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator className="m-0" />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={() => {
                                setShowDeleteDialog(true);
                            }}
                        >
                            <Trash2Icon /> Delete
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
                        <AlertDialogCancel className="shadow-none" onClick={() => setShowDeleteDialog(false)}>
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
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
                    projectId={project.id}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </li>
    );
};

export default ProjectWorkflowListItem;
