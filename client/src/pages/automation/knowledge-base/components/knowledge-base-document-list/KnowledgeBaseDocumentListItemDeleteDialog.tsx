import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import useKnowledgeBaseDocumentListItemDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItemDeleteDialog';

interface KnowledgeBaseDocumentListItemDeleteDialogProps {
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentListItemDeleteDialog = ({
    knowledgeBaseId,
}: KnowledgeBaseDocumentListItemDeleteDialogProps) => {
    const {handleClose, handleConfirm, open} = useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId});

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleConfirm} open={open} />;
};

export default KnowledgeBaseDocumentListItemDeleteDialog;
