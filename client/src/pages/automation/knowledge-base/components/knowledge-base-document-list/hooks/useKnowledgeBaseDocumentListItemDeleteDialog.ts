import {useKnowledgeBaseDocumentDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentDeleteDialogStore';
import {useDeleteKnowledgeBaseDocumentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentListItemDeleteDialogProps {
    knowledgeBaseId: string;
}

export default function useKnowledgeBaseDocumentListItemDeleteDialog({
    knowledgeBaseId,
}: UseKnowledgeBaseDocumentListItemDeleteDialogProps) {
    const {clearDialog, documentId, setDocumentId} = useKnowledgeBaseDocumentDeleteDialogStore(
        useShallow((state) => ({
            clearDialog: state.clearDialog,
            documentId: state.documentId,
            setDocumentId: state.setDocumentId,
        }))
    );
    const queryClient = useQueryClient();

    const deleteMutation = useDeleteKnowledgeBaseDocumentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast('Document deleted successfully.');

            clearDialog();
        },
    });

    const handleConfirm = () => {
        if (!documentId) {
            return;
        }

        deleteMutation.mutate({id: documentId});
    };

    return {
        handleClose: clearDialog,
        handleConfirm,
        handleOpen: setDocumentId,
        handleOpenChange: (open: boolean) => {
            if (!open) {
                clearDialog();
            }
        },
        isPending: deleteMutation.isPending,
        open: documentId !== null,
    };
}
