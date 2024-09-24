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

interface DeleteAlertDialogProps {
    open: boolean;
    onCancel: () => void;
    onDelete: () => void;
}

const DeleteAlertDialog = ({onCancel, onDelete, open}: DeleteAlertDialogProps) => {
    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the project and workflows it
                        contains.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onCancel}>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-red-600" onClick={onDelete}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteAlertDialog;
