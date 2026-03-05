import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface UnsavedChangesAlertDialogPropsI {
    onCancel: () => void;
    onClose: () => void;
    open: boolean;
}

const UnsavedChangesAlertDialog = ({onCancel, onClose, open}: UnsavedChangesAlertDialogPropsI) => {
    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Discard code changes?</AlertDialogTitle>

                    <AlertDialogDescription>
                        You have unsaved changes. Are you sure you want to discard them?
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <Button label="Keep editing" onClick={onCancel} variant="outline" />

                    <Button
                        className="opacity-100"
                        label="Close & discard"
                        onClick={onClose}
                        variant="destructiveGhost"
                    />
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default UnsavedChangesAlertDialog;
