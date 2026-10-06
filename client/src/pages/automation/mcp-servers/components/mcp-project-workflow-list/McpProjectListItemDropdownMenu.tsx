import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
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

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="relative z-10" icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditWorkflowsClick}>
                        <EditIcon /> Edit Workflows
                    </DropdownMenuItem>

                    <DropdownMenuItem className="dropdown-menu-item" onClick={onChangeProjectVersionClick}>
                        <RefreshCwIcon /> Change Project Version
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        disabled={isDeletePending}
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    >
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
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
