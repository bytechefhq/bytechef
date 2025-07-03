import {Button} from '@/components/ui/button';
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
    onAddComponentClick: () => void;
    onAddWorkflowsClick: () => void;
}

const McpServerListItemDropdownMenu = ({
    onAddComponentClick,
    onAddWorkflowsClick,
    onDeleteClick,
    onEditClick,
}: McpServerListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onAddComponentClick}>Add Component</DropdownMenuItem>

                <DropdownMenuItem onClick={onAddWorkflowsClick}>Add Workflows</DropdownMenuItem>

                <DropdownMenuSeparator />

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
