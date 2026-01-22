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

import useDeleteDataTableRowsDialog from '../hooks/useDeleteDataTableRowsDialog';

const DeleteDataTableRowsDialog = () => {
    const {handleDelete, handleOpenChange, open, rowCount} = useDeleteDataTableRowsDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete records</AlertDialogTitle>

                    <AlertDialogDescription>
                        {`Are you sure you want to delete ${rowCount} selected record${rowCount === 1 ? '' : 's'}? This action cannot be undone.`}
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

export default DeleteDataTableRowsDialog;
