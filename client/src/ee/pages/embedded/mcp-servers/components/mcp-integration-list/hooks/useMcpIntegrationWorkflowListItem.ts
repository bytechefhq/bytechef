import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import {McpIntegrationWorkflow, useDeleteMcpIntegrationWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

export default function useMcpIntegrationWorkflowListItem(mcpIntegrationWorkflow: McpIntegrationWorkflow) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const queryClient = useQueryClient();

    const integrationInstanceConfigurationId =
        mcpIntegrationWorkflow.integrationInstanceConfigurationWorkflow?.integrationInstanceConfigurationId;

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        integrationInstanceConfigurationId ? +integrationInstanceConfigurationId : 0,
        !!integrationInstanceConfigurationId
    );

    const integrationInstanceConfigurationWorkflow = useMemo(
        () =>
            integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows?.find(
                (configurationWorkflow) =>
                    configurationWorkflow.id === +mcpIntegrationWorkflow.integrationInstanceConfigurationWorkflowId
            ),
        [
            integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows,
            mcpIntegrationWorkflow.integrationInstanceConfigurationWorkflowId,
        ]
    );

    const {data: workflow} = useGetWorkflowQuery(
        integrationInstanceConfigurationWorkflow?.workflowId ?? '',
        !!integrationInstanceConfigurationWorkflow?.workflowId
    );

    const deleteMcpIntegrationWorkflowMutation = useDeleteMcpIntegrationWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpIntegrationsByServerId'],
            });

            setShowDeleteDialog(false);
        },
    });

    const handleCloseEditDialog = () => {
        setShowEditWorkflowDialog(false);

        queryClient.invalidateQueries({
            queryKey: ['mcpIntegrationsByServerId'],
        });
    };

    const handleConfirmDelete = () => {
        deleteMcpIntegrationWorkflowMutation.mutate({
            id: mcpIntegrationWorkflow.id,
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
