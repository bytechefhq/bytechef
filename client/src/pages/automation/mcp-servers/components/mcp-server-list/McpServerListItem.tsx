import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpServerListItemAlertDialog from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItemAlertDialog';
import McpServerListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItemDropdownMenu';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDown, ServerIcon} from 'lucide-react';
import {useState} from 'react';

const McpServerListItem = ({mcpServer}: {mcpServer: McpServerType}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);
    const [localMcpServer, setLocalMcpServer] = useState<McpServerType>(mcpServer);

    const queryClient = useQueryClient();

    const handleOnCheckedChange = async (value: boolean) => {
        setIsEnablePending(true);
        try {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        mutation updateMcpServer($id: Int!, $input: Map!) {
                            updateMcpServer(id: $id, input: $input) {
                                id
                                name
                                type
                                environment
                                enabled
                            }
                        }
                    `,
                    variables: {
                        id: localMcpServer.id,
                        input: {
                            enabled: value,
                        },
                    },
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            setLocalMcpServer({...localMcpServer, enabled: value});
        } catch (error) {
            console.error('Error updating MCP server:', error);
        } finally {
            setIsEnablePending(false);
        }
    };

    const handleDeleteClick = async () => {
        setIsPending(true);
        try {
            // Note: This is a placeholder for a delete mutation
            // In a real implementation, you would need to add a delete mutation to the GraphQL schema
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        mutation deleteMcpServer($id: Int!) {
                            deleteMcpServer(id: $id)
                        }
                    `,
                    variables: {
                        id: localMcpServer.id,
                    },
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            queryClient.invalidateQueries({queryKey: ['mcpServers']});
            setShowDeleteDialog(false);
        } catch (error) {
            console.error('Error deleting MCP server:', error);
        } finally {
            setIsPending(false);
        }
    };

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                {localMcpServer.name ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <div className="flex items-center">
                                                <span className="text-base font-semibold">{localMcpServer.name}</span>
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent>MCP Server</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <div className="flex items-center">
                                        <ServerIcon className="mr-2 size-4 text-gray-500" />

                                        <span className="text-base font-semibold">{localMcpServer.name}</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {localMcpServer.mcpComponents?.length === 1
                                            ? `1 component`
                                            : `${localMcpServer.mcpComponents?.length || 0} components`}
                                    </span>

                                    <ChevronDown className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge variant="secondary">{localMcpServer.type}</Badge>
                            </TooltipTrigger>

                            <TooltipContent>The server type</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-28 justify-end">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Badge variant="secondary">{localMcpServer.environment}</Badge>
                                </TooltipTrigger>

                                <TooltipContent>The environment</TooltipContent>
                            </Tooltip>
                        </div>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {isEnablePending && <LoadingIcon />}

                                <Switch
                                    checked={localMcpServer.enabled}
                                    disabled={isEnablePending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {localMcpServer.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${new Date(localMcpServer.lastModifiedDate).toLocaleDateString()} ${new Date(localMcpServer.lastModifiedDate).toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No modifications</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Modified Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <McpServerListItemDropdownMenu
                            mcpServer={localMcpServer}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                        />
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <McpServerListItemAlertDialog
                    isPending={isPending}
                    onCancelClick={() => setShowDeleteDialog(false)}
                    onDeleteClick={handleDeleteClick}
                />
            )}
        </>
    );
};

export default McpServerListItem;
