import DeleteAlertDialog from '@/components/DeleteAlertDialog';

import useDeleteIdentityProviderAlertDialog from './hooks/useDeleteIdentityProviderAlertDialog';

const DeleteIdentityProviderAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteIdentityProviderAlertDialog();

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleDelete} open={open} />;
};

export default DeleteIdentityProviderAlertDialog;
