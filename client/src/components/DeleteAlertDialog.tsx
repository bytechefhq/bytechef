import Button from '@/components/Button/Button';
import DestructiveAlertDialogAction from '@/components/DestructiveAlertDialogAction';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Trash2Icon, XIcon} from 'lucide-react';
import {ReactElement} from 'react';

interface DeleteAlertDialogProps {
    open: boolean;
    cancelLabel?: string;
    confirmIcon?: ReactElement;
    confirmLabel?: string;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
}

const DeleteAlertDialog = ({
    cancelLabel,
    confirmIcon,
    confirmLabel,
    nodeName,
    onCancel,
    onDelete,
    open,
}: DeleteAlertDialogProps) => {
    const isNodeDeleteDialog = !!nodeName;

    const resolvedCancelLabel = cancelLabel || (isNodeDeleteDialog ? 'Keep node' : 'Cancel');
    const resolvedConfirmLabel = confirmLabel || (isNodeDeleteDialog ? 'Delete node' : 'Delete');
    const resolvedConfirmIcon = confirmIcon || (isNodeDeleteDialog ? <Trash2Icon /> : undefined);

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
                    <AlertDialogCancel onClick={onCancel}>{resolvedCancelLabel}</AlertDialogCancel>

                    <DestructiveAlertDialogAction onClick={onDelete}>
                        {resolvedConfirmIcon}

                        {resolvedConfirmLabel}
                    </DestructiveAlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteAlertDialog;
