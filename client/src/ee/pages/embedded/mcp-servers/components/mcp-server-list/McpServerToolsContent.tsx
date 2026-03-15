import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import useMcpIntegrationInstanceConfigurationList from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpServer} from '@/shared/middleware/graphql';
import {WrenchIcon} from 'lucide-react';

import McpIntegrationInstanceConfigurationWorkflowDialog from '../McpIntegrationInstanceConfigurationWorkflowDialog';
import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentList from '../mcp-component-list/McpComponentList';
import McpIntegrationInstanceConfigurationList from '../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList';

const McpServerToolsContent = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);
    const {isLoading: isIntegrationsLoading, mcpIntegrationInstanceConfigurations} =
        useMcpIntegrationInstanceConfigurationList(mcpServer.id!);

    const hasNoComponents = !componentData?.mcpComponentsByServerId?.length;
    const hasNoIntegrations = !mcpIntegrationInstanceConfigurations?.length;
    const isLoading = isMcpComponentsLoading || isIntegrationsLoading;

    if (!isLoading && hasNoComponents && hasNoIntegrations) {
        return (
            <div className="flex justify-center py-8">
                <EmptyList
                    button={
                        <div className="flex items-center justify-center gap-2">
                            <McpComponentDialog
                                mcpComponent={undefined}
                                mcpServerId={mcpServer.id}
                                triggerNode={<Button label="Add Component" />}
                            />

                            <span className="text-sm text-muted-foreground">or</span>

                            <McpIntegrationInstanceConfigurationWorkflowDialog
                                mcpServer={mcpServer}
                                triggerNode={<Button label="Add Workflows" />}
                            />
                        </div>
                    }
                    icon={<WrenchIcon className="size-24 text-gray-300" />}
                    message="No tools found for this server."
                    title="No Tools"
                />
            </div>
        );
    }

    return (
        <McpActivePopoverProvider>
            <McpComponentList mcpServer={mcpServer} />

            <McpIntegrationInstanceConfigurationList mcpServer={mcpServer} />
        </McpActivePopoverProvider>
    );
};

export default McpServerToolsContent;
