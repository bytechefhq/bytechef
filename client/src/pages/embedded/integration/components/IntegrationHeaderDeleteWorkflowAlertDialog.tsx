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

const IntegrationHeaderDeleteWorkflowAlertDialog = ({
    onClose,
    onDelete,
}: {
    onClose: () => void;
    onDelete: () => void;
}) => (
    <AlertDialog open={true}>
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the workflow.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => onClose}>Cancel</AlertDialogCancel>

                <AlertDialogAction className="bg-red-600" onClick={() => onDelete()}>
                    Delete
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default IntegrationHeaderDeleteWorkflowAlertDialog;
