import {Skeleton} from '@/components/ui/skeleton';
import {McpServer} from '@/shared/middleware/graphql';
import {Fragment} from 'react';

import McpProjectListItem from './McpProjectListItem';
import McpProjectWorkflowList from './McpProjectWorkflowList';
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

    return (
        <div className="py-1 pl-4">
            {mcpProjects?.map((mcpProject) => (
                <Fragment key={mcpProject.id}>
                    <McpProjectListItem mcpProject={mcpProject} />

                    <div className="pl-6">
                        <McpProjectWorkflowList mcpProjectWorkflows={mcpProject.mcpProjectWorkflows} />
                    </div>
                </Fragment>
            ))}
        </div>
    );
};

export default McpProjectList;
