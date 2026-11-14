import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowShareDialog} from '@/pages/automation/project/components/WorkflowShareDialog';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import WorkflowListRowDropdownMenu from '@/shared/components/workflow/WorkflowListRowDropdownMenu';
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
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {isJavaCodeWorkflow} from '@/shared/util/codeWorkflowLanguage-utils';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, Share2Icon} from 'lucide-react';
import {MouseEvent, ReactNode, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

const WorkflowRowLink = ({
    children,
    disabled,
    projectWorkflowId,
    to,
    workflowLabel,
}: {
    children: ReactNode;
    disabled: boolean;
    projectWorkflowId?: number;
    to: string;
    workflowLabel?: string;
}) => {
    if (disabled) {
        return <div className="flex min-w-0 flex-1 items-center gap-2">{children}</div>;
    }

    return (
        <Link
            aria-label={`Link to workflow ${workflowLabel}`}
            className="flex min-w-0 flex-1 items-center gap-2"
            data-testid={`${projectWorkflowId}-link`}
            to={to}
        >
            {children}
        </Link>
    );
};

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

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            setShowDeleteDialog(false);
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            void queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Workflow duplicated successfully.');
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });

            void queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(project.id!),
            });

            void queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            void queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditDialog(false);
        },
    });

    const javaCodeWorkflow = isJavaCodeWorkflow(project.codeWorkflow, project.codeWorkflowLanguage);

    const workflowUrl = `/automation/projects/${project.id}/project-workflows/${workflow.projectWorkflowId}?${searchParams}`;

    const handleWorkflowListItemClick = (event: MouseEvent<HTMLLIElement>) => {
        if (javaCodeWorkflow) {
            return;
        }

        const target = event.target as HTMLElement;

        // Clicks inside portaled dialogs and menus bubble through React but are not DOM descendants of the row.
        if (!event.currentTarget.contains(target)) {
            return;
        }

        if (target.closest('a, button:not([data-slot="tooltip-trigger"]), [role="menu"], [role="menuitem"]')) {
            return;
        }

        void navigate(workflowUrl);
    };

    return (
        <li
            className={twMerge(
                'group/row flex items-center justify-between rounded-md py-1 pl-3 hover:bg-surface-neutral-primary-hover',
                !javaCodeWorkflow && 'cursor-pointer'
            )}
            key={workflow.id}
            onClick={handleWorkflowListItemClick}
            role="presentation"
        >
            <WorkflowRowLink
                disabled={javaCodeWorkflow}
                projectWorkflowId={workflow.projectWorkflowId}
                to={workflowUrl}
                workflowLabel={workflow.label}
            >
                <div className="flex w-80 min-w-0 shrink-0 flex-col gap-1 pr-1 text-sm font-semibold">
                    <div className="flex min-w-0 items-center gap-2">
                        <Tooltip>
                            <TooltipTrigger className="line-clamp-1 min-w-0 truncate text-start underline-offset-4 group-hover/row:text-content-brand-primary group-hover/row:underline">
                                {workflow.label}
                            </TooltipTrigger>

                            <TooltipContent align="start" className="max-w-md break-all">
                                {workflow.label}
                            </TooltipContent>
                        </Tooltip>
                    </div>

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
            </WorkflowRowLink>

            <div className="flex justify-end gap-x-6 [&>:last-child]:-ml-3">
                <Tooltip>
                    <TooltipTrigger className="hidden items-center text-sm text-content-neutral-secondary xl:block">
                        <span className="text-xs">
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                {project.codeWorkflow ? (
                    <div aria-hidden className="size-9 shrink-0" />
                ) : (
                    <WorkflowListRowDropdownMenu
                        exportUrl={`/api/automation/internal/workflows/${workflow.id}/export`}
                        onDeleteClick={() => setShowDeleteDialog(true)}
                        onEditClick={() => setShowEditDialog(true)}
                        workflowLabel={workflow.label}
                    >
                        {project && workflow && (
                            <DropdownMenuItem
                                icon={<CopyIcon />}
                                label="Duplicate"
                                onClick={() =>
                                    duplicateWorkflowMutation.mutate({
                                        id: project.id!,
                                        workflowId: workflow.id!,
                                    })
                                }
                            />
                        )}

                        <DropdownMenuItem
                            icon={<Share2Icon />}
                            label="Share"
                            onClick={() => setShowWorkflowShareDialog(true)}
                        />

                        {templatesSubmissionForm && (
                            <DropdownMenuItem
                                icon={<Share2Icon />}
                                label="Share with Community"
                                onClick={() => window.open(templatesSubmissionForm, '_blank')}
                            />
                        )}
                    </WorkflowListRowDropdownMenu>
                )}
            </div>

            {showDeleteDialog && (
                <DeleteWorkflowAlertDialog
                    isPending={deleteWorkflowMutation.isPending}
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
