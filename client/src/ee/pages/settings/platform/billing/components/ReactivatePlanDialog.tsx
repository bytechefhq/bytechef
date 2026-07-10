import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Loader2Icon} from 'lucide-react';

interface ReactivatePlanDialogPropsI {
    isPending: boolean;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const ReactivatePlanDialog = ({isPending, onClose, onConfirm, open}: ReactivatePlanDialogPropsI) => (
    <AlertDialog
        onOpenChange={(isOpen) => {
            if (!isOpen) {
                onClose();
            }
        }}
        open={open}
    >
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Reactivate subscription?</AlertDialogTitle>

                <AlertDialogDescription>
                    Your subscription will continue and you will be charged at the next billing cycle. Your plan will no
                    longer be cancelled.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel disabled={isPending}>Keep cancelled</AlertDialogCancel>

                <Button
                    disabled={isPending}
                    icon={isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                    label={isPending ? 'Reactivating…' : 'Reactivate subscription'}
                    onClick={onConfirm}
                    variant="default"
                />
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default ReactivatePlanDialog;
