import AlertDialog from '@/components/AlertDialog';

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
        confirmLabel="Remove"
        description={`This action cannot be undone. This will permanently remove "${fileName}" from the skill.`}
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default AiSkillFileDeleteAlertDialog;
