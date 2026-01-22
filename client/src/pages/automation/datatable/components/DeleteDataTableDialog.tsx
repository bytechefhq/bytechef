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

import useDeleteDataTableDialog from '../hooks/useDeleteDataTableDialog';

const DeleteDataTableDialog = () => {
    const {handleDelete, handleOpenChange, open, tableName} = useDeleteDataTableDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete Table</AlertDialogTitle>

                    <AlertDialogDescription>
                        {`Are you sure you want to delete table "${tableName}"? This action cannot be undone and will remove the table and all of its data.`}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-destructive" onClick={handleDelete}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteDataTableDialog;
