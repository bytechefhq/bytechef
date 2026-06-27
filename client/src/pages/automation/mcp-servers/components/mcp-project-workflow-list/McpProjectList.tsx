import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import McpProjectWorkflowDialog from '@/pages/automation/mcp-servers/components/McpProjectWorkflowDialog';
import {McpServer} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';

import McpProjectListItem from './McpProjectListItem';
import useMcpProjectList from './hooks/useMcpProjectList';

interface McpProjectListProps {
    mcpServer: McpServer;
}

const McpProjectList = ({mcpServer}: McpProjectListProps) => {
    const {isLoading, mcpProjects} = useMcpProjectList(mcpServer.id!);

    if (isLoading) {
        return (
            <div className="space-y-3 py-2">
                <Skeleton className="h-5 w-40" />

                {[1, 2].map((value) => (
                    <div className="flex items-center space-x-4" key={value}>
                        <Skeleton className="h-4 w-80" />

                        <div className="flex w-60 items-center space-x-1">
                            <Skeleton className="h-6 w-7 rounded-full" />

                            <Skeleton className="size-7 rounded-full" />

                            <Skeleton className="size-7 rounded-full" />
                        </div>

                        <Skeleton className="h-4 flex-1" />
                    </div>
                ))}
            </div>
        );
    }

    return mcpProjects && mcpProjects.length > 0 ? (
        <div className="flex flex-col gap-1.5">
            {mcpProjects.map((mcpProject) => (
                <McpProjectListItem key={mcpProject.id} mcpProject={mcpProject} />
            ))}
        </div>
    ) : (
        <div className="flex justify-center py-8">
            <EmptyList
                button={
                    <McpProjectWorkflowDialog mcpServer={mcpServer} triggerNode={<Button label="Add Workflows" />} />
                }
                icon={<WorkflowIcon className="size-24 text-gray-300" />}
                message="No MCP projects found for this server."
                title="No MCP Projects"
            />
        </div>
    );
};

export default McpProjectList;
