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

interface UpgradeConfirmationDialogPropsI {
    currentPlanName?: string;
    isPending: boolean;
    newPlanName: string;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const UpgradeConfirmationDialog = ({
    currentPlanName,
    isPending,
    newPlanName,
    onClose,
    onConfirm,
    open,
}: UpgradeConfirmationDialogPropsI) => (
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
                <AlertDialogTitle>Upgrade to {newPlanName}?</AlertDialogTitle>

                <AlertDialogDescription asChild>
                    <div>
                        {currentPlanName && (
                            <p className="mb-2">
                                {'You are upgrading from '}

                                <strong>{currentPlanName}</strong>

                                {' to '}

                                <strong>{newPlanName}</strong>

                                {'.'}
                            </p>
                        )}

                        <p>
                            {
                                'You will be charged immediately for the prorated cost for the remainder of your current billing period. This action cannot be undone.'
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
                    label={isPending ? 'Upgrading…' : 'Upgrade now'}
                    onClick={onConfirm}
                    variant="default"
                />
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default UpgradeConfirmationDialog;
