import AlertDialog from '@/components/AlertDialog';
import useKnowledgeBaseDeleteAlertDialog from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseDeleteAlertDialog';

interface KnowledgeBaseDeleteAlertDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
    open: boolean;
}

const KnowledgeBaseDeleteAlertDialog = ({knowledgeBaseId, onClose, open}: KnowledgeBaseDeleteAlertDialogProps) => {
    const {handleCancelClick, handleDeleteClick} = useKnowledgeBaseDeleteAlertDialog({
        knowledgeBaseId,
        onClose,
    });

    return <AlertDialog onCancel={handleCancelClick} onConfirm={handleDeleteClick} open={open} />;
};

export default KnowledgeBaseDeleteAlertDialog;
