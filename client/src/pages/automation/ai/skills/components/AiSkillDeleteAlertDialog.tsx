import DeleteAlertDialog from '@/components/DeleteAlertDialog';

const AiSkillDeleteAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <DeleteAlertDialog
        description="This action cannot be undone. This will permanently delete the skill."
        onCancel={onClose}
        onDelete={onDelete}
        open
    />
);

export default AiSkillDeleteAlertDialog;
