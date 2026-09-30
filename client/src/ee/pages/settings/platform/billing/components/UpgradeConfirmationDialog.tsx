import AlertDialog from '@/components/AlertDialog';

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
        cancelLabel="Keep current plan"
        confirmButtonVariant="default"
        confirmLabel={isPending ? 'Upgrading…' : 'Upgrade now'}
        description={
            <>
                {currentPlanName && (
                    <span className="mb-2 block">
                        {'You are upgrading from '}

                        <strong>{currentPlanName}</strong>

                        {' to '}

                        <strong>{newPlanName}</strong>

                        {'.'}
                    </span>
                )}

                <span className="block">
                    {
                        'You will be charged immediately for the prorated cost for the remainder of your current billing period. This action cannot be undone.'
                    }
                </span>
            </>
        }
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onConfirm}
        open={open}
        title={`Upgrade to ${newPlanName}?`}
    />
);

export default UpgradeConfirmationDialog;
