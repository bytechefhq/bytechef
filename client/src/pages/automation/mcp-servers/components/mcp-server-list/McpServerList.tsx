import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import {McpServer, Tag, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import McpServerListItem from 'pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';
import {useMemo, useState} from 'react';

import {McpProjectWorkflowItemType} from '../mcp-project-workflow-list/hooks/useMcpProjectList';
import McpServerToolsAddButton from './McpServerToolsAddButton';
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

    const mcpProjectWorkflows: McpProjectWorkflowItemType[] = mcpProjects
        .flatMap((project) => project?.mcpProjectWorkflows || [])
        .filter((workflow): workflow is NonNullable<typeof workflow> => workflow !== null);

    return <McpServerListItem mcpProjectWorkflows={mcpProjectWorkflows} mcpServer={mcpServer} tags={tags} />;
};

interface McpServerListItemTabsProps {
    mcpServer: McpServer;
    onRefresh: () => void;
}

const McpServerListItemTabs = ({mcpServer, onRefresh}: McpServerListItemTabsProps) => {
    const [activeTab, setActiveTab] = useState('tools');

    return (
        <Tabs onValueChange={setActiveTab} value={activeTab}>
            <div className="flex items-center justify-between">
                <TabsList>
                    <TabsTrigger value="tools">Tools</TabsTrigger>

                    <TabsTrigger value="connect">Connect</TabsTrigger>
                </TabsList>

                {activeTab === 'tools' && <McpServerToolsAddButton mcpServer={mcpServer} />}
            </div>

            <TabsContent className="pt-2" value="tools">
                <McpServerToolsContent mcpServer={mcpServer} />
            </TabsContent>

            <TabsContent className="max-w-(--breakpoint-lg) pt-3" value="connect">
                <McpServerConfiguration mcpServerUrl={mcpServer.url} onRefresh={onRefresh} />
            </TabsContent>
        </Tabs>
    );
};

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    const workflowReadOnlyValue = useMemo(() => ({useGetComponentDefinitionsQuery}), []);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider value={workflowReadOnlyValue}>
                {sortedMcpServers.map((mcpServer) => {
                    const handleRefresh = createHandleRefresh(mcpServer.id!);

                    return (
                        <Collapsible className="group mb-2 rounded border border-border/50" key={mcpServer.id}>
                            <McpServerListItemWithWorkflows key={mcpServer.id} mcpServer={mcpServer} tags={tags} />

                            <CollapsibleContent className="mx-3 mt-1 mb-3">
                                <McpServerListItemTabs mcpServer={mcpServer} onRefresh={handleRefresh} />
                            </CollapsibleContent>
                        </Collapsible>
                    );
                })}
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default McpServerList;
