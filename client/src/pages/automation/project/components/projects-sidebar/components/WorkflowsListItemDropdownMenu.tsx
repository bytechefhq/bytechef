import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
import {WorkflowShareDialog} from '@/pages/automation/project/components/WorkflowShareDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
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
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, DownloadIcon, Share2Icon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

interface WorkflowsListItemDropdownMenuProps {
    currentWorkflowId: string;
    project: Project;
    workflow: Workflow;
}

const WorkflowsListItemDropdownMenu = ({currentWorkflowId, project, workflow}: WorkflowsListItemDropdownMenuProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const [showWorkflowShareDialog, setShowWorkflowShareDialog] = useState(false);

    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    const ff_2939 = useFeatureFlagsStore()('ff-2939');

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const projectId = project.id!;

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            const deletedProjectWorkflowId = workflow.projectWorkflowId;

            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            if (deletedProjectWorkflowId) {
                queryClient.removeQueries({
                    queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, deletedProjectWorkflowId),
                });
            }

            queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({exact: true, queryKey: ProjectKeys.project(projectId)});

            if (workflow.id !== currentWorkflowId) {
                return;
            }

            const firstRemainingProjectWorkflowId = project.projectWorkflowIds?.find(
                (projectWorkflowId) => projectWorkflowId !== deletedProjectWorkflowId
            );

            if (firstRemainingProjectWorkflowId) {
                navigate(
                    `/automation/projects/${projectId}/project-workflows/${firstRemainingProjectWorkflowId}?${searchParams}`
                );
            } else {
                navigate('/automation/projects');
            }
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Workflow duplicated successfully.');
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });
            queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            setShowEditWorkflowDialog(false);
        },
    });

    return (
        <WorkflowListItemDropdownMenu
            dialogs={
                <>
                    {showEditWorkflowDialog && (
                        <WorkflowDialog
                            onClose={() => setShowEditWorkflowDialog(false)}
                            onSave={() => {
                                if (workflow.projectWorkflowId) {
                                    queryClient.invalidateQueries({
                                        queryKey: ProjectWorkflowKeys.projectWorkflow(
                                            projectId,
                                            workflow.projectWorkflowId
                                        ),
                                    });
                                }
                            }}
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
                </>
            }
            onDelete={() => deleteWorkflowMutation.mutate({id: workflow.id!})}
            onEditClick={() => setShowEditWorkflowDialog(true)}
            workflowLabel={workflow.label}
        >
            <DropdownMenuItem
                className="dropdown-menu-item"
                onClick={() =>
                    duplicateWorkflowMutation.mutate({
                        id: projectId,
                        workflowId: workflow.id!,
                    })
                }
            >
                <CopyIcon /> Duplicate
            </DropdownMenuItem>

            <DropdownMenuItem className="dropdown-menu-item" onClick={() => setShowWorkflowShareDialog(true)}>
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
                onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflow.id}/export`)}
            >
                <DownloadIcon /> Export
            </DropdownMenuItem>
        </WorkflowListItemDropdownMenu>
    );
};

export default WorkflowsListItemDropdownMenu;
