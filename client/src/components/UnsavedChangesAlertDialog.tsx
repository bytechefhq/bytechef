import AlertDialog from '@/components/AlertDialog';

interface UnsavedChangesAlertDialogPropsI {
    onCancel: () => void;
    onClose: () => void;
    open: boolean;
}

const UnsavedChangesAlertDialog = ({onCancel, onClose, open}: UnsavedChangesAlertDialogPropsI) => (
    <AlertDialog
        cancelLabel="Keep editing"
        confirmButtonVariant="destructiveGhost"
        confirmClassName="opacity-100"
        confirmLabel="Close & discard"
        description="You have unsaved changes. Are you sure you want to discard them?"
        onCancel={onCancel}
        onConfirm={onClose}
        open={open}
        title="Discard code changes?"
    />
);

export default UnsavedChangesAlertDialog;
