import DeleteAlertDialog from '@/components/DeleteAlertDialog';

import useDeleteDataTableAlertDialog from '../hooks/useDeleteDataTableAlertDialog';

const DeleteDataTableAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteDataTableAlertDialog();

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleDelete} open={open} />;
};

export default DeleteDataTableAlertDialog;
