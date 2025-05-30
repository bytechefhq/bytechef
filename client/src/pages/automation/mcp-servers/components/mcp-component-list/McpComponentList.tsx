import EmptyList from '@/components/EmptyList';
import {Button} from '@/components/ui/button';
import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem';
import {useGetMcpComponentsByServerIdQuery} from '@/shared/queries/platform/mcpComponents.queries';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {ComponentIcon} from 'lucide-react';

import McpComponentDialog from '../McpComponentDialog';

const McpComponentList = ({mcpServer}: {mcpServer: McpServerType}) => {
    const {data: mcpComponents, isLoading: isMcpComponentsLoading} = useGetMcpComponentsByServerIdQuery(
        mcpServer.id!,
        !!mcpServer.id
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

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            {mcpComponents && mcpComponents.length > 0 ? (
                <>
                    <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Components</h3>

                    <ul className="divide-y divide-gray-100">
                        {mcpComponents
                            .sort((a, b) => a.componentName.localeCompare(b.componentName))
                            .map((mcpComponent) => (
                                <McpComponentListItem
                                    key={mcpComponent.id}
                                    mcpComponent={mcpComponent}
                                    mcpServer={mcpServer}
                                />
                            ))}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <McpComponentDialog
                                mcpComponent={undefined}
                                mcpServerId={mcpServer.id}
                                triggerNode={<Button>Create Component</Button>}
                            />
                        }
                        icon={<ComponentIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new component."
                        title="No Components"
                    />
                </div>
            )}
        </div>
    );
};

export default McpComponentList;
