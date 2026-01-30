import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import useKnowledgeBaseDocumentListItemDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItemDeleteDialog';

interface KnowledgeBaseDocumentListItemDeleteDialogProps {
    documentId: string;
    knowledgeBaseId: string;
    onClose: () => void;
    open: boolean;
}

const KnowledgeBaseDocumentListItemDeleteDialog = ({
    documentId,
    knowledgeBaseId,
    onClose,
    open,
}: KnowledgeBaseDocumentListItemDeleteDialogProps) => {
    const {handleCancelClick, handleDeleteClick, isPending} = useKnowledgeBaseDocumentListItemDeleteDialog({
        documentId,
        knowledgeBaseId,
        onClose,
    });

    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the document and all its chunks.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel className="shadow-none" disabled={isPending} onClick={handleCancelClick}>
                        Cancel
                    </AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        disabled={isPending}
                        onClick={handleDeleteClick}
                    >
                        {isPending ? 'Deleting...' : 'Delete'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default KnowledgeBaseDocumentListItemDeleteDialog;
