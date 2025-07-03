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

interface McpProjectListItemAlertDialogProps {
    onCancelClick: () => void;
    onDeleteClick: () => void;
    isPending?: boolean;
}

const McpProjectListItemAlertDialog = ({
    isPending,
    onCancelClick,
    onDeleteClick,
}: McpProjectListItemAlertDialogProps) => {
    return (
        <AlertDialog open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the MCP project and all its
                        workflows.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onCancelClick}>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-destructive" disabled={isPending} onClick={onDeleteClick}>
                        {isPending && <LoadingIcon />}
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default McpProjectListItemAlertDialog;
