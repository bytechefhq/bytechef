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

interface CancelPlanDialogPropsI {
    isPending: boolean;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const CancelPlanDialog = ({isPending, onClose, onConfirm, open}: CancelPlanDialogPropsI) => (
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
                <AlertDialogTitle>Cancel subscription?</AlertDialogTitle>

                <AlertDialogDescription>
                    Your subscription will be cancelled at the end of the current billing period. You will retain access
                    until then and will not be charged again.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel disabled={isPending}>Keep plan</AlertDialogCancel>

                <Button
                    disabled={isPending}
                    icon={isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                    label={isPending ? 'Cancelling…' : 'Cancel subscription'}
                    onClick={onConfirm}
                    variant="destructive"
                />
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default CancelPlanDialog;
