import Badge from '@/components/Badge/Badge';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {BoltIcon, XIcon} from 'lucide-react';

import McpComponentToolPropertiesPopover from './McpComponentToolPropertiesPopover';
import useMcpProjectComponentToolDropdownMenu from './hooks/useMcpProjectComponentToolDropdownMenu';

interface McpComponentToolListItemProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    mcpTool: McpTool;
}

const McpComponentToolListItem = ({
    componentName,
    componentVersion,
    connectionId,
    mcpTool,
}: McpComponentToolListItemProps) => {
    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} = useMcpProjectComponentToolDropdownMenu({
        mcpTool,
    });

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `component-tool-${mcpTool.id}`;
    const isPopoverOpen = activePopoverId === popoverId;

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <Badge className="gap-1 py-1 pl-2.5 pr-1" styleType="secondary-filled">
                        <span className="text-sm">{mcpTool.title || mcpTool.name}</span>

                        <button
                            className="rounded p-0.5 hover:bg-foreground/10"
                            onClick={() => openPopover(popoverId)}
                            title="Configure"
                            type="button"
                        >
                            <BoltIcon className="size-3" />
                        </button>

                        <button
                            className="rounded p-0.5 hover:bg-destructive/10 hover:text-destructive"
                            onClick={() => setShowDeleteDialog(true)}
                            title="Delete"
                            type="button"
                        >
                            <XIcon className="size-3" />
                        </button>
                    </Badge>
                </PopoverAnchor>

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
