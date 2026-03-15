import {
    McpIntegrationInstanceConfiguration,
    useMcpIntegrationInstanceConfigurationsByServerIdQuery,
} from '@/shared/middleware/graphql';

const useMcpIntegrationInstanceConfigurationList = (mcpServerId: string) => {
    const {data: mcpIntegrationInstanceConfigurationsData, isLoading} =
        useMcpIntegrationInstanceConfigurationsByServerIdQuery({
            mcpServerId: mcpServerId,
        });

    const mcpIntegrationInstanceConfigurations =
        mcpIntegrationInstanceConfigurationsData?.mcpIntegrationInstanceConfigurationsByServerId?.filter(
            (integration): integration is McpIntegrationInstanceConfiguration => integration !== null
        ) || [];

    return {isLoading, mcpIntegrationInstanceConfigurations};
};

export default useMcpIntegrationInstanceConfigurationList;
