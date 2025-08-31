import LoadingIcon from '@/components/LoadingIcon';
import TagList from '@/components/TagList';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpProjectWorkflowDialog from '@/pages/automation/mcp-servers/components/McpProjectWorkflowDialog';
import McpServerDialog from '@/pages/automation/mcp-servers/components/McpServerDialog';
import McpComponentDialog from '@/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import McpServerListItemAlertDialog from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItemAlertDialog';
import McpServerListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItemDropdownMenu';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {
    McpProjectWorkflow,
    McpServer,
    Tag,
    useDeleteMcpServerMutation,
    useUpdateMcpServerMutation,
    useUpdateMcpServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDown, ServerIcon} from 'lucide-react';
import {useState} from 'react';

interface McpServerListItemProps {
    mcpServer: McpServer;
    mcpProjectWorkflows?: McpProjectWorkflow[];
    tags?: Tag[];
}

const McpServerListItem = ({mcpProjectWorkflows, mcpServer, tags}: McpServerListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
    const [isPending, setIsPending] = useState(false);
    const [isEnablePending, setIsEnablePending] = useState(false);

    const mcpServerTagIds = mcpServer.tags?.map((tag) => tag?.id);

    const queryClient = useQueryClient();

    const updateMcpServerMutation = useUpdateMcpServerMutation();
    const deleteMcpServerMutation = useDeleteMcpServerMutation();
    const updateMcpServerTagsMutation = useUpdateMcpServerTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['mcpServers']});
        },
    });

    const handleOnCheckedChange = async (value: boolean) => {
        setIsEnablePending(true);

        updateMcpServerMutation.mutate(
            {
                id: mcpServer.id,
                input: {
                    enabled: value,
                },
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['mcpServers']});
                    setIsEnablePending(false);
                },
            }
        );
    };

    const handleDeleteClick = async () => {
        setIsPending(true);

        deleteMcpServerMutation.mutate(
            {
                id: mcpServer.id,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['mcpServers']});
                    setShowDeleteDialog(false);
                },
            }
        );
    };

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                {mcpServer.name ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <div className="flex items-center">
                                                <span className="text-base font-semibold">{mcpServer.name}</span>
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent>MCP Server</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <div className="flex items-center">
                                        <ServerIcon className="mr-2 size-4 text-gray-500" />

                                        <span className="text-base font-semibold">{mcpServer.name}</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {mcpServer.mcpComponents?.length === 1
                                            ? `1 component`
                                            : `${mcpServer.mcpComponents?.length || 0} components`}
                                    </span>

                                    <span className="mx-1">-</span>

                                    <span className="mr-1">
                                        {mcpProjectWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${mcpProjectWorkflows?.length || 0} workflows`}
                                    </span>

                                    <ChevronDown className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    <TagList
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            tags: tags || [],
                                        })}
                                        id={parseInt(mcpServer.id!)}
                                        remainingTags={tags
                                            ?.filter((tag) => !mcpServerTagIds?.includes(tag.id))
                                            .map((tag) => {
                                                return {id: parseInt(tag.id), name: tag.name};
                                            })}
                                        tags={(mcpServer.tags ?? []).map((tag) => {
                                            return {id: parseInt(tag!.id), name: tag!.name};
                                        })}
                                        updateTagsMutation={updateMcpServerTagsMutation}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex min-w-28 justify-end">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <EnvironmentBadge environmentId={+mcpServer.environmentId!} />
                                </TooltipTrigger>

                                <TooltipContent>The environment</TooltipContent>
                            </Tooltip>
                        </div>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {isEnablePending && <LoadingIcon />}

                                <Switch
                                    checked={mcpServer.enabled}
                                    disabled={isEnablePending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {mcpServer.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${new Date(mcpServer.lastModifiedDate).toLocaleDateString()} ${new Date(mcpServer.lastModifiedDate).toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No modifications</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Modified Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <McpServerListItemDropdownMenu
                            mcpServer={mcpServer}
                            onAddComponentClick={() => setShowMcpComponentDialog(true)}
                            onAddWorkflowsClick={() => setShowWorkflowDialog(true)}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
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

            {showEditDialog && (
                <McpServerDialog
                    mcpServer={mcpServer}
                    onOpenChange={setShowEditDialog}
                    open={showEditDialog}
                    triggerNode={<></>}
                />
            )}

            {showMcpComponentDialog && (
                <McpComponentDialog
                    mcpServerId={mcpServer.id}
                    onOpenChange={setShowMcpComponentDialog}
                    open={showMcpComponentDialog}
                />
            )}

            {showWorkflowDialog && (
                <McpProjectWorkflowDialog mcpServer={mcpServer} onClose={() => setShowWorkflowDialog(false)} />
            )}
        </>
    );
};

export default McpServerListItem;
