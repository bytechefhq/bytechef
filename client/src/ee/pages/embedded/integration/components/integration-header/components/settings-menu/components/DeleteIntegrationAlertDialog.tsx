import LoadingIcon from '@/components/LoadingIcon';
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

const DeleteIntegrationAlertDialog = ({
    isPending,
    onClose,
    onDelete,
}: {
    isPending?: boolean;
    onClose: () => void;
    onDelete: () => void;
}) => (
    <AlertDialog open>
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the integration and workflows it
                    contains.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel disabled={isPending} onClick={() => onClose()}>
                    Cancel
                </AlertDialogCancel>

                <AlertDialogAction
                    aria-label="Confirm Integration Deletion"
                    className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                    disabled={isPending}
                    onClick={() => onDelete()}
                >
                    {isPending && <LoadingIcon />}
                    Delete
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default DeleteIntegrationAlertDialog;
