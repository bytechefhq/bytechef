import Badge from '@/components/Badge/Badge';
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
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
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

import '@/shared/styles/dropdownMenu.css';
import {useToast} from '@/hooks/use-toast';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ComponentIcon,
    CopyIcon,
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    Share2Icon,
    Trash2Icon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
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
    const [showWorkflowShareDialog, setShowWorkflowShareDialog] = useState(false);

    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    const [searchParams] = useSearchParams();

    const ff_1042 = useFeatureFlagsStore()('ff-1042');
    const ff_2939 = useFeatureFlagsStore()('ff-2939');

    const queryClient = useQueryClient();

    const {toast} = useToast();

    const triggerComponentName = workflow.workflowTriggerComponentNames?.[0];
    const triggerType = workflow.triggers?.[0]?.type;

    const triggerVersionNumber = triggerType ? +triggerType.split('/')[1].replace('v', '') : 1;

    const {data: fullTriggerComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: triggerComponentName || '',
            componentVersion: triggerVersionNumber,
        },
        !!triggerComponentName
    );

    const triggerData = useMemo(() => {
        if (!triggerComponentName && !workflow.triggers?.[0]) {
            return null;
        }

        const triggerFromWorkflow = workflow.triggers?.[0];
        const triggerDefinition = workflowComponentDefinitions[triggerComponentName || ''];

        const triggerOperationName = triggerFromWorkflow?.type?.split('/')[2];

        const matchedTrigger = triggerOperationName
            ? fullTriggerComponentDefinition?.triggers?.find((trigger) => trigger.name === triggerOperationName)
            : null;

        return {
            actionDescription: matchedTrigger?.description || triggerFromWorkflow?.description || null,
            actionLabel: matchedTrigger?.title || triggerFromWorkflow?.label || null,
            componentName: triggerDefinition?.title || triggerComponentName || 'Unknown Trigger',
            iconSrc: triggerDefinition?.icon || null,
        };
    }, [workflow, workflowComponentDefinitions, triggerComponentName, fullTriggerComponentDefinition]);

    const taskOnlyComponentNames = useMemo(() => {
        if (!filteredComponentNames) {
            return [];
        }

        const triggerCount = workflow.workflowTriggerComponentNames?.length ?? 0;

        return filteredComponentNames.slice(triggerCount);
    }, [filteredComponentNames, workflow.workflowTriggerComponentNames]);

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            setShowDeleteDialog(false);
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast({
                description: 'Workflow duplication failed.',
                variant: 'destructive',
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast({
                description: 'Workflow duplicated successfully.',
            });
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
            className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-destructive-foreground"
            key={workflow.id}
        >
            <Link
                aria-label={`Link to workflow ${workflow.label}`}
                className="flex flex-1 items-center gap-2"
                data-testid={`${workflow.projectWorkflowId}-link`}
                to={`/automation/projects/${project.id}/project-workflows/${workflow.projectWorkflowId}?${searchParams}`}
            >
                <div className="w-80 shrink-0 pr-1 text-sm font-semibold">
                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 text-start">{workflow.label}</TooltipTrigger>

                        <TooltipContent>{workflow.label}</TooltipContent>
                    </Tooltip>
                </div>

                {triggerData && (
                    <div className="flex shrink-0 items-center gap-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div className="flex shrink-0 items-center justify-center rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary p-1">
                                    {triggerData.iconSrc ? (
                                        <InlineSVG
                                            className="size-5"
                                            loader={<ComponentIcon className="size-5 flex-none" />}
                                            src={triggerData.iconSrc}
                                            title={null}
                                        />
                                    ) : (
                                        <ComponentIcon className="size-3 flex-none text-content-neutral-primary" />
                                    )}
                                </div>
                            </TooltipTrigger>

                            <TooltipContent>{triggerData.componentName}</TooltipContent>
                        </Tooltip>

                        {triggerData.actionDescription ? (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <div className="shrink-0">
                                        <Badge
                                            label={triggerData.actionLabel || triggerData.componentName}
                                            styleType="outline-outline"
                                            weight="semibold"
                                        />
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent className="max-w-xs text-sm" side="right">
                                    {triggerData.actionDescription}
                                </TooltipContent>
                            </Tooltip>
                        ) : (
                            <div className="shrink-0">
                                <Badge
                                    label={triggerData.actionLabel || triggerData.componentName}
                                    styleType="outline-outline"
                                    weight="semibold"
                                />
                            </div>
                        )}
                    </div>
                )}

                <WorkflowComponentsList
                    filteredComponentNames={taskOnlyComponentNames}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
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

                        {ff_1042 && (
                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() => setShowWorkflowShareDialog(true)}
                            >
                                <Share2Icon /> Share
                            </DropdownMenuItem>
                        )}

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
                    projectId={project.id}
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
