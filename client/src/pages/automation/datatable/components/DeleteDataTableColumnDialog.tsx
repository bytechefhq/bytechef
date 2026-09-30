import AlertDialog from '@/components/AlertDialog';

import useDeleteDataTableColumnDialog from '../hooks/useDeleteDataTableColumnDialog';

const DeleteDataTableColumnDialog = () => {
    const {columnName, handleClose, handleDelete, isPending, open} = useDeleteDataTableColumnDialog();

    return (
        <AlertDialog
            description={`Are you sure you want to delete column "${columnName}"? This action cannot be undone and will remove all data in this column.`}
            isPending={isPending}
            onCancel={handleClose}
            onConfirm={handleDelete}
            open={open}
            title="Delete column"
        />
    );
};

export default DeleteDataTableColumnDialog;
