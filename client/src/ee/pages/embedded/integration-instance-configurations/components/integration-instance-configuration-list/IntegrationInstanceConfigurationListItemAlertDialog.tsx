import DeleteAlertDialog from '@/components/DeleteAlertDialog';

interface IntegrationInstanceConfigurationListItemAlertDialogProps {
    onCancelClick: () => void;
    onDeleteClick: () => void;
    isPending?: boolean;
}

const IntegrationInstanceConfigurationListItemAlertDialog = ({
    isPending,
    onCancelClick,
    onDeleteClick,
}: IntegrationInstanceConfigurationListItemAlertDialogProps) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
        isPending={isPending}
        onCancel={onCancelClick}
        onDelete={onDeleteClick}
        open
    />
);

export default IntegrationInstanceConfigurationListItemAlertDialog;
