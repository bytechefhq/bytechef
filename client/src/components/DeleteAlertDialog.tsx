import Button from '@/components/Button/Button';
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
import {Trash2Icon, XIcon} from 'lucide-react';

interface DeleteAlertDialogProps {
    open: boolean;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
}

const DeleteAlertDialog = ({nodeName, onCancel, onDelete, open}: DeleteAlertDialogProps) => {
    const isNodeDeleteDialog = !!nodeName;

    return (
        <AlertDialog open={open}>
            <AlertDialogContent onEscapeKeyDown={onCancel}>
                <AlertDialogHeader>
                    <AlertDialogTitle>
                        {isNodeDeleteDialog ? `Delete node ${nodeName}?` : 'Are you absolutely sure?'}
                    </AlertDialogTitle>

                    <AlertDialogDescription>
                        {isNodeDeleteDialog
                            ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
                            : 'This action cannot be undone. This will permanently delete data.'}
                    </AlertDialogDescription>

                    <Button
                        aria-label="Close"
                        className="absolute top-4 right-4"
                        icon={<XIcon />}
                        onClick={onCancel}
                        size="icon"
                        variant="ghost"
                    />
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onCancel}>
                        {isNodeDeleteDialog ? 'Keep node' : 'Cancel'}
                    </AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        onClick={onDelete}
                    >
                        {isNodeDeleteDialog && <Trash2Icon />}

                        {isNodeDeleteDialog ? 'Delete node' : 'Delete'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteAlertDialog;
