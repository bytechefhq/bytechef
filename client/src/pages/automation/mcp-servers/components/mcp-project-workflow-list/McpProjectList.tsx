import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import {McpProject, McpServer, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';
import {Fragment} from 'react';

import McpProjectWorkflowDialog from '../McpProjectWorkflowDialog';
import McpProjectListItem from './McpProjectListItem';
import McpProjectWorkflowList from './McpProjectWorkflowList';

interface McpProjectListProps {
    mcpServer: McpServer;
}

const McpProjectList = ({mcpServer}: McpProjectListProps) => {
    const {data: mcpProjectsData, isLoading} = useMcpProjectsByServerIdQuery({
        mcpServerId: mcpServer.id!,
    });

    const mcpProjects =
        mcpProjectsData?.mcpProjectsByServerId?.filter((project): project is McpProject => project !== null) || [];

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
        <div className="py-3 pl-4">
            {mcpProjects && mcpProjects.length > 0 ? (
                <>
                    {mcpProjects.map((mcpProject) => {
                        return (
                            <Fragment key={mcpProject.id}>
                                <McpProjectListItem mcpProject={mcpProject} />

                                <McpProjectWorkflowList mcpProjectWorkflows={mcpProject.mcpProjectWorkflows} />
                            </Fragment>
                        );
                    })}
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <McpProjectWorkflowDialog
                                mcpServer={mcpServer}
                                triggerNode={<Button label="Add Workflows" />}
                            />
                        }
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
                        message="No MCP projects found for this server."
                        title="No MCP Projects"
                    />
                </div>
            )}
        </div>
    );
};

export default McpProjectList;
