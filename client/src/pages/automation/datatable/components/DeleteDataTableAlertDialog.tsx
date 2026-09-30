import AlertDialog from '@/components/AlertDialog';

import useDeleteDataTableAlertDialog from '../hooks/useDeleteDataTableAlertDialog';

const DeleteDataTableAlertDialog = () => {
    const {handleClose, handleDelete, isPending, open} = useDeleteDataTableAlertDialog();

    return <AlertDialog isPending={isPending} onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteDataTableAlertDialog;
