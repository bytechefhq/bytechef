import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import useMcpIntegrationInstanceConfigurationList from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import McpServerToolsPanel from '@/shared/components/mcp-server/McpServerToolsPanel';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpServer} from '@/shared/middleware/graphql';

import McpComponentList from '../mcp-component-list/McpComponentList';
import McpIntegrationInstanceConfigurationList from '../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList';

const McpServerToolsContent = ({
    mcpServer,
    ...toolsContentProps
}: McpServerToolsContentProps & {mcpServer: McpServer}) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);
    const {isLoading: isIntegrationsLoading, mcpIntegrationInstanceConfigurations} =
        useMcpIntegrationInstanceConfigurationList(mcpServer.id!);

    return (
        <McpActivePopoverProvider>
            <McpServerToolsPanel
                {...toolsContentProps}
                componentList={<McpComponentList mcpServer={mcpServer} />}
                isComponentListEmpty={!isMcpComponentsLoading && !componentData?.mcpComponentsByServerId?.length}
                isWorkflowListEmpty={!isIntegrationsLoading && !mcpIntegrationInstanceConfigurations?.length}
                workflowList={<McpIntegrationInstanceConfigurationList mcpServer={mcpServer} />}
            />
        </McpActivePopoverProvider>
    );
};

export default McpServerToolsContent;
