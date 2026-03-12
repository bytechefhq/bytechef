import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {McpProject} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon} from 'lucide-react';

import useMcpProjectListItemDropdownMenu from './hooks/useMcpProjectListItemDropdownMenu';

interface McpProjectListItemDropdownMenuProps {
    mcpProject: McpProject;
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
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
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