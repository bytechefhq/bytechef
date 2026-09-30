import Button from '@/components/Button/Button';
import {Dialog, DialogCancelButton, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Trash2Icon} from 'lucide-react';
import {ReactElement, ReactNode} from 'react';

interface DeleteAlertDialogProps {
    open: boolean;
    ariaLabel?: string;
    cancelLabel?: string;
    confirmIcon?: ReactElement;
    confirmLabel?: string;
    description?: ReactNode;
    isPending?: boolean;
    nodeName?: string;
    onCancel: () => void;
    onDelete: () => void;
    title?: string;
}

const DeleteAlertDialog = ({
    ariaLabel,
    cancelLabel,
    confirmIcon,
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

    const resolvedCancelLabel = cancelLabel || (isNodeDeleteDialog ? 'Keep node' : 'Cancel');
    const resolvedConfirmLabel = confirmLabel || (isNodeDeleteDialog ? 'Delete node' : 'Delete');
    const resolvedConfirmIcon = confirmIcon || (isNodeDeleteDialog ? <Trash2Icon /> : undefined);

    const resolvedDescription =
        description ||
        (isNodeDeleteDialog
            ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
            : 'This action cannot be undone. This will permanently delete data.');
    const resolvedTitle = title || (isNodeDeleteDialog ? `Delete node ${nodeName}?` : 'Are you absolutely sure?');

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onCancel();
                }
            }}
            open={open}
        >
            <DialogContent onInteractOutside={(event) => event.preventDefault()} role="alertdialog">
                <DialogMain>
                    <DialogHeader description={resolvedDescription} title={resolvedTitle} />

                    <DialogFooter>
                        <DialogCancelButton label={resolvedCancelLabel} />

                        <Button
                            aria-label={ariaLabel}
                            disabled={isPending}
                            icon={isPending ? <LoadingIcon /> : resolvedConfirmIcon}
                            label={resolvedConfirmLabel}
                            onClick={onDelete}
                            variant="destructive"
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default DeleteAlertDialog;
