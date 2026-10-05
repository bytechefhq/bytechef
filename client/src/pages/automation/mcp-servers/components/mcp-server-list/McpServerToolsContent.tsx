import useMcpComponentList from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import useMcpProjectList from '@/pages/automation/mcp-servers/components/mcp-project-workflow-list/hooks/useMcpProjectList';
import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import McpServerToolsPanel from '@/shared/components/mcp-server/McpServerToolsPanel';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';

import McpComponentList from '../mcp-component-list/McpComponentList';
import McpProjectList from '../mcp-project-workflow-list/McpProjectList';

const McpServerToolsContent = ({mcpServer, ...toolsContentProps}: McpServerToolsContentProps) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);
    const {isLoading: isProjectsLoading, mcpProjects} = useMcpProjectList(mcpServer.id!);

    return (
        <McpActivePopoverProvider>
            <McpServerToolsPanel
                {...toolsContentProps}
                componentList={<McpComponentList mcpServer={mcpServer} />}
                isComponentListEmpty={!isMcpComponentsLoading && !componentData?.mcpComponentsByServerId?.length}
                isWorkflowListEmpty={!isProjectsLoading && !mcpProjects?.length}
                workflowList={<McpProjectList mcpServer={mcpServer} />}
            />
        </McpActivePopoverProvider>
    );
};

export default McpServerToolsContent;
