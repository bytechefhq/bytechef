import McpProjectWorkflowDialog from '@/pages/automation/mcp-servers/components/McpProjectWorkflowDialog';
import McpComponentDialog from '@/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import McpServerListItem from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import McpServerCollapsibleItem from '@/shared/components/mcp-server/McpServerCollapsibleItem';
import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import {McpServer, Tag, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useMemo} from 'react';

import {McpProjectWorkflowItemType} from '../mcp-project-workflow-list/hooks/useMcpProjectList';
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

const McpServerList = ({mcpServers, tags}: McpServerListProps) => {
    const {createHandleRefresh, sortedMcpServers} = useMcpServerList(mcpServers);

    const workflowReadOnlyValue = useMemo(() => ({useGetComponentDefinitionsQuery}), []);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            <WorkflowReadOnlyProvider value={workflowReadOnlyValue}>
                {sortedMcpServers.map((mcpServer) => {
                    const handleRefresh = createHandleRefresh(mcpServer.id!);

                    return (
                        <McpServerCollapsibleItem
                            connectContent={
                                <McpServerConfiguration mcpServerUrl={mcpServer.url} onRefresh={handleRefresh} />
                            }
                            header={<McpServerListItemWithWorkflows mcpServer={mcpServer} tags={tags} />}
                            key={mcpServer.id}
                            mcpComponentDialog={McpComponentDialog}
                            mcpServer={mcpServer}
                            toolsContent={McpServerToolsContent}
                            workflowDialog={McpProjectWorkflowDialog}
                        />
                    );
                })}
            </WorkflowReadOnlyProvider>
        </div>
    );
};

export default McpServerList;
