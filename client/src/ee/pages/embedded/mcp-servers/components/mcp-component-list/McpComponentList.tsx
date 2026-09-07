import McpComponentListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentListItem';
import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import McpToolListSkeleton from '@/pages/automation/mcp-servers/components/McpToolListSkeleton';
import {McpComponentsByServerIdQuery, McpServer} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

type McpComponentItemType = NonNullable<NonNullable<McpComponentsByServerIdQuery['mcpComponentsByServerId']>[number]>;

const McpComponentList = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);

    const sortedComponents = useMemo<McpComponentItemType[]>(
        () =>
            (data?.mcpComponentsByServerId ?? [])
                .filter((mcpComponent): mcpComponent is McpComponentItemType => mcpComponent != null)
                .sort((a, b) => a.componentName.localeCompare(b.componentName)),
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
                <McpComponentListItem key={mcpComponent.id} mcpComponent={mcpComponent} mcpServer={mcpServer} />
            ))}
        </div>
    );
};

export default McpComponentList;
