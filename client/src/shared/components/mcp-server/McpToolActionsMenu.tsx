import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface McpToolActionsMenuProps {
    label: string;
    onDeleteClick: () => void;
    onEditClick?: () => void;
}

const McpToolActionsMenu = ({label, onDeleteClick, onEditClick}: McpToolActionsMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button
                aria-label={`${label} actions`}
                className="shrink-0"
                icon={<EllipsisVerticalIcon />}
                size="iconSm"
                variant="ghost"
            />
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end" onCloseAutoFocus={(event) => event.preventDefault()}>
            {onEditClick && (
                <>
                    <DropdownMenuItem className="dropdown-menu-item" onSelect={onEditClick}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />
                </>
            )}

            <DropdownMenuItem className="dropdown-menu-item-destructive" onSelect={onDeleteClick} variant="destructive">
                <Trash2Icon /> Delete
            </DropdownMenuItem>
        </DropdownMenuContent>
    </DropdownMenu>
);

export default McpToolActionsMenu;
