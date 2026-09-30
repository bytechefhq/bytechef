import AlertDialog from '@/components/AlertDialog';

import useDeleteUserAlertDialog from './hooks/useDeleteUserAlertDialog';

const DeleteUserAlertDialog = () => {
    const {handleClose, handleDelete, isPending, open} = useDeleteUserAlertDialog();

    return <AlertDialog isPending={isPending} onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteUserAlertDialog;
