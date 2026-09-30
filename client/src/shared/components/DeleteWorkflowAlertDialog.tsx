import Button from '@/components/Button/Button';
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
import {XIcon} from 'lucide-react';
import {useRef} from 'react';

interface DeleteWorkflowAlertDialogProps {
    onClose: () => void;
    onDelete: () => void;
}

const DeleteWorkflowAlertDialog = ({onClose, onDelete}: DeleteWorkflowAlertDialogProps) => {
    const deleteButtonRef = useRef<HTMLButtonElement>(null);

    const handleOpenAutoFocus = (event: Event) => {
        event.preventDefault();

        const deleteButton = deleteButtonRef.current;

        deleteButton?.focus();
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            onClose();
        }
    };

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={true}>
            <AlertDialogContent onEscapeKeyDown={onClose} onOpenAutoFocus={handleOpenAutoFocus}>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the workflow.
                    </AlertDialogDescription>

                    <Button
                        aria-label="Close"
                        className="absolute top-4 right-4"
                        icon={<XIcon />}
                        onClick={onClose}
                        size="icon"
                        variant="ghost"
                    />
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                    <DestructiveAlertDialogAction onClick={() => onDelete()} ref={deleteButtonRef}>
                        Delete
                    </DestructiveAlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DeleteWorkflowAlertDialog;
