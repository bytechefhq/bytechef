import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {forwardRef, useImperativeHandle} from 'react';

import useDeleteUserAlertDialog from './hooks/useDeleteUserAlertDialog';

export interface DeleteUserAlertDialogRefI {
    open: (login: string | null) => void;
}

const DeleteUserAlertDialog = forwardRef<DeleteUserAlertDialogRefI>(function DeleteUserAlertDialog(_, ref) {
    const {handleClose, handleDelete, handleOpen, open} = useDeleteUserAlertDialog();

    useImperativeHandle(ref, () => ({
        open: handleOpen,
    }));

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleDelete} open={open} />;
});

export default DeleteUserAlertDialog;
