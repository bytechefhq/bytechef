import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {EllipsisVerticalIcon} from 'lucide-react';

import {McpProjectItemType} from './hooks/useMcpProjectList';
import useMcpProjectListItemDropdownMenu from './hooks/useMcpProjectListItemDropdownMenu';

interface McpProjectListItemDropdownMenuProps {
    mcpProject: McpProjectItemType;
    onChangeProjectVersionClick: () => void;
    onEditWorkflowsClick: () => void;
}

const McpProjectListItemDropdownMenu = ({
    mcpProject,
    onChangeProjectVersionClick,
    onEditWorkflowsClick,
}: McpProjectListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditMcpServer = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');

    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpProjectListItemDropdownMenu(mcpProject.id.toString());

    if (!canEditMcpServer) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="MCP Project Actions"
                        icon={<EllipsisVerticalIcon />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onEditWorkflowsClick}>Edit Workflows</DropdownMenuItem>

                    <DropdownMenuItem onClick={onChangeProjectVersionClick}>Change Project Version</DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="text-destructive"
                        disabled={isDeletePending}
                        onClick={() => setShowDeleteDialog(true)}
                    >
                        <span className="w-full">Delete</span>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpProjectListItemDropdownMenu;
