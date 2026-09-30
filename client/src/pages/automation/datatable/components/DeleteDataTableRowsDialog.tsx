import DeleteAlertDialog from '@/components/DeleteAlertDialog';

import useDeleteDataTableRowsDialog from '../hooks/useDeleteDataTableRowsDialog';

const DeleteDataTableRowsDialog = () => {
    const {handleClose, handleDelete, open, rowCount} = useDeleteDataTableRowsDialog();

    return (
        <DeleteAlertDialog
            description={`Are you sure you want to delete ${rowCount} selected record${rowCount === 1 ? '' : 's'}? This action cannot be undone.`}
            onCancel={handleClose}
            onDelete={handleDelete}
            open={open}
            title="Delete records"
        />
    );
};

export default DeleteDataTableRowsDialog;
