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

const CustomComponentDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => {
    return (
        <AlertDialog
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onClose();
                }
            }}
            open={true}
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the custom component.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                    <DestructiveAlertDialogAction onClick={onDelete}>Delete</DestructiveAlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default CustomComponentDeleteAlertDialog;
