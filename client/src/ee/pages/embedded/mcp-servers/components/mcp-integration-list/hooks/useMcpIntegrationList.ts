import {McpIntegration, useMcpIntegrationsByServerIdQuery} from '@/shared/middleware/graphql';

const useMcpIntegrationList = (mcpServerId: string) => {
    const {data: mcpIntegrationsData, isLoading} = useMcpIntegrationsByServerIdQuery({
        mcpServerId: mcpServerId,
    });

    const mcpIntegrations =
        mcpIntegrationsData?.mcpIntegrationsByServerId?.filter(
            (integration): integration is McpIntegration => integration !== null
        ) || [];

    return {isLoading, mcpIntegrations};
};

export default useMcpIntegrationList;
