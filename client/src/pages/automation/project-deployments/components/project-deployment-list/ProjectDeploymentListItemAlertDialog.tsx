import AlertDialog from '@/components/AlertDialog';

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
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the project and workflows it contains."
        isPending={isPending}
        onCancel={onCancelClick}
        onConfirm={onDeleteClick}
        open
    />
);

export default ProjectDeploymentListItemAlertDialog;
