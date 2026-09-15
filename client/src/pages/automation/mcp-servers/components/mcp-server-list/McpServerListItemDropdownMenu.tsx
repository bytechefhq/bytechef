import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Delete is ADMIN-tier on the server (WorkspaceMcpServerFacadeImpl.deleteWorkspaceMcpServer), so offering it to a member without MCP_DELETE means a confirm dialog
    // that ends in a 403.
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'MCP_DELETE');
    const canEdit = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');

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
                    <>
                        <DropdownMenuItem onClick={onAddComponentClick}>Add Component</DropdownMenuItem>

                        <DropdownMenuItem onClick={onAddWorkflowsClick}>Add Workflows</DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>
                    </>
                )}

                {canDelete && (
                    <>
                        {canEdit && <DropdownMenuSeparator />}

                        <DropdownMenuItem className="text-destructive" onClick={onDeleteClick}>
                            Delete
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
