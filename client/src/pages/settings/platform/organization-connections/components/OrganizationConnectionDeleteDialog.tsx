import AlertDialog from '@/components/AlertDialog';
import {OrganizationConnection} from '@/shared/middleware/graphql';

interface OrganizationConnectionDeleteDialogProps {
    connection: OrganizationConnection;
    onClose: () => void;
    onConfirm: (connectionId: string) => void;
}

const OrganizationConnectionDeleteDialog = ({
    connection,
    onClose,
    onConfirm,
}: OrganizationConnectionDeleteDialogProps) => (
    <AlertDialog
        description="This action cannot be undone."
        onCancel={onClose}
        onConfirm={() => onConfirm(connection.id)}
        open
        title={`Delete "${connection.name}" connection?`}
    />
);

export default OrganizationConnectionDeleteDialog;
