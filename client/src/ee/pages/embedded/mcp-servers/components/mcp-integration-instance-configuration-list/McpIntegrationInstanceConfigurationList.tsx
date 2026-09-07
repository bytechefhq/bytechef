import McpToolListSkeleton from '@/pages/automation/mcp-servers/components/McpToolListSkeleton';
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
        <div className="flex flex-col gap-1.5">
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
