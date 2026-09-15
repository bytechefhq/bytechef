import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {McpServer} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface McpServerListItemDropdownMenuProps {
    canDelete?: boolean;
    canEdit?: boolean;
    mcpServer: McpServer;
    onDeleteClick: () => void;
    onEditClick: () => void;
}

const McpServerListItemDropdownMenu = ({
    canDelete = true,
    canEdit = true,
    onDeleteClick,
    onEditClick,
}: McpServerListItemDropdownMenuProps) => {
    if (!canEdit && !canDelete) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="MCP Server Actions" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {canEdit && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>
                )}

                {canEdit && canDelete && <DropdownMenuSeparator />}

                {canDelete && (
                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        onClick={onDeleteClick}
                        variant="destructive"
                    >
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
