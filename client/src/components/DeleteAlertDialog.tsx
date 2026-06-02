import LoadingIcon from '@/components/LoadingIcon';
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
    description?: string;
    isPending?: boolean;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
}

const DeleteAlertDialog = ({description, isPending, nodeName, onCancel, onDelete, open}: DeleteAlertDialogProps) => {
    const isNodeDeleteDialog = !!nodeName;

    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>
                        {isNodeDeleteDialog ? `Delete node ${nodeName}?` : 'Are you absolutely sure?'}
                    </AlertDialogTitle>

                    <AlertDialogDescription>
                        {description ??
                            (isNodeDeleteDialog
                                ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
                                : 'This action cannot be undone. This will permanently delete data.')}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel disabled={isPending} onClick={onCancel}>
                        {isNodeDeleteDialog ? 'Keep node' : 'Cancel'}
                    </AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        disabled={isPending}
                        onClick={onDelete}
                    >
                        {isPending ? <LoadingIcon /> : isNodeDeleteDialog && <Trash2Icon />}

                        {isNodeDeleteDialog ? 'Delete node' : 'Delete'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteAlertDialog;
