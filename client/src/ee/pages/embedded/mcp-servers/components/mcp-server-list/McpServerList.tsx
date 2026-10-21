import McpIntegrationInstanceConfigurationWorkflowDialog from '@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog';
import McpComponentDialog from '@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerCollapsibleItem from '@/shared/components/mcp-server/McpServerCollapsibleItem';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
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

const HIDDEN_MCP_SERVER_URL_HINT = 'Only members who can edit this server can see its URL.';

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const canCreateConfiguration = useHasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_CREATE');
    const canEditMcpServer = useHasEmbeddedScope('MCP_EDIT');
    const isTenantAdmin = useIsTenantAdmin();

    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider
                value={{
                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                }}
            >
                {sortedMcpServers.map((mcpServer) => {
                    return (
                        <McpServerCollapsibleItem
                            canAddWorkflows={canEditMcpServer && canCreateConfiguration}
                            canEdit={canEditMcpServer}
                            connectContent={
                                <div className="flex-1 space-y-4">
                                    <h2 className="font-semibold text-foreground">Server URL</h2>

                                    {canEditMcpServer && mcpServer.url ? (
                                        <McpServerConfigurationCode
                                            codeSnippet={mcpServer.url}
                                            onRefresh={isTenantAdmin ? createHandleRefresh(mcpServer.id!) : undefined}
                                        />
                                    ) : (
                                        <div className="space-y-1">
                                            <span className="text-sm text-muted-foreground italic">Hidden</span>

                                            <p className="text-xs text-muted-foreground">
                                                {HIDDEN_MCP_SERVER_URL_HINT}
                                            </p>
                                        </div>
                                    )}
                                </div>
                            }
                            header={<McpServerListItemWithWorkflows mcpServer={mcpServer} tags={tags} />}
                            key={mcpServer.id}
                            mcpComponentDialog={McpComponentDialog}
                            mcpServer={mcpServer}
                            toolsContent={McpServerToolsContent}
                            workflowDialog={McpIntegrationInstanceConfigurationWorkflowDialog}
                        />
                    );
                })}
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default McpServerList;
