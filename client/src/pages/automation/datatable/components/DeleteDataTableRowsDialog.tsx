import AlertDialog from '@/components/AlertDialog';

import useDeleteDataTableRowsDialog from '../hooks/useDeleteDataTableRowsDialog';

const DeleteDataTableRowsDialog = () => {
    const {handleClose, handleDelete, isPending, open, rowCount} = useDeleteDataTableRowsDialog();

    return (
        <AlertDialog
            description={`Are you sure you want to delete ${rowCount} selected record${rowCount === 1 ? '' : 's'}? This action cannot be undone.`}
            isPending={isPending}
            onCancel={handleClose}
            onConfirm={handleDelete}
            open={open}
            title="Delete records"
        />
    );
};

export default DeleteDataTableRowsDialog;
