import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';

import useDeleteDataTableColumnDialog from '../hooks/useDeleteDataTableColumnDialog';

const DeleteDataTableColumnDialog = () => {
    const {columnName, handleDelete, handleOpenChange, open} = useDeleteDataTableColumnDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete column</AlertDialogTitle>

                    <AlertDialogDescription>
                        {`Are you sure you want to delete column "${columnName}"? This action cannot be undone and will remove all data in this column.`}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteDataTableColumnDialog;
