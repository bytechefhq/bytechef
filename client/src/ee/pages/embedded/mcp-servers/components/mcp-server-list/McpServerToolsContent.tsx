import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import McpServerToolsPanel from '@/shared/components/mcp-server/McpServerToolsPanel';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';

import McpAutomationProjectConfigurationList from '../mcp-automation-project-configuration-list/McpAutomationProjectConfigurationList';
import McpComponentList from '../mcp-component-list/McpComponentList';
import McpIntegrationInstanceConfigurationList from '../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList';

const McpServerToolsContent = ({mcpServer, ...toolsContentProps}: McpServerToolsContentProps) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);

    return (
        <McpActivePopoverProvider>
            <McpServerToolsPanel
                {...toolsContentProps}
                componentList={<McpComponentList mcpServer={mcpServer} />}
                isComponentListEmpty={
                    !isMcpComponentsLoading && !componentData?.embeddedMcpComponentsByServerId?.length
                }
                isWorkflowListEmpty={false}
                workflowList={
                    <>
                        <McpIntegrationInstanceConfigurationList mcpServer={mcpServer} />

                        <McpAutomationProjectConfigurationList mcpServer={mcpServer} />
                    </>
                }
            />
        </McpActivePopoverProvider>
    );
};

export default McpServerToolsContent;
