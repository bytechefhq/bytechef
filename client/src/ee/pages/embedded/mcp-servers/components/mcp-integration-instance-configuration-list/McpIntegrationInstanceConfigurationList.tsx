import McpToolListSkeleton from '@/pages/platform/mcp-servers/components/McpToolListSkeleton';
import {McpServer} from '@/shared/middleware/graphql';

import McpIntegrationInstanceConfigurationListItem from './McpIntegrationInstanceConfigurationListItem';
import useMcpIntegrationInstanceConfigurationList from './hooks/useMcpIntegrationInstanceConfigurationList';

interface McpIntegrationInstanceConfigurationListProps {
    mcpServer: McpServer;
}

const McpIntegrationInstanceConfigurationList = ({mcpServer}: McpIntegrationInstanceConfigurationListProps) => {
    const {isLoading, mcpIntegrationInstanceConfigurations} = useMcpIntegrationInstanceConfigurationList(mcpServer.id!);

    if (isLoading) {
        return <McpToolListSkeleton />;
    }

    if (!mcpIntegrationInstanceConfigurations?.length) {
        return <></>;
    }

    return (
        <div className="divide-y divide-stroke-neutral-primary">
            {mcpIntegrationInstanceConfigurations.map((mcpIntegrationInstanceConfiguration) => (
                <McpIntegrationInstanceConfigurationListItem
                    key={mcpIntegrationInstanceConfiguration.id}
                    mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
                />
            ))}
        </div>
    );
};

export default McpIntegrationInstanceConfigurationList;
