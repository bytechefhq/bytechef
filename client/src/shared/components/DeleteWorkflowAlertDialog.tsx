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
import {useRef} from 'react';

const DeleteWorkflowAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => {
    const deleteButtonRef = useRef<HTMLButtonElement>(null);

    return (
        <AlertDialog open={true}>
            <AlertDialogContent
                onOpenAutoFocus={(event) => {
                    event.preventDefault();
                    deleteButtonRef.current?.focus();
                }}
            >
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the workflow.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        onClick={() => onDelete()}
                        ref={deleteButtonRef}
                    >
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteWorkflowAlertDialog;
