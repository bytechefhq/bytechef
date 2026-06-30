import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentListItem';
import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
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

    if (!data || !data.mcpComponentsByServerId) {
        return <></>;
    }

    return (
        <div className="flex flex-col gap-1.5 py-2">
            {sortedComponents.map((mcpComponent) => (
                <McpComponentListItem key={mcpComponent.id} mcpComponent={mcpComponent} mcpServer={mcpServer} />
            ))}
        </div>
    );
};

export default McpComponentList;
