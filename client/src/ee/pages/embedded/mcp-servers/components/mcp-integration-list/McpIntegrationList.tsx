import {Skeleton} from '@/components/ui/skeleton';
import {McpServer} from '@/shared/middleware/graphql';
import {Fragment} from 'react';

import McpIntegrationListItem from './McpIntegrationListItem';
import McpIntegrationWorkflowList from './McpIntegrationWorkflowList';
import useMcpIntegrationList from './hooks/useMcpIntegrationList';

interface McpIntegrationListProps {
    mcpServer: McpServer;
}

const McpIntegrationList = ({mcpServer}: McpIntegrationListProps) => {
    const {isLoading, mcpIntegrations} = useMcpIntegrationList(mcpServer.id!);

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

    return (
        <div className="py-1 pl-4">
            {mcpIntegrations?.map((mcpIntegration) => (
                <Fragment key={mcpIntegration.id}>
                    <McpIntegrationListItem mcpIntegration={mcpIntegration} />

                    <div className="pl-6">
                        <McpIntegrationWorkflowList
                            componentName={mcpIntegration.integration?.name || ''}
                            mcpIntegrationWorkflows={mcpIntegration.mcpIntegrationWorkflows}
                        />
                    </div>
                </Fragment>
            ))}
        </div>
    );
};

export default McpIntegrationList;
