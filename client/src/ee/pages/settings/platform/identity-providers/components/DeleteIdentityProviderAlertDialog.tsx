import AlertDialog from '@/components/AlertDialog';

import useDeleteIdentityProviderAlertDialog from './hooks/useDeleteIdentityProviderAlertDialog';

const DeleteIdentityProviderAlertDialog = () => {
    const {handleClose, handleDelete, isPending, open} = useDeleteIdentityProviderAlertDialog();

    return <AlertDialog isPending={isPending} onCancel={handleClose} onConfirm={handleDelete} open={open} />;
};

export default DeleteIdentityProviderAlertDialog;
