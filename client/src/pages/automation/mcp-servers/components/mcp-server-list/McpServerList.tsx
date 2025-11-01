import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {McpProjectWorkflow, McpServer, Tag, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import McpServerListItem from 'pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';

import McpComponentList from '../mcp-component-list/McpComponentList';
import McpProjectList from '../mcp-project-workflow-list/McpProjectList';

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
    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {mcpServers.map((mcpServer) => {
                return (
                    <Collapsible className="group" key={mcpServer.id}>
                        <McpServerListItemWithWorkflows key={mcpServer.id} mcpServer={mcpServer} tags={tags} />

                        <CollapsibleContent>
                            <Tabs className="w-full" defaultValue="components">
                                <TabsList className="grid w-full grid-cols-2">
                                    <TabsTrigger value="components">Components</TabsTrigger>

                                    <TabsTrigger value="workflows">Workflows</TabsTrigger>
                                </TabsList>

                                <TabsContent value="components">
                                    <McpComponentList mcpServer={mcpServer} />
                                </TabsContent>

                                <TabsContent value="workflows">
                                    <McpProjectList mcpServer={mcpServer} />
                                </TabsContent>
                            </Tabs>
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};

export default McpServerList;
