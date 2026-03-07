import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import McpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem';
import McpComponentToolList from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolList';
import useMcpComponentList from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList';
import {McpServer} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';
import {Fragment, useMemo, useState} from 'react';

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
        <div className="py-1 pl-4">
            {sortedComponents.length > 0 ? (
                sortedComponents.map((mcpComponent) => (
                    <Fragment key={mcpComponent!.id}>
                        <McpComponentListItem mcpComponent={mcpComponent!} mcpServer={mcpServer} />

                        <McpComponentToolList
                            componentName={mcpComponent!.componentName}
                            componentVersion={mcpComponent!.componentVersion}
                            connectionId={mcpComponent!.connectionId}
                            mcpComponent={mcpComponent!}
                            mcpServerId={mcpServer.id!}
                            mcpTools={mcpComponent!.mcpTools}
                        />
                    </Fragment>
                ))
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
