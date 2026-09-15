import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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
                {canEdit && <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />}

                {canEdit && canDelete && <DropdownMenuSeparator />}

                {canDelete && (
                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={onDeleteClick}
                        variant="destructive"
                    />
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
