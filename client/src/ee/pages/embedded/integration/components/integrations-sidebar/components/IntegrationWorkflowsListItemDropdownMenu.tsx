import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteWorkflowMutation, useUpdateWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';

import '@/shared/styles/dropdownMenu.css';
import {useQueryClient} from '@tanstack/react-query';
import {DownloadIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

interface IntegrationWorkflowsListItemDropdownMenuProps {
    currentWorkflowId: string;
    integration: Integration;
    workflow: Workflow;
}

const IntegrationWorkflowsListItemDropdownMenu = ({
    currentWorkflowId,
    integration,
    workflow,
}: IntegrationWorkflowsListItemDropdownMenuProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const integrationId = integration.id!;

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            const deletedIntegrationWorkflowId = workflow.integrationWorkflowId;

            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            if (deletedIntegrationWorkflowId) {
                queryClient.removeQueries({
                    queryKey: IntegrationWorkflowKeys.integrationWorkflow(integrationId, deletedIntegrationWorkflowId),
                });
            }

            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });
            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});

            if (workflow.id !== currentWorkflowId) {
                return;
            }

            const firstRemainingIntegrationWorkflowId = integration.integrationWorkflowIds?.find(
                (integrationWorkflowId) => integrationWorkflowId !== deletedIntegrationWorkflowId
            );

            if (firstRemainingIntegrationWorkflowId) {
                navigate(
                    `/embedded/integrations/${integrationId}/integration-workflows/${firstRemainingIntegrationWorkflowId}?${searchParams}`
                );
            } else {
                navigate('/embedded/integrations');
            }
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });
            queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            setShowEditWorkflowDialog(false);
        },
    });

    return (
        <WorkflowListItemDropdownMenu
            dialogs={
                showEditWorkflowDialog && (
                    <WorkflowDialog
                        onClose={() => setShowEditWorkflowDialog(false)}
                        onSave={() => {
                            if (workflow.integrationWorkflowId) {
                                queryClient.invalidateQueries({
                                    queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                                        integrationId,
                                        workflow.integrationWorkflowId
                                    ),
                                });
                            }
                        }}
                        updateWorkflowMutation={updateWorkflowMutation}
                        useGetWorkflowQuery={useGetWorkflowQuery}
                        workflowId={workflow.id!}
                    />
                )
            }
            onDelete={() => deleteWorkflowMutation.mutate({id: workflow.id!})}
            onEditClick={() => setShowEditWorkflowDialog(true)}
            workflowLabel={workflow.label}
        >
            <DropdownMenuItem
                className="dropdown-menu-item"
                onClick={() => (window.location.href = `/api/embedded/internal/workflows/${workflow.id}/export`)}
            >
                <DownloadIcon /> Export
            </DropdownMenuItem>
        </WorkflowListItemDropdownMenu>
    );
};

export default IntegrationWorkflowsListItemDropdownMenu;
