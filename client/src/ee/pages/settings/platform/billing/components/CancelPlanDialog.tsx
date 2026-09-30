import AlertDialog from '@/components/AlertDialog';

interface CancelPlanDialogPropsI {
    isPending: boolean;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const CancelPlanDialog = ({isPending, onClose, onConfirm, open}: CancelPlanDialogPropsI) => (
    <AlertDialog
        cancelLabel="Keep plan"
        confirmLabel={isPending ? 'Cancelling…' : 'Cancel subscription'}
        description="Your subscription will be cancelled at the end of the current billing period. You will retain access until then and will not be charged again."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onConfirm}
        open={open}
        title="Cancel subscription?"
    />
);

export default CancelPlanDialog;
