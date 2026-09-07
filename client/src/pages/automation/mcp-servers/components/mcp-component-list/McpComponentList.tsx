import McpToolListSkeleton from '@/pages/automation/mcp-servers/components/McpToolListSkeleton';
import McpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem';
import useMcpComponentList from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import {McpServer} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const McpComponentList = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);

    const sortedComponents = useMemo(
        () =>
            data?.mcpComponentsByServerId
                ? [...data.mcpComponentsByServerId].sort((a, b) => a!.componentName.localeCompare(b!.componentName))
                : [],
        [data?.mcpComponentsByServerId]
    );

    if (isMcpComponentsLoading) {
        return <McpToolListSkeleton />;
    }

    if (!sortedComponents.length) {
        return <></>;
    }

    return (
        <div className="flex flex-col gap-1.5">
            {sortedComponents.map((mcpComponent) => (
                <McpComponentListItem key={mcpComponent!.id} mcpComponent={mcpComponent!} mcpServer={mcpServer} />
            ))}
        </div>
    );
};

export default McpComponentList;
