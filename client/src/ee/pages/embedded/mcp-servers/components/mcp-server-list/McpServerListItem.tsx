import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import LoadingIcon from '@/components/LoadingIcon';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpIntegrationWorkflowDialog from '@/ee/pages/embedded/mcp-servers/components/McpIntegrationWorkflowDialog';
import McpServerDialog from '@/ee/pages/embedded/mcp-servers/components/McpServerDialog';
import McpComponentDialog from '@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import McpServerListItemDropdownMenu from '@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerListItemDropdownMenu';
import TagList from '@/shared/components/TagList';
import {McpIntegrationWorkflow, McpServer, Tag} from '@/shared/middleware/graphql';
import {ChevronDown, ServerIcon} from 'lucide-react';

import useMcpServerListItem from './hooks/useMcpServerListItem';

interface McpServerListItemProps {
    mcpServer: McpServer;
    mcpIntegrationWorkflows?: McpIntegrationWorkflow[];
    tags?: Tag[];
}

const McpServerListItem = ({mcpIntegrationWorkflows, mcpServer, tags}: McpServerListItemProps) => {
    const {
        handleDeleteClick,
        handleOnCheckedChange,
        isEnablePending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        setShowMcpComponentDialog,
        setShowWorkflowDialog,
        showDeleteDialog,
        showEditDialog,
        showMcpComponentDialog,
        showWorkflowDialog,
        updateMcpServerTagsMutation,
    } = useMcpServerListItem(mcpServer);

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <CollapsibleTrigger className="text-base font-semibold">
                                <div className="flex items-center">
                                    <ServerIcon className="mr-2 size-4 text-gray-500" />

                                    <span>{mcpServer.name}</span>
                                </div>
                            </CollapsibleTrigger>
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
                                        {mcpIntegrationWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${mcpIntegrationWorkflows?.length || 0} workflows`}
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

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleDeleteClick}
                open={showDeleteDialog}
            />

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
                <McpIntegrationWorkflowDialog mcpServer={mcpServer} onClose={() => setShowWorkflowDialog(false)} />
            )}
        </>
    );
};

export default McpServerListItem;
