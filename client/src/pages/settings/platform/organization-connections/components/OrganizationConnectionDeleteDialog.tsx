import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {OrganizationConnection} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';

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
    <AlertDialog open>
        <AlertDialogContent onEscapeKeyDown={onClose}>
            <AlertDialogHeader>
                <AlertDialogTitle>{`Delete "${connection.name}" connection?`}</AlertDialogTitle>

                <AlertDialogDescription>This action cannot be undone.</AlertDialogDescription>

                <Button
                    aria-label="Close"
                    className="absolute right-2 top-0"
                    icon={<XIcon />}
                    onClick={onClose}
                    size="icon"
                    variant="ghost"
                />
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                <AlertDialogAction
                    className="bg-surface-destructive-primary hover:bg-surface-destructive-primary-hover"
                    onClick={() => onConfirm(connection.id)}
                >
                    Delete
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default OrganizationConnectionDeleteDialog;
