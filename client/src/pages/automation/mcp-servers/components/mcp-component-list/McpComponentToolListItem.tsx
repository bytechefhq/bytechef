import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {useCloseActivePopoverOnUnmount, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {BoltIcon, Trash2Icon} from 'lucide-react';

import McpComponentToolPropertiesPopover from './McpComponentToolPropertiesPopover';
import useMcpProjectComponentToolDropdownMenu from './hooks/useMcpProjectComponentToolDropdownMenu';

interface McpComponentToolListItemProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    description?: string | null;
    mcpTool: McpTool;
}

const McpComponentToolListItem = ({
    componentName,
    componentVersion,
    connectionId,
    description,
    mcpTool,
}: McpComponentToolListItemProps) => {
    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} = useMcpProjectComponentToolDropdownMenu({
        mcpTool,
    });

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `component-tool-${mcpTool.id}`;
    const isPopoverOpen = activePopoverId === popoverId;

    useCloseActivePopoverOnUnmount(isPopoverOpen);

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <div className="flex items-center gap-2 py-0.5">
                    <div className="flex min-w-0 flex-1 flex-col">
                        <span className="truncate text-sm font-medium">{mcpTool.title || mcpTool.name}</span>

                        {description && <span className="truncate text-xs text-muted-foreground">{description}</span>}
                    </div>

                    <div className="flex shrink-0 items-center gap-0.5">
                        {/* Anchor the popover to the Configure button so it opens right-aligned to that button. */}

                        <PopoverAnchor asChild>
                            <Button
                                aria-label="Configure"
                                className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                icon={<BoltIcon className="size-4" />}
                                onClick={() => openPopover(popoverId)}
                                size="iconSm"
                                title="Configure"
                                variant="ghost"
                            />
                        </PopoverAnchor>

                        <Button
                            aria-label="Delete"
                            className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                            icon={<Trash2Icon className="size-4" />}
                            onClick={() => setShowDeleteDialog(true)}
                            size="iconSm"
                            title="Delete"
                            variant="ghost"
                        />
                    </div>
                </div>

                {isPopoverOpen && (
                    <McpComponentToolPropertiesPopover
                        componentName={componentName}
                        componentVersion={componentVersion}
                        connectionId={connectionId}
                        mcpTool={mcpTool}
                        onClose={closePopover}
                    />
                )}
            </Popover>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpComponentToolListItem;
