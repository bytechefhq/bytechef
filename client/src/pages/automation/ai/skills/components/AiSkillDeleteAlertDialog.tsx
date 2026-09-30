import AlertDialog from '@/components/AlertDialog';

interface AiSkillDeleteAlertDialogProps {
    isPending?: boolean;
    onClose: () => void;
    onDelete: () => void;
}

const AiSkillDeleteAlertDialog = ({isPending, onClose, onDelete}: AiSkillDeleteAlertDialogProps) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the skill."
        isPending={isPending}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default AiSkillDeleteAlertDialog;
