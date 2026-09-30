import AlertDialog from '@/components/AlertDialog';

interface ReactivatePlanDialogPropsI {
    isPending: boolean;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const ReactivatePlanDialog = ({isPending, onClose, onConfirm, open}: ReactivatePlanDialogPropsI) => (
    <AlertDialog
        cancelLabel="Keep cancelled"
        confirmButtonVariant="default"
        confirmLabel={isPending ? 'Reactivating…' : 'Reactivate subscription'}
        description="Your subscription will continue and you will be charged at the next billing cycle. Your plan will no longer be cancelled."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onConfirm}
        open={open}
        title="Reactivate subscription?"
    />
);

export default ReactivatePlanDialog;
