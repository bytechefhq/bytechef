import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import {useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {BoltIcon, XIcon} from 'lucide-react';

import McpComponentToolPropertiesPopover from './McpComponentToolPropertiesPopover';
import useMcpComponentToolDropdownMenu from './hooks/useMcpComponentToolDropdownMenu';

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
    const {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog} = useMcpComponentToolDropdownMenu({mcpTool});

    const {activePopoverId, closePopover, openPopover} = useMcpActivePopover();

    const popoverId = `component-tool-${mcpTool.id}`;
    const isPopoverOpen = activePopoverId === popoverId;

    return (
        <>
            <Popover onOpenChange={(open) => !open && closePopover()} open={isPopoverOpen}>
                <PopoverAnchor asChild>
                    <Badge className="gap-1 py-1 pl-2.5 pr-1" styleType="secondary-filled">
                        <span className="text-sm">{mcpTool.title || mcpTool.name}</span>

                        <Button
                            className="rounded p-0.5 hover:bg-foreground/10"
                            icon={<BoltIcon className="size-3" />}
                            onClick={() => openPopover(popoverId)}
                            size="iconXxs"
                            title="Edit"
                            variant="ghost"
                        />

                        <Button
                            className="rounded p-0.5 hover:bg-destructive/10 hover:text-destructive"
                            icon={<XIcon className="size-3" />}
                            onClick={() => setShowDeleteDialog(true)}
                            size="iconXxs"
                            title="Delete"
                            variant="ghost"
                        />
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
