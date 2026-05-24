import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserMcpServerListItemToolRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemToolRow';
import {ConnectedUserMcpServer} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

const ConnectedUserMcpServerListItem = ({mcpServer}: {mcpServer: ConnectedUserMcpServer}) => {
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

                        <div className="flex gap-4 text-xs text-muted-foreground">
                            <span className="font-semibold">
                                {toolCount === 1 ? `${toolCount} tool` : `${toolCount} tools`}
                            </span>

                            {lastModifiedDate && (
                                <Tooltip>
                                    <TooltipTrigger className="text-xs">
                                        {`Updated ${lastModifiedDate.toLocaleDateString()} ${lastModifiedDate.toLocaleTimeString()}`}
                                    </TooltipTrigger>

                                    <TooltipContent>Last Modified Date</TooltipContent>
                                </Tooltip>
                            )}
                        </div>
                    </div>
                </CollapsibleTrigger>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Badge
                            label={mcpServer.enabled ? 'ENABLED' : 'DISABLED'}
                            styleType={mcpServer.enabled ? 'secondary-filled' : 'secondary-outline'}
                            weight="semibold"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Server status (managed at the workspace level)</TooltipContent>
                </Tooltip>
            </div>

            <CollapsibleContent>
                {toolCount > 0 ? (
                    <div className="flex w-full flex-col py-3 pl-4">
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
