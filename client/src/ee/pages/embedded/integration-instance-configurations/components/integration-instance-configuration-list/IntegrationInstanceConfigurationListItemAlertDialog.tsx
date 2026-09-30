import DestructiveAlertDialogAction from '@/components/DestructiveAlertDialogAction';
import LoadingIcon from '@/components/LoadingIcon';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface IntegrationInstanceConfigurationListItemAlertDialogProps {
    onCancelClick: () => void;
    onDeleteClick: () => void;
    isPending?: boolean;
}

const IntegrationInstanceConfigurationListItemAlertDialog = ({
    isPending,
    onCancelClick,
    onDeleteClick,
}: IntegrationInstanceConfigurationListItemAlertDialogProps) => {
    return (
        <AlertDialog
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onCancelClick();
                }
            }}
            open={true}
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
                    <AlertDialogCancel onClick={onCancelClick}>Cancel</AlertDialogCancel>

                    <DestructiveAlertDialogAction disabled={isPending} onClick={onDeleteClick}>
                        {isPending && <LoadingIcon />}
                        Delete
                    </DestructiveAlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default IntegrationInstanceConfigurationListItemAlertDialog;
