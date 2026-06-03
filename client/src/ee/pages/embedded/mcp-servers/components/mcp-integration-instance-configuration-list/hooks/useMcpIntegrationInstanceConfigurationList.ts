import {
    McpIntegrationInstanceConfigurationsByServerIdQuery,
    useMcpIntegrationInstanceConfigurationsByServerIdQuery,
} from '@/shared/middleware/graphql';

export type McpIntegrationInstanceConfigurationItemType = NonNullable<
    NonNullable<
        McpIntegrationInstanceConfigurationsByServerIdQuery['mcpIntegrationInstanceConfigurationsByServerId']
    >[number]
>;

export type McpIntegrationInstanceConfigurationWorkflowItemType = NonNullable<
    NonNullable<McpIntegrationInstanceConfigurationItemType['mcpIntegrationInstanceConfigurationWorkflows']>[number]
>;

const useMcpIntegrationInstanceConfigurationList = (mcpServerId: string) => {
    const {data: mcpIntegrationInstanceConfigurationsData, isLoading} =
        useMcpIntegrationInstanceConfigurationsByServerIdQuery({
            mcpServerId: mcpServerId,
        });

    const mcpIntegrationInstanceConfigurations =
        mcpIntegrationInstanceConfigurationsData?.mcpIntegrationInstanceConfigurationsByServerId?.filter(
            (integration): integration is NonNullable<typeof integration> => integration !== null
        ) || [];

    return {isLoading, mcpIntegrationInstanceConfigurations};
};

export default useMcpIntegrationInstanceConfigurationList;
