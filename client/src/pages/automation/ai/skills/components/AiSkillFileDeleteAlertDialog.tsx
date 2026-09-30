import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const AiSkillFileDeleteAlertDialog = ({
    fileName,
    onClose,
    onDelete,
}: {
    fileName: string;
    onClose: () => void;
    onDelete: () => void;
}) => (
    <DeleteAlertDialog
        confirmLabel="Remove"
        description={`This action cannot be undone. This will permanently remove "${fileName}" from the skill.`}
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default AiSkillFileDeleteAlertDialog;
