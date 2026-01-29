import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import useKnowledgeBaseDocumentChunkDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkDeleteDialog';

interface KnowledgeBaseDocumentChunkDeleteDialogProps {
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentChunkDeleteDialog = ({knowledgeBaseId}: KnowledgeBaseDocumentChunkDeleteDialogProps) => {
    const {handleClose, handleConfirm, open} = useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId});

    return <DeleteAlertDialog onCancel={handleClose} onDelete={handleConfirm} open={open} />;
};

export default KnowledgeBaseDocumentChunkDeleteDialog;
