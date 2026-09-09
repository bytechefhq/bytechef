import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowShareDialog} from '@/pages/automation/project/components/WorkflowShareDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
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

import '@/shared/styles/dropdownMenu.css';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, Share2Icon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

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
    const [showWorkflowShareDialog, setShowWorkflowShareDialog] = useState(false);

    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    const [searchParams] = useSearchParams();

    const ff_2939 = useFeatureFlagsStore()('ff-2939');

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

            toast('Workflow duplicated successfully.');
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });

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
            className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            key={workflow.id}
        >
            <Link
                aria-label={`Link to workflow ${workflow.label}`}
                className="flex min-w-0 flex-1 items-center gap-2"
                data-testid={`${workflow.projectWorkflowId}-link`}
                to={`/automation/projects/${project.id}/project-workflows/${workflow.projectWorkflowId}?${searchParams}`}
            >
                <div className="flex w-80 min-w-0 shrink-0 flex-col gap-1 pr-1 text-sm font-semibold">
                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 w-full truncate text-start">
                            {workflow.label}
                        </TooltipTrigger>

                        <TooltipContent className="max-w-md break-all">{workflow.label}</TooltipContent>
                    </Tooltip>

                    <div className="flex gap-x-6">
                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm font-normal text-content-neutral-secondary xl:hidden">
                                <span className="text-xs">
                                    {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                                </span>
                            </TooltipTrigger>

                            <TooltipContent>Last Modified Date</TooltipContent>
                        </Tooltip>
                    </div>
                </div>

                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            </Link>

            <div className="flex justify-end gap-x-6">
                <Tooltip>
                    <TooltipTrigger className="hidden items-center text-sm text-content-neutral-secondary xl:block">
                        <span className="text-xs">
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
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
                            onClick={() => setShowWorkflowShareDialog(true)}
                        >
                            <Share2Icon /> Share
                        </DropdownMenuItem>

                        {ff_2939 && (
                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() => {
                                    if (templatesSubmissionForm) {
                                        window.open(templatesSubmissionForm, '_blank');
                                    }
                                }}
                            >
                                <Share2Icon /> Share with Community
                            </DropdownMenuItem>
                        )}

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() =>
                                (window.location.href = `/api/automation/internal/workflows/${workflow.id}/export`)
                            }
                        >
                            <DownloadIcon /> Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator className="m-0" />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={() => {
                                setShowDeleteDialog(true);
                            }}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showDeleteDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={() => {
                        if (workflow?.id) {
                            deleteWorkflowMutation.mutate({
                                id: workflow.id,
                            });
                        }
                    }}
                />
            )}

            {showEditDialog && workflow && (
                <WorkflowDialog
                    onClose={() => setShowEditDialog(false)}
                    onSave={() =>
                        queryClient.invalidateQueries({
                            queryKey: ProjectWorkflowKeys.projectWorkflow(project.id!, parseInt(workflow.id!)),
                        })
                    }
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}

            {showWorkflowShareDialog && (
                <WorkflowShareDialog
                    onOpenChange={() => setShowWorkflowShareDialog(false)}
                    open={showWorkflowShareDialog}
                    projectVersion={project.lastProjectVersion!}
                    workflowId={workflow.id!}
                    workflowUuid={workflow.workflowUuid!}
                />
            )}
        </li>
    );
};

export default ProjectWorkflowListItem;
