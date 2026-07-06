import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import McpIntegrationInstanceConfigurationWorkflowDialog from '@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog';
import {McpServer} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';

import McpIntegrationInstanceConfigurationListItem from './McpIntegrationInstanceConfigurationListItem';
import useMcpIntegrationInstanceConfigurationList from './hooks/useMcpIntegrationInstanceConfigurationList';

interface McpIntegrationInstanceConfigurationListProps {
    mcpServer: McpServer;
}

const McpIntegrationInstanceConfigurationList = ({mcpServer}: McpIntegrationInstanceConfigurationListProps) => {
    const {isLoading, mcpIntegrationInstanceConfigurations} = useMcpIntegrationInstanceConfigurationList(mcpServer.id!);

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

    return mcpIntegrationInstanceConfigurations && mcpIntegrationInstanceConfigurations.length > 0 ? (
        <div className="flex flex-col gap-1.5">
            {mcpIntegrationInstanceConfigurations?.map((mcpIntegrationInstanceConfiguration) => (
                <McpIntegrationInstanceConfigurationListItem
                    key={mcpIntegrationInstanceConfiguration.id}
                    mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
                />
            ))}
        </div>
    ) : (
        <div className="flex justify-center py-8">
            <EmptyList
                button={
                    <McpIntegrationInstanceConfigurationWorkflowDialog
                        mcpServer={mcpServer}
                        triggerNode={<Button label="Add Workflows" />}
                    />
                }
                icon={<WorkflowIcon className="size-24 text-gray-300" />}
                message="No MCP integrations found for this server."
                title="No MCP Integrations"
            />
        </div>
    );
};

export default McpIntegrationInstanceConfigurationList;
