import DestructiveAlertDialogAction from '@/components/DestructiveAlertDialogAction';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';

const DeleteIntegrationAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        onOpenChange={(isOpen) => {
            if (!isOpen) {
                onClose();
            }
        }}
        open
    >
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the integration and workflows it
                    contains.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                <DestructiveAlertDialogAction aria-label="Confirm Integration Deletion" onClick={() => onDelete()}>
                    Delete
                </DestructiveAlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default DeleteIntegrationAlertDialog;
