import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {McpIntegration} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

const useMcpIntegrationListItem = (mcpIntegration: McpIntegration) => {
    const [showEditWorkflowsDialog, setShowEditWorkflowsDialog] = useState(false);
    const [showUpdateIntegrationVersionDialog, setShowUpdateIntegrationVersionDialog] = useState(false);

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        +mcpIntegration.integrationInstanceConfigurationId!
    );

    const queryClient = useQueryClient();

    const mcpConfigurationWorkflowIds = useMemo(
        () =>
            new Set(
                mcpIntegration.mcpIntegrationWorkflows
                    ?.filter((workflow) => workflow != null)
                    .map((workflow) => String(workflow.integrationInstanceConfigurationWorkflowId)) || []
            ),
        [mcpIntegration.mcpIntegrationWorkflows]
    );

    const mcpWorkflowUuids = useMemo(
        () =>
            integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows
                ?.filter(
                    (configurationWorkflow) =>
                        configurationWorkflow.workflowUuid != null &&
                        mcpConfigurationWorkflowIds.has(String(configurationWorkflow.id))
                )
                .map((configurationWorkflow) => configurationWorkflow.workflowUuid!) || [],
        [mcpConfigurationWorkflowIds, integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows]
    );

    const handleOnIntegrationInstanceConfigurationDialogClose = () => {
        queryClient
            .invalidateQueries({
                queryKey: ['mcpIntegrationsByServerId'],
            })
            .then(() => setShowUpdateIntegrationVersionDialog(false));
    };

    return {
        handleOnIntegrationInstanceConfigurationDialogClose,
        integrationInstanceConfiguration,
        mcpWorkflowUuids,
        setShowEditWorkflowsDialog,
        setShowUpdateIntegrationVersionDialog,
        showEditWorkflowsDialog,
        showUpdateIntegrationVersionDialog,
    };
};

export default useMcpIntegrationListItem;
