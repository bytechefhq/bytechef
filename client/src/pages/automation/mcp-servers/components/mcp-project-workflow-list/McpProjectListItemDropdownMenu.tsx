import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
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
    const {handleConfirmDelete, isDeletePending, setShowDeleteDialog, showDeleteDialog} =
        useMcpProjectListItemDropdownMenu(mcpProject.id.toString());

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="relative z-10" icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
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
