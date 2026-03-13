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
import {Trash2Icon} from 'lucide-react';

interface DeleteAlertDialogProps {
    open: boolean;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
}

const DeleteAlertDialog = ({nodeName, onCancel, onDelete, open}: DeleteAlertDialogProps) => {
    const isNodeDelete = !!nodeName;

    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>
                        {isNodeDelete ? `Delete node ${nodeName}?` : 'Are you absolutely sure?'}
                    </AlertDialogTitle>

                    <AlertDialogDescription>
                        {isNodeDelete
                            ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
                            : 'This action cannot be undone. This will permanently delete data.'}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onCancel}>{isNodeDelete ? 'Keep node' : 'Cancel'}</AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        onClick={onDelete}
                    >
                        {isNodeDelete && <Trash2Icon />}

                        {isNodeDelete ? 'Delete node' : 'Delete'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteAlertDialog;
