import AlertDialog from '@/components/AlertDialog';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpServerDialog from '@/pages/automation/mcp-servers/components/McpServerDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import McpServerListItemDropdownMenu from '@/shared/components/mcp-server/McpServerListItemDropdownMenu';
import McpServerToolCounts from '@/shared/components/mcp-server/McpServerToolCounts';
import EEVersion from '@/shared/edition/EEVersion';
import {McpServer, PromotionResourceType, Tag} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDown, ServerIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';

import {McpProjectWorkflowItemType} from '../mcp-project-workflow-list/hooks/useMcpProjectList';
import useMcpServerListItem from './hooks/useMcpServerListItem';

const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);

interface McpServerListItemProps {
    mcpServer: McpServer;
    mcpProjectWorkflows?: McpProjectWorkflowItemType[];
    tags?: Tag[];
}

const McpServerListItem = ({mcpProjectWorkflows, mcpServer, tags}: McpServerListItemProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        handleDeleteClick,
        handleMcpServerListItemClick,
        handleOnCheckedChange,
        isEnablePending,
        isPending,
        mcpServerTagIds,
        setShowDeleteDialog,
        setShowEditDialog,
        setShowPromotionDialog,
        showDeleteDialog,
        showEditDialog,
        showPromoteToEnvironment,
        showPromotionDialog,
        toolsCollapsibleTriggerRef,
        updateMcpServerTagsMutation,
    } = useMcpServerListItem(mcpServer);

    const queryClient = useQueryClient();

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                onClick={(event) => handleMcpServerListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center justify-between">
                            <CollapsibleTrigger className="text-base font-semibold">
                                <div className="flex items-center">
                                    <ServerIcon className="mr-2 size-4 text-content-neutral-secondary" />

                                    <span>{mcpServer.name}</span>
                                </div>
                            </CollapsibleTrigger>
                        </div>

                        <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger
                                    className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                    ref={toolsCollapsibleTriggerRef}
                                >
                                    <McpServerToolCounts
                                        mcpServer={mcpServer}
                                        workflowToolCount={mcpProjectWorkflows?.length || 0}
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
                                        updateTagsMutation={updateMcpServerTagsMutation}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                {isEnablePending && <LoadingIcon />}

                                <Switch
                                    checked={mcpServer.enabled}
                                    disabled={isEnablePending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
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
                            onPromoteClick={() => setShowPromotionDialog(true)}
                            showPromoteToEnvironment={showPromoteToEnvironment}
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

            {showPromotionDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <EnvironmentPromotionDialog
                            onClose={() => setShowPromotionDialog(false)}
                            onPromoted={() => {
                                queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
                            }}
                            resourceType={PromotionResourceType.McpServer}
                            sourceEnvironmentId={+mcpServer.environmentId}
                            sourceId={mcpServer.id}
                            sourceName={mcpServer.name}
                            workspaceId={currentWorkspaceId!}
                        />
                    </Suspense>
                </EEVersion>
            )}
        </>
    );
};

export default McpServerListItem;
