import AlertDialog from '@/components/AlertDialog';

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
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
        isPending={isPending}
        onCancel={onCancelClick}
        onConfirm={onDeleteClick}
        open
    />
);

export default IntegrationInstanceConfigurationListItemAlertDialog;
