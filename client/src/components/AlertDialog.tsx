import Button from '@/components/Button/Button';
import {Dialog, DialogCancelButton, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {ReactElement, ReactNode, useRef} from 'react';

type ConfirmVariantType = 'default' | 'destructive' | 'destructiveGhost';

interface AlertDialogProps {
    open: boolean;
    ariaLabel?: string;
    autoFocusConfirm?: boolean;
    cancelLabel?: string;
    confirmButtonVariant?: ConfirmVariantType;
    confirmClassName?: string;
    confirmDisabled?: boolean;
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
    autoFocusConfirm = false,
    cancelLabel = 'Cancel',
    confirmButtonVariant = 'destructive',
    confirmClassName,
    confirmDisabled,
    confirmIcon,
    confirmLabel = 'Delete',
    description = 'This action cannot be undone. This will permanently delete data.',
    isPending,
    onCancel,
    onConfirm,
    open,
    title = 'Are you absolutely sure?',
}: AlertDialogProps) => {
    const confirmButtonRef = useRef<HTMLButtonElement>(null);

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (!isOpen && !isPending) {
                    onCancel();
                }
            }}
            open={open}
        >
            <DialogContent
                onInteractOutside={(event) => event.preventDefault()}
                onOpenAutoFocus={(event) => {
                    if (autoFocusConfirm && confirmButtonRef.current) {
                        event.preventDefault();

                        confirmButtonRef.current.focus();
                    }
                }}
                role="alertdialog"
            >
                <DialogMain>
                    <DialogHeader description={description} showCloseButton={!isPending} title={title} />

                    <DialogFooter>
                        <DialogCancelButton disabled={isPending} label={cancelLabel} />

                        <Button
                            aria-label={ariaLabel}
                            className={confirmClassName}
                            disabled={isPending || confirmDisabled}
                            icon={isPending ? <LoadingIcon /> : confirmIcon}
                            label={confirmLabel}
                            onClick={onConfirm}
                            ref={confirmButtonRef}
                            variant={confirmButtonVariant}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AlertDialog;
export type {AlertDialogProps, ConfirmVariantType};
