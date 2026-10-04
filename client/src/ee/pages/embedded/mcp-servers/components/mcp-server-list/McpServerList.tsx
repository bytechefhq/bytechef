import McpIntegrationInstanceConfigurationWorkflowDialog from '@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog';
import McpComponentDialog from '@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerCollapsibleItem from '@/shared/components/mcp-server/McpServerCollapsibleItem';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import {McpServer, Tag, useMcpIntegrationInstanceConfigurationsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';

import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import McpServerListItem from './McpServerListItem';
import McpServerToolsContent from './McpServerToolsContent';
import useMcpServerList from './hooks/useMcpServerList';

interface McpServerListProps {
    mcpServers: McpServer[];
    tags?: Tag[];
}

const McpServerListItemWithWorkflows = ({mcpServer, tags}: {mcpServer: McpServer; tags?: Tag[]}) => {
    const {data: mcpIntegrationInstanceConfigurationsData} = useMcpIntegrationInstanceConfigurationsByServerIdQuery({
        mcpServerId: mcpServer.id!,
    });

    const mcpIntegrationInstanceConfigurations =
        mcpIntegrationInstanceConfigurationsData?.mcpIntegrationInstanceConfigurationsByServerId?.filter(
            (integration) => integration !== null
        ) || [];

    const mcpIntegrationInstanceConfigurationWorkflows: McpIntegrationInstanceConfigurationWorkflowItemType[] =
        mcpIntegrationInstanceConfigurations
            .flatMap((integration) => integration?.mcpIntegrationInstanceConfigurationWorkflows || [])
            .filter((workflow): workflow is NonNullable<typeof workflow> => workflow !== null);

    return (
        <McpServerListItem
            mcpIntegrationInstanceConfigurationWorkflows={mcpIntegrationInstanceConfigurationWorkflows}
            mcpServer={mcpServer}
            tags={tags}
        />
    );
};

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider
                value={{
                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                }}
            >
                {sortedMcpServers.map((mcpServer) => {
                    const handleRefresh = createHandleRefresh(mcpServer.id!);

                    return (
                        <McpServerCollapsibleItem
                            connectContent={
                                <div className="flex-1 space-y-4">
                                    <h2 className="font-semibold text-foreground">Server URL</h2>

                                    <McpServerConfigurationCode
                                        codeSnippet={mcpServer.url || ''}
                                        onRefresh={handleRefresh}
                                    />
                                </div>
                            }
                            header={<McpServerListItemWithWorkflows mcpServer={mcpServer} tags={tags} />}
                            key={mcpServer.id}
                            mcpComponentDialog={McpComponentDialog}
                            mcpServer={mcpServer}
                            toolsContent={<McpServerToolsContent mcpServer={mcpServer} />}
                            workflowDialog={McpIntegrationInstanceConfigurationWorkflowDialog}
                        />
                    );
                })}
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default McpServerList;
