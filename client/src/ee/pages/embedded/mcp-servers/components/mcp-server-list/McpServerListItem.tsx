import AlertDialog from '@/components/AlertDialog';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpServerDialog from '@/ee/pages/embedded/mcp-servers/components/McpServerDialog';
import TagList from '@/shared/components/TagList';
import McpServerListItemDropdownMenu from '@/shared/components/mcp-server/McpServerListItemDropdownMenu';
import McpServerToolCounts from '@/shared/components/mcp-server/McpServerToolCounts';
import {McpServer, Tag} from '@/shared/middleware/graphql';
import {ChevronDown, ServerIcon} from 'lucide-react';

import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpServerListItem from './hooks/useMcpServerListItem';

interface McpServerListItemProps {
    mcpServer: McpServer;
    mcpIntegrationInstanceConfigurationWorkflows?: McpIntegrationInstanceConfigurationWorkflowItemType[];
    tags?: Tag[];
}

const McpServerListItem = ({mcpIntegrationInstanceConfigurationWorkflows, mcpServer, tags}: McpServerListItemProps) => {
    const {
        handleDeleteClick,
        handleMcpServerListItemClick,
        handleOnCheckedChange,
        isEnablePending,
        isPending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
        toolsCollapsibleTriggerRef,
        updateEmbeddedMcpServerTagsMutation,
    } = useMcpServerListItem(mcpServer);

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-destructive-foreground"
                onClick={(event) => handleMcpServerListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <CollapsibleTrigger className="text-base font-semibold">
                                <div className="flex items-center">
                                    <ServerIcon className="mr-2 size-4 text-content-neutral-secondary" />

                                    <span>{mcpServer.name}</span>
                                </div>
                            </CollapsibleTrigger>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger
                                    className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                    ref={toolsCollapsibleTriggerRef}
                                >
                                    <McpServerToolCounts
                                        mcpServer={mcpServer}
                                        workflowToolCount={mcpIntegrationInstanceConfigurationWorkflows?.length || 0}
                                    />

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
                                        updateTagsMutation={updateEmbeddedMcpServerTagsMutation}
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
                                <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
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
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                        />
                    </div>
                </div>
            </div>

            <AlertDialog
                isPending={isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleDeleteClick}
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
        </>
    );
};

export default McpServerListItem;
