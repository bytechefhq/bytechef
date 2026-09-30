import DestructiveAlertDialogAction from '@/components/DestructiveAlertDialogAction';
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
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
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    handleCancelClick();
                }
            }}
            open={open}
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the knowledge base and all documents
                        it contains.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel className="shadow-none" onClick={handleCancelClick}>
                        Cancel
                    </AlertDialogCancel>

                    <DestructiveAlertDialogAction onClick={handleDeleteClick}>Delete</DestructiveAlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default KnowledgeBaseListItemDeleteDialog;
