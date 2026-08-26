import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import EEVersion from '@/shared/edition/EEVersion';
import {McpServer} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon} from 'lucide-react';

interface McpServerListItemDropdownMenuProps {
    mcpServer: McpServer;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onAddComponentClick: () => void;
    onAddWorkflowsClick: () => void;
    onPromoteClick: () => void;
    showPromoteToEnvironment: boolean;
}

const McpServerListItemDropdownMenu = ({
    onAddComponentClick,
    onAddWorkflowsClick,
    onDeleteClick,
    onEditClick,
    onPromoteClick,
    showPromoteToEnvironment,
}: McpServerListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onAddComponentClick}>Add Component</DropdownMenuItem>

                <DropdownMenuItem onClick={onAddWorkflowsClick}>Add Workflows</DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>

                {showPromoteToEnvironment && (
                    <EEVersion hidden={true}>
                        <DropdownMenuItem onClick={onPromoteClick}>Promote to environment…</DropdownMenuItem>
                    </EEVersion>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem onClick={onDeleteClick} variant="destructive">
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
