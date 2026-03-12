import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import {McpIntegrationWorkflow, McpServer, Tag, useMcpIntegrationsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';

import McpServerListItem from './McpServerListItem';
import McpServerToolsContent from './McpServerToolsContent';
import useMcpServerList from './hooks/useMcpServerList';

interface McpServerListProps {
    mcpServers: McpServer[];
    tags?: Tag[];
}

const McpServerListItemWithWorkflows = ({mcpServer, tags}: {mcpServer: McpServer; tags?: Tag[]}) => {
    const {data: mcpIntegrationsData} = useMcpIntegrationsByServerIdQuery({
        mcpServerId: mcpServer.id!,
    });

    const mcpIntegrations =
        mcpIntegrationsData?.mcpIntegrationsByServerId?.filter((integration) => integration !== null) || [];

    const mcpIntegrationWorkflows: McpIntegrationWorkflow[] = mcpIntegrations
        .flatMap((integration) => integration?.mcpIntegrationWorkflows || [])
        .filter((workflow): workflow is McpIntegrationWorkflow => workflow !== null);

    return <McpServerListItem mcpIntegrationWorkflows={mcpIntegrationWorkflows} mcpServer={mcpServer} tags={tags} />;
};

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider
                value={{
                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                }}
            >
                {sortedMcpServers.map((mcpServer) => {
                    const handleRefresh = createHandleRefresh(mcpServer.id!);

                    return (
                        <Collapsible className="group" key={mcpServer.id}>
                            <McpServerListItemWithWorkflows key={mcpServer.id} mcpServer={mcpServer} tags={tags} />

                            <CollapsibleContent className="mt-1">
                                <Tabs defaultValue="tools">
                                    <TabsList>
                                        <TabsTrigger value="tools">Tools</TabsTrigger>

                                        <TabsTrigger value="connect">Connect</TabsTrigger>
                                    </TabsList>

                                    <TabsContent value="tools">
                                        <McpServerToolsContent mcpServer={mcpServer} />
                                    </TabsContent>

                                    <TabsContent className="max-w-screen-lg py-3" value="connect">
                                        <div className="flex-1 space-y-4">
                                            <h2 className="font-semibold text-foreground">Server URL</h2>

                                            {mcpServer.url && (
                                                <McpServerConfigurationCode
                                                    codeSnippet={mcpServer.url}
                                                    onRefresh={handleRefresh}
                                                />
                                            )}
                                        </div>
                                    </TabsContent>
                                </Tabs>
                            </CollapsibleContent>
                        </Collapsible>
                    );
                })}
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default McpServerList;
