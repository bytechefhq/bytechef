import AlertDialog from '@/components/AlertDialog';

const AiSkillDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog
        description="This action cannot be undone. This will permanently delete the skill."
        onCancel={onClose}
        onConfirm={onDelete}
        open
    />
);

export default AiSkillDeleteAlertDialog;
