import Button from '@/components/Button/Button';
import {Dialog, DialogCancelButton, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Trash2Icon} from 'lucide-react';
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
                    <DialogHeader
                        description={
                            isNodeDeleteDialog
                                ? 'This action cannot be undone. This will permanently delete the node and properties it contains.'
                                : 'This action cannot be undone. This will permanently delete data.'
                        }
                        title={isNodeDeleteDialog ? `Delete node ${nodeName}?` : 'Are you absolutely sure?'}
                    />

                    <DialogFooter>
                        <DialogCancelButton label={resolvedCancelLabel} />

                        <Button
                            icon={resolvedConfirmIcon}
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
