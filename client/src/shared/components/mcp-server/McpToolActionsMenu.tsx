import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {PopoverAnchor} from '@/components/ui/popover';
import {BoltIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface McpToolActionsMenuProps {
    anchorsPopover?: boolean;
    label: string;
    onConfigureClick: () => void;
    onDeleteClick: () => void;
    onEditClick?: () => void;
}

const McpToolActionsMenu = ({
    anchorsPopover = false,
    label,
    onConfigureClick,
    onDeleteClick,
    onEditClick,
}: McpToolActionsMenuProps) => {
    const trigger = (
        <DropdownMenuTrigger asChild>
            <Button
                aria-label={`${label} actions`}
                className="shrink-0"
                icon={<EllipsisVerticalIcon />}
                size="iconSm"
                variant="ghost"
            />
        </DropdownMenuTrigger>
    );

    return (
        <DropdownMenu>
            {anchorsPopover ? <PopoverAnchor asChild>{trigger}</PopoverAnchor> : trigger}

            <DropdownMenuContent align="end" onCloseAutoFocus={(event) => event.preventDefault()}>
                <DropdownMenuItem className="dropdown-menu-item" onSelect={onConfigureClick}>
                    <BoltIcon /> Configure
                </DropdownMenuItem>

                {onEditClick && (
                    <DropdownMenuItem className="dropdown-menu-item" onSelect={onEditClick}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    className="dropdown-menu-item-destructive"
                    onSelect={onDeleteClick}
                    variant="destructive"
                >
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpToolActionsMenu;
