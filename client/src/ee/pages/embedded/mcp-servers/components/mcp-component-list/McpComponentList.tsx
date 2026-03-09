import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentListItem';
import McpComponentToolList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentToolList';
import useMcpComponentList from '@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import {McpServer} from '@/shared/middleware/graphql';
import {Fragment} from 'react';

const McpComponentList = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);

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
        <div className="py-1 pl-4">
            {data.mcpComponentsByServerId
                .sort((previousComponent, currentComponent) =>
                    previousComponent!.componentName.localeCompare(currentComponent!.componentName)
                )
                .map((mcpComponent) => (
                    <Fragment key={mcpComponent!.id}>
                        <McpComponentListItem
                            key={mcpComponent?.id}
                            mcpComponent={mcpComponent!}
                            mcpServer={mcpServer}
                        />

                        <div className="pl-6">
                            <McpComponentToolList
                                componentName={mcpComponent!.componentName}
                                componentVersion={mcpComponent!.componentVersion}
                                connectionId={mcpComponent!.connectionId}
                                mcpComponent={mcpComponent!}
                                mcpServerId={mcpServer.id!}
                                mcpTools={mcpComponent!.mcpTools}
                            />
                        </div>
                    </Fragment>
                ))}
        </div>
    );
};

export default McpComponentList;
