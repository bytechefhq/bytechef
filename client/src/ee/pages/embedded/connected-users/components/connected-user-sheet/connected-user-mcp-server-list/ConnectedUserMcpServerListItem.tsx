import LoadingIcon from '@/components/LoadingIcon';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserMcpServerListItemToolRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemToolRow';
import {ConnectedUserMcpServer, useEnableConnectedUserMcpServerMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';

const ConnectedUserMcpServerListItem = ({
    connectedUserId,
    mcpServer,
}: {
    connectedUserId: number;
    mcpServer: ConnectedUserMcpServer;
}) => {
    const queryClient = useQueryClient();

    const enableConnectedUserMcpServerMutation = useEnableConnectedUserMcpServerMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    const toolCount = mcpServer.tools.length;

    const lastModifiedDate = mcpServer.lastModifiedDate ? new Date(Date.parse(mcpServer.lastModifiedDate)) : undefined;

    return (
        <Collapsible key={mcpServer.id}>
            <div className="flex w-full items-center justify-between px-2 hover:bg-muted/50">
                <CollapsibleTrigger className="flex-1 py-3">
                    <div className="flex flex-col items-start justify-center gap-y-2">
                        <div
                            className={twMerge(
                                'text-base font-semibold',
                                !mcpServer.enabled && 'text-muted-foreground'
                            )}
                        >
                            {mcpServer.name}
                        </div>

                        <div className="text-xs font-semibold text-muted-foreground">
                            {toolCount === 1 ? `${toolCount} tool` : `${toolCount} tools`}
                        </div>
                    </div>
                </CollapsibleTrigger>

                <div className="flex min-w-52 flex-col items-end gap-y-2">
                    <div className="relative flex items-center">
                        {enableConnectedUserMcpServerMutation.isPending && (
                            <LoadingIcon className="absolute left-[-15px] top-[3px]" />
                        )}

                        <Switch
                            checked={mcpServer.enabled}
                            onCheckedChange={(value) => {
                                enableConnectedUserMcpServerMutation.mutate({
                                    connectedUserId: connectedUserId.toString(),
                                    enable: value,
                                    mcpServerId: mcpServer.id,
                                });
                            }}
                        />
                    </div>

                    {lastModifiedDate && (
                        <Tooltip>
                            <TooltipTrigger className="text-xs text-muted-foreground">
                                {`Updated ${lastModifiedDate.toLocaleDateString()} ${lastModifiedDate.toLocaleTimeString()}`}
                            </TooltipTrigger>

                            <TooltipContent>Last Modified Date</TooltipContent>
                        </Tooltip>
                    )}
                </div>
            </div>

            <CollapsibleContent>
                {toolCount > 0 ? (
                    <div className="flex w-full flex-col gap-y-3 py-3 pl-4">
                        <h3 className="flex justify-start px-2 text-sm font-semibold uppercase text-muted-foreground">
                            Tools
                        </h3>

                        <ul>
                            {mcpServer.tools.map((tool) => (
                                <ConnectedUserMcpServerListItemToolRow key={tool.id} tool={tool} />
                            ))}
                        </ul>
                    </div>
                ) : (
                    <div className="px-4 py-3 text-sm text-muted-foreground">No tools enabled for this user.</div>
                )}
            </CollapsibleContent>
        </Collapsible>
    );
};

export default ConnectedUserMcpServerListItem;
