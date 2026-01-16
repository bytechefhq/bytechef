import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {forwardRef, useImperativeHandle} from 'react';

import useDeleteUserAlertDialog from './hooks/useDeleteUserAlertDialog';

export interface DeleteUserAlertDialogRefI {
    open: (login: string | null) => void;
}

const DeleteUserAlertDialog = forwardRef<DeleteUserAlertDialogRefI>(function DeleteUserAlertDialog(_, ref) {
    const {handleDeleteUserAlertDialogClose, handleDeleteUserAlertDialogDelete, handleDeleteUserAlertDialogOpen, open} =
        useDeleteUserAlertDialog();

    useImperativeHandle(ref, () => ({
        open: handleDeleteUserAlertDialogOpen,
    }));

    return (
        <DeleteAlertDialog
            onCancel={handleDeleteUserAlertDialogClose}
            onDelete={handleDeleteUserAlertDialogDelete}
            open={open}
        />
    );
});

export default DeleteUserAlertDialog;
