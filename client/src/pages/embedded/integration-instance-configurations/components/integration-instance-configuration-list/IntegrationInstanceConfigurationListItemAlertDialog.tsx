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

interface IntegrationInstanceConfigurationListItemAlertDialogProps {
    onCancelClick: () => void;
    onDeleteClick: () => void;
}

const IntegrationInstanceConfigurationListItemAlertDialog = ({
    onCancelClick,
    onDeleteClick,
}: IntegrationInstanceConfigurationListItemAlertDialogProps) => {
    return (
        <AlertDialog open={true}>
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

                    <AlertDialogAction className="bg-destructive" onClick={onDeleteClick}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default IntegrationInstanceConfigurationListItemAlertDialog;
