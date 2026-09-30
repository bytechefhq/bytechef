import AlertDialog from '@/components/AlertDialog';

import useDeleteUserAlertDialog from './hooks/useDeleteUserAlertDialog';

const DeleteUserAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteUserAlertDialog();

    return <AlertDialog onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteUserAlertDialog;
