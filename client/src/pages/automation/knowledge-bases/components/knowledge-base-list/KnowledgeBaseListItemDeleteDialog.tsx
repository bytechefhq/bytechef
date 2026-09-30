import AlertDialog from '@/components/AlertDialog';
import useKnowledgeBaseListItemDeleteDialog from '@/pages/automation/knowledge-bases/components/knowledge-base-list/hooks/useKnowledgeBaseListItemDeleteDialog';

interface KnowledgeBaseListItemDeleteDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
    open: boolean;
}

const KnowledgeBaseListItemDeleteDialog = ({
    knowledgeBaseId,
    onClose,
    open,
}: KnowledgeBaseListItemDeleteDialogProps) => {
    const {handleCancelClick, handleDeleteClick} = useKnowledgeBaseListItemDeleteDialog({
        knowledgeBaseId,
        onClose,
    });

    return (
        <AlertDialog
            description="This action cannot be undone. This will permanently delete the knowledge base and all documents it contains."
            onCancel={handleCancelClick}
            onConfirm={handleDeleteClick}
            open={open}
        />
    );
};

export default KnowledgeBaseListItemDeleteDialog;
