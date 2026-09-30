import AlertDialog from '@/components/AlertDialog';

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
        cancelLabel="Keep current plan"
        confirmLabel={isPending ? 'Downgrading…' : 'Confirm downgrade'}
        description={
            <>
                {currentPlanName && (
                    <span className="mb-2 block">
                        {'You are downgrading from '}

                        <strong>{currentPlanName}</strong>

                        {' to '}

                        <strong>{newPlanName}</strong>

                        {'.'}
                    </span>
                )}

                <span className="block">
                    {
                        'This change will take effect at the end of your current billing period. You will retain access to your current plan features until then.'
                    }
                </span>
            </>
        }
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onConfirm}
        open={open}
        title={`Downgrade to ${newPlanName}?`}
    />
);

export default DowngradeConfirmationDialog;
