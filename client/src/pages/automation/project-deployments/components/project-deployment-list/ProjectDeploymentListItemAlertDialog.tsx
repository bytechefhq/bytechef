import DeleteAlertDialog from '@/components/DeleteAlertDialog';

interface ProjectDeploymentListItemAlertDialogProps {
    onCancelClick: () => void;
    onDeleteClick: () => void;
    isPending?: boolean;
}

const ProjectDeploymentListItemAlertDialog = ({
    isPending,
    onCancelClick,
    onDeleteClick,
}: ProjectDeploymentListItemAlertDialogProps) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the project and workflows it contains."
        isPending={isPending}
        onCancel={onCancelClick}
        onDelete={onDeleteClick}
        open
    />
);

export default ProjectDeploymentListItemAlertDialog;
