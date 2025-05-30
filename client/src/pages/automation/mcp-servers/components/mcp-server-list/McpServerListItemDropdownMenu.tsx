import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import McpServerDialog from '@/pages/automation/mcp-servers/components/McpServerDialog';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {EllipsisVerticalIcon} from 'lucide-react';

interface McpServerListItemDropdownMenuProps {
    mcpServer: McpServerType;
    onDeleteClick: () => void;
}

const McpServerListItemDropdownMenu = ({mcpServer, onDeleteClick}: McpServerListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem>
                    <McpServerDialog mcpServer={mcpServer} triggerNode={<span className="w-full">Edit</span>} />
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
