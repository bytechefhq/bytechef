import AlertDialog from '@/components/AlertDialog';

interface DeleteDataSyncAlertDialogProps {
    dataSyncTitle: string;
    onClose: () => void;
    onDelete: () => void;
}

const DeleteDataSyncAlertDialog = ({dataSyncTitle, onClose, onDelete}: DeleteDataSyncAlertDialogProps) => (
    <AlertDialog
        ariaLabel="Confirm Data Sync Deletion"
        description={`This action cannot be undone. This will permanently delete the data sync ${dataSyncTitle}.`}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default DeleteDataSyncAlertDialog;
