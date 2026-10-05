import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {McpServer} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon} from 'lucide-react';

interface McpServerListItemDropdownMenuProps {
    mcpServer: McpServer;
    onDeleteClick: () => void;
    onEditClick: () => void;
}

const McpServerListItemDropdownMenu = ({onDeleteClick, onEditClick}: McpServerListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
