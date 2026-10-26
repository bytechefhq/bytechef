import AlertDialog from '@/components/AlertDialog';
import {Trash2Icon} from 'lucide-react';

interface DeleteAlertDialogProps {
    open: boolean;
    confirmLabel?: string;
    description?: string;
    isPending?: boolean;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
    title?: string;
}

const DeleteAlertDialog = ({
    confirmLabel,
    description,
    isPending,
    nodeName,
    onCancel,
    onDelete,
    open,
    title,
}: DeleteAlertDialogProps) => {
    const isNodeDeleteDialog = !!nodeName;

    return (
        <AlertDialog
            cancelLabel={isNodeDeleteDialog ? 'Keep node' : 'Cancel'}
            confirmIcon={isNodeDeleteDialog ? <Trash2Icon /> : undefined}
            confirmLabel={confirmLabel || (isNodeDeleteDialog ? 'Delete node' : 'Delete')}
            description={
                description ||
                (isNodeDeleteDialog
                    ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
                    : 'This action cannot be undone. This will permanently delete data.')
            }
            isPending={isPending}
            onCancel={onCancel}
            onConfirm={onDelete}
            open={open}
            title={title || (isNodeDeleteDialog ? `Delete node ${nodeName}?` : 'Are you absolutely sure?')}
        />
    );
};

export default DeleteAlertDialog;
