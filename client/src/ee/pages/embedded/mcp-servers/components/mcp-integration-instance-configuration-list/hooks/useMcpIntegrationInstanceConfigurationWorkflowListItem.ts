import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import {
    McpIntegrationInstanceConfigurationWorkflow,
    useDeleteMcpIntegrationInstanceConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

export default function useMcpIntegrationInstanceConfigurationWorkflowListItem(
    mcpIntegrationInstanceConfigurationWorkflow: McpIntegrationInstanceConfigurationWorkflow
) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const queryClient = useQueryClient();

    const integrationInstanceConfigurationId =
        mcpIntegrationInstanceConfigurationWorkflow.integrationInstanceConfigurationWorkflow
            ?.integrationInstanceConfigurationId;

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        integrationInstanceConfigurationId ? +integrationInstanceConfigurationId : 0,
        !!integrationInstanceConfigurationId
    );

    const integrationInstanceConfigurationWorkflow = useMemo(
        () =>
            integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows?.find(
                (configurationWorkflow) =>
                    configurationWorkflow.id ===
                    +mcpIntegrationInstanceConfigurationWorkflow.integrationInstanceConfigurationWorkflowId
            ),
        [
            integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows,
            mcpIntegrationInstanceConfigurationWorkflow.integrationInstanceConfigurationWorkflowId,
        ]
    );

    const {data: workflow} = useGetWorkflowQuery(
        integrationInstanceConfigurationWorkflow?.workflowId ?? '',
        !!integrationInstanceConfigurationWorkflow?.workflowId
    );

    const deleteMcpIntegrationInstanceConfigurationWorkflowMutation =
        useDeleteMcpIntegrationInstanceConfigurationWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
                });

                setShowDeleteDialog(false);
            },
        });

    const handleCloseEditDialog = () => {
        setShowEditWorkflowDialog(false);

        queryClient.invalidateQueries({
            queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
        });
    };

    const handleConfirmDelete = () => {
        deleteMcpIntegrationInstanceConfigurationWorkflowMutation.mutate({
            id: mcpIntegrationInstanceConfigurationWorkflow.id,
        });
    };

    return {
        handleCloseEditDialog,
        handleConfirmDelete,
        integrationInstanceConfigurationWorkflow,
        setShowDeleteDialog,
        setShowEditWorkflowDialog,
        showDeleteDialog,
        showEditWorkflowDialog,
        workflow,
    };
}
