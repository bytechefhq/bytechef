import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon} from 'lucide-react';

interface McpComponentToolDropdownMenuProps {
    handleConfirmDelete: () => void;
    onEditClick: () => void;
    setShowDeleteDialog: (show: boolean) => void;
    showDeleteDialog: boolean;
}

const McpComponentToolDropdownMenu = ({
    handleConfirmDelete,
    onEditClick,
    setShowDeleteDialog,
    showDeleteDialog,
}: McpComponentToolDropdownMenuProps) => {
    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onEditClick}>
                        <span className="w-full">Edit</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
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

export default McpComponentToolDropdownMenu;
