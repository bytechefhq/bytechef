import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem';
import useMcpComponentList from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import {McpServer} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';

const McpComponentList = ({mcpServer}: {mcpServer: McpServer}) => {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const {data, isMcpComponentsLoading} = useMcpComponentList(mcpServer.id!);

    const sortedComponents = useMemo(
        () =>
            data?.mcpComponentsByServerId
                ? [...data.mcpComponentsByServerId].sort((a, b) => a!.componentName.localeCompare(b!.componentName))
                : [],
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
        <div>
            {sortedComponents.length > 0 ? (
                <div className="flex flex-col gap-1.5">
                    {sortedComponents.map((mcpComponent) => (
                        <McpComponentListItem
                            key={mcpComponent!.id}
                            mcpComponent={mcpComponent!}
                            mcpServer={mcpServer}
                        />
                    ))}
                </div>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={<Button label="Add Component" onClick={() => setShowAddDialog(true)} />}
                        icon={<ComponentIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new component."
                        title="No Components"
                    />

                    <McpComponentDialog
                        mcpComponent={undefined}
                        mcpServerId={mcpServer.id!}
                        onOpenChange={setShowAddDialog}
                        open={showAddDialog}
                    />
                </div>
            )}
        </div>
    );
};

export default McpComponentList;
