import AlertDialog from '@/components/AlertDialog';

const ApiConnectorDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the API connector."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default ApiConnectorDeleteAlertDialog;
