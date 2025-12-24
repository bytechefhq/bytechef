import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import {
    McpProjectWorkflow,
    McpServer,
    Tag,
    useMcpProjectsByServerIdQuery,
    useUpdateMcpServerUrlMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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
    const updateMcpServerUrlMutation = useUpdateMcpServerUrlMutation({});

    const queryClient = useQueryClient();

    const sortedMcpServers = [...mcpServers].sort((a, b) => a.name.localeCompare(b.name));

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {sortedMcpServers.map((mcpServer) => {
                const handleRefresh = () => {
                    updateMcpServerUrlMutation.mutate(
                        {
                            id: mcpServer.id,
                        },
                        {
                            onSuccess: () => {
                                queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                            },
                        }
                    );
                };

                return (
                    <Collapsible className="group" key={mcpServer.id}>
                        <McpServerListItemWithWorkflows key={mcpServer.id} mcpServer={mcpServer} tags={tags} />

                        <CollapsibleContent className="mt-1">
                            <Tabs defaultValue="components">
                                <TabsList>
                                    <TabsTrigger value="components">Components</TabsTrigger>

                                    <TabsTrigger value="workflows">Workflows</TabsTrigger>

                                    <TabsTrigger value="connect">Connect</TabsTrigger>
                                </TabsList>

                                <TabsContent value="components">
                                    <McpComponentList mcpServer={mcpServer} />
                                </TabsContent>

                                <TabsContent value="workflows">
                                    <McpProjectList mcpServer={mcpServer} />
                                </TabsContent>

                                <TabsContent className="max-w-screen-lg py-5" value="connect">
                                    <McpServerConfiguration mcpServerUrl={mcpServer.url} onRefresh={handleRefresh} />
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
