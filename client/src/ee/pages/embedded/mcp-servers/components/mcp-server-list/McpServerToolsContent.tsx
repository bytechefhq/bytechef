import Button from '@/components/Button/Button';
import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import useMcpIntegrationInstanceConfigurationList from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import McpServerToolsPanel from '@/shared/components/mcp-server/McpServerToolsPanel';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';

import McpAutomationProjectConfigurationDialog from '../mcp-automation-project-configuration-list/McpAutomationProjectConfigurationDialog';
import McpAutomationProjectConfigurationList from '../mcp-automation-project-configuration-list/McpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationList from '../mcp-automation-project-configuration-list/hooks/useMcpAutomationProjectConfigurationList';
import McpComponentList from '../mcp-component-list/McpComponentList';
import McpIntegrationInstanceConfigurationList from '../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList';

const McpServerToolsContent = ({mcpServer, ...toolsContentProps}: McpServerToolsContentProps) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);
    const {isLoading: isIntegrationsLoading, mcpIntegrationInstanceConfigurations} =
        useMcpIntegrationInstanceConfigurationList(mcpServer.id!);
    const {isLoading: isAutomationsLoading, mcpAutomationProjectConfigurations} =
        useMcpAutomationProjectConfigurationList(mcpServer.id!);

    const isWorkflowListEmpty =
        !isIntegrationsLoading &&
        !isAutomationsLoading &&
        !mcpIntegrationInstanceConfigurations?.length &&
        !mcpAutomationProjectConfigurations.length;

    return (
        <McpActivePopoverProvider>
            <McpServerToolsPanel
                {...toolsContentProps}
                componentList={<McpComponentList mcpServer={mcpServer} />}
                isComponentListEmpty={
                    !isMcpComponentsLoading && !componentData?.embeddedMcpComponentsByServerId?.length
                }
                isWorkflowListEmpty={isWorkflowListEmpty}
                workflowEmptyStateAction={
                    toolsContentProps.canEdit !== false && (
                        <McpAutomationProjectConfigurationDialog
                            mcpServer={mcpServer}
                            triggerNode={<Button label="Add Automations" variant="secondary" />}
                        />
                    )
                }
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
