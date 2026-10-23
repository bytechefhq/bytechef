import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface McpToolActionsMenuProps {
    label: string;
    onDeleteClick?: () => void;
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
            {onEditClick && <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onEditClick} />}

            {onEditClick && onDeleteClick && <DropdownMenuSeparator />}

            {onDeleteClick && (
                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onSelect={onDeleteClick} variant="destructive" />
            )}
        </DropdownMenuContent>
    </DropdownMenu>
);

export default McpToolActionsMenu;
