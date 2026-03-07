import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import {McpProjectWorkflow, McpServer, Tag, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import McpServerListItem from 'pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';
import {useMemo} from 'react';

import McpServerToolsContent from './McpServerToolsContent';
import useMcpServerList from './hooks/useMcpServerList';

interface McpServerListProps {
    mcpServers: McpServer[];
    tags?: Tag[];
}

const McpServerListItemWithWorkflows = ({mcpServer, tags}: {mcpServer: McpServer; tags?: Tag[]}) => {
    const {data: mcpProjectsData} = useMcpProjectsByServerIdQuery({
        mcpServerId: mcpServer.id!,
    });

    const mcpProjects = mcpProjectsData?.mcpProjectsByServerId?.filter((project) => project !== null) || [];

    const mcpProjectWorkflows: McpProjectWorkflow[] = mcpProjects
        .flatMap((project) => project?.mcpProjectWorkflows || [])
        .filter((workflow): workflow is McpProjectWorkflow => workflow !== null);

    return <McpServerListItem mcpProjectWorkflows={mcpProjectWorkflows} mcpServer={mcpServer} tags={tags} />;
};

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    const workflowReadOnlyValue = useMemo(() => ({useGetComponentDefinitionsQuery}), []);

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider value={workflowReadOnlyValue}>
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
                                        <McpServerConfiguration
                                            mcpServerUrl={mcpServer.url}
                                            onRefresh={handleRefresh}
                                        />
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
