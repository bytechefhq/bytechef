import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {EditIcon, EllipsisVerticalIcon, RefreshCwIcon, Trash2Icon} from 'lucide-react';

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
    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpProjectListItemDropdownMenu(mcpProject.id.toString());

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditMcpServer = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');

    if (!canEditMcpServer) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="MCP Project Actions"
                        className="relative z-10"
                        icon={<EllipsisVerticalIcon />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem icon={<EditIcon />} label="Edit Workflows" onClick={onEditWorkflowsClick} />

                    <DropdownMenuItem
                        icon={<RefreshCwIcon />}
                        label="Change Project Version"
                        onClick={onChangeProjectVersionClick}
                    />

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        disabled={isDeletePending}
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    />
                </DropdownMenuContent>
            </DropdownMenu>

            <AlertDialog
                isPending={isDeletePending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </>
    );
};

export default McpProjectListItemDropdownMenu;
