import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const CustomComponentDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the custom component."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default CustomComponentDeleteAlertDialog;
