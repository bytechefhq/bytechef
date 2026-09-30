import AlertDialog from '@/components/AlertDialog';

import useDeleteDataTableAlertDialog from '../hooks/useDeleteDataTableAlertDialog';

const DeleteDataTableAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteDataTableAlertDialog();

    return <AlertDialog onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteDataTableAlertDialog;
