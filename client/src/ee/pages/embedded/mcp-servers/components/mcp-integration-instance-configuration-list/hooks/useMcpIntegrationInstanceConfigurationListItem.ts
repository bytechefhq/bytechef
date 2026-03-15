import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {McpIntegrationInstanceConfiguration} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

const useMcpIntegrationInstanceConfigurationListItem = (
    mcpIntegrationInstanceConfiguration: McpIntegrationInstanceConfiguration
) => {
    const [showEditWorkflowsDialog, setShowEditWorkflowsDialog] = useState(false);
    const [showUpdateIntegrationVersionDialog, setShowUpdateIntegrationVersionDialog] = useState(false);

    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        +mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationId!
    );

    const queryClient = useQueryClient();

    const mcpConfigurationWorkflowIds = useMemo(
        () =>
            new Set(
                mcpIntegrationInstanceConfiguration.mcpIntegrationInstanceConfigurationWorkflows
                    ?.filter((workflow) => workflow != null)
                    .map((workflow) => String(workflow.integrationInstanceConfigurationWorkflowId)) || []
            ),
        [mcpIntegrationInstanceConfiguration.mcpIntegrationInstanceConfigurationWorkflows]
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
                queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
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

export default useMcpIntegrationInstanceConfigurationListItem;
