import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import useDeleteDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useDeleteDataTableAlertDialog';

const DeleteDataTableAlertDialog = () => {
    const {handleClose, handleDelete, open} = useDeleteDataTableAlertDialog();

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleDelete} open={open} />;
};

export default DeleteDataTableAlertDialog;
