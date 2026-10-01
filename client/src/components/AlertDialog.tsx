import Button from '@/components/Button/Button';
import {Dialog, DialogCancelButton, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {ReactElement, ReactNode} from 'react';

type ConfirmVariantType = 'default' | 'destructive' | 'destructiveGhost';

interface AlertDialogProps {
    open: boolean;
    ariaLabel?: string;
    cancelLabel?: string;
    confirmButtonVariant?: ConfirmVariantType;
    confirmClassName?: string;
    confirmIcon?: ReactElement;
    confirmLabel?: string;
    description?: ReactNode;
    isPending?: boolean;
    onCancel: () => void;
    onConfirm: () => void;
    title?: string;
}

const AlertDialog = ({
    ariaLabel,
    cancelLabel = 'Cancel',
    confirmButtonVariant = 'destructive',
    confirmClassName,
    confirmIcon,
    confirmLabel = 'Delete',
    description = 'This action cannot be undone. This will permanently delete data.',
    isPending,
    onCancel,
    onConfirm,
    open,
    title = 'Are you absolutely sure?',
}: AlertDialogProps) => (
    <Dialog
        onOpenChange={(isOpen) => {
            if (!isOpen && !isPending) {
                onCancel();
            }
        }}
        open={open}
    >
        <DialogContent onInteractOutside={(event) => event.preventDefault()} role="alertdialog">
            <DialogMain>
                <DialogHeader description={description} showCloseButton={!isPending} title={title} />

                <DialogFooter>
                    <DialogCancelButton disabled={isPending} label={cancelLabel} />

                    <Button
                        aria-label={ariaLabel}
                        className={confirmClassName}
                        disabled={isPending}
                        icon={isPending ? <LoadingIcon /> : confirmIcon}
                        label={confirmLabel}
                        onClick={onConfirm}
                        variant={confirmButtonVariant}
                    />
                </DialogFooter>
            </DialogMain>
        </DialogContent>
    </Dialog>
);

export default AlertDialog;
export type {AlertDialogProps, ConfirmVariantType};
