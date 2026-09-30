import DeleteAlertDialog from '@/components/DeleteAlertDialog';

import useDeleteDataTableColumnDialog from '../hooks/useDeleteDataTableColumnDialog';

const DeleteDataTableColumnDialog = () => {
    const {columnName, handleClose, handleDelete, open} = useDeleteDataTableColumnDialog();

    return (
        <DeleteAlertDialog
            description={`Are you sure you want to delete column "${columnName}"? This action cannot be undone and will remove all data in this column.`}
            onCancel={handleClose}
            onDelete={handleDelete}
            open={open}
            title="Delete column"
        />
    );
};

export default DeleteDataTableColumnDialog;
