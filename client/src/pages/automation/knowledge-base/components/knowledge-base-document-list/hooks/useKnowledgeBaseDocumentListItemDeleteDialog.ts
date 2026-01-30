import {useToast} from '@/hooks/use-toast';
import {useDeleteKnowledgeBaseDocumentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseKnowledgeBaseDocumentListItemDeleteDialogProps {
    documentId: string;
    knowledgeBaseId: string;
    onClose: () => void;
}

export default function useKnowledgeBaseDocumentListItemDeleteDialog({
    documentId,
    knowledgeBaseId,
    onClose,
}: UseKnowledgeBaseDocumentListItemDeleteDialogProps) {
    const queryClient = useQueryClient();
    const {toast} = useToast();

    const deleteMutation = useDeleteKnowledgeBaseDocumentMutation({
        onError: () => {
            toast({description: 'Failed to delete document. Please try again.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast({description: 'Document deleted successfully.'});

            onClose();
        },
    });

    const handleDeleteClick = () => {
        deleteMutation.mutate({id: documentId});
    };

    const handleCancelClick = () => {
        onClose();
    };

    return {
        handleCancelClick,
        handleDeleteClick,
        isPending: deleteMutation.isPending,
    };
}
