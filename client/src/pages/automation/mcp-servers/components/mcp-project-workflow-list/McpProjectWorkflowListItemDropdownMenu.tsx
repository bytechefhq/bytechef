import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon} from 'lucide-react';

interface McpProjectWorkflowListItemDropdownMenuProps {
    onEditClick: () => void;
}

const McpProjectWorkflowListItemDropdownMenu = ({onEditClick}: McpProjectWorkflowListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => onEditClick()}>
                    <span className="w-full">Edit Workflow</span>
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpProjectWorkflowListItemDropdownMenu;
