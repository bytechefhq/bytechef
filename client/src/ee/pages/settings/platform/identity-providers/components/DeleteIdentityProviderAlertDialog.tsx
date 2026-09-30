import AlertDialog from '@/components/AlertDialog';

import useDeleteIdentityProviderAlertDialog from './hooks/useDeleteIdentityProviderAlertDialog';

const DeleteIdentityProviderAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteIdentityProviderAlertDialog();

    return <AlertDialog onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteIdentityProviderAlertDialog;
