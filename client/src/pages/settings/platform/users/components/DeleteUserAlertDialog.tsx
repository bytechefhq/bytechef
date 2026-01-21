import DeleteAlertDialog from '@/components/DeleteAlertDialog';

import useDeleteUserAlertDialog from './hooks/useDeleteUserAlertDialog';

const DeleteUserAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteUserAlertDialog();

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleDelete} open={open} />;
};

export default DeleteUserAlertDialog;
