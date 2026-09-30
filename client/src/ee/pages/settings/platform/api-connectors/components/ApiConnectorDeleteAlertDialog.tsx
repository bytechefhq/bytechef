import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const ApiConnectorDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the API connector."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default ApiConnectorDeleteAlertDialog;
