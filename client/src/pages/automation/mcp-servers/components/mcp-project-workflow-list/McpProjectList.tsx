import McpToolListSkeleton from '@/pages/platform/mcp-servers/components/McpToolListSkeleton';
import {McpServer} from '@/shared/middleware/graphql';

import McpProjectListItem from './McpProjectListItem';
import useMcpProjectList from './hooks/useMcpProjectList';

interface McpProjectListProps {
    mcpServer: McpServer;
}

const McpProjectList = ({mcpServer}: McpProjectListProps) => {
    const {isLoading, mcpProjects} = useMcpProjectList(mcpServer.id!);

    if (isLoading) {
        return <McpToolListSkeleton />;
    }

    if (!mcpProjects?.length) {
        return <></>;
    }

    return (
        <div className="divide-y divide-stroke-neutral-primary">
            {mcpProjects.map((mcpProject) => (
                <McpProjectListItem key={mcpProject.id} mcpProject={mcpProject} />
            ))}
        </div>
    );
};

export default McpProjectList;
