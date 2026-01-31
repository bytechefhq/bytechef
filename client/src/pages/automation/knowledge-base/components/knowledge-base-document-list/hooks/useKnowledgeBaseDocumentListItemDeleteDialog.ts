import {useToast} from '@/hooks/use-toast';
import {useKnowledgeBaseDocumentDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentDeleteDialogStore';
import {useDeleteKnowledgeBaseDocumentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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

    const {toast} = useToast();
    const queryClient = useQueryClient();

    const deleteMutation = useDeleteKnowledgeBaseDocumentMutation({
        onError: () => {
            toast({description: 'Failed to delete document. Please try again.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast({description: 'Document deleted successfully.'});

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
