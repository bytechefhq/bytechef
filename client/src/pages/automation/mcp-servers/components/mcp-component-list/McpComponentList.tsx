import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem';
import {McpServer, useMcpComponentsByServerIdQuery} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';

const McpComponentList = ({mcpServer}: {mcpServer: McpServer}) => {
    const {data, isLoading: isMcpComponentsLoading} = useMcpComponentsByServerIdQuery({
        mcpServerId: mcpServer.id!,
    });

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
        <div className="py-3 pl-4">
            {data.mcpComponentsByServerId.length > 0 ? (
                <>
                    <ul className="divide-y divide-gray-100">
                        {data.mcpComponentsByServerId
                            .sort((a, b) => a!.componentName.localeCompare(b!.componentName))
                            .map((mcpComponent) => (
                                <McpComponentListItem
                                    key={mcpComponent?.id}
                                    mcpComponent={mcpComponent!}
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
                                triggerNode={<Button label="Add Component" />}
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
