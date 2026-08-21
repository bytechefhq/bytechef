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

interface DowngradeConfirmationDialogPropsI {
    currentPlanName?: string;
    isPending: boolean;
    newPlanName: string;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const DowngradeConfirmationDialog = ({
    currentPlanName,
    isPending,
    newPlanName,
    onClose,
    onConfirm,
    open,
}: DowngradeConfirmationDialogPropsI) => (
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
                <AlertDialogTitle>Downgrade to {newPlanName}?</AlertDialogTitle>

                <AlertDialogDescription asChild>
                    <div>
                        {currentPlanName && (
                            <p className="mb-2">
                                {'You are downgrading from '}

                                <strong>{currentPlanName}</strong>

                                {' to '}

                                <strong>{newPlanName}</strong>

                                {'.'}
                            </p>
                        )}

                        <p>
                            {
                                'This change will take effect at the end of your current billing period. You will retain access to your current plan features until then.'
                            }
                        </p>
                    </div>
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel disabled={isPending}>Keep current plan</AlertDialogCancel>

                <Button
                    disabled={isPending}
                    icon={isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                    label={isPending ? 'Downgrading…' : 'Confirm downgrade'}
                    onClick={onConfirm}
                    variant="destructive"
                />
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default DowngradeConfirmationDialog;
