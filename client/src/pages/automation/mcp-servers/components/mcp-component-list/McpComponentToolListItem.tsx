import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import McpComponentToolPropertiesPopover from '@/pages/platform/mcp-servers/components/McpComponentToolPropertiesPopover';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {
    McpTool,
    useUpdateEmbeddedMcpToolEnabledMutation,
    useUpdateWorkspaceMcpToolEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BoltIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import useMcpProjectComponentToolDropdownMenu from './hooks/useMcpProjectComponentToolDropdownMenu';

interface McpComponentToolListItemProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    description?: string | null;
    embedded?: boolean;
    enabledSwitchVisible?: boolean;
    mcpTool: McpTool;
}

const McpComponentToolListItem = ({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    description,
    embedded,
    enabledSwitchVisible,
    mcpTool,
}: McpComponentToolListItemProps) => {
    const [isEnablePending, setIsEnablePending] = useState(false);

    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpProjectComponentToolDropdownMenu({
            embedded,
            mcpTool,
        });

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const queryClient = useQueryClient();

    const updateEmbeddedMcpToolEnabledMutation = useUpdateEmbeddedMcpToolEnabledMutation();
    const updateWorkspaceMcpToolEnabledMutation = useUpdateWorkspaceMcpToolEnabledMutation();

    const popoverId = `component-tool-${mcpTool.id}`;
    const toolLabel = mcpTool.title || mcpTool.name;
    const isPopoverOpen = activePopoverId === popoverId;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    const handleEnabledChange = (value: boolean) => {
        setIsEnablePending(true);

        const mutateOptions = {
            onSettled: () => {
                setIsEnablePending(false);
            },
            onSuccess: () => {
                if (embedded) {
                    void queryClient.invalidateQueries({queryKey: ['embeddedMcpComponentsByServerId']});
                } else {
                    void queryClient.invalidateQueries({queryKey: ['mcpComponentsByServerId']});
                    void queryClient.invalidateQueries({queryKey: ['mcpToolsByComponentId']});
                }
            },
        };

        if (embedded) {
            updateEmbeddedMcpToolEnabledMutation.mutate({enabled: value, id: mcpTool.id}, mutateOptions);
        } else {
            updateWorkspaceMcpToolEnabledMutation.mutate({enabled: value, id: mcpTool.id}, mutateOptions);
        }
    };

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <div className="flex items-center gap-2 py-0.5">
                    <div className="flex min-w-0 flex-1 flex-col">
                        <span className="truncate text-sm font-medium">{toolLabel}</span>

                        {description && <span className="truncate text-xs text-muted-foreground">{description}</span>}
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                        {(embedded || enabledSwitchVisible) && (
                            <Switch
                                aria-label={`Enable ${toolLabel}`}
                                checked={mcpTool.enabled}
                                className="mr-2"
                                disabled={isEnablePending}
                                onCheckedChange={handleEnabledChange}
                            />
                        )}

                        <DropdownMenu>
                            <PopoverAnchor asChild>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label={`${toolLabel} actions`}
                                        icon={<EllipsisVerticalIcon />}
                                        size="iconSm"
                                        variant="ghost"
                                    />
                                </DropdownMenuTrigger>
                            </PopoverAnchor>

                            <DropdownMenuContent align="end" onCloseAutoFocus={(event) => event.preventDefault()}>
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onSelect={() => openPopover(popoverId)}
                                >
                                    <BoltIcon /> Configure
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onSelect={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

                {isPopoverOpen && (
                    <McpComponentToolPropertiesPopover
                        componentName={componentName}
                        componentVersion={componentVersion}
                        connectionId={connectionId}
                        connectionRequired={connectionRequired}
                        embedded={embedded}
                        mcpTool={mcpTool}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpComponentToolListItem;
