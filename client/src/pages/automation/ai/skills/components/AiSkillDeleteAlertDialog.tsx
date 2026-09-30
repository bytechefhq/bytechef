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

const AiSkillDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        onOpenChange={(open) => {
            if (!open) {
                onClose();
            }
        }}
        open
    >
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the skill.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                <DestructiveAlertDialogAction onClick={onDelete}>Delete</DestructiveAlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default AiSkillDeleteAlertDialog;
