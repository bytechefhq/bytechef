import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import useMcpComponentList from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import useMcpProjectList from '@/pages/automation/mcp-servers/components/mcp-project-workflow-list/hooks/useMcpProjectList';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpServer} from '@/shared/middleware/graphql';
import {WrenchIcon} from 'lucide-react';

import McpProjectWorkflowDialog from '../McpProjectWorkflowDialog';
import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentList from '../mcp-component-list/McpComponentList';
import McpProjectList from '../mcp-project-workflow-list/McpProjectList';

const McpServerToolsContent = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data: componentData, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);
    const {isLoading: isProjectsLoading, mcpProjects} = useMcpProjectList(mcpServer.id!);

    const hasNoComponents = !componentData?.mcpComponentsByServerId?.length;
    const hasNoProjects = !mcpProjects?.length;
    const isLoading = isMcpComponentsLoading || isProjectsLoading;

    if (!isLoading && hasNoComponents && hasNoProjects) {
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

                            <McpProjectWorkflowDialog
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

            <McpProjectList mcpServer={mcpServer} />
        </McpActivePopoverProvider>
    );
};

export default McpServerToolsContent;
