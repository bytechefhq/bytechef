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

const AiSkillFileDeleteAlertDialog = ({
    fileName,
    onClose,
    onDelete,
}: {
    fileName: string;
    onClose: () => void;
    onDelete: () => void;
}) => (
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
                    This action cannot be undone. This will permanently remove &quot;{fileName}&quot; from the skill.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                <DestructiveAlertDialogAction onClick={onDelete}>Remove</DestructiveAlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

export default AiSkillFileDeleteAlertDialog;
