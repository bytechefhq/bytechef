import AlertDialog from '@/components/AlertDialog';

const CustomComponentDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the custom component."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default CustomComponentDeleteAlertDialog;
